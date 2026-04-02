# journeyExpander

`journeyExpander` receives verified journeys and expands them into candidate journeys by appending valid next interactions discovered on the journey's resulting page.

## Behavior summary
- Accepts a Pub/Sub push-style HTTP POST on `/`.
- Decodes `message.data` (Base64 JSON) into a `VerifiedJourneyMessage`.
- Validates that the incoming journey can be expanded.
- Collects interactive page elements and creates candidate click steps.
- Persists generated steps/journeys and publishes `JourneyCandidateMessage` payloads.

## Architecture

```
POST /  (Pub/Sub push)
  │
  ├─ Decode & validate payload
  ├─ Check journey expandability (shouldBeExpanded)
  ├─ Resolve domain & verify page is internal
  ├─ Retrieve interactive elements from resulting page
  ├─ Filter structure tags and non-interactive elements
  ├─ For each interactive element:
  │   ├─ Create candidate SimpleStep
  │   ├─ Deduplicate against existing journey steps & domain map
  │   ├─ Persist step and expanded journey
  │   └─ Publish JourneyCandidateMessage to Pub/Sub
  └─ Return 200 OK
```

### Key classes

| Class | Purpose |
|---|---|
| `Application` | Spring Boot entry point |
| `AuditController` | REST endpoint handling journey expansion logic |

### External dependencies (from `com.looksee:core`)

- **Services**: `DomainService`, `JourneyService`, `DomainMapService`, `AuditRecordService`, `PageStateService`, `StepService`
- **Models**: `Journey`, `Step` (`SimpleStep`, `LandingStep`), `PageState`, `ElementState`, `Domain`, `DomainMap`
- **GCP**: `PubSubJourneyCandidatePublisherImpl`
- **Utilities**: `BrowserUtils`, `BrowserService`, `ElementStateUtils`

## Request contract
The endpoint expects this shape:

```json
{
  "message": {
    "data": "<base64-encoded VerifiedJourneyMessage JSON>"
  }
}
```

### Response behavior
- `200 OK`: request handled (including "no expansion" scenarios).
- `400 BAD REQUEST`: payload is malformed or journey data is missing required fields.
- `500 INTERNAL SERVER ERROR`: unexpected processing failure.

### Expansion rules
A journey is eligible for expansion when:
- The last step is a `LandingStep` with a non-null start page, **or**
- The last step is a `SimpleStep` whose start and end pages differ (page key changed).

Expansion is skipped when:
- The resulting page is on an external domain.
- Steps already exist in the domain map for the resulting page.
- A candidate step or journey with the same key already exists (deduplication).

## Design by Contract

This service follows Design by Contract principles. Java assertions (`-ea`) are
enabled in the production Docker image.

### `AuditController.receiveMessage(Body)`
- **Precondition**: `body` contains a non-null `message` with Base64-encoded JSON
  data deserializable to `VerifiedJourneyMessage`. The decoded journey must have
  at least one `Step`.
- **Postcondition**: On success, zero or more `JourneyCandidateMessage` payloads
  are published and each candidate journey is persisted in the domain map.

### `AuditController.shouldBeExpanded(Journey)` *(private)*
- **Precondition**: `journey` is non-null (enforced by assertion).
- **Postcondition**: Returns `true` only when the last step indicates a navigable
  page-state change.

### `AuditController.existsInJourney(Journey, Step)` *(private)*
- **Precondition**: Both `journey` and `step` are non-null (enforced by assertion).
- **Postcondition**: Returns `true` if and only if an equivalent step (matching
  page key, element key, action, and input) exists in the journey.

## Build and run
### Prerequisites
- Java 17+
- Maven 3.8+

### Commands
```bash
mvn clean package        # Build (runs download-core.sh automatically)
mvn spring-boot:run      # Run locally
mvn test                 # Run tests
```

Default application port: `8080`.

## Testing

Tests use JUnit 5 with Mockito (including `mockito-inline` for static method mocking). JaCoCo is configured for code coverage reporting.

```bash
mvn test                          # Run all tests
mvn jacoco:report                 # Generate coverage report (after test)
```

Coverage reports are generated at `target/site/jacoco/index.html`.

### Test structure

| Test class | Covers |
|---|---|
| `AuditControllerTest` | All `receiveMessage` branches, `shouldBeExpanded`, `existsInJourney` |
| `ApplicationTest` | Spring Boot entry point |

## Configuration

Configuration file:
- `src/main/resources/application.properties` — server port, GCP Pub/Sub settings, Neo4j logging level

Configure GCP project and Pub/Sub topic values using properties or environment overrides for your deployment environment.

## CI/CD

- **Test workflow** (`docker-ci-test.yml`): runs on PRs to `main`; builds, tests, and checks coverage.
- **Release workflow** (`docker-ci-release.yml`): builds and publishes Docker images.
- Versioning managed by [semantic-release](https://github.com/semantic-release/semantic-release) with Conventional Commits.

## Docker

```bash
docker build -t journey-expander .
docker run -p 8080:8080 journey-expander
```

The multi-stage Dockerfile uses Maven 3.9.6 + Eclipse Temurin 17 for building and Temurin 17 JRE for the runtime image. Assertions are enabled at runtime via the `-ea` JVM flag.

## Development
- Commit style follows Conventional Commits (see `CONTRIBUTING.md`).
- `scripts/download-core.sh` runs during Maven `validate` to fetch the `com.looksee:core` artifact used by this service.
- `scripts/update-version.sh` is used by semantic-release to bump the version in `pom.xml`.

## License

Apache License 2.0 — see `LICENSE`.
