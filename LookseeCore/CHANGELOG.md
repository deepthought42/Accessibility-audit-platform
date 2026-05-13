# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

## [1.1.0] - 2026-05-13

### Added

- **`OutboxPublishingGateway`** in `looksee-persistence` — the single entry point through which every cross-service Pub/Sub publish must now flow. Two methods: `enqueue(topic, payload, correlationId)` runs `@Transactional(Propagation.MANDATORY)` so the outbox row commits or rolls back with the caller's domain transaction; `enqueueOutOfBand(...)` runs `Propagation.REQUIRES_NEW` for `catch`-block error publishes whose signal must survive a rolling-back outer tx. Eliminates the "DB committed but Pub/Sub publish failed" failure mode that previously stranded audits mid-pipeline.
- **`OutboxEventStatus` constants enum** in `looksee-models` — `PENDING`, `PROCESSED`, `FAILED`. Used as a constants source at write/read sites; the `OutboxEvent.status` getter still returns `String`, so the LookseeCore 1.0.0 public API is preserved.
- **`OutboxSerializationException`** in `looksee-models` — unchecked wrapper around `JsonProcessingException` thrown by the gateway when a payload cannot be serialized.
- **`OutboxEvent.correlationId` + `nextAttemptAt` fields** — the correlation id holds the inbound W3C `traceparent` (or a freshly minted one) so downstream consumers join the same distributed trace; `nextAttemptAt` powers the new exponential-backoff retry schedule.
- **`OutboxEventRepository.findDueEvents(now)` / `countDueEvents(now)` / `deleteOldFailedEvents()`** — `findDueEvents` honors `nextAttemptAt` so the publisher respects backoff; `countDueEvents` backs the new pending-gauge (a non-paged true count, not the limit-100 page size); `deleteOldFailedEvents` sweeps terminal-failure rows 30 days after they flip to `FAILED`.
- **Hardened `OutboxEventPublisher`** — replaces `PubSubTemplate` injection with `TracingPubSubPublisher`; reconstructs the originator's W3C trace context from `event.correlationId` so consumers see lineage rather than the scheduler thread's span; applies exponential backoff (`5 · 2^retryCount` seconds, 10s/20s/40s/80s) on failure; flips `PENDING → FAILED` with a `processedAt` stamp on the 5th failed attempt so the daily sweeper can reap exhausted rows.
- **`TracingPubSubPublisher.publishWithCorrelation(topic, payload, traceparent)`** in `looksee-messaging` — publishes with an explicit W3C `traceparent` as the propagated parent context. Falls back to the existing `publish(topic, payload)` path when the input is null or syntactically invalid.
- **`TraceContextPropagation.currentTraceparent()` and `currentOrMintTraceparent(inboundAttributes)`** helpers — handlers extending `PubSubAuditController` (with an active `pubsub.handle.*` span) use the former; raw handlers without a span use the latter, which prefers the inbound `traceparent` attribute when present and otherwise mints a freshly-random, syntactically-valid W3C id.
- **Outbox metrics** in `PubSubMetrics`: `looksee.outbox.published{topic,result}` (counter, results `success`/`retrying`/`exhausted`), `looksee.outbox.lag.seconds{topic}` (timer, write-to-publish latency), `looksee.outbox.failed{topic,reason}` (counter, reasons `pubsub_unavailable`/`serialization`/`exhausted`/`unknown`), and `looksee.outbox.pending` (gauge supplied by `countDueEvents`).
- **`PubSubPublisher.getTopic()`** public accessor on the GCP publisher base class — migrated outbox call sites read the topic name from this rather than hardcoding strings, so the config-driven `pubsub.*` property remains the single source of truth.

### Changed

