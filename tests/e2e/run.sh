#!/usr/bin/env bash
# End-to-end smoke test for the Looksee Pub/Sub pipeline infrastructure.
#
# Wave 5.1 of the architecture review
# (/root/.claude/plans/gleaming-jumping-thunder.md).
#
# SCOPE: this driver intentionally does NOT boot PageBuilder / AuditManager /
# contentAudit as subprocesses. Booting those services in CI requires
# Selenium/Chromium, full Neo4j schema migration, Spring Cloud GCP emulator
# wiring, and per-service env-var plumbing — none of which exists in-repo
# today. Instead this test validates the shared infrastructure those
# services depend on, so that a regression in any of the layers below
# surfaces as a red CI check on the PR that caused it:
#
#   1.  The Pub/Sub emulator is reachable and healthy.
#   2.  Every canonical topic the pipeline relies on has been provisioned.
#   3.  A Pub/Sub Body envelope (the `com.looksee.mapper.Body` DTO that
#       every service consumes) round-trips through the emulator with its
#       Base64-encoded data AND its W3C trace-context attributes intact.
#       This is the Wave 2.2 contract: without it, distributed traces
#       cannot stitch across services.
#   4.  Neo4j is reachable over the HTTP transactional endpoint and
#       responds to a trivial smoke query.
#
# When a real multi-service integration test exists it can be layered on
# top of the same scaffolding in .github/workflows/e2e-pipeline.yml by
# extending this script or replacing the body of main().
set -euo pipefail

PROJECT="${PUBSUB_PROJECT_ID:-looksee-test}"
EMU="${PUBSUB_EMULATOR_HOST:-localhost:8085}"
EMU_URL="http://${EMU}"
NEO4J_HTTP="${NEO4J_HTTP_URL:-http://localhost:7474}"
NEO4J_USER="${SPRING_DATA_NEO4J_USERNAME:-neo4j}"
NEO4J_PASSWORD="${SPRING_DATA_NEO4J_PASSWORD:-looksee-test-password}"
NEO4J_DATABASE="${SPRING_DATA_NEO4J_DATABASE:-neo4j}"

# Traceparent used to prove Wave 2.2 context propagation survives the
# emulator. Valid W3C format: 00-<trace-id>-<span-id>-<flags>.
TRACEPARENT="00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01"
TRACESTATE="looksee=smoke-test"

TEST_SUBSCRIPTION="url-e2e-smoke-$$"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

log() { printf '[e2e] %s\n' "$*"; }
fail() { printf '[e2e] FAIL: %s\n' "$*" >&2; exit 1; }

require_tool() {
    command -v "$1" >/dev/null 2>&1 || fail "required tool '$1' is not on PATH"
}

require_tools() {
    require_tool curl
    require_tool jq
    require_tool base64
    require_tool python3
}

wait_for_emulator() {
    log "waiting for Pub/Sub emulator at ${EMU_URL}"
    for _ in $(seq 1 30); do
        if curl -fsS "${EMU_URL}" >/dev/null 2>&1; then
            log "emulator is reachable"
            return 0
        fi
        sleep 1
    done
    fail "Pub/Sub emulator never became reachable at ${EMU_URL}"
}

wait_for_neo4j() {
    log "waiting for Neo4j HTTP at ${NEO4J_HTTP}"
    for _ in $(seq 1 30); do
        if curl -fsS "${NEO4J_HTTP}" >/dev/null 2>&1; then
            log "Neo4j is reachable"
            return 0
        fi
        sleep 1
    done
    fail "Neo4j HTTP never became reachable at ${NEO4J_HTTP}"
}

verify_topic_exists() {
    local topic="$1"
    local url="${EMU_URL}/v1/projects/${PROJECT}/topics/${topic}"
    if ! curl -fsS "${url}" >/dev/null 2>&1; then
        fail "canonical topic '${topic}' is missing from the emulator"
    fi
}

verify_all_topics() {
    log "verifying canonical topics exist"
    for topic in url page_created page_audit audit_update audit_error; do
        verify_topic_exists "${topic}"
    done
}

create_test_subscription() {
    log "creating throwaway test subscription ${TEST_SUBSCRIPTION}"
    curl -fsS -X PUT \
        "${EMU_URL}/v1/projects/${PROJECT}/subscriptions/${TEST_SUBSCRIPTION}" \
        -H 'content-type: application/json' \
        -d "$(jq -n \
                --arg topic "projects/${PROJECT}/topics/url" \
                '{topic:$topic, ackDeadlineSeconds:60}')" \
        >/dev/null
}

delete_test_subscription() {
    curl -fsS -X DELETE \
        "${EMU_URL}/v1/projects/${PROJECT}/subscriptions/${TEST_SUBSCRIPTION}" \
        >/dev/null 2>&1 || true
}

