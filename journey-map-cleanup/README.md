# Journey Map Cleanup

Spring Boot microservice that cleans up stale journey candidates in domain maps to prevent orphaned data from accumulating.

## Overview

The **Journey Map Cleanup** service runs on a scheduled Pub/Sub trigger. It scans domain maps from the last 7 days and checks whether any new journeys were created recently. If no journey activity is found within the inactivity threshold, all remaining `CANDIDATE` journeys are marked as `ERROR` to prevent them from being reprocessed indefinitely.

**Role in the platform:**

```
(scheduled Pub/Sub trigger)
         |
         v
  [journey-map-cleanup]
         |
         +---> Neo4j (query domain maps, update journey statuses)
         |
         v
  CANDIDATE journeys ---> ERROR status (stale journeys cleaned up)
```

### Cleanup Logic

1. Query all domain maps created within the last **7 days**.
2. For each domain map, check if any new journeys were created in the last **30 minutes**.
3. If **no recent journey activity** is found for a domain map, mark all remaining `CANDIDATE` journeys as `ERROR`.
4. This prevents stale candidates from being picked up by the journey pipeline indefinitely.

## Architecture

### Key Components

| Class | Purpose |
|-------|---------|
| `Application` | Spring Boot entry point |
| `AuditController` | Single `POST /` endpoint; receives scheduled trigger, performs cleanup logic |
| `JourneyService` | Queries and updates journey statuses in Neo4j |
| `DomainMapService` | Queries domain maps by creation date |

## API Reference

### POST /

Receives a Pub/Sub push message (scheduled trigger) to initiate cleanup.

**Request body:**

```json
{
  "message": {
    "data": "<Base64-encoded trigger payload>",
    "messageId": "..."
  }
}
```

**Responses:**

| Status | Condition |
|--------|-----------|
| `200 OK` | Cleanup completed |
| `500 Internal Server Error` | Processing failure |

## Configuration

### application.properties

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | HTTP server port |
| `management.server.port` | `80` | Management endpoint port |
| `management.health.pubsub.enabled` | `false` | Pub/Sub health check |
| `logging.file` | `look-see.log` | Log file path |

### application.yml (Resilience4j Retry Policies)

| Profile | Max Attempts | Wait | Use Case |
|---------|-------------|------|----------|
| webdriver | 15 | 1s | WebDriver/browser errors |
| neoforj | 10 | 1s | Neo4j connection/transaction errors |
| gcp | 3 | 1s | GCP storage and gRPC errors |

## Tech Stack

- **Java 17** / **Maven** / **Spring Boot 2.6.13**
- **A11yCore 0.5.0** -- shared models, persistence, GCP integration
- **Neo4j** -- graph database for journey/domain map state
- **Google Cloud Pub/Sub** -- scheduled trigger via push subscription

## Prerequisites

- Java 17+
- Maven 3.x
- Neo4j instance
- LookseeCore installed locally (`cd LookseeCore && mvn clean install -DskipTests`)

## Build & Run

```bash
# Install core dependency
cd ../LookseeCore && mvn clean install -DskipTests && cd ../journey-map-cleanup

# Build
mvn clean package -DskipTests

# Run (assertions enabled)
java -ea -jar target/*.jar
```

## Docker

```bash
docker build -t deepthought42/journey-map-cleanup:latest .
docker run -p 8080:8080 -p 80:80 deepthought42/journey-map-cleanup:latest
```

The multi-stage Dockerfile uses Maven 3.9.6 + Eclipse Temurin 17 for building and AdoptOpenJDK 14 for runtime. The container starts with `-ea -Xms6G` (6 GB minimum heap) due to the large dataset scans across domain maps and journeys.

### Deploy to Docker Hub

```bash
docker build -t deepthought42/journey-map-cleanup:<version> .
docker push deepthought42/journey-map-cleanup:<version>
```

## Testing

```bash
mvn clean test
```

## CI/CD

A single GitHub Actions workflow (`docker-ci.yml`) handles both testing and releases:

- **Triggers:** Push to `main` or pull requests to `main`
- **Jobs:**
  1. `test` -- Builds LookseeCore, runs Maven tests
  2. `bump-version` -- Semantic version bump via Node.js semantic-release
  3. `build-and-release` -- Docker build and push to Docker Hub (`deepthought42/journey-map-cleanup`)
  4. `github-release` -- Creates GitHub Release with CHANGELOG

## Design by Contract

This service follows Design by Contract (DbC) principles. Assertions are enabled at runtime via the `-ea` JVM flag (enabled by default in the Docker image).

## Deployment (GCP Cloud Run)

The service is deployed via Terraform as a Cloud Run service with a Pub/Sub push subscription to the `journey_completion_cleanup` topic. See `LookseeIaC/GCP/modules.tf` for the full deployment configuration.

```bash
docker build --no-cache -t gcr.io/<PROJECT_ID>/journey-map-cleanup:<version> .
docker push gcr.io/<PROJECT_ID>/journey-map-cleanup:<version>
```

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