- The four producer-side `AuditController` classes (`PageBuilder`, `journeyExecutor`, `journeyExpander`, `AuditManager`) no longer call `PubSub*PublisherImpl.publish(...)` directly. Every cross-service publish now stages an `OutboxEvent` via `OutboxPublishingGateway` in the same Neo4j transaction as the domain write. `AuditManager.handle(...)` gained `@Transactional` so the gateway's `MANDATORY` propagation is satisfied. The publisher beans remain wired for their `getTopic()` accessor and for non-migrated services elsewhere in the monorepo.

### Fixed

- Cross-service publish atomicity: a graph write followed by a failed Pub/Sub publish no longer leaves the audit stranded. The outbox row commits or rolls back with the domain change; the asynchronous publisher delivers (or fails after 5 retries → `FAILED`) on its own schedule.
- Outbox events that exhaust retries now transition `PENDING → FAILED` with a `processedAt` stamp, so the daily sweeper (`deleteOldFailedEvents`) can clean them. Previously the scaffold set `status = "FAILED"` but never `processedAt`, leaving exhausted rows in the table indefinitely.
- `OutboxEventPublisher` previously polled every 5 seconds regardless of failure count, hammering Pub/Sub with retries. It now honors `nextAttemptAt` and spaces retries at 10s / 20s / 40s / 80s.

## [1.0.0] - 2026-05-12

### Breaking changes

