# Front-End Broadcaster

Spring Boot microservice that bridges backend audit events to the frontend UI in real time via Pusher WebSockets.

## Overview

The **Front-End Broadcaster** receives page-built and audit-update events from Google Cloud Pub/Sub and broadcasts them to the appropriate user's Pusher channel. This enables the Look-see UI to show real-time progress updates during audits without polling.

**Role in the platform:**

```
  [PageBuilder]                      [audit-service]
       |                                   |
       | PageBuiltMessage        AuditProgressUpdate
       v                                   v
  (page_created topic)            (audit_update topic)
       |                                   |
       +-----------------------------------+
                       |
                       v
            [front-end-broadcaster]
                       |
                       | Pusher WebSocket
                       v
              [Look-see-UI-v3]
              (real-time updates)
```

## Architecture

### Key Components

| Class | Purpose |
|-------|---------|
| `Application` | Spring Boot entry point |
| `AuditController` | Single `POST /` endpoint; decodes Pub/Sub messages, routes to broadcaster |
| `MessageBroadcaster` | Formats and sends events to Pusher channels |
| `PusherConnector` | Manages Pusher SDK client connection and configuration |
| `AccountService` | Looks up user accounts for channel routing |

### Request Flow

1. Pub/Sub pushes a `PageBuiltMessage` or `AuditProgressUpdate` to `POST /`.
2. The controller Base64-decodes and attempts deserialization as `PageBuiltMessage` first, then `AuditProgressUpdate`.
3. For page-built events:
   - Loads the `PageState` from Neo4j.
   - Constructs a `PageStateDto` with the audit record context.
   - Looks up the user account.
   - Broadcasts a `pageFound` event to the user's Pusher channel.
4. For audit-update events:
   - Broadcasts an `auditUpdate` event to the user's Pusher channel.

### Pusher Channel Conventions

- **Channel name:** The user's Auth0 ID with the `|` character removed (e.g., `auth0abc123def`)
- **Events:**
  - `pageFound` -- new page discovered during audit (payload: `PageStateDto`)
  - `auditUpdate` -- audit progress update (payload: `AuditProgressUpdate`)
  - Additional events: `subscription-exceeded`, `test-discovered`, `discovered-form`, `ux-issue-added`

## API Reference

### POST /

Receives a Pub/Sub push message containing either a page-built or audit-update notification.

**Request body:**

```json
{
  "message": {
    "data": "<Base64-encoded JSON>",
    "messageId": "..."
  }
}
```

**Supported message types:**

#### PageBuiltMessage

```json
{
  "accountId": 16961,
  "pageId": 2970,
  "auditRecordId": 2114
}
```

#### AuditProgressUpdate

```json
{
  "auditRecordId": 3356,
  "category": "CONTENT",
  "level": "PAGE",
  "progress": 1.0,
  "message": "Content Audit Complete!"
}
```

**Responses:**

| Status | Condition |
|--------|-----------|
| `200 OK` | Message processed and broadcast sent |
| `200 OK` | Invalid payload acknowledged to prevent redelivery |
| `500 Internal Server Error` | Processing failure |

## Configuration

### Pusher Properties

| Property | Description |
|----------|-------------|
| `pusher.appId` | Pusher application ID |
| `pusher.key` | Pusher API key |
| `pusher.secret` | Pusher secret key |
| `pusher.cluster` | Pusher cluster region (e.g., `us2`) |

### Spring Properties

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | HTTP server port |
| `management.server.port` | `80` | Management endpoint port |

### application.yml (Resilience4j Retry Policies)

Retry policies are configured for transient failures in Neo4j, GCP, and WebDriver connections.

Environment-specific settings (Pusher credentials, Neo4j connection, Pub/Sub topics) are provided via environment variables or GCP Secret Manager at deployment time.

## Tech Stack

- **Java 17** / **Maven** / **Spring Boot 2.6.13**
- **A11yCore 0.5.0** -- shared models, persistence, GCP integration
- **Neo4j** -- graph database for page/account state
- **Pusher** (`pusher-http-java`) -- real-time WebSocket broadcasting to frontend
- **Google Cloud Pub/Sub** -- async event consumption

## Prerequisites

- Java 17+
- Maven 3.x
- Neo4j instance
- Pusher account (app ID, key, secret, cluster)
- LookseeCore installed locally (`cd LookseeCore && mvn clean install -DskipTests`)

## Build & Run

```bash
# Install core dependency
cd ../LookseeCore && mvn clean install -DskipTests && cd ../look-see-front-end-broadcaster

# Build
mvn clean package -DskipTests

# Run (assertions enabled)
java -ea -jar target/*.jar
```

## Docker

```bash
docker build -t deepthought42/front-end-broadcaster:latest .
docker run -p 8080:8080 -p 80:80 deepthought42/front-end-broadcaster:latest
```

The Dockerfile uses AdoptOpenJDK 14 as the runtime base image. The container starts with `-ea -Xms1G` (1 GB minimum heap).

### Deploy to Docker Hub

```bash
docker build -t deepthought42/front-end-broadcaster:<version> .
docker push deepthought42/front-end-broadcaster:<version>
```

## Testing

```bash
mvn clean test
```

## Design by Contract

This service follows Design by Contract (DbC) principles. Assertions are enabled at runtime via the `-ea` JVM flag (enabled by default in the Docker image).

## Deployment (GCP Cloud Run)

The service is deployed via Terraform as a Cloud Run service with Pub/Sub push subscriptions to the `page_created` and `audit_update` topics. Pusher credentials are injected via GCP Secret Manager.

This service also has its own Terraform configuration in `terraform/` for standalone deployment, including Cloud Run service, Pub/Sub topic, and KMS encryption.

```bash
docker build --no-cache -t gcr.io/<PROJECT_ID>/front-end-broadcaster:<version> .
docker push gcr.io/<PROJECT_ID>/front-end-broadcaster:<version>
```

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
