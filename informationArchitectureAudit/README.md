# Information Architecture Accessibility Audit

A Spring Boot service that runs page-level information architecture and accessibility audits (WCAG-oriented checks) against stored `PageState` data and emits completion updates.

## What this service does

When a Pub/Sub message is received, the service:

1. Decodes a `PageAuditMessage` payload.
2. Loads the corresponding `AuditRecord` and `PageState`.
3. Executes a set of IA/accessibility audits (headers, tables, forms, language, links, metadata, etc.).
4. Persists generated `Audit` records.
5. Publishes an `AuditProgressUpdate` marking completion.

Primary entrypoint: `AuditController` (`POST /`).

## Design by Contract

This codebase follows **Design by Contract (DbC)** principles throughout all audit implementations. Every public and internal method documents and enforces its contract:

### Preconditions

Preconditions are enforced at runtime using `Objects.requireNonNull()` (for null checks) and `IllegalArgumentException` (for value constraints). This ensures that invalid inputs are caught immediately with clear error messages, regardless of JVM assertion settings.

```java
Objects.requireNonNull(page_state, “page_state must not be null”);
Objects.requireNonNull(audit_record, “audit_record must not be null”);
```

### Postconditions

Each `execute()` method validates that the constructed `Audit` object is non-null before persisting:

```java
Objects.requireNonNull(audit, “Postcondition failed: audit must not be null”);
return auditService.save(audit);
```

### Class Invariants

Every audit class documents its invariant in the class-level Javadoc, specifying which `@Autowired` dependencies must be non-null after Spring construction:

```java
/**
 * <p><b>Class invariant:</b> All {@code @Autowired} dependencies ({@code auditService},
 * {@code elementStateService}) are non-null after Spring construction.</p>
 */
```

### Javadoc Contract Tags

All methods use `@pre` and `@post` tags to formally document their contracts:

```java
/**
 * @pre {@code page_state != null}
 * @pre {@code audit_record != null}
 * @post returned {@code Audit} is non-null and persisted
 */
```

## Audit classes

### Active audits (in `audits/`)

| Class | WCAG Section | Purpose |
|-------|-------------|---------|
| `HeaderStructureAudit` | 1.3.1 | H1 validation and heading hierarchy |
| `TableStructureAudit` | 1.3.1 | Table header and scope attribute checks |
| `FormStructureAudit` | 3.3.2 | Form labeling and fieldset structure |
| `OrientationAudit` | 1.3.4 | Orientation restriction detection |
| `InputPurposeAudit` | 1.3.5 | Autocomplete and ARIA label compliance |
| `IdentifyPurposeAudit` | 1.3.6 | Programmatic purpose identification |
| `UseOfColorAudit` | 1.4.1 | Color-only information conveyance |
| `AudioControlAudit` | 1.4.2 | Autoplay audio/video controls |
| `VisualPresentationAudit` | 1.4.8 | Color, font, justification, spacing |
| `ReflowAudit` | 1.4.10 | Responsive layout and reflow |
| `TextSpacingAudit` | 1.4.12 | Line height, letter/word/paragraph spacing |
| `PageLanguageAudit` | 3.1.1 | HTML lang attribute validation |
| `LinksAudit` | Navigation | Link href, URL validity, text quality |
| `TitleAndHeaderAudit` | SEO | Page titles, favicons, heading structure |
| `SecurityAudit` | Security | HTTPS/SSL validation |
| `MetadataAudit` | SEO | Title, meta description, refresh tags |

### In-development audits (in `models/`)

| Class | WCAG Section | Purpose |
|-------|-------------|---------|
| `InputLabelAudit` | 3.3.2 | Input label association |
| `KeyboardAccessibleAudit` | 2.1.1 | Keyboard navigation and focus |
| `ListStructureAudit` | 1.3.1 | List structure (ul/ol with li children) |
| `WcagEmphasisComplianceAudit` | 1.3.1 | Semantic emphasis tags |

## Local development

### Prerequisites

- Java 17+
- Maven 3.9+
- Access to Maven Central (or an internal mirror/proxy)

### Run tests

```bash
mvn test
```

> Note: In restricted environments, dependency resolution can fail (e.g., HTTP 403 from Maven Central).

### Run the app

```bash
mvn spring-boot:run
```

## Configuration

Main configuration files:

- `src/main/resources/application.properties`
- `src/main/resources/application.yml`

Typical values to configure:

- `server.port`
- datasource settings (`spring.datasource.*`)
- logging levels (`logging.level.*`)

## Deployment to GCP (Cloud Run)

1. Build container:

```bash
docker build -t gcr.io/[PROJECT-ID]/ia-accessibility-audit:v1 .
```

2. Push image:

```bash
docker push gcr.io/[PROJECT-ID]/ia-accessibility-audit:v1
```

3. Deploy:

```bash
gcloud run deploy ia-accessibility-audit \
  --image gcr.io/[PROJECT-ID]/ia-accessibility-audit:v1 \
  --platform managed \
  --region [REGION] \
  --allow-unauthenticated
```

Replace `[PROJECT-ID]` and `[REGION]` with your values.

## Contributing

Please follow `CONTRIBUTING.md` and include:

- tests for behavior changes
- clear commit messages
- updated documentation when audit behavior changes

## License

See `LICENSE`.
