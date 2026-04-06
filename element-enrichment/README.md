# Element Enrichment

Spring Boot microservice that enriches page element states with visual and semantic metadata using headless browser inspection.

## Overview

The **Element Enrichment** service is part of the Look-see audit pipeline. After PageBuilder creates a page state with its DOM elements, this service loads the page in a headless Chrome browser and enriches each element with computed visual properties (such as background colors) that cannot be determined from static HTML alone.

**Role in the platform:**

```
[PageBuilder] ---> (page_created topic) ---> [element-enrichment]
                                                     |
                                                     +---> Neo4j (enriched element states saved)
                                                     +---> Selenium (headless Chrome for inspection)
```

## Architecture

### Key Components

| Class | Purpose |
|-------|---------|
| `Application` | Spring Boot entry point |
| `AuditController` | Single `POST /` endpoint; decodes Pub/Sub messages, orchestrates browser-based element enrichment |
| `BrowserService` | Manages headless Chrome sessions for element inspection |
| `PageStateService` | Loads and saves page state data from/to Neo4j |
| `ElementStateService` | Loads and saves enriched element state data |
| `PusherService` | Broadcasts real-time updates to the frontend |
| `UXIssueMessageService` | Handles UX issue message creation |

### Request Flow

1. Google Cloud Pub/Sub pushes a `PageBuiltMessage` to `POST /`.
2. The controller Base64-decodes and deserializes the message.
3. Idempotency check prevents duplicate processing.
4. The page URL is loaded in a headless Chrome browser.
5. Each element's computed styles (e.g., background color) are extracted via Selenium.
6. Enriched element states are saved back to Neo4j.
7. The browser connection is closed.

## API Reference

### POST /

Receives a Pub/Sub push message containing a page-built notification.

**Request body:**

```json
{
  "message": {
    "data": "<Base64-encoded PageBuiltMessage JSON>",
    "messageId": "..."
  }
}
```

**Decoded `PageBuiltMessage` fields:**

| Field | Type | Description |
|-------|------|-------------|
| `accountId` | `long` | Account identifier |
| `pageId` | `long` | Page state identifier |
| `auditRecordId` | `long` | Parent audit record identifier |

**Responses:**

| Status | Condition |
|--------|-----------|
| `200 OK` | Message processed successfully or acknowledged (invalid/duplicate) |
| `500 Internal Server Error` | Processing failure |

## Configuration

### application.yml (Resilience4j Retry Policies)

| Profile | Max Attempts | Wait | Use Case |
|---------|-------------|------|----------|
| default | 1 | 5s | General HTTP/IO errors |
| webdriver | 5 | 1s | WebDriver/browser errors |
| neoforj | 5 | 1s | Neo4j connection/transaction errors |
| gcp | 3 | 1s | GCP storage and gRPC errors |

Environment-specific settings (Pub/Sub topics, Neo4j credentials, Selenium URLs) are provided via environment variables or GCP Secret Manager at deployment time.

## Tech Stack

- **Java 17** / **Maven** / **Spring Boot 2.6.13**
- **A11yCore 0.5.0** -- shared models, persistence, browser automation, GCP integration
- **Neo4j** -- graph database for element/page state storage
- **Selenium** -- headless browser for element inspection
- **Google Cloud Pub/Sub** -- async message processing

## Prerequisites

- Java 17+
- Maven 3.x
- Neo4j instance
- Selenium Chrome instance
- LookseeCore installed locally (`cd LookseeCore && mvn clean install -DskipTests`)

## Build & Run

```bash
# Install core dependency
cd ../LookseeCore && mvn clean install -DskipTests && cd ../element-enrichment

# Build
mvn clean package -DskipTests

# Run (assertions enabled)
java -ea -jar target/*.jar
```

## Docker

```bash
docker build -t deepthought42/element-enrichment:latest .
docker run -p 8080:8080 -p 80:80 deepthought42/element-enrichment:latest
```

The Dockerfile uses AdoptOpenJDK 14 as the runtime base image. The container starts with `-Xms512M` minimum heap.

### Deploy to Docker Hub

```bash
docker build -t deepthought42/element-enrichment:<version> .
docker push deepthought42/element-enrichment:<version>
```

## Testing

```bash
mvn clean test
```

## Design by Contract

This service follows Design by Contract (DbC) principles. Assertions are enabled at runtime via the `-ea` JVM flag to enforce preconditions and postconditions throughout the enrichment pipeline.

## Deployment (GCP Cloud Run)

The service is deployed via Terraform as a Cloud Run service with a Pub/Sub push subscription to the `page_created` topic. See `LookseeIaC/GCP/modules.tf` for the full deployment configuration.

```bash
docker build --no-cache -t gcr.io/<PROJECT_ID>/element-enrichment:<version> .
docker push gcr.io/<PROJECT_ID>/element-enrichment:<version>
```

## License

Licensed under the MIT License. See [LICENSE.md](LICENSE.md) for details.
