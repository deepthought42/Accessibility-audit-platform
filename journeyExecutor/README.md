# Journey Executor

A Spring Boot microservice that executes user journey workflows by navigating through web pages via a headless Chrome browser, validating journey paths, and publishing verified or discarded journey results to downstream services.

## Overview

The **Journey Executor** is part of the Look-see accessibility audit platform's journey pipeline. It receives candidate journeys from the Journey Expander, replays each journey's steps in a headless browser, builds the resulting page state, and determines whether the journey is valid (verified) or invalid (discarded).

**Role in the platform:**

```
[journeyExpander] ---> (journey_candidate topic) ---> [journeyExecutor]
                                                            |
                                        +-------------------+-------------------+
                                        |                   |                   |
                                        v                   v                   v
                              (journey_verified)   (journey_discarded)   (audit_error)
                                        |
                                        v
                              [journeyExpander]  (re-enters expansion loop)
```

## Architecture

### Key Components

| Class | Purpose |
|-------|---------|
| `Application` | Spring Boot entry point; sets the WebDriver HTTP factory system property |
| `AuditController` | Single `POST /` endpoint that decodes Pub/Sub messages, executes journey steps in the browser, and publishes results |
| `StepExecutor` | Executes individual journey steps (click, navigate, etc.) in the browser |
| `RetryConfig` | Resilience4j retry configuration for transient failures |

### Request Flow

1. Google Cloud Pub/Sub pushes a `JourneyCandidateMessage` to `POST /`.
2. The controller Base64-decodes and deserializes the message, extracting the `Journey`.
3. Idempotency check prevents duplicate processing.
4. Retry tracking ensures a journey is not re-attempted more than 4 times.
5. The journey status is set to `REVIEWING` and a headless Chrome browser is acquired.
6. All journey steps are executed sequentially in the browser with pauses between steps.
7. The resulting page state is built (DOM snapshot, elements extracted, persisted to Neo4j).
8. The journey is evaluated as **VERIFIED** or **DISCARDED** based on:
   - DISCARDED: journey has multiple steps AND the final page matches the previous page or is external.
   - VERIFIED: all other cases (a genuine page navigation occurred).
9. Results are published to the appropriate Pub/Sub topic.
10. The browser connection is closed.

## API Reference

### POST /

Receives a Pub/Sub push message containing a journey candidate for execution.

**Request body:**

```json
{
  "message": {
    "data": "<Base64-encoded JourneyCandidateMessage JSON>",
    "messageId": "..."
  }
}
```

**Decoded `JourneyCandidateMessage` fields:**

| Field | Type | Description |
|-------|------|-------------|
| `journey` | `Journey` | The journey to execute (with ordered steps) |
| `accountId` | `long` | Account identifier |
| `auditRecordId` | `long` | Parent audit record identifier |
| `browser` | `string` | Browser type (e.g., `CHROME`) |
| `mapId` | `long` | Domain map identifier |

**Responses:**

| Status | Condition |
|--------|-----------|
| `200 OK` | Message processed (journey verified, discarded, or skipped) |
| `200 OK` | Invalid/empty payload acknowledged to prevent Pub/Sub redelivery |
| `500 Internal Server Error` | Recoverable browser/element errors (triggers retry) |

### Published Messages

| Condition | Topic | Message Type |
|-----------|-------|-------------|
| Journey verified with page navigation | `page_created` | `PageBuiltMessage` |
| Journey verified | `journey_verified` | `VerifiedJourneyMessage` |
| Journey discarded | `journey_discarded` | `DiscardedJourneyMessage` |

## Configuration

### application.properties

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | HTTP server port |
| `management.server.port` | `80` | Actuator management port |
| `management.health.pubsub.enabled` | `false` | Pub/Sub health check |
| `logging.file` | `look-see.log` | Log file path |

### application.yml (Resilience4j Retry Policies)

| Profile | Max Attempts | Wait | Use Case |
|---------|-------------|------|----------|
| default | 1 | 5s | General HTTP/IO errors |
| webdriver | 15 | 1s | WebDriver/browser errors |
| neoforj | 10 | 1s | Neo4j connection/transaction errors |
| gcp | 3 | 1s | GCP storage and gRPC errors |
| builder | 5 | 2s | Page building retries |

Environment-specific settings (Pub/Sub topics, Neo4j credentials, Selenium URLs) are provided via environment variables or GCP Secret Manager at deployment time.

## Prerequisites

- Java 17 (Eclipse Temurin recommended)
- Maven 3.9+
- Neo4j database instance
- Selenium Chrome instance (standalone or hub)
- Google Cloud SDK (for Pub/Sub and Secret Manager)
- Docker (optional)

## Build & Run

### 1. Build LookseeCore (required dependency)

```bash
cd ../LookseeCore
mvn clean install -DskipTests
cd ../journeyExecutor
```

### 2. Build

```bash
mvn clean package -DskipTests
```

### 3. Run (with assertions enabled)

```bash
java -ea -jar target/*.jar
```

The `-ea` flag enables Java assertions used for Design by Contract checks.

## Docker

```bash
docker build -t deepthought42/journey-executor:latest .
docker run -p 8080:8080 -p 80:80 deepthought42/journey-executor:latest
```

The multi-stage Dockerfile uses Maven 3.9.6 + Eclipse Temurin 17 for building and Temurin 17 JRE for runtime. The container starts with `-ea -Xms6G` (6 GB minimum heap) to accommodate browser automation workloads.

### Deploy to Docker Hub

```bash
docker build -t deepthought42/journey-executor:<version> .
docker push deepthought42/journey-executor:<version>
```

## Testing

```bash
mvn test
```

Tests use JUnit 5 with Mockito (including `mockito-inline` for static method mocking). JaCoCo enforces a minimum 80% instruction coverage ratio.

After running tests, view the HTML coverage report at `target/site/jacoco/index.html`.

### Test Structure

| Test Class | Covers |
|------------|--------|
| `ApplicationTest` | Boot entry point and WebDriver system property setup |
| `AuditControllerTest` | Helper methods (`safeMessage`, `existsInJourney`, `performJourneyStepsInBrowser`), input validation |
| `AuditControllerIdempotencyTest` | Duplicate message handling, invalid payload edge cases, all null/empty/malformed input branches |
| `RetryConfigTest` | Spring annotation verification on retry configuration |

## CI/CD

Two GitHub Actions workflows:

- **`docker-ci-test.yml`** -- Runs on pull requests to `main`. Builds LookseeCore, runs tests, and performs a Docker build.
- **`docker-ci-release.yml`** -- Runs on push to `main`. Runs tests, bumps version via Semantic Release, builds and pushes the Docker image to Docker Hub, and creates a GitHub Release.

Versioning is managed by [semantic-release](https://github.com/semantic-release/semantic-release) with Conventional Commits.

## Design by Contract

This service follows Design by Contract (DbC) principles. Every public method documents and enforces:

- **Preconditions** -- validated via `assert` statements at method entry (non-null arguments, valid journey state)
- **Postconditions** -- validated via `assert` at method exit (non-null page states, valid URLs)

Run with `-ea` to activate assertion checking (enabled by default in the Docker image).

## Deployment (GCP Cloud Run)

```bash
docker build --no-cache -t gcr.io/<PROJECT_ID>/journey-executor:<version> .
docker push gcr.io/<PROJECT_ID>/journey-executor:<version>
```

The service is deployed via Terraform as a Cloud Run service with a Pub/Sub push subscription to the `journey_candidate` topic. See `LookseeIaC/GCP/modules.tf` for the full deployment configuration.

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
