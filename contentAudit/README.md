# Content Audit

Spring Boot service that consumes Pub/Sub page-audit messages and runs content-focused accessibility audits (alt text, readability, and paragraphing).

## What this service does

The POST `/` endpoint accepts a Pub/Sub push payload (`Body`), decodes a `PageAuditMessage`, loads the related `AuditRecord` and `PageState`, then executes:

- Alt text audits (`img`, `applet`, `canvas`, `iframe`, `object`, `svg`)
- Readability audit
- Paragraphing audit

When complete, the service publishes an `AuditProgressUpdate` message to notify downstream systems.

## Request format

The controller expects a Pub/Sub push body with base64-encoded JSON in `message.data`.

```json
{
  "message": {
    "data": "<base64 encoded PageAuditMessage JSON>"
  }
}
```

`PageAuditMessage` fields used by this service:

- `pageAuditId`
- `pageId`
- `accountId`

## Design by Contract

This codebase follows [Design by Contract](https://en.wikipedia.org/wiki/Design_by_contract) (DbC) principles to enforce correctness at method boundaries. Every public and significant private method documents and enforces its contract through three mechanisms:

### Preconditions

Preconditions validate that callers satisfy required input constraints before a method executes. They are enforced at runtime using `Objects.requireNonNull()` (which throws `NullPointerException`) and `IllegalArgumentException` for invalid argument values. Preconditions are **always active** regardless of JVM assertion settings.

Examples:
- All `execute()` methods on audit classes require a non-null `page_state`
- `ReadabilityAudit.execute()` additionally requires a non-null `audit_record`
- `calculateParagraphScore()` requires a non-negative `sentence_count`

### Postconditions

Postconditions verify that methods produce valid results. They are enforced using `assert` statements at method exit points, validating return values and side effects.

Examples:
- All `execute()` methods assert the saved audit object is non-null after persistence
- `getPointsForEducationLevel()` asserts the returned score is in the valid range [0, 4]

### Invariants

Invariants enforce conditions that must always hold true during execution. They are checked using `assert` statements at critical points within method bodies.

Examples:
- All audit score calculations assert `points_earned <= max_points`
- Class-level invariants documented in JavaDoc (e.g., `AuditController` requires all `@Autowired` dependencies to be non-null)

### Contract documentation

All contracts are documented in JavaDoc using structured `<strong>Preconditions</strong>`, `<strong>Postconditions</strong>`, and `<strong>Invariants</strong>` sections. Contract violations are declared via `@throws` tags.

### Enabling assertion checks

Postcondition and invariant assertions require the `-ea` JVM flag to be active at runtime:

```bash
java -ea -jar target/content-audit-<version>.jar
```

Precondition checks (`Objects.requireNonNull`, `IllegalArgumentException`) are always enforced regardless of this flag.

## Reliability and validation improvements

Recent code review fixes include:

- Defensive validation for malformed Pub/Sub payloads and invalid base64/JSON. Invalid events are acknowledged with `200 OK` so Pub/Sub does not redeliver poison messages.
- Missing `AuditRecord` / `PageState` messages are also acknowledged and logged to avoid infinite retry loops.
- Improved exception logging (stack traces logged through SLF4J, not `printStackTrace`).
- Corrected user-facing audit status typo (`"Content Audit Complete!"`).
- Null-safe and whitespace-safe text handling in readability/paragraphing audits.
- Fixed readability scoring bug for very difficult text (`ease < 30`) where higher-education scoring branches were being unintentionally overridden.

## Configuration

Main runtime configuration files:

- `src/main/resources/application.properties`
- `src/main/resources/application.yml`

The project uses Google Cloud services (including Secret Manager and NLP). Configure credentials before local runs:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
```

## Build and test

```bash
mvn clean test
```

If Maven dependency resolution to `https://repo.maven.apache.org/maven2` is blocked (e.g., HTTP 403 from the environment), tests will fail before compilation. In that case, run in an environment with working Maven Central access or a configured internal mirror/proxy.

## Running locally

```bash
mvn spring-boot:run
```

Or package and run:

```bash
mvn clean package
java -jar target/content-audit-<version>.jar
```

## Security note

This service is typically deployed behind trusted infrastructure (Pub/Sub push + internal routing). If exposed publicly, add request authentication/authorization and signature verification for push requests before production use.
