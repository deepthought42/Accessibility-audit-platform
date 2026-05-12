# Issue #28 — Atomic Idempotency: Step 10 Verification Report

Closes the [#28 umbrella](https://github.com/brandonkindred/look-see/issues/28)
("Make idempotency atomic, persistent, and mandatory") and its final sub-issue
[#89](https://github.com/brandonkindred/look-see/issues/89) ("Step 10 —
Verification + PR").

Code work for Steps 1–9 landed via per-step PRs:

| Sub-issue | Merge | What landed |
|-----------|-------|-------------|
| #80 / Step 1 | #90 | `neo4j-migrations-spring-boot-starter 1.16.0` wired into `looksee-persistence` |
| #81 / Step 2 | #91 | `IdempotencyService.claim()` + `ProcessedMessageRepository.claim()` MERGE query |
| #82–#84 / Steps 3–5 | #92 | `PubSubAuditController.claim()` path; `journeyErrors` + `AuditManager` migrated to base class; in-memory `ConcurrentHashMap` deduper removed |
| #85 / Step 6 | #95 | Fail-fast startup when `ProcessedMessageRepository` is missing |
| #86 / Step 7 | #96 | 8-day retention keyed on `processedAt` |
| #87 / Step 8 | (alongside) | `IdempotencyServiceConcurrencyTest`, `MigrationsApplyIntegrationTest`, `IdempotencyIntegrationTest` (all `@Tag("requires-docker")`) |
| #88 / Step 9 | #98 | LookseeCore 1.0.0 + CHANGELOG |

This document records the end-to-end verification run that closes out the
umbrella.

## 1. Local build verification (#89 §10.1)

`mvn clean verify` executed against the three modules touched by the
idempotency work. Two environmental conditions were encountered and worked
around — both pre-existing and unrelated to the idempotency changes:

- The sandbox runs JDK 21, but project poms target JDK 17. Lombok 1.18.30 and
  Mockito 4.x — both pinned in the build — cannot operate on JDK 21
  (`NoSuchFieldError: JCTree$JCImport.qualid` and `Mockito cannot mock this
  class` respectively). CI uses JDK 17 (`.github/workflows/ci.yml` →
  `setup-java@v4 with java-version: '17'`), so installed OpenJDK 17 locally
  and re-ran with `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`.
- `LookseeCore/looksee-core/.../BrowserTest#verifyUrlReaderForHttps` makes a
  live HTTP call to `https://www.amazon.com` and fails on egress-restricted
  networks with `HTTP 403`. This test predates the idempotency work (file
  history shows commits from before #28 was opened). Excluded via
  `-Dtest='!BrowserTest#verifyUrlReaderForHttps' -DfailIfNoTests=false`.

Results:

| Module | Tests run | Failures | Errors | Skipped | Result |
|--------|-----------|----------|--------|---------|--------|
| LookseeCore (11 sub-modules) | 1072 | 0 | 0 | 1¹ | BUILD SUCCESS |
| journeyErrors | 6 | 0 | 0 | 0 | BUILD SUCCESS |
| AuditManager | 19 | 0 | 0 | 0 | BUILD SUCCESS |

¹ The single skip is `BrowserTest#verifyUrlReaderForHttps` (egress).
Docker-tagged tests are excluded by default in plain `verify`; they are
covered separately in §2.

The idempotency-relevant test classes all pass on the baseline:

- `looksee-persistence`: `IdempotencyServiceTest` (20), `IdempotencyIntegrationTest` (9), `ProcessedMessageRepositoryTest` (9), `MigrationsResolveTest` (2), `MessageFlowEndToEndTest` (6), `OutboxEventPublisherTest` (9)
- `looksee-messaging`: `PubSubAuditControllerTest` (9)
- `looksee-models`: `ProcessedMessageTest` (9), `OutboxEventTest` (9)
- `AuditManager`: `AuditControllerIdempotencyTest` (3), `AuditControllerTest` (7) — includes `redelivery_doesNotDoubleProcess_thePerIssue83Contract`
- `journeyErrors`: `AuditControllerTest` (6)

## 2. Docker-gated empirical proof (#89 §10.2 mechanism)

The three `@Tag("requires-docker")` integration tests spin up real Neo4j
5.18-community testcontainers and exercise the atomicity invariant against an
actual graph database:

| Test class | Module | Tests | Time | Result |
|------------|--------|-------|------|--------|
| `IdempotencyServiceConcurrencyTest` | LookseeCore/looksee-persistence | 2 | 17.4s | PASS |
| `MigrationsApplyIntegrationTest` | LookseeCore/looksee-persistence | 3 | 14.5s | PASS |
| `IdempotencyIntegrationTest` | journeyErrors | 2 | 18.5s | PASS |

Invocation:

```bash
export DOCKER_HOST=unix:///var/run/docker.sock
mvn test -DexcludedGroups= -Dgroups=requires-docker -Dapi.version=1.43
```

The `-Dapi.version=1.43` flag is required because the testcontainers
1.19.8 / docker-java 3.3.6 client defaults to Docker API v1.32, which Docker
Engine 29.x rejects (`Minimum supported API version is 1.40`). This is a
local-environment knob; CI runners with older Docker engines do not need it.

### 2.1 Atomic-claim concurrency proof

`IdempotencyServiceConcurrencyTest` is the central regression contract for
#28: 100 threads invoke the raw Cypher `MERGE` that
`ProcessedMessageRepository.claim()` compiles to, against a single
`(pubsubMessageId, serviceName)` key, behind a `CyclicBarrier` so every
thread releases simultaneously. The test asserts:

- Exactly **one** thread observes `justCreated = true` (the winner).
- Exactly **one** `ProcessedMessage` node exists in Neo4j after the storm.
- Concurrent claims for *different* `serviceName` values do not interfere.

Without the V002 uniqueness constraint Neo4j does not serialize concurrent
writers and multiple winners appear. With it, Neo4j physically serializes
the MERGE and the invariant holds. Both assertions passed against
Neo4j 5.18.

### 2.2 Migration rehearsal (#89 §10.2)

`MigrationsApplyIntegrationTest` seeds three duplicate `ProcessedMessage`
nodes into a fresh Neo4j container, then runs the `neo4j-migrations` startup
chain that real applications execute on boot. It asserts:

1. **V001** (dedupe) collapses the three duplicates to one.
2. **V002** (uniqueness constraint) is created with the expected name
   `processed_message_unique`.
3. A subsequent direct `CREATE` of a duplicate `(pubsubMessageId, serviceName)`
   pair fails with a `ClientException` carrying status code
   `Neo.ClientError.Schema.ConstraintValidationFailed`.

This satisfies the *mechanism* of #89 §10.2. It does **not** substitute for
the prod-snapshot rehearsal — see §4 below for the recipe the owner runs
before the next deploy of `looksee-persistence` consumers to prod.

### 2.3 Test-assertion fix landed alongside this verification

While running #89 §10.2 against Neo4j 5.18 we discovered that
`MigrationsApplyIntegrationTest.v002_constraint_rejectsSubsequentDuplicateInserts`
asserted on the *exception message text* containing
`"ConstraintValidationFailed"`. Neo4j 5.x no longer embeds the status code
in the user-facing message — it only appears on
`ClientException.code()`. The constraint was firing correctly; the assertion
just no longer matched. We updated the assertion to use
`isInstanceOfSatisfying(ClientException.class, e -> e.code() contains
"ConstraintValidationFailed")`, which is the stable Neo4j contract across
versions.

This bug had not been caught earlier because **CI does not currently run any
`requires-docker` tests**. See §3.

## 3. Gap discovered: CI does not exercise docker-gated tests

`.github/workflows/ci.yml` runs `mvn -B -ntp test` against each module
without `-DexcludedGroups=` or `-Dgroups=requires-docker`. With the
project's surefire convention this *intentionally* skips every
`@Tag("requires-docker")` test, so the three integration tests above —
including the 100-thread concurrency proof that is the central regression
contract for #28 — have not been running in any pipeline since they were
written in Step 8 (#87).

Recommendation (not in scope for this PR, suggested as follow-up): add a
separate CI job that runs the docker-gated tests on push to `main` and on
PRs that touch `looksee-persistence`, `looksee-messaging`, `journeyErrors`,
or `AuditManager`. GitHub-hosted runners include a usable Docker daemon by
default. A minimum invocation:

```yaml
- name: Docker integration tests
  run: |
    cd LookseeCore && mvn -B -ntp test \
      -pl looksee-persistence -am \
      -DexcludedGroups= -Dgroups=requires-docker
```

Without this job, future regressions in the atomic-claim path will not be
caught by CI.

## 4. Out of scope here (recipes for the owner)

### 4.1 Production-snapshot migration rehearsal (#89 §10.2 — full form)

The testcontainer rehearsal in §2.2 proves the migration *mechanism* but
does not exercise it against real production data. Before deploying the
next `looksee-persistence`-dependent service to prod, run:

```bash
# 1. Pull a recent prod backup into a staging Neo4j (Aura or self-hosted)
neo4j-admin database load --from-path=/path/to/prod-backup --overwrite-destination

# 2. Point the locally-running looksee-persistence Spring context at it
SPRING_NEO4J_URI=bolt://staging-neo4j:7687 \
SPRING_NEO4J_AUTHENTICATION_USERNAME=neo4j \
SPRING_NEO4J_AUTHENTICATION_PASSWORD=*** \
  java -jar AuditManager-*.jar

# 3. In application logs, confirm:
#    - "Applied migration V001"   (dedupes any pre-existing duplicates)
#    - "Applied migration V002"   (creates the uniqueness constraint)
#    - No "ConstraintViolationException" during V002 — if any appear, prod
#      already has duplicates and V001 didn't catch them.
```

If V001 surfaces unexpected duplicates that the rehearsal does not dedupe
cleanly, file as a blocker before rollout.

### 4.2 Staging smoke test (#89 §10.3)

Once `journeyErrors` and `AuditManager` are deployed to staging at the new
version, the smoke test from #89:

1. Open a Pub/Sub subscription metrics dashboard for the consumer subscriptions.
2. Publish the same message twice (Pub/Sub redelivery simulation):
   ```bash
   gcloud pubsub topics publish journey-errors-topic --message='...' --attribute=test-redelivery=1
   gcloud pubsub topics publish journey-errors-topic --message='...' --attribute=test-redelivery=1
   ```
3. Confirm:
   - Exactly one `ProcessedMessage` node in staging Neo4j for that message id.
   - `looksee.pubsub.messages.received{result="duplicate"}` increments by 1.
   - No error in `journeyErrors` logs.

## 5. Closure

- Closes [#89](https://github.com/brandonkindred/look-see/issues/89)
- Closes [#28](https://github.com/brandonkindred/look-see/issues/28)
- Unblocks [#27](https://github.com/brandonkindred/look-see/issues/27)
  (transactional outbox) and [#29](https://github.com/brandonkindred/look-see/issues/29)
  (poison-message protocol) — both depended on atomic idempotency.

## Appendix: Reproduce locally

```bash
# JDK 17 required (Lombok / Mockito pinned).
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Baseline.
cd LookseeCore   && mvn clean verify -Dtest='!BrowserTest#verifyUrlReaderForHttps' -DfailIfNoTests=false
cd ../LookseeCore && mvn install -DskipTests
cd ../journeyErrors && mvn clean verify
cd ../AuditManager   && mvn clean verify

# Docker-gated tests (Docker daemon must be running).
cd ../LookseeCore/looksee-persistence \
  && mvn test -DexcludedGroups= -Dgroups=requires-docker -Dapi.version=1.43
cd ../../journeyErrors \
  && mvn test -DexcludedGroups= -Dgroups=requires-docker -Dapi.version=1.43
```
