# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.6.1] - 2026-04-24

### Added
- **`BrowsingClient` facade instrumentation.** Every public method on `com.looksee.browsing.client.BrowsingClient` now emits a Micrometer `Timer` named `browser_service_calls` with tags `operation=<method-name>` and `outcome=success|failure`. Failure paths also log a structured warn line (operation + status code + error message) before rethrowing `BrowsingClientException`. See `browser-service/phase-4-consumer-cutover.md` §Observability prereqs for the full metric contract.
- New `BrowsingClient(BrowsingClientConfig, MeterRegistry)` constructor. `BrowsingClientConfiguration` injects the `MeterRegistry` via `ObjectProvider` so consumers without a registry still work — instrumentation becomes a no-op, no NPE.

### Changed
- No behavior change for consumers. Default `looksee.browsing.mode` remains `local`. Consumers without a Micrometer `MeterRegistry` bean see no new metric series.

### Notes
- Phase 3b (element-handle ops) is planned but not yet implemented. This release contains the facade instrumentation from phase 4a.1 only — originally planned as LookseeCore 0.7.1, delivered as 0.6.1 because 0.7.0 (phase 3b code) is still outstanding. When phase 3b lands, the version will bump to 0.7.0 with the instrumentation carried forward.
- Consumer-side `MeterFilter.commonTags("consumer", "<name>")` is each consumer's responsibility. PageBuilder adds this in `com.looksee.pageBuilder.config.BrowsingClientMetricsConfig` (guarded by `@ConditionalOnBean(MeterRegistry.class)`) — same pattern when cutting over additional consumers.

## [0.6.0] - 2026-04-24

### Added
- New `looksee-browsing-client` (`A11yBrowsingClient`) module: generated DTOs + `BrowsingClient` facade for `brandonkindred/browser-service`. OpenAPI spec is consumed from `looksee-browsing-client/src/main/resources/openapi.yaml`; `openapi-generator-maven-plugin` with `<library>native</library>` (JDK 11+ `HttpClient`) keeps Spring/OkHttp/Jersey out of consumer classpaths.
- `looksee.browsing.mode` config flag (`local` | `remote`) bound via `LookseeBrowsingProperties`, with `service-url`, `connect-timeout`, `read-timeout` properties under the same prefix.
- `RemoteBrowser` (in `looksee-core`) — `Browser` subclass that forwards page-level operations (navigate, source, screenshots, viewport, status, close) to browser-service over HTTP via `BrowsingClient`.
- `BrowserService.capturePage(URL, BrowserType, long)` — one-shot page capture. Local mode runs the existing open→navigate→adapt→close sequence; remote mode uses a single `POST /v1/capture` round-trip plus a `GET /v1/capture/{id}/screenshot` fetch.
- `PageStateAdapter.toPageState(byte[], String, long, String)` overload — builds a `PageState` from pre-captured screenshot bytes + source, no live `Browser` required.

### Changed
- `BrowserService.getConnection()` and `TestService.runTest()` now fork on `looksee.browsing.mode`. Default (`local`) behavior is byte-identical to 0.5.0 — existing consumers upgrade with no code change.

### Deferred (phase 3b)
- Element-handle ops in `RemoteBrowser` (findElement, extractAttributes, scroll helpers, removeDriftChat, removeGDPR, isAlertPresent, mouse moves, getElementScreenshot) throw `UnsupportedOperationException`. Consumers that exercise those paths must stay on `looksee.browsing.mode=local` until phase 3b ships.
- Remote-mode `capturePage` stores the same screenshot bytes for both viewport and full-page fields of the resulting `PageState`. Splitting them out is phase-3b work.

### Migration
- Default mode is `local`. Existing consumers pick up 0.6.0 with no behavior change.
- To opt a consumer into remote mode, set in its `application.yml`:

  ```yaml
  looksee:
    browsing:
      mode: remote
      service-url: http://browser-service.internal/v1
  ```

