# Journey Errors (Dead-Letter Handler)

Spring Boot microservice that handles failed journey candidate messages. When a journey candidate fails processing in the normal pipeline, this service receives the dead-letter message and marks the journey status as `ERROR`.

## Overview

The **Journey Errors** service acts as a dead-letter handler for the journey pipeline. When the Journey Executor fails to process a `JourneyCandidateMessage` (after exhausting Pub/Sub retry attempts), the message is forwarded to this service's dead-letter subscription. If the journey is still in `CANDIDATE` status, it updates the status to `ERROR` in Neo4j to prevent infinite reprocessing.

**Role in the platform:**

```
  [journeyExecutor]
         |
         | (processing fails, Pub/Sub retries exhausted)
         v
  (journey_candidate dead-letter subscription)
         |
         v
  [journeyErrors]
         |
         +---> Neo4j (update journey status: CANDIDATE -> ERROR)
```

## Architecture

### Key Components

| Class | Purpose |
|-------|---------|
| `Application` | Spring Boot entry point |
| `AuditController` | Single `POST /` endpoint; decodes dead-letter messages, updates journey status |
| `JourneyService` | Updates journey status in Neo4j |

### Request Flow

1. Pub/Sub dead-letter subscription pushes a failed `JourneyCandidateMessage` to `POST /`.
2. The controller Base64-decodes and deserializes the message.
3. Idempotency check (via `ConcurrentHashMap`) prevents duplicate processing.
4. If the journey status is still `CANDIDATE`, it is updated to `ERROR`.
5. If the journey has already been processed (status is not `CANDIDATE`), the message is acknowledged without changes.

## API Reference

### POST /

Receives a Pub/Sub dead-letter message containing a failed journey candidate.

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
| `accountId` | `long` | Account identifier |
| `journey` | `Journey` | The failed journey (with ID and status) |
| `browser` | `string` | Browser type |
| `auditRecordId` | `long` | Parent audit record identifier |
| `mapId` | `long` | Domain map identifier |

**Responses:**

| Status | Condition |
|--------|-----------|
| `200 OK` | Message processed (journey marked as ERROR or already handled) |
| `200 OK` | Invalid/empty payload acknowledged to prevent redelivery |

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
| neoforj | 2 | 1s | Neo4j connection/transaction errors |
| gcp | 10 | 1s | GCP storage and gRPC errors |

## Tech Stack

- **Java 17** / **Maven** / **Spring Boot 2.6.13**
- **A11yCore 0.5.0** -- shared models, persistence, GCP integration
- **Neo4j** -- graph database for journey state storage
- **Google Cloud Pub/Sub** -- dead-letter message consumption

## Prerequisites

- Java 17+
- Maven 3.x
- Neo4j instance
- LookseeCore installed locally (`cd LookseeCore && mvn clean install -DskipTests`)

## Build & Run

```bash
# Install core dependency
cd ../LookseeCore && mvn clean install -DskipTests && cd ../journeyErrors

# Build
mvn clean package -DskipTests

# Run (assertions enabled)
java -ea -jar target/*.jar
```

## Docker

```bash
docker build -t deepthought42/journey-errors:latest .
docker run -p 8080:8080 -p 80:80 deepthought42/journey-errors:latest
```

The Dockerfile uses AdoptOpenJDK 14 as the runtime base image.

### Deploy to Docker Hub

```bash
docker build -t deepthought42/journey-errors:<version> .
docker push deepthought42/journey-errors:<version>
```

## Testing

```bash
mvn clean test
```

## Design by Contract

This service follows Design by Contract (DbC) principles. Assertions guard non-null journey references and valid status transitions.

## Deployment (GCP Cloud Run)

The service is deployed via Terraform as a Cloud Run service with a Pub/Sub dead-letter subscription on the `journey_candidate` topic. See `LookseeIaC/GCP/modules.tf` for the full deployment configuration.

```bash
docker build --no-cache -t gcr.io/<PROJECT_ID>/journey-errors:<version> .
docker push gcr.io/<PROJECT_ID>/journey-errors:<version>
```

## License

Licensed under the MIT License. See [LICENSE.md](LICENSE.md) for details.
