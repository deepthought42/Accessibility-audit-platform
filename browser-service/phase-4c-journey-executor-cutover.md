# Phase 4c — journeyExecutor Cutover

> **Goal:** Run journeyExecutor with `looksee.browsing.mode=remote` against browser-service. **Deployment-only — no call-site refactor.** Unlike PageBuilder's 4a.2, journeyExecutor must not swap its `buildPageState(browser, ...)` for `capturePage(URL, ...)`: the page capture has to come from the **same live session** that just executed the journey steps, and `capturePage` opens a fresh session.
>
> **Sibling references:** [`phase-4-consumer-cutover.md`](./phase-4-consumer-cutover.md) §4c is the umbrella spec. Phase-4b (element-enrichment) plan: [`phase-4b-element-enrichment-cutover.md`](./phase-4b-element-enrichment-cutover.md). Established mode-override pattern: see 4b §Locked decisions; reused here for journey-executor.

## Why this phase exists now

journeyExecutor is the third and final browser-using consumer in the umbrella's census. Its browser surface is wider than element-enrichment's and structurally different from PageBuilder's: it opens a browser session via `browser_service.getConnection`, drives a sequence of journey steps via `step_executor.execute(browser, step)` — clicks, navigations, form fills, dynamic state — and ultimately captures the resulting page via the private `buildPage(browser, …)` helper. That helper calls `browser_service.buildPageState(browser, audit_record_id, browser_url)` at line 482, which delegates to `pageStateAdapter.toPageState(browser, ...)`. **That path reads from the live session**: `browser.getSource()`, `browser.getViewportScreenshot()`, `browser.getFullPageScreenshotShutterbug()`, `browser.removeDriftChat()`, `browser.removeGDPRmodals()`. The captured DOM and screenshots reflect the post-journey state.

Phase-3b made all of those `Browser` methods remote-compatible via `RemoteBrowser` (the shim that adapts `BrowsingClient` calls to a `Browser`-shaped interface). So `buildPageState(browser, ...)` already works in remote mode — no refactor required. Flipping `looksee.browsing.mode=remote` is sufficient.

### Why the umbrella's 4c.1 prescription is wrong

The umbrella doc §4c.1 says to migrate `buildPage(browser, …)` to `capturePage(...)` "same pattern as 4a.2". That prescription would be unsafe here. `BrowserService.capturePage(URL, BrowserType, long)` opens a brand-new browser-service session and re-navigates by URL (see `BrowserService.java:166–212`). For PageBuilder's flow that's correct — PageBuilder captures a fresh page and the source/screenshots are byte-identical regardless of which session captures them. For journeyExecutor it's a behavior change: the post-journey state (cookies, client-side mutations, same-URL DOM changes accumulated by `step_executor.execute`) is gone in the new session. xpaths extracted from the fresh capture would not match the elements still present in the journey-state browser used for `getDomElementStates(...)` — silent missing/incorrect element extraction.

Codex's review of an earlier draft of this plan caught the issue. This plan supersedes the umbrella's 4c.1 step.

## Scope

