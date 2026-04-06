# Crawler API

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/e2376d355755402aaa5bf7c533750851)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=deepthought42/CrawlerApi&amp;utm_campaign=Badge_Grade)

The primary user-facing REST API for the Look-see accessibility audit platform. It handles authentication, account management, domain configuration, audit orchestration, report generation, and integrations with third-party services.

## Overview

The Crawler API is the entry point for all client interactions with the platform. It:

- **Authenticates users** via Auth0 (OAuth2 / JWT)
- **Manages accounts** with onboarding workflows and API tokens
- **Manages domains** with WCAG compliance levels and expertise settings
- **Starts audits** by publishing `AuditStartMessage` payloads to Google Cloud Pub/Sub
- **Serves audit results** including pages, elements, statistics, and insights
- **Generates reports** in Excel and PDF formats
- **Manages integrations** with Product Board, Jira, Slack, GitHub, GitLab, Figma, Google Drive, Trello, and Jenkins
- **Handles subscriptions** via Stripe billing

## Architecture

```
  [Look-see-UI / Extensions]
           |
           | HTTP/REST (Auth0 JWT)
           v
     [CrawlerAPI]
           |
           +---> Neo4j (domain, page, audit, account data)
           +---> Auth0 (user identity and roles)
           +---> Stripe (subscription management)
           +---> Segment (analytics tracking)
           +---> (url topic) ---> [PageBuilder]  (starts audit pipeline)
```

### Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 21 | Core language |
| Spring Boot 3.5.0 | Application framework |
| Spring Security + Auth0 | OAuth2 authentication and authorization |
| Neo4j + Spring Data Neo4j | Graph database for domain models |
| Google Cloud Pub/Sub | Publish audit start messages |
| Google Cloud Secret Manager | Secrets management |
| Stripe | Subscription billing |
| Segment | Analytics and user tracking |
| JaCoCo | Code coverage reporting |

## API Reference

All endpoints are prefixed with `v1/`. Authentication via Auth0 JWT bearer token is required unless noted.

### Accounts (`v1/accounts`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/v1/accounts` | Create a new account |
| GET | `/v1/accounts` | Get current user's account |
| PUT | `/v1/accounts/{id}` | Update account |
| DELETE | `/v1/accounts` | Delete account |
| PUT | `/v1/accounts/{id}/refreshToken` | Generate new API token |
| POST | `/v1/accounts/onboarding_step` | Mark onboarding step complete |
| GET | `/v1/accounts/onboarding_steps_completed` | Get completed onboarding steps |

### Domains (`v1/domains`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/v1/domains` | List user's domains |
| POST | `/v1/domains` | Create a new domain |
| PUT | `/v1/domains/{id}` | Update domain |
| GET | `/v1/domains/{id}/settings` | Get domain settings |
| POST | `/v1/domains/{id}/settings/wcag` | Update WCAG compliance level |
| POST | `/v1/domains/{id}/settings/expertise` | Update expertise level |
| GET | `/v1/domains/{id}/pages` | List pages in domain |
| GET | `/v1/domains/{id}/pages/{page_id}/stats` | Get page statistics |
| POST | `/v1/domains/{id}/users` | Add user to domain |
| DELETE | `/v1/domains/{id}/users/{user_id}` | Remove user from domain |
| POST | `/v1/domains/{id}/test-users` | Add test user |
| DELETE | `/v1/domains/{id}/test-users/{user_id}` | Remove test user |
| GET | `/v1/domains/{id}/competitors` | List competitors |
| POST | `/v1/domains/{id}/competitors` | Add competitor |
| POST | `/v1/domains/{id}/competitors/{comp_id}/palette` | Manage competitor palette |
| POST | `/v1/domains/{id}/start` | Start domain audit |
| GET | `/v1/domains/{id}/audits` | Get audit records for domain |
| GET | `/v1/domains/{id}/audits/{audit_id}/report/excel` | Export Excel report |
| GET | `/v1/domains/{id}/audits/{audit_id}/report/pdf` | Export PDF report |
| POST | `/v1/domains/{id}/image-policies` | Manage image policies |

