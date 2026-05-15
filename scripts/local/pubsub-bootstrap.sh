#!/bin/sh
# Bootstrap topics + subscriptions against the GCP Pub/Sub emulator for the
# local docker-compose stack. Mirrors the topic -> service push wiring from
# the production Terraform (LookseeIaC/GCP/modules.tf) so the local stack
# actually delivers messages between services.
#
# Fails loudly (non-zero exit) on any HTTP error so the dependent Java
# services don't start with a half-bootstrapped messaging layer.
set -eu

PROJECT_ID="${PROJECT_ID:-looksee-local}"
EMULATOR_HOST="${PUBSUB_EMULATOR_HOST:-pubsub-emulator:8085}"
API_URL="http://${EMULATOR_HOST}"

# Topic names (overridable from the compose environment so they stay in
# lock-step with what the Java services read from local-overrides.properties).
URL_TOPIC="${URL_TOPIC:-url-topic}"
PAGE_AUDIT_TOPIC="${PAGE_AUDIT_TOPIC:-page-audit-topic}"
AUDIT_UPDATE_TOPIC="${AUDIT_UPDATE_TOPIC:-audit-update-topic}"
ERROR_TOPIC="${ERROR_TOPIC:-audit-error-topic}"
PAGE_CREATED_TOPIC="${PAGE_CREATED_TOPIC:-page-created-topic}"
JOURNEY_VERIFIED_TOPIC="${JOURNEY_VERIFIED_TOPIC:-journey-verified-topic}"
JOURNEY_DISCARDED_TOPIC="${JOURNEY_DISCARDED_TOPIC:-journey-discarded-topic}"
JOURNEY_CANDIDATE_TOPIC="${JOURNEY_CANDIDATE_TOPIC:-journey-candidate-topic}"
ELEMENT_EXTRACTION_TOPIC="${ELEMENT_EXTRACTION_TOPIC:-element-extraction-topic}"
JOURNEY_MAP_CLEANUP_TOPIC="${JOURNEY_MAP_CLEANUP_TOPIC:-journey-map-cleanup-topic}"

echo "[pubsub-bootstrap] waiting for emulator at ${API_URL}"
i=0
while [ $i -lt 60 ]; do
  if curl -sf "${API_URL}/" >/dev/null 2>&1; then
    break
  fi
  i=$((i+1))
  sleep 1
done

# `code` global is set by the helpers below so the caller can decide
# whether a non-2xx response is fatal.
http_put() {
  url="$1"
  body="$2"
  resp_code=$(curl -s -o /tmp/resp.out -w "%{http_code}" -X PUT \
    -H 'Content-Type: application/json' --data "${body}" "${url}")
  code="${resp_code}"
  if [ "${code}" -ge 400 ] && [ "${code}" -ne 409 ]; then
    echo "[pubsub-bootstrap] FAIL ${code} on PUT ${url}" >&2
    cat /tmp/resp.out >&2 || true
    echo >&2
    return 1
  fi
  return 0
}

create_topic() {
  topic="$1"
  echo "[pubsub-bootstrap] topic: ${topic}"
  http_put "${API_URL}/v1/projects/${PROJECT_ID}/topics/${topic}" "{}"
}

# Push subscription -> HTTP endpoint inside the docker network.
create_push_sub() {
  topic="$1"
  sub="$2"
  endpoint="$3"
  echo "[pubsub-bootstrap] push sub: ${sub} (${topic} -> ${endpoint})"
  body=$(printf '{"topic":"projects/%s/topics/%s","ackDeadlineSeconds":60,"pushConfig":{"pushEndpoint":"%s"}}' \
    "${PROJECT_ID}" "${topic}" "${endpoint}")
  http_put "${API_URL}/v1/projects/${PROJECT_ID}/subscriptions/${sub}" "${body}"
}