| In scope | Out of scope |
|---|---|
| `BrowsingClientMetricsConfig` for `consumer=journey-executor` common tag (mirrors PageBuilder's 4a.2 config). The only Java change in this phase. | **`AuditController.java:482` swap to `capturePage`.** Explicitly out of scope — would break post-journey capture (see §"Why the umbrella's 4c.1 prescription is wrong" above). The existing `buildPageState(browser, ...)` path is remote-compatible after phase-3b. |
| `journeyExecutor/src/main/resources/application.yml` — `LOOKSEE_BROWSING_*` env-var bindings (mirrors 4a.5 / 4b). | Spring profile (`application-staging.yml` / `application-prod.yml`) introduction — env-var pattern is the established approach. |
| LookseeIaC `journey_executor_cloud_run` module: wire `plain_environment_variables`, add per-consumer mode-override + smoke-check tfvars. | New cloud_run module work — `journey_executor_cloud_run` already exists in `LookseeIaC/GCP/modules.tf:318` and `plain_environment_variables` already exists on the cloud_run module from 4a.5. |
| Staged flip: staging tfvars (48h burn-in) → prod tfvars (1h observation). | Per umbrella, 4c is the final consumer; after it stabilizes, declare phase-4 complete. |
| Rollback playbook (one-edit revert; `journey_executor_browsing_mode=local` AND `journey_executor_smoke_check_enabled=false` together to avoid the 4a.4 mode-gate crash-loop). | 7-day calm window between staging and prod, or between 4b and 4c. **Skipped per the project's not-yet-in-prod posture** — those gates are aspirational while no consumer has prod traffic on remote. |

## Locked decisions

| Area | Decision |
|---|---|
| Mode knob | Per-consumer pin, **not** an inheritance overlay. Add `journey_executor_browsing_mode` (string, default `"local"`); `journey_executor_cloud_run` reads `var.journey_executor_browsing_mode` **directly** (no `coalesce` against the global). Default `"local"` keeps Commit 2 inert in every environment — including staging where 4a.5 set `looksee_browsing_mode="remote"` — and is the deliberate fix Codex flagged on the original 4b plan (PR #62) where landing the IaC commit auto-flipped a new consumer via the global knob. The shared `looksee_browsing_mode` remains in tfvars but is read only by consumers that haven't yet adopted a per-consumer override (today, only page-builder until the 4b plan's retroactive note is acted on). Coarse "flip everything" via the global knob therefore only affects unadopted consumers; surgical pinning is the everyday path. |
| Smoke-check enable | Per-consumer `journey_executor_smoke_check_enabled` tfvar (default false). Independent burn-in observation per consumer, matching the page_builder + element_enrichment pattern. |
| Smoke-check target URL / interval | Reuse the existing shared tfvars (`looksee_browsing_smoke_check_target_url`, `looksee_browsing_smoke_check_interval`). The smoke-check **browser** is intentionally not terraformed: the LookseeCore property and `application.yml` both default to `CHROME`, every current consumer wants Chrome, and adding a tfvar for a value no one varies is surface clutter. If a future consumer needs a non-Chrome probe, add a shared tfvar at that point. |
| Prod calm-window | **Skipped.** Project not yet in production, so 7-day prod-stability gates from the umbrella are aspirational. Reinstate before the first real prod traffic. |
| Call-site migration | **None.** The `buildPageState(browser, ...)` path captures from the live post-journey session via `RemoteBrowser`'s phase-3b implementation of `getSource`/`getViewportScreenshot`/`getFullPageScreenshotShutterbug`/`removeDriftChat`/`removeGDPRmodals`. Migrating to `capturePage` would silently lose journey state and produce an xpath/element mismatch. journeyExecutor's structural difference from PageBuilder (capture-after-stateful-journey vs. capture-fresh) requires keeping the in-session capture. |
| Metrics common-tag wiring | New `BrowsingClientMetricsConfig` in `journeyExecutor/src/main/java/com/looksee/journeyExecutor/config/`, copy of PageBuilder's, with `consumer=journey-executor`. `@ConditionalOnBean(MeterRegistry.class)` so deployments without a registry still work. |
| Metrics export path | journeyExecutor must explicitly depend on **both** `spring-boot-starter-actuator` (drives Spring Boot's `MetricsAutoConfiguration` + the `management.metrics.export.stackdriver.*` property binding) **and** `io.micrometer:micrometer-registry-stackdriver` (so the auto-config can actually instantiate a `StackdriverMeterRegistry`). With both on the classpath plus `MANAGEMENT_METRICS_STACKDRIVER_ENABLED=true` (set by the cloud_run module's observability defaults) plus `gcp.project-id` (also set by the module via `SPRING_CLOUD_GCP_PROJECT_ID`), Spring Boot wires the registry into the `CompositeMeterRegistry` and `browser_service_calls_seconds_*` reaches Stackdriver. Without either dep, the `MeterFilter.commonTags(...)` attaches to whatever falls back — most likely the `SimpleMeterRegistry` registered by `looksee-messaging`'s `MessagingObservabilityAutoConfiguration` (`@ConditionalOnMissingBean`) — and burn-in queries return zero results even when remote browsing is healthy. **PageBuilder has the same registry-side gap** (declares actuator but not `micrometer-registry-stackdriver`) — fixed in a separate PR, but it's a hard prereq for 4c's Commit 3a since the burn-in's cross-consumer guardrail relies on `consumer=page-builder` metrics actually reaching Stackdriver. See §Follow-ups + the prereq-checklist entry. |

## Authoritative design references

- [`browser-service/phase-4-consumer-cutover.md`](./phase-4-consumer-cutover.md) §4c — umbrella spec. **Note:** §4c.1's prescription to migrate to `capturePage` is superseded by this plan; see §"Why the umbrella's 4c.1 prescription is wrong" above.
- Commit `2d22d2e` (`refactor(page-builder): migrate buildPageState → capturePage + browsing config`) — the 4a.2 swap. Referenced for the metrics common-tag config shape only; the call-site swap there is **not** copied here for the architectural reason above.
- [`phase-4b-element-enrichment-cutover.md`](./phase-4b-element-enrichment-cutover.md) — per-consumer mode-override pattern to copy.

## Prerequisites — must be GO before execution

- [x] Phase-3b code merged to main (PR #38, commit `748d42a`). Without it, `step_executor.execute(browser, step)` and the element-state extraction branch throw on remote.
- [x] journeyExecutor pinned to LookseeCore 0.8.2 (`journeyExecutor/pom.xml:15`: `<core.version>0.8.2</core.version>`).
- [x] `journey_executor_cloud_run` module exists in `LookseeIaC/GCP/modules.tf:318`.
- [x] `plain_environment_variables` parameter exists on `modules/cloud_run` (shipped in 4a.5 PR #60). No module-level work needed in 4c.
- [ ] Phase 4b consumer-side wiring + IaC merged (Commit 1 + Commit 2 of 4b plan executed). The shared `looksee_browsing_mode` knob and the per-consumer mode-override pattern need to exist in IaC before 4c reuses them. Once 4b's Commit 2 lands, 4c's IaC commit is additive and small. **This is sufficient to unblock 4c Commits 1 and 2** (config code and IaC wiring) — both are inert at default `mode=local`, so 4b's flip status doesn't matter for them.
- [ ] **Phase 4b staging flip applied + element-enrichment running on `mode=remote` in staging, with `consumer=element-enrichment` metrics actively reaching Stackdriver.** Hard gate on **4c Commit 3a** specifically (not on Commits 1/2). 4c's staging-burn-in cross-consumer guardrail asserts that `consumer=element-enrichment` metrics stay green throughout — that signal is unevaluable if 4b is still on `mode=local` (the BrowsingClient timer never fires) or if 4b's IaC commit shipped without the staging tfvar flip applied (no remote traffic, no metrics). Verify before starting Commit 3a: Stackdriver Metrics Explorer must show non-zero `rate(browser_service_calls_seconds_count{consumer="element-enrichment"}[5m])` over the preceding 24 hours. If empty, block Commit 3a until 4b's staging flip is applied and producing metrics.
- [ ] **PageBuilder `micrometer-registry-stackdriver` dependency added + redeployed.** Hard gate on 4c's Commit 3a: 4c's cross-consumer guardrail (`consumer=page-builder` metrics stay green during 4c's burn-in) requires real Stackdriver data from PageBuilder, which doesn't reach Stackdriver today (see §Follow-ups). Single-line pom edit + redeploy.

## Execution plan — 3 logical commits, 4 PRs (Commit 3 splits into 3a/3b)

### Commit 1 (journeyExecutor) — `feat(config): consumer=journey-executor metrics common-tag + browsing env vars`

**No call-site refactor.** This commit is config-only: a Micrometer registry-exporter dependency, a common-tag config, and an `application.yml` env-var binding.

**Add to `journeyExecutor/pom.xml` `<dependencies>`** (versions come from `A11yParent`'s `<dependencyManagement>` — no version literals here):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-stackdriver</artifactId>
</dependency>
```

Both are required:
- **`spring-boot-starter-actuator`** — explicitly declare it (currently inherited transitively, which is fragile). Drives Spring Boot's `MetricsAutoConfiguration`, which registers a `CompositeMeterRegistry` and binds `management.metrics.export.stackdriver.*` properties.
- **`micrometer-registry-stackdriver`** — without this on the classpath, `MANAGEMENT_METRICS_STACKDRIVER_ENABLED=true` (set by the cloud_run module's observability defaults at `modules/cloud_run/main.tf:62`) is a no-op. The `StackdriverMeterRegistry` auto-config requires the dep + the property + a `gcp.project-id` (the latter is already set by the module's `SPRING_CLOUD_GCP_PROJECT_ID` env var).

> **`MessagingObservabilityAutoConfiguration` fallback note:** `looksee-messaging` registers a `SimpleMeterRegistry` via `@Bean @ConditionalOnMissingBean` (`looksee-messaging/.../observability/MessagingObservabilityAutoConfiguration.java:33–36`) so consumers without their own registry still get one. With actuator + stackdriver-registry on classpath in prod, Spring Boot's auto-config registers the `CompositeMeterRegistry` first and the fallback's `@ConditionalOnMissingBean` doesn't fire. In tests where neither auto-config kicks in (slim context), the fallback wins — that's expected and correct test isolation.

> **Cross-consumer note:** PageBuilder declares `spring-boot-starter-actuator` but **not** `micrometer-registry-stackdriver` — same metrics-export gap on the registry side. Tracked in §Follow-ups below as a **prereq gate for Commit 3a** (not optional): without the PageBuilder pom edit shipped + redeployed, phase-4c burn-in queries are evaluated against empty Stackdriver data for the cross-consumer guardrail (`consumer=page-builder` panel), so 4c can't sign off.

Create `journeyExecutor/src/main/java/com/looksee/journeyExecutor/config/BrowsingClientMetricsConfig.java` — direct copy of `PageBuilder/src/main/java/com/looksee/pageBuilder/config/BrowsingClientMetricsConfig.java`, with the package adjusted and the tag value changed to `consumer=journey-executor`:

```java
package com.looksee.journeyExecutor.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(MeterRegistry.class)
public class BrowsingClientMetricsConfig {
    @Autowired
    private MeterRegistry meterRegistry;

    @PostConstruct
    void addCommonTags() {
        meterRegistry.config().meterFilter(
            MeterFilter.commonTags(Tags.of("consumer", "journey-executor")));
    }
}
```

Append to `journeyExecutor/src/main/resources/application.yml` — same yaml block PageBuilder added in 4a.5, defaults preserve current behavior:

```yaml
# LookseeCore browsing configuration. Default mode is `local` — every instance
# opens a local Selenium driver. Flip to `remote` to route browser ops through
# brandonkindred/browser-service; requires LOOKSEE_BROWSING_SERVICE_URL set.
# See browser-service/phase-4c-journey-executor-cutover.md.
looksee:
    browsing:
        mode: ${LOOKSEE_BROWSING_MODE:local}
        service-url: ${LOOKSEE_BROWSING_SERVICE_URL:}
        connect-timeout: ${LOOKSEE_BROWSING_CONNECT_TIMEOUT:5s}
        read-timeout: ${LOOKSEE_BROWSING_READ_TIMEOUT:120s}
        smoke-check:
            enabled: ${LOOKSEE_BROWSING_SMOKE_CHECK_ENABLED:false}
            interval: ${LOOKSEE_BROWSING_SMOKE_CHECK_INTERVAL:60s}
            target-url: ${LOOKSEE_BROWSING_SMOKE_CHECK_TARGET_URL:https://example.com}
            browser: ${LOOKSEE_BROWSING_SMOKE_CHECK_BROWSER:CHROME}
```

PR title: **"feat(journey-executor): consumer metrics tag + browsing env vars (phase-4c)"**.

### Commit 2 (LookseeIaC) — `feat(journey-executor): cutover env vars (phase-4c)`

Edit `LookseeIaC/GCP/variables.tf` — add two new tfvars at the end of the existing "LookseeCore browsing" block (which 4a.5 introduced):

```hcl
variable "journey_executor_browsing_mode" {
  description = "Per-consumer pin for journey-executor's looksee.browsing.mode. Defaults to 'local' so this commit lands inert in every environment regardless of var.looksee_browsing_mode; staging/prod tfvars set 'remote' explicitly to flip this consumer."
  type        = string
  default     = "local"
  validation {
    condition     = contains(["local", "remote"], var.journey_executor_browsing_mode)
    error_message = "journey_executor_browsing_mode must be 'local' or 'remote'."
  }
}

variable "journey_executor_smoke_check_enabled" {
  description = "Enables the CapturePageSmokeCheck watchdog in journey-executor."
  type        = bool
  default     = false
}
```

Edit `LookseeIaC/GCP/modules.tf` (`module "journey_executor_cloud_run"`) — add a sibling `plain_environment_variables` argument; **leave existing `environment_variables` untouched**:

```hcl
plain_environment_variables = {
  "LOOKSEE_BROWSING_MODE"                   = var.journey_executor_browsing_mode
  "LOOKSEE_BROWSING_SERVICE_URL"            = var.looksee_browsing_service_url
  "LOOKSEE_BROWSING_SMOKE_CHECK_ENABLED"    = tostring(var.journey_executor_smoke_check_enabled)
  "LOOKSEE_BROWSING_SMOKE_CHECK_INTERVAL"   = var.looksee_browsing_smoke_check_interval
  "LOOKSEE_BROWSING_SMOKE_CHECK_TARGET_URL" = var.looksee_browsing_smoke_check_target_url
}
```

Reading `var.journey_executor_browsing_mode` directly (no `coalesce` against the global) means this commit lands inert at `"local"` in every environment — including staging where `var.looksee_browsing_mode="remote"` from 4a.5. The staged 3a/3b flips set the per-consumer pin to `"remote"` explicitly. Page-builder / element-enrichment rollback decisions don't drag journey-executor along, and journey-executor's flips don't drag them.

PR title: **"feat(journey-executor): cutover env vars (phase-4c)"**.

### Commit 3 (LookseeIaC, **staging then prod tfvars**) — `chore(<env>): flip journey-executor to remote browsing`

Two **separate** PRs, identical-shape, staggered by the staging burn-in.

#### Commit 3a — staging tfvars

Once Commit 2 has landed and browser-service-staging is reachable:

```hcl
# Staging tfvars override
journey_executor_browsing_mode       = "remote"
journey_executor_smoke_check_enabled = true

# Pre-flight check: looksee_browsing_service_url MUST be non-blank in this
# environment. LookseeCore's BrowsingClientConfig throws on a blank URL when
# mode=remote, which would fail consumer startup. 4a.5 set this in staging
# tfvars; if executing 4c against a fresh environment, set it explicitly:
# looksee_browsing_service_url = "https://browser-service-staging.internal/v1"
```

Apply via the staging Terraform workflow. Cloud Run rolling redeploy of the journey-executor revision picks up the new env vars. **Service-URL preflight:** `LookseeIaC/GCP` doesn't expose a root-module `output` for `looksee_browsing_service_url`, so `terraform output` won't print it. Verify the resolved value either via `terraform console` from the workspace root:

```
$ terraform console
> var.looksee_browsing_service_url
"https://browser-service-staging.internal/v1"
```

…or directly grep the staging tfvars file (`grep '^looksee_browsing_service_url' staging.tfvars`). Empty / unset → set it explicitly in this same flip commit.

48-hour burn-in pass criteria (umbrella §4a.5, identical to 4b). The `BrowsingClient` facade timer is built with `.publishPercentileHistogram()` (verified at `LookseeCore/looksee-browsing-client/src/main/java/com/looksee/browsing/client/BrowsingClient.java:414`), so `browser_service_calls_seconds_bucket` is available alongside the contractual `_count`/`_sum`/`_max` series — burn-in queries can use real percentiles, not just `_max`.
- **Minimum-traffic guard.** All gates below are evaluated only when `sum(rate(browser_service_calls_seconds_count{consumer="journey-executor"}[5m])) >= 0.5` (≥1 call every 2 seconds, smoothed over 5 minutes). At lower throughput, dashboard panels show "insufficient traffic"; signed-off staging burn-in requires the guard to hold for ≥40 of the 48 hours so we're not certifying a quiet period.
- **Error rate <1%** averaged over any 15-minute window:
  ```
  sum(rate(browser_service_calls_seconds_count{consumer="journey-executor", outcome="failure"}[15m]))
    /
  sum(rate(browser_service_calls_seconds_count{consumer="journey-executor"}[15m]))
  ```
- **p95 latency within 2× the local-mode baseline** for journey-executor:
  ```
  histogram_quantile(0.95, sum by (le) (rate(browser_service_calls_seconds_bucket{consumer="journey-executor"}[5m])))
  ```
  Capture the local-mode baseline from the same expression evaluated against the 24 hours immediately before the staging flip.
- **Mean latency check (cross-validation):** `sum(rate(browser_service_calls_seconds_sum{consumer="journey-executor"}[5m])) / sum(rate(browser_service_calls_seconds_count{consumer="journey-executor"}[5m]))` should track within 2× of its pre-flip baseline. Using `sum/count` instead of `_max` avoids the trap where Micrometer's window-scoped `_max` resets on quiet samples and looks artificially healthy at low traffic.
- No new Sentry regressions tagged `service:journey-executor`.
- Smoke-check failure rate <1%: `sum(rate(browser_service_smoke_checks_total{consumer="journey-executor", outcome="failure"}[15m])) / sum(rate(browser_service_smoke_checks_total{consumer="journey-executor"}[15m]))`.
- **Cross-consumer guardrail:** `consumer=page-builder` and `consumer=element-enrichment` metrics stay green throughout — bringing on the third consumer must not destabilize the first two.

#### Commit 3b — prod tfvars

```hcl
# Prod tfvars override
journey_executor_browsing_mode       = "remote"
journey_executor_smoke_check_enabled = true

# Pre-flight: same service-url precondition as staging. 4a.6 prod flip set
# looksee_browsing_service_url for prod; verify via `terraform console`
# (`> var.looksee_browsing_service_url`) or `grep ^looksee_browsing_service_url
# prod.tfvars` before applying — there's no root-module `output` for it.
# If unset, flipping will crash-loop the next Cloud Run revision because
# BrowsingClientConfig rejects a blank service URL when mode=remote.
# To set it explicitly in this commit:
# looksee_browsing_service_url = "https://browser-service-prod.internal/v1"
```

Same six prereqs as 4a.6: staging burn-in signed off, oncall scheduled, rollback dry-run within 7 days, cost alert armed, browser-service prod reachable, dashboard renders prod `consumer=journey-executor` tag.

1-hour prod observation. If green, declare 4c stable.

PR titles: **"chore(staging): flip journey-executor to remote browsing (phase-4c)"** and **"chore(prod): flip journey-executor to remote browsing (phase-4c)"**.

## Verification

### Per-commit

- Commit 1: `cd journeyExecutor && mvn verify`. Use `verify`, not `test`: journeyExecutor binds JaCoCo coverage enforcement to the `verify` phase, so `mvn test` can pass while CI's coverage gate fails later. `verify` runs both.

  **Required test work in this commit:** journeyExecutor's existing tests (`ApplicationTest`, `AuditControllerTest`, `AuditControllerIdempotencyTest`, `RetryConfigTest`) are all unit-style (Mockito mocks, no `@SpringBootTest`), so without explicit work the Spring context never boots during `mvn verify` and a wiring/scanning regression on the new `BrowsingClientMetricsConfig` would ship undetected. **Add** a new `BrowsingClientMetricsConfigTest` under `src/test/java/com/looksee/journeyExecutor/config/` annotated `@SpringBootTest(classes = Application.class)` that boots the full context and asserts:
  1. A `MeterRegistry` bean is present in the context (will be the messaging-fallback `SimpleMeterRegistry` in the test slice — that's correct, see note below).
  2. The `consumer=journey-executor` common tag is **applied by the filter, not pre-supplied at the call site**. Register a meter without the consumer tag (`meterRegistry.counter("phase4c.metrics.test")` or `meterRegistry.timer("phase4c.metrics.test")`) and assert that the returned meter's `getId().getTag("consumer")` equals `"journey-executor"`. Calling `meterRegistry.timer("test", "consumer", "journey-executor")` would pre-populate the tag at call time and pass even if the `BrowsingClientMetricsConfig` `@PostConstruct` never ran — the regression this test must catch is "filter never applied", so the input meter must not carry the tag the filter is supposed to inject.
  3. **Negative control** to prove the assertion above is meaningful: a second sub-test that constructs a fresh `SimpleMeterRegistry` outside the Spring context, registers the same un-tagged meter, and confirms `getTag("consumer")` is `null`. Without the negative control, an always-on common tag (e.g. one inadvertently added by some other auto-config) would make the positive assertion green for the wrong reason.

  **Do not assert** the bean type is `StackdriverMeterRegistry`: default `application.yml` resolves `MANAGEMENT_METRICS_STACKDRIVER_ENABLED:false` from the env var, so Stackdriver auto-config is intentionally off in tests, and asserting the prod-only bean type would force test-only divergent config. Production observability is verified at deploy time (boot-log inspection + Stackdriver Metrics Explorer rendering `consumer=journey-executor` series), not in unit tests. `application.yml` defaults preserve current runtime behavior (mode=local, smoke-check off), so no probe fires during test.
- Commit 2: `terraform plan` — shows new env vars added to journey-executor revision; non-staging environments unchanged because per-consumer mode override defaults to `"local"`.
- Commit 3a/3b: `terraform plan` against the target environment shows `LOOKSEE_BROWSING_MODE` flipping `local → remote` and `LOOKSEE_BROWSING_SMOKE_CHECK_ENABLED=true`.

### Post-deploy (per environment)

- Cloud Run journey-executor revision rolls out with new env vars.
- Boot log shows `CapturePageSmokeCheck started: interval=<configured-interval>ms target=<configured-target-url>` (only when smoke-check enabled). The target URL and interval come from the shared `looksee_browsing_smoke_check_target_url` and `looksee_browsing_smoke_check_interval` tfvars (LookseeCore defaults: `https://example.com` / `60s`), so the literal logged value will differ per environment. Verify by resolving the configured value first (`terraform console` → `var.looksee_browsing_smoke_check_target_url`, or `grep ^looksee_browsing_smoke_check_target_url <env>.tfvars`) and grepping the boot log for that exact URL. Comparing against a hardcoded literal would cause false rollout failures whenever an environment overrides the default.
- First probe fires immediately (initial-delay 0 from the 4a.4 fix).
- Dashboard renders `browser_service_smoke_checks_total{outcome="success",consumer="journey-executor"}` ticking once per 60s.
- Real journey-executor traffic produces `rate(browser_service_calls_seconds_count{consumer="journey-executor"}[1m]) > 0` in step with journey-step execution + the in-session `buildPageState` capture path.

## Rollback playbook

Identical shape to 4b: edit the relevant tfvar override, `terraform apply`, ≤10-minute Cloud Run rolling redeploy reverts. Two granularities, both one-edit:

- **Surgical (default)**: set both at once in the affected environment's tfvars:
  ```hcl
  journey_executor_browsing_mode       = "local"
  journey_executor_smoke_check_enabled = false
  ```
  Both are required: `CapturePageSmokeCheck.prepare()` throws `IllegalStateException` on startup if `smoke-check.enabled=true` while `mode!=remote` (the mode-gate added in 4a.4 to prevent false-green metrics from a local-mode probe). Flipping mode without disabling the watchdog crash-loops the next Cloud Run revision and turns rollback into an outage. PageBuilder + element-enrichment are unaffected because each reads its own per-consumer override.
- **Coarse / multi-consumer**: only useful for consumers that haven't adopted a per-consumer pin (today, page-builder pre-retrofit). Setting `looksee_browsing_mode = "local"` flips those; consumers with their own `<consumer>_browsing_mode` pin (element-enrichment after 4b ships, journey-executor after this phase) are unaffected. To roll back multiple per-consumer-pinned consumers at once, edit each consumer's pin individually — there is no shared rollback knob in this design. Trade-off accepted to preserve the staged-flip gate. For each consumer being rolled back with its watchdog on, also flip its `<consumer>_smoke_check_enabled = false` for the mode-gate crash-loop reason.

## Critical files

- `journeyExecutor/pom.xml` — Commit 1 (add `micrometer-registry-stackdriver` dependency)
- `journeyExecutor/src/main/java/com/looksee/journeyExecutor/config/BrowsingClientMetricsConfig.java` — Commit 1 (new file)
- `journeyExecutor/src/main/resources/application.yml` — Commit 1
- `LookseeIaC/GCP/variables.tf` — Commit 2 (two new tfvars)
- `LookseeIaC/GCP/modules.tf` — Commit 2 (`journey_executor_cloud_run` block, add sibling `plain_environment_variables`)
- `LookseeIaC/<staging tfvars>` — Commit 3a
- `LookseeIaC/<prod tfvars>` — Commit 3b
- **Not touched: `AuditController.java`.** The umbrella's prescribed swap is unsafe here.

## Issue + PRs

One tracking issue covering all three commits, since the burn-in is the unit of completion. Each PR body links to this plan doc. Closing the tracking issue happens on Commit 3b merge + 1-hour observation green.

## Follow-ups (separate PRs, but one of them is a 4c gate)

- **PageBuilder missing `micrometer-registry-stackdriver` dependency — gating prereq for 4c Commit 3a.** PageBuilder declares `spring-boot-starter-actuator` but inherits the stackdriver registry only via `<dependencyManagement>` without an explicit `<dependencies>` entry, so `MANAGEMENT_METRICS_STACKDRIVER_ENABLED=true` (set unconditionally by the cloud_run module) is a no-op for PageBuilder today. Phase 4a's burn-in/observation criteria implicitly assume metrics reach Stackdriver — they don't. The fix is a single-line pom addition + redeploy. **Lives in a separate PR (not 4c code), but is a hard gate on Commit 3a:** the cross-consumer guardrail in 4c's burn-in (`consumer=page-builder` panel staying green) cannot be evaluated against empty data. If PageBuilder's fix hasn't shipped + been redeployed by the time staging burn-in starts, **block Commit 3a** until it has. The Prerequisites checklist lists this as an explicit `[ ]` item.

## Definition of done

- [ ] Commit 1 merged (metrics common-tag config + application.yml env-var bindings).
- [ ] Commit 2 merged (LookseeIaC tfvars + module wiring).
- [ ] Commit 3a merged + applied (staging flip).
- [ ] 48 consecutive hours of green staging burn-in.
- [ ] Commit 3b merged + applied (prod flip).
- [ ] 60 consecutive minutes of green prod observation.
- [ ] No rollback executed at any point.

When all seven boxes check out, **phase 4c is done — and phase 4 is complete.** Every browser-using consumer in the umbrella's census runs against browser-service. Mention in the next LookseeCore release CHANGELOG. The 7-day calm window from the umbrella applies before declaring "done done" once production traffic is real, but is currently aspirational per the project's not-yet-in-prod posture.
