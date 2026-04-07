# E2E Pub/Sub pipeline smoke test

Driver invoked by `.github/workflows/e2e-pipeline.yml` (Wave 5.1 of the
architecture review).

## What this actually tests

This is a **shared-infrastructure smoke test**, not a full multi-service
integration test. It validates that every layer the audit pipeline depends
on is reachable and behaves correctly on the wire, without booting
PageBuilder, AuditManager, contentAudit, or any other service. The
scoping rationale lives in `run.sh`'s top comment.

Specifically, `run.sh`:

1. Waits for the Pub/Sub emulator and Neo4j HTTP endpoint to be reachable.
2. Asserts every canonical topic (`url`, `page_created`, `page_audit`,
   `audit_update`, `audit_error`) has been provisioned in the emulator by
   the workflow setup step.
3. Creates a throwaway pull subscription on `url`, publishes a synthetic
   `com.looksee.mapper.Body` envelope with:
   - a Base64-encoded JSON `UrlMessage` payload,
   - W3C `traceparent` and `tracestate` attributes, and
   - a `source` attribute for provenance tagging.
4. Pulls the message back and asserts that the Base64 data is preserved
   **byte-for-byte** and the trace attributes round-trip unmodified. This
   is the Wave 2.2 contract — without it, distributed traces cannot stitch
   across services.
5. Decodes the pulled payload and asserts it still parses as JSON (so a
   service's Jackson `ObjectMapper.readValue` step would succeed).
6. Runs a trivial Cypher smoke query (`RETURN 1 AS smoke`) against Neo4j
   over the HTTP transactional API.

## Why this scope instead of a full pipeline

Booting real services in CI would require:

- A Selenium/Chromium browser for PageBuilder / journeyExpander.
- Full Neo4j schema migration via `neo4j-migrations` (Wave 5.2 work).
- Spring Cloud GCP wiring that honors `PUBSUB_EMULATOR_HOST`.
- Per-service env-var plumbing (Stripe keys, GCP Vision, Pusher, etc.).

None of that infrastructure exists in-repo today. The narrower scope still
catches the regressions that silent Pub/Sub / Neo4j wire-format drift
would cause — which was the original motivation for Wave 5.1.

## Dependencies

`run.sh` uses only tools available by default on `ubuntu-latest` GitHub
Actions runners: `bash`, `curl`, `jq`, `base64`, `python3`. No Java, no
npm, no pip install required.

## Running locally

```sh
# Start the emulator and Neo4j (same as the workflow does):
docker run -d --rm --name pubsub-emulator -p 8085:8085 \
  -e PUBSUB_PROJECT_ID=looksee-test \
  gcr.io/google.com/cloudsdktool/cloud-sdk:emulators \
  gcloud beta emulators pubsub start --host-port=0.0.0.0:8085
docker run -d --rm --name neo4j-smoke -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/looksee-test-password \
  neo4j:5-community

# Create the canonical topics (the workflow does this; locally do it manually):
for t in url page_created page_audit audit_update audit_error; do
  curl -fsS -X PUT \
    "http://localhost:8085/v1/projects/looksee-test/topics/$t" \
    -H 'content-type: application/json' -d '{}'
done

bash tests/e2e/run.sh
```

## Extending this driver

If a real multi-service integration test becomes feasible later, extend
`main()` in `run.sh` with additional checks or replace the body wholesale.
The emulator and Neo4j scaffolding in
`.github/workflows/e2e-pipeline.yml` is generic enough to support either
approach.
