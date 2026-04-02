# Audit Manager

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/e2376d355755402aaa5bf7c533750851)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=deepthought42/AuditManager&amp;utm_campaign=Badge_Grade)

## Overview

The **Audit Manager** is a Spring Boot microservice that orchestrates web page audits as part of the Looksee platform. It receives page-built notifications via Google Cloud Pub/Sub, determines whether a page is eligible for auditing, creates audit records in Neo4j, and publishes page-audit messages for downstream microservices to consume.

## What It Does

- **Audit Orchestration** -- Receives page-built notifications and coordinates the execution of various audit types.
- **Audit Tracking** -- Creates and persists `PageAuditRecord` entries linked to a parent `DomainAuditRecord`.
- **Message Routing** -- Publishes `PageAuditMessage` payloads to Google Cloud Pub/Sub for downstream consumers.
- **Duplicate Prevention** -- Ensures a page is not audited more than once within the same domain audit.
- **Audit Type Management** -- Resolves audit types from the parent domain record or falls back to a default set.

## Supported Audit Types

The system supports the following audit categories:

### Visual Design
| Audit | Description |
|-------|-------------|
| Text Background Contrast | Evaluates text readability against background colors |
| Non-Text Background Contrast | Assesses contrast for UI elements and graphics |

### Information Architecture
| Audit | Description |
|-------|-------------|
| Links | Analyzes link structure and navigation |
| Titles | Evaluates page titles and heading hierarchy |
| Encrypted | Checks for proper encryption (HTTPS) |
| Metadata | Reviews meta tags and SEO elements |

### Content Quality
| Audit | Description |
|-------|-------------|
| Alt Text | Validates image accessibility with proper alt text |
| Reading Complexity | Analyzes content readability and complexity |
| Paragraphing | Evaluates content structure and paragraph organization |
| Image Copyright | Checks for proper image licensing and attribution |
| Image Policy | Ensures images comply with organizational policies |

## Architecture

### Technology Stack

| Technology | Purpose |
|------------|---------|
| Java 17 | Core language |
| Spring Boot 2.6.13 | Application framework |
| Neo4j + Spring Data Neo4j | Graph database for audit records |
| Google Cloud Pub/Sub | Asynchronous message passing |
| Google Cloud Secret Manager | Secrets management |
| Jackson | JSON serialization / deserialization |

### Key Components

#### AuditController

REST controller (`POST /`) that:
1. Validates and Base64-decodes incoming Pub/Sub push messages.
2. Deserializes the payload into a `PageBuiltMessage`.
3. Checks eligibility (not already audited, page is landable, `PageState` exists).
4. Creates a `PageAuditRecord`, links it to the domain audit, and publishes a `PageAuditMessage`.

**Design-by-contract highlights:**
- Constructor enforces non-null dependencies via `Objects.requireNonNull`.
- Assertions guard internal invariants (non-null intermediate values, non-empty audit name sets).
- Every code path returns an appropriate HTTP status (`200`, `400`, or `500`).

#### PubSubConfig

Spring `@Configuration` class that manually defines the LookseeCore beans
(`AuditRecordService`, `PageStateService`, `PubSubPageAuditPublisherImpl`)
excluded from auto-configuration. Each bean uses `@ConditionalOnMissingBean`
so test overrides take precedence.

### Data Flow

```
Pub/Sub push  ──►  AuditController.receiveMessage()
                        │
                        ├─ validate & decode Base64
                        ├─ parse JSON → PageBuiltMessage
                        ├─ check eligibility (duplicate? landable? state exists?)
                        ├─ resolve audit names (domain labels or defaults)
                        ├─ create & persist PageAuditRecord
                        └─ publish PageAuditMessage → Pub/Sub topic
```

## Configuration

### Environment Variables / Properties

```properties
# Server
server.port=8080
management.server.port=80

# Neo4j
spring.data.neo4j.uri=bolt://host:7687
spring.data.neo4j.username=neo4j
spring.data.neo4j.password=<secret>
spring.data.neo4j.database=neo4j

# Google Cloud
spring.cloud.gcp.project-id=<project-id>
spring.cloud.gcp.credentials.location=<path>

# Pub/Sub Topics
pubsub.page_audit_topic=<topic>
pubsub.audit_update=<topic>
pubsub.error_topic=<topic>
```

### Resilience4j Retry Policies

Configured in `application.yml`:

| Profile | Max Attempts | Wait | Use Case |
|---------|-------------|------|----------|
| default | 1 | 5 s | General HTTP / IO errors |
| neoforj | 10 | 10 s | Neo4j connection / transaction errors |
| gcp | 5 | 5 s | GCP storage and gRPC errors |

## Getting Started

### Prerequisites

- Java 17
- Maven 3.9+
- Neo4j database
- Google Cloud Platform account with Pub/Sub and Secret Manager enabled
- Docker (optional)

### Local Development

#### 1. Install the LookseeCore JAR

```bash
mvn install:install-file \
  -Dfile=libs/core-0.3.1.jar \
  -DgroupId=com.looksee \
  -DartifactId=core \
  -Dversion=0.3.1 \
  -Dpackaging=jar
```

#### 2. Build

```bash
mvn clean install
```

#### 3. Run

```bash
java -ea -jar target/audit-manager-1.0.11.jar
```

The `-ea` flag enables Java assertions used for design-by-contract checks.

#### 4. Docker (alternative)

```bash
docker build --tag audit-manager .
docker run -p 80:80 -p 8080:8080 --name audit-manager audit-manager
```

### Deployment

#### Google Cloud Run

```bash
docker build --no-cache -t gcr.io/<PROJECT_ID>/audit-manager:<version> .
docker push gcr.io/<PROJECT_ID>/audit-manager:<version>
```

#### Docker Hub

```bash
docker build -t <username>/audit-manager:<version> .
docker push <username>/audit-manager:<version>
```

## API Reference

### POST /

Receives a Pub/Sub push message containing a page-built notification.

**Request body:**
```json
{
  "message": {
    "data": "<base64-encoded PageBuiltMessage JSON>",
    "messageId": "...",
    "publishTime": "2024-01-01T00:00:00Z"
  }
}
```

**Decoded `data` payload (`PageBuiltMessage`):**
```json
{
  "accountId": 1,
  "pageId": 2,
  "auditRecordId": 3
}
```

**Responses:**

| Status | Condition |
|--------|-----------|
| `200 OK` | Message processed (audit created or page skipped) |
| `400 Bad Request` | Missing/empty payload, invalid Base64, or unparseable JSON |
| `500 Internal Server Error` | Infrastructure failure (Pub/Sub publish error, interruption) |

## Testing

```bash
mvn test
```

Unit tests cover:
- Invalid and missing payloads (`400` responses)
- Duplicate, non-landable, and missing-state skip paths
- Successful audit creation and Pub/Sub publishing
- Domain-level audit label resolution
- Error handling for `ExecutionException` and `InterruptedException`

## Logging

| Destination | Level |
|-------------|-------|
| Console (`CONSOLE` appender) | WARN |
| File (`look-see.log`) | WARN |

Application-level log output uses SLF4J with structured messages (key=value pairs).

## License

This project is licensed under the Apache License 2.0 -- see the [LICENSE](LICENSE) file for details.