# Pull subscription (used for topics that have no canonical single-service
# consumer in production - they still need to exist so publishers succeed).
create_pull_sub() {
  topic="$1"
  sub="$2"
  echo "[pubsub-bootstrap] pull sub: ${sub} (${topic})"
  body=$(printf '{"topic":"projects/%s/topics/%s","ackDeadlineSeconds":30}' \
    "${PROJECT_ID}" "${topic}")
  http_put "${API_URL}/v1/projects/${PROJECT_ID}/subscriptions/${sub}" "${body}"
}

# ---- topics ----
create_topic "${URL_TOPIC}"
create_topic "${PAGE_AUDIT_TOPIC}"
create_topic "${AUDIT_UPDATE_TOPIC}"
create_topic "${ERROR_TOPIC}"
create_topic "${PAGE_CREATED_TOPIC}"
create_topic "${JOURNEY_VERIFIED_TOPIC}"
create_topic "${JOURNEY_DISCARDED_TOPIC}"
create_topic "${JOURNEY_CANDIDATE_TOPIC}"
create_topic "${ELEMENT_EXTRACTION_TOPIC}"
create_topic "${JOURNEY_MAP_CLEANUP_TOPIC}"

# ---- push subscriptions: mirror the production Cloud Run wiring ----
# url-topic -> pagebuilder
create_push_sub "${URL_TOPIC}" "page-builder-subscription" "http://pagebuilder:8080/"
# page-created-topic -> auditmanager
create_push_sub "${PAGE_CREATED_TOPIC}" "audit-manager-subscription" "http://auditmanager:8080/"
# page-audit-topic fans out to the three audit workers that actually
# consume PageAuditMessage payloads. audit-service intentionally does NOT
# subscribe here - its controller only handles progress/update message
# types (AuditProgressUpdate, PageAuditProgressMessage, JourneyCandidate*,
# *VerifiedJourney*), so it gets its own subscription on audit-update-topic.
create_push_sub "${PAGE_AUDIT_TOPIC}" "content-audit-subscription" "http://contentaudit:8080/"
create_push_sub "${PAGE_AUDIT_TOPIC}" "visual-design-audit-subscription" "http://visualdesignaudit:8080/"
create_push_sub "${PAGE_AUDIT_TOPIC}" "info-arch-audit-subscription" "http://informationarchitectureaudit:8080/"
# journey-candidate-topic -> journeyexecutor
create_push_sub "${JOURNEY_CANDIDATE_TOPIC}" "journey-executor-subscription" "http://journeyexecutor:8080/"
# journey-verified-topic -> journeyexpander
create_push_sub "${JOURNEY_VERIFIED_TOPIC}" "journey-expander-subscription" "http://journeyexpander:8080/"
# audit-update-topic fans out to:
#   - broadcaster (turns updates into Pusher real-time events for the UI)
#   - audit-service (persists progress + journey-status changes to Neo4j)
create_push_sub "${AUDIT_UPDATE_TOPIC}" "broadcaster-subscription" "http://broadcaster:8080/"
create_push_sub "${AUDIT_UPDATE_TOPIC}" "audit-service-subscription" "http://auditservice:8080/"
# journey-map-cleanup-topic -> journey-map-cleanup
create_push_sub "${JOURNEY_MAP_CLEANUP_TOPIC}" "journey-map-cleanup-subscription" "http://journeymapcleanup:8080/"

# Topics without a canonical single-service push consumer in production get
# pull subscriptions so the publisher side still has a valid target.
# `audit-error-topic` carries `PageDataExtractionError` payloads which the
# `journeyErrors` push controller (typed as `JourneyCandidateMessage`)
# cannot deserialize - it's a monitoring/DLQ topic in Terraform, not a
# service-consumed one. Wire a real consumer here if/when that changes.
create_pull_sub "${ERROR_TOPIC}" "${ERROR_TOPIC}-pull"
create_pull_sub "${JOURNEY_DISCARDED_TOPIC}" "${JOURNEY_DISCARDED_TOPIC}-pull"
create_pull_sub "${ELEMENT_EXTRACTION_TOPIC}" "${ELEMENT_EXTRACTION_TOPIC}-pull"

echo "[pubsub-bootstrap] done. Topics:"
curl -sf "${API_URL}/v1/projects/${PROJECT_ID}/topics"
echo
