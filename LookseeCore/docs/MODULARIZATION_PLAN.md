# LookseeCore Modularization Analysis

## Context

LookseeCore (`com.looksee:core`, 381 Java files) is a monolithic shared library that serves as the foundation for the entire accessibility audit platform. Every microservice in the system depends on it, but most services only use a fraction of its capabilities. This creates problems:

- **Heavy dependency**: Services pull in Selenium, Stanford NLP, GCP Vision, Neo4j, Pusher, etc. even when they only need models and enums
- **Version skew**: Different services depend on different LookseeCore versions (0.3.1 to 0.4.1)
- **Tight coupling**: Changes to any part of LookseeCore force a new release that all consumers must adopt
- **Slow builds**: Every consumer compiles against the entire library

## Critical Blocker: Model-to-Service Coupling

Before any modularization can proceed, the following architectural violations must be fixed. Domain models currently depend on services and infrastructure, creating circular dependencies that would prevent clean library extraction.

1. **`PageState.getSrc()`** calls `googleCloudStorage.getHtmlContent()` and `setSrc()` calls `googleCloudStorage.uploadHtmlContent()`. The model has an `@Autowired GoogleCloudStorage` field. **Fix**: Make `src` a plain field getter/setter; move GCS read/write logic to `PageStateService`.

2. **`PageState` constructor** calls `BrowserService.generalizeSrc(src)` and `BrowserService.extractHost(urlString)`. **Fix**: Extract these static string/HTML transformation methods into a new `HtmlGeneralizer` utility class in the models package.

3. **`ElementState.generateKey()`** calls `BrowserService.generalizeSrc()`. **Fix**: Same as above - use `HtmlGeneralizer`.

4. **`Page`** imports `BrowserService` for the same static utility methods. **Fix**: Same as above.

5. **`BrowserService`** is ~1800 lines and mixes static utilities with service logic. **Fix**: Decompose into (a) `HtmlGeneralizer` for static methods called by models, (b) element extraction service, (c) page capture service, (d) screenshot service.

These fixes can all be done within the existing monolith before any library extraction begins.

### Key files requiring Phase 0 refactoring:
- `LookseeCore/src/main/java/com/looksee/models/PageState.java`
- `LookseeCore/src/main/java/com/looksee/models/ElementState.java`
- `LookseeCore/src/main/java/com/looksee/models/Page.java`
- `LookseeCore/src/main/java/com/looksee/services/BrowserService.java`
- `LookseeCore/src/main/java/com/looksee/config/LookseeCoreAutoConfiguration.java`
- `LookseeCore/src/main/java/com/looksee/config/LookseeCoreComponentConfiguration.java`

## Additional Finding: Forked Consumers

About half the consumer modules have **copied/forked LookseeCore source code** into their own packages rather than using it as a Maven dependency:
- `element-enrichment` (100 files)
- `look-see-front-end-broadcaster` (91 files)
- `journeyErrors` (54 files)
- `journey-map-cleanup` (15 files)

These forks may have diverged from the canonical LookseeCore. Before these modules can consume the new sub-libraries, their duplicated code must be reconciled and replaced with proper Maven dependencies.

## Current Package Structure (381 files)

| Package | Files | Responsibility |
|---------|-------|----------------|
| `models/` | 233 | Domain models, enums, DTOs, repositories, audit models, messages, journeys, rules |
| `services/` | 37 | Business logic (audit, page, element, account, domain, journey, browser services) |
| `utils/` | 23 | Helpers (browser, color, image, HTML, CSS, form, audit utilities) |
| `audits/` | 19 | PageSpeed audit detail models (not actual audit implementations) |
| `gcp/` | 17 | Cloud Vision, NLP, Storage, PageSpeed, Pub/Sub publishers |
| `browsing/` | 16 | Selenium automation (Crawler, BrowserFactory, form extraction) |
| `exceptions/` | 16 | Custom exceptions |
| `config/` | 9 | Spring auto-configuration |
| `mapper/` | 5 | Data mappers |
| `vscodePlugin/` | 5 | VS Code plugin integration |
| `integrations/` | 1 | External integrations |

## Proposed Modularization: 7 Libraries

### 1. `looksee-models` (Foundation Layer)
**~120 files** | Every service depends on this

Contains:
- `models/enums/` (37 files) - All enum types (AuditCategory, AuditName, BrowserType, ElementClassification, WCAGComplianceLevel, ExecutionStatus, etc.)
- Core domain models: `PageState`, `ElementState`, `Element`, `Domain`, `Account`, `Browser`, `Form`, `Label`, `Screenshot`, `Template`, `Page`, `SimplePage`, `SimpleElement`, `HeaderNode`, `Group`, etc.
- `models/dto/` (18 files) - All DTOs
- `models/journeys/` (8 files) - Step, LoginStep, journey models
- `models/designsystem/` (2 files) - DesignSystem, color models
- `models/competitiveanalysis/` (4 files) - Competitive analysis models
- `models/rules/` (13 files) - Rule definitions
- `exceptions/` (16 files) - All custom exceptions
- `models/audit/` core types only: `Audit`, `AuditRecord`, `AuditScore`, `AuditStats`, `UXIssueMessage` (the model classes, not PageSpeed details)
- Base class: `LookseeObject`, `Persistable`
- Color models: `ColorData`, `ColorUsageStat`, `PaletteColor`, `CIEColorSpace`, `XYZColorSpace`
- Image models: `ImageElementState`, `ImageFaceAnnotation`, `ImageLandmarkInfo`, `ImageSearchAnnotation`, `ImageSafeSearchAnnotation`, `Logo`
- `models/serializer/` (1 file)