## [0.3.24] - 2026-03-27

### Added
- Comprehensive Design by Contract (DbC) enforcement across all packages:
  - **Service layer**: All 37 service classes now enforce preconditions with `assert` statements and document contracts in Javadoc (`precondition:` comments)
  - **Model/entity classes**: All domain models include class-level `invariant:` documentation and constructor preconditions
  - **Audit models**: AuditRecord, Audit, Score, and all audit message/recommendation classes enforce parameter contracts
  - **Journey models**: Journey, Step, DomainMap and all step types include precondition assertions
  - **DTO classes**: All data transfer objects validate constructor parameters
  - **Utility classes**: BrowserUtils, ColorUtils, ContentUtils, and other utilities enforce input contracts
  - **Browsing classes**: Crawler, ActionFactory, and form/table helpers include precondition checks
  - **GCP classes**: All PubSub publishers, CloudVisionUtils, CloudNLPUtils, and GoogleCloudStorage enforce contracts
  - **Rule classes**: Rule, RuleFactory, and all rule implementations validate inputs
  - **Message classes**: All inter-service message types include constructor preconditions
- Fixed constructor parameter assignment bugs in AuditRecord (startTime and aestheticAuditProgress were assigned wrong values)
- New `docs/DESIGN_BY_CONTRACT.md` documenting the project's DbC conventions and patterns

### Changed
- Updated README.md with Design by Contract section and updated version to 0.3.23
- Updated CONTRIBUTING.md with DbC guidelines for contributors
- LookseeObject base class now enforces key non-null precondition in parameterized constructor

## [0.3.22] - 2026-03-25

### Added
- Comprehensive test suite covering all major packages:
  - All 37 enum classes with factory method, toString, round-trip, and edge case tests
  - Model/entity classes (Account, Domain, ColorData, Screenshot, Group, Animation, etc.)
  - Extended model classes (Label, LatLng, SimpleElement, SimplePage, Template, etc.)
  - All 16 exception classes with message and HTTP status annotation verification
  - All DTO classes (AuditUpdateDto, Subscription, PageBuiltMessage, UXIssueReportDto, etc.)
  - Audit model hierarchy (Audit, AuditRecord, PageAuditRecord, DomainAuditRecord, Score, etc.)
  - Audit message and recommendation classes
  - Audit statistics classes
  - Rule classes and RuleFactory with all rule type builds
  - Journey model classes (Journey, SimpleStep, LoginStep, LandingStep, Redirect)
  - Message classes for inter-service communication
  - Browsing package classes (Coordinates, ElementNode, ValueDomain, FormField)
  - Competitive analysis and design system model classes
  - Mapper classes (Body, Base64 deserializers)
  - Configuration property classes (LookseeCoreProperties, PusherProperties, SeleniumProperties)
  - VS Code plugin classes (Tree, TreeNode, SessionTestTracker, TestMapper)
  - Utility classes (AuditUtils, ColorUtils, ColorPaletteUtils, JourneyUtils, ListUtils)
  - Service layer tests with Mockito mocking (AuditService)

### Changed
- Updated README.md with current version (0.3.22), comprehensive test documentation, and project structure
- Updated CONTRIBUTING.md with development guidelines, testing instructions, and code standards
- Updated CHANGELOG.md with project history

## [0.3.21] - Previous

### Added
- Expanded utility and enum test coverage
- Enum factory and timing utility unit tests

## [0.3.0] - Earlier

### Added
- Spring Boot auto-configuration for repositories and services
- Modular Pusher configuration with fallback support
- Conditional Pub/Sub publisher creation
- Selenium WebDriver configuration with properties
- MessageBroadcaster guaranteed availability pattern

### Fixed
- PageService.saveForUser scoped to user-specific records
- PageService.findLatestInsight null-safe returns
- PageService.addPageState guards against missing pages
- Removed debug System.out.println statements
