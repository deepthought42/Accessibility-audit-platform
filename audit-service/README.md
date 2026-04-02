# Look-see Audit Service

A Spring Boot service that processes audit progress events and publishes live audit updates for page and domain audits.

## Features

- Processes audit-related broker messages and converts them into user-facing progress updates.
- Supports both page and domain audit progress calculations.
- Broadcasts updates via `MessageBroadcaster` (for example, Pusher-backed real-time delivery).
- Uses Looksee core domain models/services for audit state, journeys, and scoring.

## Tech Stack

- Java 17
- Spring Boot 2.6.x
- Maven
- Looksee Core (`com.looksee:core`)

## Prerequisites

- Java 17+
- Maven 3.9+
- Access to the private `com.looksee:core` package (or a locally installed core JAR)

## Build and Run

### 1) Install Looksee Core locally (if not pulling from GitHub Packages)

```bash
mvn install:install-file -Dfile=libs/core-0.3.13.jar -DgroupId=com.looksee -DartifactId=core -Dversion=0.3.13 -Dpackaging=jar
```

### 2) Configure GitHub Packages auth (if needed)

```bash
export GITHUB_TOKEN=your_personal_access_token
export GITHUB_USERNAME=your_github_username
```

### 3) Build

```bash
mvn clean package
```

### 4) Run

```bash
mvn spring-boot:run
```

Or run the packaged jar:

```bash
java -jar target/audit-update-service-1.0.33.jar
```

## Service Endpoint

The service receives broker push payloads at:

```http
POST /
Content-Type: application/json
```

Expected request body shape:

```json
{
  "message": {
    "data": "<base64-encoded-json-message>"
  }
}
```

### Supported Message Types

The `data` field (after Base64 decoding) is attempted in order against each of the following types. The first successful deserialization is processed:

| Message Type | Purpose |
|---|---|
| `AuditProgressUpdate` | General audit progress for a page audit |
| `PageAuditProgressMessage` | Page-level audit completion events |
| `JourneyCandidateMessage` | New journey candidate discovered |
| `VerifiedJourneyMessage` | Journey verified |
| `DiscardedJourneyMessage` | Journey discarded |

### Response Codes

| Code | Meaning |
|---|---|
| `200 OK` | Message processed and audit update broadcast successfully |
| `400 Bad Request` | Invalid payload, undecodable Base64, or unrecognized message type |

## Design by Contract

This codebase follows **Design by Contract (DbC)** principles. Every method documents and enforces its contract:

- **Preconditions** validate that callers provide correct inputs (e.g., non-null arguments, positive IDs). Violations in public endpoints return `400 Bad Request`; violations in internal methods use Java `assert` statements.
- **Postconditions** verify return value guarantees (e.g., non-null DTOs, progress values in `[0.0, 1.0]`, non-null execution status). These are enforced via `assert` statements.
- **Class invariants** are documented on the `AuditController` class (all `@Autowired` dependencies are non-null after Spring initialization).

Assertions are enabled in production via the `-ea` JVM flag (see `Dockerfile`). To run with assertions enabled locally:

```bash
java -ea -jar target/audit-update-service-1.0.33.jar
```

## Configuration

See `src/main/resources/application.properties` for runtime configuration. Key areas:

- Server and management ports
- GCP Pub/Sub topic names
- Pusher credentials (typically via env vars)

## Contributing

Contributions are welcome. Please follow Conventional Commits as documented in `CONTRIBUTING.md`.

## License

See `LICENSE`.