**Dependencies**: None (pure POJOs, enums, Neo4j OGM annotations only)

**Consumers**: ALL services

---

### 2. `looksee-persistence` (Data Access Layer)
**~85 files**

Contains:
- `models/repository/` (45 files) - All Neo4j Spring Data repositories
- `services/` (37 files) - All service classes (AccountService, AuditService, PageStateService, ElementStateService, DomainService, BrowserService, JourneyService, etc.)
- `mapper/` (5 files) - Data mappers

**Dependencies**: `looksee-models`, Spring Data Neo4j, Neo4j OGM

**Consumers**: Services that need database access (CrawlerAPI, element-enrichment, front-end-broadcaster, journeyErrors, journey-map-cleanup, audit services)

---

### 3. `looksee-browser` (Browser Automation Layer)
**~40 files**

Contains:
- `browsing/` (16 files) - Crawler, BrowserFactory, BrowserConnectionHelper, ElementNode, ActionFactory, ActionHelper, FormField, ElementRuleExtractor, Table, Row, etc.
- `utils/BrowserUtils.java` - Browser utility functions
- `utils/ElementStateUtils.java` - Element analysis helpers
- `utils/HtmlUtils.java` - HTML parsing utilities
- `utils/CssUtils.java` - CSS processing utilities
- `utils/FormUtils.java` - Form field analysis
- `utils/PathUtils.java` - XPath/selector utilities

**Dependencies**: `looksee-models`, Selenium WebDriver, jStyleParser, CSSParser, xSoup

**Consumers**: CrawlerAPI, PageBuilder, element-enrichment

---

### 4. `looksee-messaging` (Messaging & Events Layer)
**~35 files**

Contains:
- `models/message/` (30 files) - All message types and Pub/Sub publisher implementations (PubSubAuditUpdatePublisherImpl, PubSubPageAuditPublisherImpl, PubSubErrorPublisherImpl, PubSubJourneyVerifiedPublisherImpl, etc.)
- `services/MessageBroadcaster.java` - Pusher real-time messaging
- `config/` related to Pusher and messaging auto-configuration (PusherProperties, MessageBroadcasterAutoConfiguration)

**Dependencies**: `looksee-models`, Spring Cloud GCP Pub/Sub, Pusher

**Consumers**: CrawlerAPI, audit-service, front-end-broadcaster, journey services

---

### 5. `looksee-gcp` (Cloud Services Layer)
**~20 files**

Contains:
- `gcp/CloudVisionUtils.java` - Image recognition
- `gcp/CloudNLPUtils.java` - Natural language processing
- `gcp/GoogleCloudStorage.java` + `GoogleCloudStorageProperties.java` - Cloud storage
- `gcp/PageSpeedInsightUtils.java` - PageSpeed Insights API
- `gcp/SyntaxAnalysis.java` - Syntax analysis
- `gcp/ImageSafeSearchAnnotation.java` - Safe search (if not already in models)
- `audits/` (19 files) - PageSpeed audit detail models (AssetSize, BootUpTime, DomSize, NetworkRequestDetail, etc.)
- `utils/ImageUtils.java` - Image processing utilities
- `utils/ColorUtils.java` - Color analysis utilities

**Dependencies**: `looksee-models`, Google Cloud Vision, Google Cloud Language, Google Cloud Storage, Google PageSpeed API

**Consumers**: CrawlerAPI, visualDesignAudit, element-enrichment, contentAudit

---

### 6. `looksee-nlp` (Text Analysis Layer)
**~5 files**

Contains:
- `utils/ContentUtils.java` - Content analysis
- Any Stanford CoreNLP wrapper classes
- Readability analysis utilities (Flesch-Kincaid integration)

**Dependencies**: `looksee-models`, Stanford CoreNLP, Whelk Flesch-Kincaid

**Consumers**: contentAudit, informationArchitectureAudit

---

### 7. `looksee-utils` (Shared Utilities)
**~15 files**

Contains remaining utilities that don't fit elsewhere:
- `utils/AuditUtils.java`
- `utils/TimingUtils.java`
- `utils/NetworkUtils.java`
- `utils/ListUtils.java`
- `utils/LabelSetsUtils.java`
- `utils/PageUtils.java`
- `utils/JourneyUtils.java`
- `utils/ColorPaletteUtils.java`
- `config/` remaining configuration classes
- `vscodePlugin/` (5 files)
- `integrations/` (1 file)

**Dependencies**: `looksee-models`