### Audits (`v1/audits`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/v1/audits` | List all audits |
| GET | `/v1/audits/{id}` | Get audit by ID |
| POST | `/v1/audits/{key}/issues` | Add issue to audit |
| GET | `/v1/audits/stop` | Stop running audit |
| GET | `/v1/audits/{audit_id}/report/excel` | Export Excel report |
| GET | `/v1/audits/{audit_id}/report/pdf` | Export PDF report |

### Auditor (`v1/auditor`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/v1/auditor/start` | Start a page audit |
| GET | `/v1/auditor` | Get page by URL |
| GET | `/v1/auditor/{page_key}/insights` | Get page performance insights |

### Audit Records (`v1/auditrecords`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/v1/auditrecords` | List audit records |
| POST | `/v1/auditrecords/{id}/report` | Request email report |
| GET | `/v1/auditrecords/{id}/pages` | Get pages in audit record |
| GET | `/v1/auditrecords/{id}/elements` | Get element-to-issue mappings |
| GET | `/v1/auditrecords/{id}/stats` | Get audit statistics |

### Pages (`v1/pages`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/v1/pages` | Get page by URL |
| GET | `/v1/pages/{page_key}/insights` | Get performance insights |

### Elements (`v1/elements`)

| Method | Path | Description |
|--------|------|-------------|
| PUT | `/v1/elements/elements` | Update element |
| PUT | `/v1/elements/forms/{form_key}/elements` | Update form element |
| POST | `/v1/elements/{element_key}/rules` | Add rule to element |
| DELETE | `/v1/elements/{element_key}/rules/{rule_key}` | Remove rule from element |

### Design System (`v1/designsystem`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/v1/designsystem/{id}/color` | Update design system color palette |

### Integrations (`v1/integrations`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/v1/integrations` | List available integrations |
| GET | `/v1/integrations/{type}` | Get integration by type |
| GET | `/v1/integrations/{type}/config` | Get integration config |
| PUT | `/v1/integrations/{type}/config` | Create/update integration config |
| DELETE | `/v1/integrations/{type}/config` | Delete integration config |
| POST | `/v1/integrations/{type}/test` | Test integration connection |
| POST | `/v1/integrations/product-board` | Create Product Board token |

Supported integration types: Product Board, Jira, Slack, GitHub, GitLab, Figma, Google Drive, Trello, Jenkins.

### Test Records (`v1/testrecords`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/v1/testrecords` | List test records |
| GET | `/v1/testrecords/{key}` | Get test record by key |

### Test Users (`v1/testusers`)

| Method | Path | Description |
|--------|------|-------------|
| PUT | `/v1/testusers/{user_id}` | Update test user |

### User Info (`v1/userinfo`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/v1/userinfo/account` | Get current user account |
| GET | `/v1/userinfo/info` | Get user info from Auth0 |
| GET | `/v1/userinfo/username` | Get username |
| GET | `/v1/userinfo/email` | Get user email |
| GET | `/v1/userinfo/config/status` | Check Auth0 configuration status |

### IDE Test Export (`v1/ide-test-export`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/v1/ide-test-export` | Create IDE test export |
| PUT | `/v1/ide-test-export` | Update IDE test export |

### Competitors (`v1/competitors`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/v1/competitors/{competitor_id}` | Start competitor analysis |

## Configuration

### application.properties

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `9080` | HTTP server port |
| `management.server.port` | `80` | Management endpoint port |
| `logging.file` | `crawlerApi.log` | Log file path |

### Third-Party Service Properties