# Build a Body envelope that matches com.looksee.mapper.Body and publish it
# to the `url` topic with W3C trace attributes. The payload is a synthetic
# URL message; the exact content is irrelevant, only that every byte
# round-trips unchanged.
publish_test_message() {
    local payload_json='{"url":"https://example.test/e2e","messageType":"UrlMessage","accountId":1,"auditRecordId":42}'
    local encoded
    encoded="$(printf '%s' "${payload_json}" | base64 -w0)"

    log "publishing test Body envelope with traceparent attribute"
    local publish_body
    publish_body="$(jq -n \
        --arg data "${encoded}" \
        --arg tp "${TRACEPARENT}" \
        --arg ts "${TRACESTATE}" \
        '{messages:[{data:$data, attributes:{traceparent:$tp, tracestate:$ts, source:"e2e-smoke"}}]}')"

    local publish_response
    publish_response="$(curl -fsS -X POST \
        "${EMU_URL}/v1/projects/${PROJECT}/topics/url:publish" \
        -H 'content-type: application/json' \
        -d "${publish_body}")"
    local message_id
    message_id="$(printf '%s' "${publish_response}" | jq -r '.messageIds[0] // empty')"
    if [ -z "${message_id}" ]; then
        fail "publish response missing messageIds: ${publish_response}"
    fi
    log "published messageId=${message_id}"
    printf '%s' "${encoded}" > "${TMP_DIR}/expected_data"
}

# Pull the message back from our throwaway subscription and assert the
# Base64 data and trace attributes were preserved byte-for-byte. This is
# the actual contract the Wave 2.2 trace propagation relies on.
verify_round_trip() {
    log "pulling message back to verify envelope round-trip"
    local pull_response
    pull_response="$(curl -fsS -X POST \
        "${EMU_URL}/v1/projects/${PROJECT}/subscriptions/${TEST_SUBSCRIPTION}:pull" \
        -H 'content-type: application/json' \
        -d '{"maxMessages":5,"returnImmediately":true}')"

    printf '%s' "${pull_response}" > "${TMP_DIR}/pull.json"

    local received_count
    received_count="$(jq '.receivedMessages // [] | length' "${TMP_DIR}/pull.json")"
    if [ "${received_count}" -eq 0 ]; then
        fail "pull returned zero messages; round-trip failed. response=$(cat "${TMP_DIR}/pull.json")"
    fi

    local got_data
    got_data="$(jq -r '.receivedMessages[0].message.data' "${TMP_DIR}/pull.json")"
    local expected_data
    expected_data="$(cat "${TMP_DIR}/expected_data")"
    if [ "${got_data}" != "${expected_data}" ]; then
        fail "payload mismatch after round-trip.
  expected: ${expected_data}
  got:      ${got_data}"
    fi

    local got_traceparent
    got_traceparent="$(jq -r '.receivedMessages[0].message.attributes.traceparent // empty' "${TMP_DIR}/pull.json")"
    if [ "${got_traceparent}" != "${TRACEPARENT}" ]; then
        fail "traceparent attribute was not preserved.
  expected: ${TRACEPARENT}
  got:      ${got_traceparent}
Wave 2.2 distributed tracing depends on this attribute surviving the emulator."
    fi

    local got_tracestate
    got_tracestate="$(jq -r '.receivedMessages[0].message.attributes.tracestate // empty' "${TMP_DIR}/pull.json")"
    if [ "${got_tracestate}" != "${TRACESTATE}" ]; then
        fail "tracestate attribute was not preserved.
  expected: ${TRACESTATE}
  got:      ${got_tracestate}"
    fi

    # Decode the payload and make sure Jackson could parse it back into the
    # Body.Message.data shape expected by every service AuditController.
    local decoded
    decoded="$(printf '%s' "${got_data}" | base64 -d)"
    printf '%s' "${decoded}" | python3 -c 'import json,sys; json.load(sys.stdin)' \
        >/dev/null 2>&1 \
        || fail "decoded payload is not valid JSON: ${decoded}"
    log "round-trip OK: data preserved, traceparent=${got_traceparent}, tracestate=${got_tracestate}"
}

# Runs a trivial Cypher smoke query via Neo4j's HTTP transactional endpoint.
# Validates that the testcontainer is up and speaks the same wire protocol
# the Looksee persistence layer will use at runtime.
verify_neo4j_smoke() {
    log "running Neo4j smoke query via HTTP transactional API"
    local auth
    auth="$(printf '%s:%s' "${NEO4J_USER}" "${NEO4J_PASSWORD}" | base64 -w0)"

    local body
    body='{"statements":[{"statement":"RETURN 1 AS smoke, datetime() AS ts"}]}'

    local response
    response="$(curl -fsS -X POST \
        "${NEO4J_HTTP}/db/${NEO4J_DATABASE}/tx/commit" \
        -H "authorization: Basic ${auth}" \
        -H 'content-type: application/json' \
        -H 'accept: application/json' \
        -d "${body}")"

    printf '%s' "${response}" > "${TMP_DIR}/neo4j.json"

    local errors
    errors="$(jq '.errors // [] | length' "${TMP_DIR}/neo4j.json")"
    if [ "${errors}" -ne 0 ]; then
        fail "Neo4j smoke query returned errors: $(cat "${TMP_DIR}/neo4j.json")"
    fi

    local smoke_value
    smoke_value="$(jq -r '.results[0].data[0].row[0] // empty' "${TMP_DIR}/neo4j.json")"
    if [ "${smoke_value}" != "1" ]; then
        fail "Neo4j smoke query did not return 1: $(cat "${TMP_DIR}/neo4j.json")"
    fi
    log "Neo4j smoke query OK"
}

main() {
    require_tools
    wait_for_emulator
    wait_for_neo4j
    verify_all_topics
    create_test_subscription
    trap 'delete_test_subscription; rm -rf "$TMP_DIR"' EXIT
    publish_test_message
    verify_round_trip
    verify_neo4j_smoke
    log "all smoke checks passed"
}

main "$@"