**Consumers**: Various services as needed

---

## Dependency Graph

```
                    looksee-models  (foundation - no deps)
                   /   |   |   \    \      \
                  /    |   |    \    \      \
    looksee-persistence |  |  looksee-utils  looksee-nlp
                  |    |   |        |
                  | looksee-messaging |
                  |    |             |
             looksee-browser    looksee-gcp
```

All libraries depend on `looksee-models`. No circular dependencies.

## Consumer Dependency Map

| Consumer Service | models | persistence | browser | messaging | gcp | nlp | utils |
|-----------------|--------|-------------|---------|-----------|-----|-----|-------|
| CrawlerAPI | x | x | x | x | x | | x |
| element-enrichment | x | x | x | | x | | x |
| front-end-broadcaster | x | x | | x | | | x |
| journeyErrors | x | x | | x | | | x |
| informationArchitectureAudit | x | x | | | | x | x |
| contentAudit | x | x | | | | x | x |
| visualDesignAudit | x | x | | | x | | x |
| journey-map-cleanup | x | x | | | | | x |
| AuditManager | x | x | | x | | | |
| audit-service | x | x | | x | | | |
| PageBuilder | x | x | x | | | | |
| journeyExecutor | x | x | | x | | | |
| journeyExpander | x | x | | x | | | |

Key win: services like `contentAudit` and `informationArchitectureAudit` no longer pull in Selenium, GCP Vision, or Pusher.

## Migration Strategy

### Phase 0: Fix Architectural Violations (prerequisite)
- Extract `generalizeSrc()` and `extractHost()` from `BrowserService` into a new `HtmlGeneralizer` utility
- Remove `@Autowired GoogleCloudStorage` from `PageState`; make `src` a plain field; move GCS logic to `PageStateService`
- Update `ElementState.generateKey()` and `Page` to use `HtmlGeneralizer`
- All changes within the existing monolith - no consumer changes required

### Phase 1: Extract `looksee-models`
- Move enums, domain models, DTOs, exceptions into new module
- This is the safest first step since models have no dependencies on services/utils
- All other LookseeCore packages import from models, not the other way around

### Phase 2: Extract `looksee-persistence`
- Move repositories and services
- These depend only on models + Spring Data Neo4j

### Phase 3: Extract `looksee-messaging`
- Move message types and Pub/Sub publishers
- Depends only on models + Spring Cloud GCP

### Phase 4: Extract remaining libraries in parallel
- `looksee-browser` (Selenium deps)
- `looksee-gcp` (Cloud API deps)
- `looksee-nlp` (Stanford NLP deps)
- `looksee-utils` (remaining utilities)

### Phase 5: Backward-compatible `looksee-core` umbrella
- Create a transitional `looksee-core` that re-exports all sub-libraries
- Consumers migrate at their own pace
- Eventually remove the umbrella dependency

### Phase 6: Reconcile forked consumers
- Diff each forked consumer's model classes against canonical LookseeCore to identify drift
- Upstream any intentional feature additions
- Replace duplicated code with proper Maven dependencies on new sub-libraries
- This is the hardest phase - forks may have diverged intentionally

## Maven Structure

Convert to a multi-module reactor build:
```xml
<modules>
  <module>looksee-models</module>
  <module>looksee-persistence</module>
  <module>looksee-gcp</module>
  <module>looksee-browser</module>
  <module>looksee-messaging</module>
  <module>looksee-nlp</module>
  <module>looksee-utils</module>
  <module>looksee-core</module>  <!-- backward compat umbrella -->
</modules>
```

Each sub-library should have its own Spring Boot auto-configuration class registered in `META-INF/spring.factories`, replacing the current blanket `@ComponentScan` in `LookseeCoreComponentConfiguration`.

## Verification

- Each new library should compile independently
- Run existing tests in each consumer service after switching dependencies
- Verify no circular dependencies between new modules
- Check that each consumer's classpath is smaller than before

---

## Consolidation Completed (April 2026)

Four modules that previously maintained forked copies of core code have been migrated to use `A11yCore`:

| Module | Files Removed | Now Uses |
|--------|--------------|----------|
| `element-enrichment` | ~178 | A11yCore 0.5.0 |
| `journeyErrors` | ~121 | A11yCore 0.5.0 |
| `look-see-front-end-broadcaster` | ~144 | A11yCore 0.5.0 |
| `journey-map-cleanup` | ~78 | A11yCore 0.5.0 |

This removed ~51,000 lines of duplicated models, enums, utilities, services, repositories, and GCP integration code. All four modules were also upgraded from Java 8 to Java 17.

Methods that only existed in the local forks were added to the core:
- `DomainMapRepository.getAllMapsWithinLastDay()` / `DomainMapService.getAllMapsWithinLastDay()`
- `JourneyRepository.getDomainMapJourneys()` / `JourneyService.getDomainMapJourneys()`
- `JourneyRepository.changeJourneyStatus()` / `JourneyService.changeJourneyStatus()`
- `PageStateDto(long auditRecordId, PageState)` constructor
