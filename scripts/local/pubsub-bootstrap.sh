#!/bin/sh
set -eu

PROJECT_ID="${PROJECT_ID:-looksee-local}"
EMULATOR_HOST="${PUBSUB_EMULATOR_HOST:-pubsub-emulator:8085}"
API_URL="http://${EMULATOR_HOST}"

TOPICS="url-topic page-audit-topic audit-update-topic audit-error-topic page-created-topic journey-verified-topic journey-discarded-topic journey-candidate-topic element-extraction-topic journey-map-cleanup-topic"

echo "[pubsub-bootstrap] waiting for emulator at ${API_URL}"
i=0
while [ $i -lt 60 ]; do
  if curl -sf "${API_URL}/" >/dev/null 2>&1; then
    break
  fi
  i=$((i+1))
  sleep 1
done

for t in $TOPICS; do
  echo "[pubsub-bootstrap] create topic: $t"
  curl -s -X PUT \
    "${API_URL}/v1/projects/${PROJECT_ID}/topics/${t}" \
    -H 'Content-Type: application/json' \
    -d '{}' >/dev/null || true

  sub="${t}-sub"
  echo "[pubsub-bootstrap] create subscription: $sub -> $t"
  curl -s -X PUT \
    "${API_URL}/v1/projects/${PROJECT_ID}/subscriptions/${sub}" \
    -H 'Content-Type: application/json' \
    -d "{\"topic\":\"projects/${PROJECT_ID}/topics/${t}\",\"ackDeadlineSeconds\":30}" >/dev/null || true
done

echo "[pubsub-bootstrap] done. Topics:"
curl -s "${API_URL}/v1/projects/${PROJECT_ID}/topics" || true
echo
