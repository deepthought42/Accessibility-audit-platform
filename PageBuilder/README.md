[![Codacy Badge](https://app.codacy.com/project/badge/Grade/e2376d355755402aaa5bf7c533750851)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=deepthought42/PageBuilder&amp;utm_campaign=Badge_Grade)

# PageBuilder

A Spring Boot microservice that builds `PageState` objects and their associated `ElementState` data from incoming Google Cloud Pub/Sub audit messages. It acts as the page-extraction step in the Looksee audit pipeline, receiving a URL, loading the page via a headless browser, persisting the resulting DOM structure, and publishing downstream messages for further processing.

## Architecture

```
Pub/Sub (AuditStart) ──> PageBuilder ──> Pub/Sub (PageCreated / PageAudit / JourneyVerified / AuditError)
                              │
                              ├── BrowserService  (headless Chrome)
                              ├── Neo4j           (page/element/journey persistence)
                              └── GCS             (screenshot storage)
```

### Key Components

| Class | Purpose |
|-------|---------|
| `Application` | Spring Boot entry point; sets the WebDriver HTTP factory system property. |
| `AuditController` | Single `POST /` endpoint that decodes Pub/Sub push messages, orchestrates page building, and publishes results. |
| `BodySchema` / `MessageSchema` / `AuditStartMessageSchema` | OpenAPI-annotated DTOs for the Pub/Sub push envelope. |

### Request Flow

1. Google Cloud Pub/Sub pushes a message to `POST /`.
2. The controller decodes the Base64 `message.data` field into an `AuditStartMessage`.
3. The target URL is loaded via headless Chrome; HTTP status and security are checked.
4. If the HTTP status is `404` or `408`, an error message is published and processing stops.
5. A `PageState` (DOM snapshot) is created or retrieved from the database.
6. Element states (individual DOM nodes) are extracted and persisted.
7. Depending on the audit level:
   - **PAGE** -- the page is attached to the audit record and a `PageAuditMessage` is published.
   - **DOMAIN** -- a `DomainMap` is created/updated, a single-step `Journey` is saved, and both `PageCreated` and `JourneyVerified` messages are published.

## Design by Contract

This project follows **Design by Contract** (DbC) principles. Each public method documents its:

- **Preconditions** -- what must be true before the method is called.
- **Postconditions** -- what the method guarantees upon return.
- **Class invariants** -- properties that hold true for every instance after construction and between method calls.

Runtime assertions (`assert`) enforce key invariants during development and testing. Run the application with `-ea` to enable assertion checking:

```bash
java -ea -jar target/PageBuilder-*.jar
```

## Prerequisites

- **Java 17** (Temurin recommended)
- **Maven 3.8+**
- **Docker** (for containerised builds)
- **Google Cloud SDK** (for GCR / Pub/Sub interaction)
- **LookseeCore JAR** -- downloaded automatically by `scripts/download-core.sh`

## Getting Started

### Build and Run Locally

```bash
# Download the LookseeCore dependency
bash scripts/download-core.sh

# Install it into the local Maven repository
mvn install:install-file \
  -Dfile=libs/core-$(sed -n 's:.*<looksee-core.version>\(.*\)</looksee-core.version>.*:\1:p' pom.xml | head -n1).jar \
  -DgroupId=com.looksee -DartifactId=core \
  -Dversion=$(sed -n 's:.*<looksee-core.version>\(.*\)</looksee-core.version>.*:\1:p' pom.xml | head -n1) \
  -Dpackaging=jar

# Build
mvn clean install

# Run (with assertions enabled)
java -ea -jar target/PageBuilder-*.jar
```

### Docker

```bash
# Build
docker build -t deepthought42/page-builder:latest .

# Run
docker run -p 8080:8080 -p 80:80 deepthought42/page-builder:latest
```

### Deploy to GCR

```bash
gcloud auth print-access-token | docker login -u oauth2accesstoken --password-stdin https://us-central1-docker.pkg.dev

docker build --no-cache \
  -t us-central1-docker.pkg.dev/cosmic-envoy-280619/page-builder/<VERSION> .

docker push us-central1-docker.pkg.dev/cosmic-envoy-280619/page-builder/<VERSION>
```

## API

The service exposes a single endpoint:

### `POST /`

Accepts a Pub/Sub push message envelope containing a Base64-encoded `AuditStartMessage`.

**Request body:**

```json
{
  "message": {
    "data": "<Base64-encoded AuditStartMessage JSON>"
  }
}
```

**Decoded `AuditStartMessage` fields:**

| Field | Type | Description |
|-------|------|-------------|
| `url` | `string` | URL to audit |
| `type` | `string` | Audit level: `PAGE` or `DOMAIN` |
| `accountId` | `string` | Account identifier |
| `auditId` | `long` | Audit record identifier |

**Responses:**

| Status | Meaning |
|--------|---------|
| `200` | Message processed successfully |
| `400` | Missing or malformed payload |
| `500` | Internal processing error |

Interactive API docs are available at `/swagger-ui.html` when the service is running (powered by SpringDoc OpenAPI).

## Dependencies

| Dependency | Purpose |
|-----------|---------|
| Spring Boot 2.6.13 | Web framework, REST endpoints, actuator |
| Jackson 2.12.2 (`jackson-databind`, `jackson-datatype-jsr310`) | JSON serialization with Java Time support |
| Lombok 1.18.34 | Boilerplate reduction via annotations |
| SpringDoc OpenAPI 1.7.0 | Swagger UI and OpenAPI 3.0 documentation |
| Swagger Annotations 2.2.31 | OpenAPI annotation support |
| LookseeCore 0.3.16 | Shared models, services, and GCP utilities |
| Resilience4j | Retry policies for WebDriver, Neo4j, and GCP calls |

## Testing

### Running Tests

```bash
mvn test
```

### Test Coverage

Tests use **JUnit 5** and **Mockito** with `MockedStatic` for static utility methods. A **JaCoCo** coverage gate is configured in the POM to enforce a minimum 90 % line-coverage ratio.

After running `mvn test`, view the HTML coverage report at:

```
target/site/jacoco/index.html
```

### Test Structure

| Test Class | Covers |
|------------|--------|
| `ApplicationTest` | Boot entry point and system property setup |
| `AuditControllerTest` | All controller branches: input validation, HTTP error statuses, PAGE audit path, DOMAIN audit path, exception handling, browser cleanup |
| `BodySchemaTest` | Lombok-generated constructors, getters, setters, equals, hashCode, toString |
| `MessageSchemaTest` | Same as above for the message wrapper |
| `AuditStartMessageSchemaTest` | Same as above for the decoded payload schema |

### Sending a Manual Test Message

1. Log in to the Google Cloud Console and navigate to **Pub/Sub**.
2. Find the audit-start topic and select **Messages**.
3. Publish a message with the following JSON body (Base64-encode it as the `data` field):

```json
{
  "url": "https://example.com",
  "type": "PAGE",
  "accountId": "5",
  "auditId": 11
}
```

## Configuration

| File | Purpose |
|------|---------|
| `src/main/resources/application.properties` | Server ports, logging, Neo4j log level |
| `src/main/resources/application.yml` | Resilience4j retry policies (default, WebDriver, Neo4j, GCP) |
| `src/main/resources/logback.xml` | Logging appenders (console + file) |

Environment-specific settings (Pub/Sub topics, Neo4j credentials, GCS bucket, GCP credentials) are provided via environment variables or external configuration at deployment time.

## CI/CD

Two GitHub Actions workflows drive the pipeline:

- **`docker-ci-test.yml`** -- Runs on pull requests to `master`. Builds the project, runs tests, and performs a Docker build.
- **`docker-ci-release.yml`** -- Runs on pushes to `master`. Runs tests, bumps the version via Semantic Release, builds and pushes the Docker image to DockerHub, and creates a GitHub Release.

## Security

### Generating a PKCS12 certificate for SSL

```bash
openssl pkcs12 -export -inkey private.key -in certificate.crt -out api_key.p12
```

## Migration Notes

- **2023-01-06:** Replace `isSecure` property with `secured` property in `PageState` objects.

```cypher
MATCH (n:PageState) SET n.secured=n.isSecure RETURN n
MATCH (n:PageState) SET n.isSecure=NULL RETURN n
```

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