- **`IdempotencyService` now fails startup if `ProcessedMessageRepository` is missing on the classpath** (PR #95, closes #85). The previous silent-fallback behavior — null repository → every dedupe call no-ops → at-least-once delivery with no upper bound — has been replaced with a `@PostConstruct` fail-fast throwing `IllegalStateException`. **Migration:** services that consume `IdempotencyService` or extend `PubSubAuditController` must depend on `looksee-persistence`. Any service that was relying on the silent disablement must either add the persistence module or remove its `IdempotencyGuard` wiring.

### Added

- **Atomic `IdempotencyService.claim()` via Cypher `MERGE`** (PR #91, closes #82). Collapses the prior `isAlreadyProcessed` + `markProcessed` pair into a single atomic operation backed by the new `processed_message_unique` constraint. Returns `true` to exactly one of N concurrent callers per `(pubsubMessageId, serviceName)` key.
- **`neo4j-migrations` Spring Boot starter** (1.16.0, PR #90, closes #81). Schema changes now ship as versioned Cypher files under `looksee-persistence/src/main/resources/neo4j/migrations/`:
  - `V001__dedupe_processed_message.cypher` — one-shot dedupe of pre-existing `(pubsubMessageId, serviceName)` collisions.
  - `V002__processed_message_unique.cypher` — uniqueness constraint that enables the atomic `claim()`.
- **`PubSubAuditController` wired through atomic `claim()`** (PR #92, closes #83). `IdempotencyGuard.claim()` replaces the two-step check-then-write in the base controller; `journeyErrors` (PR #93, closes #84) and `AuditManager` (PR #94) now extend `PubSubAuditController` and have dropped their in-memory `ConcurrentHashMap` deduplicators.

### Changed

- `IdempotencyService` retention extended from 3 to 8 days; cleanup keys off `ProcessedMessage.processedAt` (PR #96, closes #86). Pub/Sub retains undelivered messages for 7 days, so 8 days ensures a service offline for the full window still finds its dedupe record on recovery. **Operational note:** services down longer than 8 days may reprocess messages.

### Tests

- 100-thread concurrency proof for `claim()` (`IdempotencyServiceConcurrencyTest`), two-replica redelivery integration test (`IdempotencyIntegrationTest`), and V001+V002 migration rehearsal (`MigrationsApplyIntegrationTest`) — all gated by the `requires-docker` JUnit tag so plain `mvn test` runs without Docker (PR #97, closes #87).

## [0.8.2] - 2026-04-26

### Added
- **`CapturePageSmokeCheck` Spring bean** in `LookseeCore/looksee-core/src/main/java/com/looksee/services/health/`. Periodically calls `browserService.capturePage` against the configured target URL and reports outcome via Micrometer counter `browser_service_smoke_checks{outcome=success|failure}` per the phase-4 metric contract. Doubles as a watchdog during phase-4 staging burn-in (4a.5) and prod cutover (4a.6).
  - Disabled by default. Opt in per-consumer with `looksee.browsing.smoke-check.enabled=true`. The bean isn't even instantiated when off — zero runtime overhead.
  - Implements `SchedulingConfigurer` + `PeriodicTrigger` rather than using `@Scheduled(fixedRateString=...)`. PR #56 review caught that an `@Scheduled` SpEL bean reference would fail to resolve at startup against the `@EnableConfigurationProperties`-bound `LookseeBrowsingProperties` bean (which has a non-camelCase name) — the SchedulingConfigurer pattern reads the bound `Duration` directly via constructor injection.
  - Catches `Throwable` in the probe so a transient failure can't poison the scheduler thread.
  - `MeterRegistry` is optional via `ObjectProvider.getIfAvailable()` — consumers without a registry see no-op metrics, no NPE.
- **`LookseeBrowsingProperties.smokeCheck`** sub-properties: `enabled` (default false), `interval` (default 60s `Duration`), `target-url` (default `https://example.com`), `browser` (default `CHROME`).

### Changed
- No behavior change for opted-out consumers (the default).

### State of phase-4 prep
**The LookseeCore-side preparation for phase-4 cutover is complete.** Every executable LookseeCore deliverable from `browser-service/phase-4-consumer-cutover.md` has shipped:
- 4a.1 — `BrowsingClient` Micrometer instrumentation (0.6.1)
- 4a.3 — `looksee.browsing.*` env-var plumbing (0.6.1)
- 4a.4 — `CapturePageSmokeCheck` watchdog bean (this release)
- The entire 3-series remote-compat work (3 → 3b → 3c → 3d → 3e → 3f) that was a phase-4 prerequisite.

The remaining phase-4 work — 4a.5 staging flip, 4a.6 prod flip, 4b element-enrichment cutover, 4c journeyExecutor cutover — is gated entirely on **browser-service deployment** + the infra-repo dashboard / alert rules. Both live outside this repo. Recommend pausing LookseeCore-side phase work and pivoting to those external blockers.

## [0.8.1] - 2026-04-25

### Added
- **`RemoteWebElement` now implements 9 of the 11 phase-3c-deferred WebElement methods** — `click`, `submit`, `sendKeys`, `clear`, `getText`, `isSelected`, `isEnabled`, `getCssValue`, `getScreenshotAs(OutputType.BYTES)`. All route through facades that already exist (`performElementAction`, `executeScript`, `captureElementScreenshot`) — no new browser-service contract change required.
  - `click` and `sendKeys` use the dedicated `/v1/sessions/{id}/element/action` endpoint (same path `Browser.performClick`/`performAction` already use).
  - `submit`, `clear`, `getText`, `isSelected`, `isEnabled`, `getCssValue` use `/v1/sessions/{id}/execute` with literal JS snippets.
  - `clear` dispatches a synthetic `input` event after setting `value=""` for React/Vue/etc. framework parity (Selenium's local `clear()` fires the event via WebDriver routing).
  - `getScreenshotAs` only implements `OutputType.BYTES` — other output types throw with a "convert client-side" pointer (BASE64 = one Base64 encoder call from BYTES; FILE = one `Files.write`).

### Changed
- `RemoteWebElement` constructor grows a `BrowsingClient` parameter so each element can route its own WebElement-API calls without needing a parent `RemoteBrowser` reference. Existing 2-arg and 3-arg constructors stay (without a client = wired methods throw with a clear "constructed without BrowsingClient" pointer).
- `RemoteBrowser.findElement` passes the `BrowsingClient` through to the constructed element. End-to-end: every element returned from `remote.findElement(xpath)` carries a working facade ref.
- No behavior change in local mode. Default `looksee.browsing.mode` remains `local`.

### Still deferred (phase 3g — trigger on demand)
- `RemoteWebElement.findElement(By)` and `findElements(By)` — both need either client-side xpath composition or a new `/v1/sessions/{id}/element/{handle}/find-children` server endpoint.
- `BrowserService` form extraction (lines 3339, 3340, 3354, 3433, 3449)
- `com.looksee.browsing.table.Table` helper

### State of phase-4 prep
After this release, **the LookseeCore-side prep for phase 4 is functionally complete**. Every code path the active consumer set (PageBuilder, element-enrichment, journeyExecutor) exercises works in remote mode, plus 9 of 11 standard WebElement methods on `RemoteWebElement` for any future consumer. The remaining work to enable production cutover (4a.5 staging flip, 4a.6 prod flip, 4b/4c) is **browser-service deployment** — the external prerequisite enumerated in `browser-service/phase-4-consumer-cutover.md` §Prerequisites.

## [0.8.0] - 2026-04-25

### Added
- **`Browser.waitForElementClickable(WebElement, Duration)`** — local body uses Selenium's `WebDriverWait` + `ExpectedConditions.elementToBeClickable`. `RemoteBrowser` overrides it with a 250ms-cadence client-side poll of `client.findElement` for the `displayed` flag — no new browser-service endpoint needed.
- **`Browser.getComputedCssProperties(WebElement)`** — local body delegates to the existing `CssUtils.loadCssProperties(element, driver)`. `RemoteBrowser` overrides it by running `window.getComputedStyle()` server-side via the new `BrowsingClient.executeScript` facade method.
- **`BrowsingClient.executeScript(sessionId, script, args)`** — wraps the previously-unused `POST /v1/sessions/{id}/execute` endpoint. Phase-3b §14.3 deferred this; phase 3e wires it up. **`script` must be a literal — never user-controlled input** to avoid script-injection on the remote browser. Element references pass via `Map.of("element_handle", ...)` per the OpenAPI contract.
- New 3-arg `RemoteWebElement` constructor `(sessionId, sourceXpath, state)` — the existing 2-arg form delegates with `sourceXpath=null`. Lets `waitForElementClickable` re-issue `client.findElement` to refresh the displayed flag during polling.

### Changed
- **`BrowserService.enrichElementState`** is remote-safe — three reach-throughs migrated to the new `Browser` methods (`browser.findElement` at line 1942, `browser.waitForElementClickable` at line 1986, `browser.getComputedCssProperties` at lines 1992 + 2499). Both `enrichElementStates` (used by element-enrichment) and the 6-arg `getDomElementStates` (used by journeyExecutor's `buildPage`) now run end-to-end against a `RemoteBrowser`.
- No behavior change in local mode. Default `looksee.browsing.mode` remains `local`.

### Unblocks
- **Phase 4b** — element-enrichment cutover becomes a config-only flip.
- **Phase 4c** — journeyExecutor cutover becomes a config-only flip.

The remote-mode flip is still gated on browser-service deployment for both — same external prerequisite as 4a.

### Still deferred (phase 3f)
- `BrowserService` form extraction (lines 3339, 3340, 3354, 3433, 3449)
- `com.looksee.browsing.table.Table` helper (3 sites)
- Remaining `RemoteWebElement` unsupported `WebElement` methods (10 methods: click, submit, sendKeys, clear, isSelected, isEnabled, getText, findElement(s), getCssValue, getScreenshotAs)

Phase 3f triggers when a phase-4 cutover surfaces a blocker.

## [0.7.2] - 2026-04-24

### Added
- New static helper `BrowserService.extractTagFromXpath(String)` — derives an HTML tag from the last segment of an xpath (handles indexed predicates, attribute predicates, namespace prefixes, nested paths). Mode-agnostic; no driver round-trip.
- `RemoteWebElement.getTagName()` now reads from a cached `tag_name` attribute when the server's `/v1/sessions/{id}/element/find` response includes one. Falls back to a clearer `UnsupportedOperationException` that points at both the server-side synthesis option (phase 3e) and the xpath-derived workaround.

### Changed
- `BrowserService.getDomElementStates` is remote-safe:
  - Line 454 `browser.getDriver().getCurrentUrl()` → `browser.getCurrentUrl()`.
  - Line 471 `web_element.getTagName()` → `extractTagFromXpath(xpath)` for the structure-tag filter.
  - The deprecated `isImageElement(WebElement)` overload (only callers were two `web_element.getTagName()` consumers in this method) is removed; both call sites now use the existing `isImageElement(String)` overload with `extractTagFromXpath(xpath)`.
- No behavior change in local mode. Default `looksee.browsing.mode` remains `local`.

### Unblocks
- Phase 4a.2 — PageBuilder `buildPageState` → `capturePage` migration is now **fully** unblocked. The single-session flow (`getConnection` + `buildPageState` + `getDomElementStates` + `close`) works transparently in remote mode after this merges. Phase 3c only handled `buildPageState`'s call graph; this phase closes `getDomElementStates`.

### Still deferred (phase 3e)
- `BrowserService.enrichElementStates` (lines ~1953, 1997, 2003, 2510) — needs a new `Browser.findElements` or `Browser.waitForElement` method backed by a server endpoint.
- `BrowserService` form extraction (lines 3291, 3292, 3306, 3385, 3401).
- `com.looksee.browsing.table.Table` helper (`element.findElements` on a passed-in WebElement).
- Remaining `RemoteWebElement` unsupported `WebElement` methods (click, submit, sendKeys, clear, isSelected, isEnabled, getText, findElement(s), getCssValue, getScreenshotAs).

Phase 3e triggers when phase 4b (element-enrichment cutover) surfaces the first blocker.

## [0.7.1] - 2026-04-24

### Added
- **`Browser.getTitle()`** with a local default (`driver.getTitle()`) and a `RemoteBrowser` override that Jsoup-parses the `getSource()` response. No new browser-service endpoint; Jsoup is already a LookseeCore dependency.

### Changed
- **`PageStateAdapter`'s two `Browser`-taking overloads** no longer reach through `browser.getDriver()`. Seven call-site swaps route through `browser.getCurrentUrl()` / `getSource()` / `getTitle()` / `getViewportSize()` instead. Both overloads now work transparently against a `RemoteBrowser`.
- **`BrowserUtils`** URL-sanitization helpers (lines 1377, 1465) migrated to `browser.getCurrentUrl()`.
- No behavior change in local mode. Default `looksee.browsing.mode` remains `local`.

### Unblocks
- Phase 4a.2 — PageBuilder `buildPageState` → `capturePage` migration (reverted in PR #36 for DOM-consistency reasons) can re-land now that `buildPageState`'s call graph runs in remote mode.

### Still deferred (phase 3d — trigger on demand)
- `BrowserService` form extraction (lines 454, 1953, 3291, 3292, 3401) — needs server-side endpoint or full rewrite.
- `com.looksee.browsing.table.Table` helper (`element.findElements` on passed-in WebElement) — same constraint.
- `RemoteWebElement` unsupported `WebElement` methods (click, submit, sendKeys, clear, getTagName, isSelected, isEnabled, getText, findElement(s), getCssValue, getScreenshotAs — 11 methods).

See `browser-service/phase-3c-internal-remote-compat.md` §14 for the full deferral list with trigger conditions.

## [0.7.0] - 2026-04-24

### Added
- **Remote element-handle ops in `RemoteBrowser`.** Every method previously stubbed with `UnsupportedOperationException("phase 3b")` now forwards to the corresponding browser-service endpoint: `findElement`, `findWebElementByXpath`, `isDisplayed`, `extractAttributes`, `getElementScreenshot`, six scroll ops (`scrollTo*`, `scrollDown*`), four DOM-removal ops (`removeElement`, `removeDriftChat`, `removeGDPRmodals`, `removeGDPR`), `moveMouseOutOfFrame`, `moveMouseToNonInteractive`, `isAlertPresent`. Only `getDriver()` still throws — that's the point of the shim.
- **`RemoteWebElement`** (`com.looksee.services.browser.RemoteWebElement`) — implements `org.openqa.selenium.WebElement`, bound to a remote session via `sessionId + element_handle`. Caches `rect` / `attributes` / `displayed` from the `/element/find` response so `isDisplayed` and `extractAttributes` serve without round-trips. Equality is `sessionId + elementHandle`.
- **`RemoteAlert`** (`com.looksee.services.browser.RemoteAlert`) — implements `org.openqa.selenium.Alert`; `accept()`/`dismiss()` forward to `POST /v1/sessions/{id}/alert/respond`.
- **Three new `Browser` methods** with mode-agnostic local defaults: `performClick(WebElement)`, `performAction(WebElement, Action, String)`, `getCurrentUrl()`. `RemoteBrowser` overrides them to route through browser-service. Consumers can now use these instead of reaching through `browser.getDriver()`.
- **`BrowsingClient` facade extensions.** 15 new methods wrapping `ElementsApi`, `TouchApi`, `DomApi`, `MouseApi`, `AlertsApi`, plus the new scroll + element-screenshot methods on existing APIs. All routed through the phase-4 `recordCall` instrumentation so each emits a `browser_service_calls` timer.
- **`PageStateAdapter.toPageState(byte[] viewport, byte[] fullPage, …)` overload** — accepts both screenshots separately. The single-byte overload delegates by passing the same bytes twice for backward compatibility.

### Changed
- **`BrowserService.capturePage` remote mode** now uses an explicit session lifecycle (`createSession → navigate → getSource → screenshot(VIEWPORT) → screenshot(FULL_PAGE_SHUTTERBUG) → deleteSession`) instead of `POST /capture`. The resulting `PageState` stores distinct viewport and full-page screenshot URLs — matches local-mode fidelity. `BrowsingClient.capture` / `getCaptureScreenshotBytes` remain on the facade for callers that want the cheaper single-shot.
- **`StepExecutor.execute`** no longer reaches through `browser.getDriver()`. The SimpleStep click, the three LoginStep field entries, and the exception-path URL read now use `browser.performClick` / `browser.performAction` / `browser.getCurrentUrl`. Local behavior is byte-identical; remote mode now works end-to-end for journey execution.
- **`journeyExecutor/.../AuditController.java:562`** migrated from `browser.getDriver().getCurrentUrl()` to `browser.getCurrentUrl()` — the last direct `getDriver()` call in non-test consumer code.

### Deferred (phase 3c)
- Every `UnsupportedOperationException` in `RemoteWebElement` (click, submit, sendKeys, clear, getTagName, isSelected, isEnabled, getText, findElement(s), getCssValue, getScreenshotAs) — current consumer census has no callers; they'll be wired when a consumer needs them.
- `RemoteAlert.sendKeys` — no current callers.
- LookseeCore-internal `browser.getDriver()` reach-throughs enumerated in `browser-service/phase-3b-element-handle-ops.md` §14.9 (PageStateAdapter's `Browser`-taking overloads, `BrowserService` form extraction, `BrowserUtils` URL sanitization, `com.looksee.browsing.table.Table`). These were remote-incompatible in 0.6.0 too — the remote path just never reached them.

### Version sequencing
0.7.0 stacks on 0.6.0 (phase 3) + 0.6.1 (phase 4a.1 facade instrumentation). The phase-3b plan doc originally called this release 0.7.0 bumping from 0.6.0; intermediate 0.6.1 delivered just the instrumentation ahead of the full 3b wire-up.

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