| Property | Description |
|----------|-------------|
| `spring.data.neo4j.uri` | Neo4j bolt connection URI |
| `spring.data.neo4j.username` | Neo4j username |
| `spring.data.neo4j.password` | Neo4j password |
| `spring.data.neo4j.database` | Neo4j database name |
| `auth0.client_id` | Auth0 client ID |
| `auth0.client_secret` | Auth0 client secret |
| `auth0.domain` | Auth0 tenant domain |
| `auth0.audience` | Auth0 API audience |
| `stripe.secretKey` | Stripe API secret key |
| `stripe.agency_basic_price_id` | Stripe price ID |
| `segment.analytics.writeKey` | Segment analytics write key |
| `integrations.encryption.key` | AES-GCM encryption key for integration configs |
| `spring.mail.host` | SMTP host |
| `spring.mail.username` | SMTP username |
| `spring.mail.password` | SMTP password |

## Prerequisites

- Java 21
- Maven 3.9+
- Neo4j database instance
- Auth0 account (for authentication)
- Docker (optional)

## Build & Run

### 1. Build LookseeCore (required dependency)

```bash
cd ../LookseeCore
mvn clean install -DskipTests
cd ../CrawlerAPI
```

### 2. Build

```bash
mvn clean install
```

### 3. Run

```bash
java -ea -jar target/crawlerApi-*.jar
```

The `-ea` flag enables Java assertions used for Design by Contract checks.

## Docker

```bash
docker build -t deepthought42/looksee-api:latest .
docker run -p 9080:9080 -p 80:80 deepthought42/looksee-api:latest
```

The multi-stage Dockerfile uses Maven 3.9.6 + Eclipse Temurin 21 for building and Temurin 21 JRE for runtime. The container starts with `-Xms800M` minimum heap.

### Deploy to Docker Hub

```bash
docker build -t deepthought42/looksee-api:<version> .
docker push deepthought42/looksee-api:<version>
```

## Testing

```bash
mvn test
```

This project uses [JaCoCo](https://www.jacoco.org/) for code coverage reporting. The HTML report is available at `target/site/jacoco/index.html` after tests complete.

### Test Structure

| Package | Description |
|---------|-------------|
| `api/` | Controller and API annotation tests |
| `config/` | Configuration class tests (CORS, Auth0) |
| `generators/` | Field generator tests (String, Integer, Decimal) |
| `integrations/` | Integration framework tests (facade, encryption, providers, factory) |
| `models/` | Domain model tests |
| `security/` | Security configuration and handler tests |
| `service/` | Service layer tests (Auth0Service) |

### Testing Frameworks

- **JUnit 5** (Jupiter) -- test framework
- **Mockito** -- mocking and stubbing
- **JaCoCo** -- code coverage reporting

## CI/CD

Two GitHub Actions workflows:

- **`docker-ci-test.yml`** -- Runs on pull requests to `master`. Builds the project, runs tests, and performs a Docker build.
- **`docker-ci-release.yml`** -- Runs on push to `master`. Runs tests, bumps version via Semantic Release, builds and pushes the Docker image to Docker Hub, and creates a GitHub Release.

## Security

### Authentication

All API endpoints require a valid Auth0 JWT bearer token. Role-based access control is enforced via Spring Security `@PreAuthorize` annotations with scopes such as `read:accounts`, `update:accounts`, `write:domains`, and `read:test_records`.

### SSL

For environments not behind a load balancer or API gateway, generate a PKCS12 certificate:

```bash
openssl pkcs12 -export -inkey private.key -in certificate.crt -out api_key.p12
```

## Deployment (GCP Cloud Run)

The service is deployed via Terraform as a Cloud Run service. See `LookseeIaC/GCP/modules.tf` for the full deployment configuration.

```bash
docker build --no-cache -t gcr.io/<PROJECT_ID>/crawler-api:<version> .
docker push gcr.io/<PROJECT_ID>/crawler-api:<version>
```

## License

This project is licensed under the terms specified in the [LICENSE](LICENSE) file.
