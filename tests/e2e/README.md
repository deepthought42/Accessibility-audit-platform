# E2E Pub/Sub pipeline smoke test

This directory hosts the smoke test driver invoked by
`.github/workflows/e2e-pipeline.yml` (Wave 5.1 of the architecture review).

## Goal

Prove that publishing to the `url` topic results in a `PageAuditRecord` node
materializing in Neo4j within 60 seconds, by exercising the entire happy-path
pipeline (PageBuilder → AuditManager → contentAudit) against a Pub/Sub
emulator and a Neo4j community testcontainer.

## Why a separate driver?

Per-service unit tests cover deserialization and business logic but cannot
catch:

- Pub/Sub schema drift between services (one service publishes a field that
  the next service does not deserialize).
- Subscription wiring regressions (a service starts but never receives a
  message because of a bad push endpoint).
- Idempotency-table primary key collisions across services.
- Trace-context propagation regressions introduced in Wave 2.

## Implementation contract for `run.sh`

`run.sh` is intentionally a placeholder so the CI workflow can be reviewed and
merged independently from the test driver. When implemented it must:

1. Boot PageBuilder, AuditManager, and contentAudit as background processes
   pointed at the emulator and testcontainer (`PUBSUB_EMULATOR_HOST=localhost:8085`,
   `SPRING_DATA_NEO4J_URI=bolt://localhost:7687`).
2. Publish a synthetic `url` message:
   ```bash
   curl -fsS -X POST \
     "http://localhost:8085/v1/projects/looksee-test/topics/url:publish" \
     -H 'content-type: application/json' \
     -d '{"messages":[{"data":"<base64 of the test url JSON>"}]}'
   ```
3. Poll Neo4j for a `PageAuditRecord` node with the expected URL, failing
   after 60 seconds.
4. Tear down the background processes and emit any service stdout/stderr to
   the GitHub Actions log on failure.

The `looksee-pipeline-smoke-test` JUnit class in
`LookseeCore/looksee-persistence/src/test/java/com/looksee/e2e/` is the
preferred home for the assertion logic — it can reuse the existing test
fixtures and `OutboxEventPublisher` mocks.
