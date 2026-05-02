# Phase 4c — journeyExecutor Cutover

> **Goal:** Run journeyExecutor with `looksee.browsing.mode=remote` against browser-service. **No call-site refactor — the existing `buildPageState(browser, ...)` path stays.** Unlike PageBuilder's 4a.2, journeyExecutor must not swap to `capturePage(URL, ...)`: the page capture has to come from the **same live session** that just executed the journey steps, and `capturePage` opens a fresh session. The phase still ships consumer-side code in Commit 1 (a metrics common-tag config + two new pom dependencies + an `application.yml` env-var binding), so reviewers should treat Commit 1 as service-code-bearing, not as a config-only no-op. The "no refactor" framing is specifically about `AuditController.java`, which is **not touched** in this phase.
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
| LookseeIaC `journey_executor_cloud_run` module: wire `plain_environment_variables`, add per-consumer mode-override + smoke-check Terraform variables (delivered at runtime via `TF_VAR_*` GitHub Actions secrets, not committed `*.tfvars` files — see §Commit 3 banner). | New cloud_run module work — `journey_executor_cloud_run` already exists in `LookseeIaC/GCP/modules.tf:318` and `plain_environment_variables` already exists on the cloud_run module from 4a.5. |
| Staged flip via GitHub Actions Environment secret updates: `staging` Environment (48h burn-in) → `production` Environment (1h observation). | Per umbrella, 4c is the final consumer; after it stabilizes, declare phase-4 complete. |
| Rollback playbook (one-operation revert via two `gh secret set` calls + deploy-workflow re-run; `TF_VAR_JOURNEY_EXECUTOR_BROWSING_MODE=local` AND `TF_VAR_JOURNEY_EXECUTOR_SMOKE_CHECK_ENABLED=false` together to avoid the 4a.4 mode-gate crash-loop). | 7-day calm window between staging and prod, or between 4b and 4c. **Skipped per the project's not-yet-in-prod posture** — those gates are aspirational while no consumer has prod traffic on remote. |

## Locked decisions

| Area | Decision |
|---|---|
| Mode knob | Per-consumer pin, **not** an inheritance overlay. Add `journey_executor_browsing_mode` (string, default `"local"`); `journey_executor_cloud_run` reads `var.journey_executor_browsing_mode` **directly** (no `coalesce` against the global). Default `"local"` keeps Commit 2 inert in every environment — including staging where 4a.5 set `looksee_browsing_mode="remote"` — and is the deliberate fix Codex flagged on the original 4b plan (PR #62) where landing the IaC commit auto-flipped a new consumer via the global knob. The shared `looksee_browsing_mode` remains as a Terraform variable (delivered via `TF_VAR_LOOKSEE_BROWSING_MODE`) but is read only by consumers that haven't yet adopted a per-consumer override (today, only page-builder until the 4b plan's retroactive note is acted on). Coarse "flip everything" via the global knob therefore only affects unadopted consumers; surgical pinning is the everyday path. |
| Smoke-check enable | Per-consumer `journey_executor_smoke_check_enabled` Terraform variable (default false), delivered via `TF_VAR_JOURNEY_EXECUTOR_SMOKE_CHECK_ENABLED` in the targeted GitHub Actions Environment. Independent burn-in observation per consumer, matching the page_builder + element_enrichment pattern. |
| Smoke-check target URL / interval | Reuse the existing shared Terraform variables (`looksee_browsing_smoke_check_target_url`, `looksee_browsing_smoke_check_interval`), delivered via the corresponding `TF_VAR_*` GitHub Actions Environment secrets. The smoke-check **browser** is intentionally not terraformed: the LookseeCore property and `application.yml` both default to `CHROME`, every current consumer wants Chrome, and adding a variable for a value no one varies is surface clutter. If a future consumer needs a non-Chrome probe, add a shared variable at that point. |
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
- [ ] **Phase 4b staging flip applied + element-enrichment running on `mode=remote` in staging, with `consumer=element-enrichment` metrics actively reaching Stackdriver.** Hard gate on **4c Commit 3a** specifically (not on Commits 1/2). 4c's staging-burn-in cross-consumer guardrail asserts that `consumer=element-enrichment` metrics stay green throughout — that signal is unevaluable if 4b is still on `mode=local` (the BrowsingClient timer never fires) or if 4b's IaC commit shipped without the staging GitHub Actions Environment secret update applied (no remote traffic, no metrics). Verify before starting Commit 3a: Stackdriver Metrics Explorer must show non-zero `rate(browser_service_calls_seconds_count{consumer="element-enrichment"}[5m])` over the preceding 24 hours. If empty, block Commit 3a until 4b's staging flip is applied and producing metrics.
- [ ] **PageBuilder `micrometer-registry-stackdriver` dependency added + redeployed.** Hard gate on 4c's Commit 3a: 4c's cross-consumer guardrail (`consumer=page-builder` metrics stay green during 4c's burn-in) requires real Stackdriver data from PageBuilder, which doesn't reach Stackdriver today (see §Follow-ups). Single-line pom edit + redeploy.

## Execution plan — 3 logical commits, 4 PRs (Commit 3 splits into 3a/3b)

### Commit 1 (journeyExecutor) — `feat(config): consumer=journey-executor metrics common-tag + browsing env vars`

**No call-site refactor.** This commit is config-only: a Micrometer registry-exporter dependency, a common-tag config, and an `application.yml` env-var binding.

**Add to `journeyExecutor/pom.xml` `<dependencies>`** (versions come from journeyExecutor's own `<dependencyManagement>` block, which imports the `spring-boot-dependencies`, `spring-cloud-dependencies`, and `spring-cloud-gcp-dependencies` BOMs directly — note that journeyExecutor does **not** inherit from an `A11yParent` POM, unlike some sibling consumers, so no version literals are needed here because Spring Boot's BOM manages all `io.micrometer:micrometer-*` and `org.springframework.boot:spring-boot-starter-*` artifacts):

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

Edit `LookseeIaC/GCP/variables.tf` — add two new Terraform variables at the end of the existing "LookseeCore browsing" block (which 4a.5 introduced). Values are delivered at runtime via `TF_VAR_*` GitHub Actions Environment secrets — see §Commit 3 banner for the variable-delivery model:

```hcl
variable "journey_executor_browsing_mode" {
  description = "Per-consumer pin for journey-executor's looksee.browsing.mode. Defaults to 'local' so this commit lands inert in every environment regardless of var.looksee_browsing_mode; staging/production GitHub Actions Environments set TF_VAR_JOURNEY_EXECUTOR_BROWSING_MODE='remote' explicitly to flip this consumer."
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
  "LOOKSEE_BROWSING_CONNECT_TIMEOUT"        = var.looksee_browsing_connect_timeout
  "LOOKSEE_BROWSING_READ_TIMEOUT"           = var.looksee_browsing_read_timeout
  "LOOKSEE_BROWSING_SMOKE_CHECK_ENABLED"    = tostring(var.journey_executor_smoke_check_enabled)
  "LOOKSEE_BROWSING_SMOKE_CHECK_INTERVAL"   = var.looksee_browsing_smoke_check_interval
  "LOOKSEE_BROWSING_SMOKE_CHECK_TARGET_URL" = var.looksee_browsing_smoke_check_target_url
}
```

`LOOKSEE_BROWSING_CONNECT_TIMEOUT` / `LOOKSEE_BROWSING_READ_TIMEOUT` are bound in `application.yml` (see Commit 1's yaml block, lines 122–123), so omitting them from `plain_environment_variables` would leave the LookseeCore defaults (`5s` / `120s`) hardcoded with no per-environment knob — fine until journey-executor needs longer reads in prod for slow journeys. The two shared `looksee_browsing_connect_timeout` / `looksee_browsing_read_timeout` Terraform variables already exist from 4a.5 (delivered via `TF_VAR_LOOKSEE_BROWSING_CONNECT_TIMEOUT` / `TF_VAR_LOOKSEE_BROWSING_READ_TIMEOUT` GitHub Actions Environment secrets); this just wires them through journey-executor's module so the env-var path is end-to-end terraform-able. Same shape PageBuilder uses.

Reading `var.journey_executor_browsing_mode` directly (no `coalesce` against the global) means this commit lands inert at `"local"` in every environment — including staging where `var.looksee_browsing_mode="remote"` from 4a.5. The staged 3a/3b flips set the per-consumer pin to `"remote"` explicitly. Page-builder / element-enrichment rollback decisions don't drag journey-executor along, and journey-executor's flips don't drag them.

PR title: **"feat(journey-executor): cutover env vars (phase-4c)"**.

### Commit 3 (Variable delivery: `TF_VAR_*` GitHub Actions secrets, **staging then prod**) — `chore(<env>): flip journey-executor to remote browsing`

> **Variable-delivery model.** This repo's IaC delivers Terraform values via `TF_VAR_*` environment variables sourced from GitHub Actions secrets, **not** committed `staging.tfvars` / `prod.tfvars` files (see [`LookseeIaC/GCP/README.md`](../LookseeIaC/GCP/README.md) §Required Terraform variables and §GitHub Actions secrets reference). Per-environment separation is by GitHub Actions Environment (e.g. `staging`, `production`) — each Environment has its own scoped secret set. There are no `.tfvars` files in `LookseeIaC/GCP/` and the deploy workflows feed Terraform via `TF_VAR_<name>` env vars. Commit 3a/3b are therefore **secret-update operations against the staging / production GitHub Actions Environments**, not file-edit PRs. The "commit" framing is preserved for audit-trail purposes — each secret update is recorded as an environment-change event in GitHub Actions audit logs and links back to the tracking issue.

Two **separate** rollout operations, identical-shape, staggered by the staging burn-in.

#### Commit 3a — staging GitHub Actions secrets

Once Commit 2 has landed and browser-service-staging is reachable:

| Secret name (GitHub Actions Environment: `staging`) | Value | Purpose |
|---|---|---|
| `TF_VAR_JOURNEY_EXECUTOR_BROWSING_MODE` | `"remote"` | Per-consumer pin → `LOOKSEE_BROWSING_MODE` |
| `TF_VAR_JOURNEY_EXECUTOR_SMOKE_CHECK_ENABLED` | `"true"` | Enable `CapturePageSmokeCheck` |
| `TF_VAR_LOOKSEE_BROWSING_SERVICE_URL` | `"https://browser-service-staging.internal/v1"` (only if not already set from 4a.5) | **Hard precondition.** `BrowsingClientConfig` throws on blank URL when `mode=remote`, crash-looping the next Cloud Run revision. |

Update each secret via repo Settings → Environments → `staging` → "Environment secrets" (or `gh secret set <NAME> --env staging --body "<value>"`). Then re-run the staging deploy workflow (`.github/workflows/<deploy-staging>.yml` or trigger via `gh workflow run …`) — the workflow's terraform-apply step picks up the new `TF_VAR_*` values and Cloud Run rolling-redeploys journey-executor. **Service-URL preflight (executable in CI, not locally):** there's no committed `staging.tfvars`, so the only authoritative read of `var.looksee_browsing_service_url` for the staging environment is from inside a workflow that has the staging environment's secrets bound. Two operationally usable options:

1. **Inspect the GitHub Actions secret directly:** `gh secret list --env staging | grep TF_VAR_LOOKSEE_BROWSING_SERVICE_URL` — confirms the secret exists. Reading the *value* of a secret via the API isn't supported (by design), so to assert non-blankness, run a one-off staging-environment workflow with a step:

   ```yaml
   - name: Preflight - assert browsing-service URL is non-blank
     env:
       TF_VAR_LOOKSEE_BROWSING_SERVICE_URL: ${{ secrets.TF_VAR_LOOKSEE_BROWSING_SERVICE_URL }}
     run: |
       test -n "${TF_VAR_LOOKSEE_BROWSING_SERVICE_URL}" \
         || { echo "::error::TF_VAR_LOOKSEE_BROWSING_SERVICE_URL is blank in staging environment"; exit 1; }
   ```

   The empty-value check has to run server-side because GitHub masks secret values in logs.

2. **Or augment the existing terraform-plan step with a `terraform console` echo** that prints `var.looksee_browsing_service_url`'s length (not value, to avoid leaking the URL in logs): `terraform console <<< 'length(var.looksee_browsing_service_url) > 0'` — returns `true` / `false`. Acceptable to leak the boolean.

Empty / unset → set the secret in the staging Environment in the same operation as the mode flip. Don't run the deploy workflow until the preflight returns true.

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

#### Commit 3b — production GitHub Actions secrets

Same shape as 3a, against the `production` GitHub Actions Environment:

| Secret name (GitHub Actions Environment: `production`) | Value | Purpose |
|---|---|---|
| `TF_VAR_JOURNEY_EXECUTOR_BROWSING_MODE` | `"remote"` | Per-consumer pin |
| `TF_VAR_JOURNEY_EXECUTOR_SMOKE_CHECK_ENABLED` | `"true"` | Enable smoke-check |
| `TF_VAR_LOOKSEE_BROWSING_SERVICE_URL` | `"https://browser-service-prod.internal/v1"` (only if not already set from 4a.6) | Same precondition as staging — blank URL crash-loops `BrowsingClientConfig` startup. |

Run the same preflight (workflow step asserting `TF_VAR_LOOKSEE_BROWSING_SERVICE_URL` is non-blank in the `production` Environment) before triggering the production deploy workflow. Same six human gates as 4a.6: staging burn-in signed off, oncall scheduled, rollback dry-run within 7 days, cost alert armed, browser-service prod reachable, dashboard renders prod `consumer=journey-executor` tag.

1-hour prod observation. If green, declare 4c stable.

Operation titles for tracking issue + audit log: **"chore(staging): flip journey-executor to remote browsing (phase-4c)"** and **"chore(prod): flip journey-executor to remote browsing (phase-4c)"**. Each operation references the corresponding GitHub Actions secret-update events + the deploy workflow run that applied them.

## Verification

### Per-commit

- Commit 1: `cd journeyExecutor && mvn verify`. Use `verify`, not `test`: journeyExecutor binds JaCoCo coverage enforcement to the `verify` phase, so `mvn test` can pass while CI's coverage gate fails later. `verify` runs both.

  **Required test work in this commit:** journeyExecutor's existing tests (`ApplicationTest`, `AuditControllerTest`, `AuditControllerIdempotencyTest`, `RetryConfigTest`) are all unit-style (Mockito mocks, no `@SpringBootTest`), so without explicit work the Spring context never boots during `mvn verify` and a wiring/scanning regression on the new `BrowsingClientMetricsConfig` would ship undetected. **Add** a new `BrowsingClientMetricsConfigTest` under `src/test/java/com/looksee/journeyExecutor/config/` annotated `@SpringBootTest(classes = Application.class)` that boots the full context and asserts:
  1. A `MeterRegistry` bean is present in the context (will be the messaging-fallback `SimpleMeterRegistry` in the test slice — that's correct, see note below).
  2. The `consumer=journey-executor` common tag is **applied by the filter, not pre-supplied at the call site**. Register a meter without the consumer tag (`meterRegistry.counter("phase4c.metrics.test")` or `meterRegistry.timer("phase4c.metrics.test")`) and assert that the returned meter's `getId().getTag("consumer")` equals `"journey-executor"`. Calling `meterRegistry.timer("test", "consumer", "journey-executor")` would pre-populate the tag at call time and pass even if the `BrowsingClientMetricsConfig` `@PostConstruct` never ran — the regression this test must catch is "filter never applied", so the input meter must not carry the tag the filter is supposed to inject.
  3. **Negative control** to prove the assertion above is meaningful: a second sub-test that constructs a fresh `SimpleMeterRegistry` outside the Spring context, registers the same un-tagged meter, and confirms `getTag("consumer")` is `null`. Without the negative control, an always-on common tag (e.g. one inadvertently added by some other auto-config) would make the positive assertion green for the wrong reason.

  **Do not assert** the bean type is `StackdriverMeterRegistry`: default `application.yml` resolves `MANAGEMENT_METRICS_STACKDRIVER_ENABLED:false` from the env var, so Stackdriver auto-config is intentionally off in tests, and asserting the prod-only bean type would force test-only divergent config. Production observability is verified at deploy time (boot-log inspection + Stackdriver Metrics Explorer rendering `consumer=journey-executor` series), not in unit tests. `application.yml` defaults preserve current runtime behavior (mode=local, smoke-check off), so no probe fires during test.
- Commit 2: `terraform plan` against each environment — **expect a Cloud Run revision change in every environment**, not an empty plan in non-staging. Adding `plain_environment_variables` to the `journey_executor_cloud_run` module changes the Cloud Run service spec wherever this stack is applied (the `LOOKSEE_BROWSING_*` env vars become part of the revision template), so Terraform will plan a new revision and Cloud Run will roll one out on apply. The new vars' **values** are inert in non-staging — `LOOKSEE_BROWSING_MODE=local`, `LOOKSEE_BROWSING_SMOKE_CHECK_ENABLED=false` — so runtime behavior is unchanged, but the redeploy itself is real. Treat this as expected, not as a misconfiguration. The plan is "unchanged in behavior", not "unchanged in revisions". Coordinate the apply window with whoever owns each environment so the redeploy isn't a surprise.
- Commit 3a/3b: `terraform plan` against the target environment shows `LOOKSEE_BROWSING_MODE` flipping `local → remote` and `LOOKSEE_BROWSING_SMOKE_CHECK_ENABLED=true`.

### Post-deploy (per environment)

- Cloud Run journey-executor revision rolls out with new env vars.
- Boot log shows `CapturePageSmokeCheck started: interval=<configured-interval>ms target=<configured-target-url>` (only when smoke-check enabled). The target URL and interval come from the shared `TF_VAR_LOOKSEE_BROWSING_SMOKE_CHECK_TARGET_URL` and `TF_VAR_LOOKSEE_BROWSING_SMOKE_CHECK_INTERVAL` GitHub Actions Environment secrets (LookseeCore defaults: `https://example.com` / `60s`), so the literal logged value will differ per environment. Resolve the configured values from the `staging` / `production` GitHub Actions Environment (`gh secret list --env <env> | grep TF_VAR_LOOKSEE_BROWSING_SMOKE_CHECK_TARGET_URL` confirms presence; the value itself is masked in logs, so emit a non-secret length/hash check from a workflow step if you need to compare against the boot log mechanically). Then grep the boot log for the exact URL the workflow injected. Comparing against a hardcoded literal would cause false rollout failures whenever an environment overrides the default.
- First probe fires immediately (initial-delay 0 from the 4a.4 fix).
- Dashboard renders `browser_service_smoke_checks_total{outcome="success",consumer="journey-executor"}` ticking at the **configured probe cadence** for that environment, not a hardcoded 60s. Resolve the configured interval from the `TF_VAR_LOOKSEE_BROWSING_SMOKE_CHECK_INTERVAL` secret in the targeted GitHub Actions Environment (echo it from a workflow step that has the env's secrets bound), and assert the counter advances by ≥1 per that interval, with a sample window scaled to the cadence so the check stays meaningful for both fast and slow probes. The LookseeCore default is `60s`; environments overriding the shared secret (e.g. `30s` for tighter staging observation, or `10m` for cost-conservative prod) would log + tick at that overridden cadence, so checking against a 60s literal would false-fail those environments. Concretely, given `<I>` = configured interval in seconds, evaluate over a window of `max(5m, 5×<I>)` so the assertion always covers ≥5 expected probes:

  ```
  increase(browser_service_smoke_checks_total{outcome="success",consumer="journey-executor"}[max(5m, 5×<I>s)]) >= 4
  ```

  The fixed `>= 4` floor (≥4 successful probes in the window, allowing one cold-start miss out of 5 expected) avoids the bug where `floor(300 / <I>) - 1` collapsed to `0` (vacuously true regardless of probe health) whenever the configured interval exceeded 5 minutes. With a 600s interval, the window stretches to 50 minutes and the gate still requires 4 healthy probes; with a 30s interval, the window is 5 minutes and the same gate fires off the same 10 expected ticks. The lower bound on the window (`max(5m, ...)`) keeps the check responsive at fast cadences without the fixed denominator going pathological at slow ones.
- Real journey-executor traffic produces `rate(browser_service_calls_seconds_count{consumer="journey-executor"}[1m]) > 0` in step with journey-step execution + the in-session `buildPageState` capture path.

## Rollback playbook

Same shape as 4b conceptually — flip the per-consumer mode + watchdog back to off and let Cloud Run roll a new revision — but the **mechanism is GitHub Actions secret update + deploy-workflow re-run**, not a tfvar-file edit, because there are no committed tfvars in this repo. Two granularities, both one-operation:

- **Surgical (default)**: in the affected GitHub Actions Environment (`staging` or `production`), update both secrets at once via Settings → Environments or `gh secret set`:
  ```
  $ gh secret set TF_VAR_JOURNEY_EXECUTOR_BROWSING_MODE       --env <env> --body "local"
  $ gh secret set TF_VAR_JOURNEY_EXECUTOR_SMOKE_CHECK_ENABLED --env <env> --body "false"
  $ gh workflow run <deploy-workflow>.yml --ref main          # re-run the deploy
  ```
  Both secret updates are required: `CapturePageSmokeCheck.prepare()` throws `IllegalStateException` on startup if `smoke-check.enabled=true` while `mode!=remote` (the mode-gate added in 4a.4 to prevent false-green metrics from a local-mode probe). Flipping mode without disabling the watchdog crash-loops the next Cloud Run revision and turns rollback into an outage. End-to-end rollback (secret edit → workflow run → terraform apply → Cloud Run rolling redeploy) targets ≤10 minutes; verify by tailing the workflow run + watching Cloud Run revision status. PageBuilder + element-enrichment are unaffected because each reads its own per-consumer secret.
- **Coarse / multi-consumer**: only useful for consumers that haven't adopted a per-consumer pin (today, page-builder pre-retrofit). Updating `TF_VAR_LOOKSEE_BROWSING_MODE = "local"` in the affected Environment flips those; consumers with their own `TF_VAR_<CONSUMER>_BROWSING_MODE` pin (element-enrichment after 4b ships, journey-executor after this phase) are unaffected. To roll back multiple per-consumer-pinned consumers at once, update each consumer's pin individually — there is no shared rollback knob in this design. Trade-off accepted to preserve the staged-flip gate. For each consumer being rolled back with its watchdog on, also set its `TF_VAR_<CONSUMER>_SMOKE_CHECK_ENABLED = false` in the same secret-update batch, for the mode-gate crash-loop reason.

> **Rollback dry-run.** Per the umbrella's "rollback dry-run within 7 days of prod flip" gate, exercise the surgical path against staging *before* the prod flip — set the secret pair, watch the workflow + Cloud Run rollout, confirm `LOOKSEE_BROWSING_MODE=local` on the new revision via `gcloud run revisions describe`, then flip back. Captures the secret-update → workflow-run muscle memory before it's needed under incident pressure.

## Critical files

Tracked source artifacts in this repo (Commits 1 + 2):

- `journeyExecutor/pom.xml` — Commit 1 (add `spring-boot-starter-actuator` + `micrometer-registry-stackdriver` dependencies)
- `journeyExecutor/src/main/java/com/looksee/journeyExecutor/config/BrowsingClientMetricsConfig.java` — Commit 1 (new file)
- `journeyExecutor/src/test/java/com/looksee/journeyExecutor/config/BrowsingClientMetricsConfigTest.java` — Commit 1 (new file, `@SpringBootTest` covering common-tag wiring with negative control)
- `journeyExecutor/src/main/resources/application.yml` — Commit 1 (`looksee.browsing.*` env-var bindings)
- `LookseeIaC/GCP/variables.tf` — Commit 2 (two new variables: `journey_executor_browsing_mode`, `journey_executor_smoke_check_enabled`)
- `LookseeIaC/GCP/modules.tf` — Commit 2 (`journey_executor_cloud_run` block, add sibling `plain_environment_variables` with all seven `LOOKSEE_BROWSING_*` keys)

Untracked-in-repo artifacts (Commits 3a + 3b — these are GitHub Actions Environment secret updates, not file edits, per `LookseeIaC/GCP/README.md` §GitHub Actions secrets reference):

- GitHub Actions Environment `staging` — secrets `TF_VAR_JOURNEY_EXECUTOR_BROWSING_MODE`, `TF_VAR_JOURNEY_EXECUTOR_SMOKE_CHECK_ENABLED`, and (if not already set from 4a.5) `TF_VAR_LOOKSEE_BROWSING_SERVICE_URL`. Audit-trail link: GitHub Settings → Environments → `staging` → "Environment secrets" history. Commit 3a's tracking-issue entry references the secret-update timestamps + the deploy workflow run ID that applied them.
- GitHub Actions Environment `production` — same three secrets, scoped to the `production` Environment. Audit-trail link as above. Commit 3b's tracking-issue entry references the same.

**Not touched: `AuditController.java`.** The umbrella's prescribed swap is unsafe here (see §"Why the umbrella's 4c.1 prescription is wrong").

## Issue + PRs

One tracking issue covering all three commits, since the burn-in is the unit of completion. Each PR body (Commits 1 + 2) links to this plan doc. Commits 3a + 3b are not PRs but secret-update operations — record each in the tracking issue as a comment containing the secret names changed, the timestamp, the deploy workflow run URL, and a `gcloud run revisions describe` snippet showing the new env-var values on the live revision. Closing the tracking issue happens on Commit 3b's secret-update applied + 1-hour observation green.

## Follow-ups (separate PRs, but one of them is a 4c gate)

- **PageBuilder missing `micrometer-registry-stackdriver` dependency — gating prereq for 4c Commit 3a.** PageBuilder declares `spring-boot-starter-actuator` but inherits the stackdriver registry only via `<dependencyManagement>` without an explicit `<dependencies>` entry, so `MANAGEMENT_METRICS_STACKDRIVER_ENABLED=true` (set unconditionally by the cloud_run module) is a no-op for PageBuilder today. Phase 4a's burn-in/observation criteria implicitly assume metrics reach Stackdriver — they don't. The fix is a single-line pom addition + redeploy. **Lives in a separate PR (not 4c code), but is a hard gate on Commit 3a:** the cross-consumer guardrail in 4c's burn-in (`consumer=page-builder` panel staying green) cannot be evaluated against empty data. If PageBuilder's fix hasn't shipped + been redeployed by the time staging burn-in starts, **block Commit 3a** until it has. The Prerequisites checklist lists this as an explicit `[ ]` item.

## Definition of done

Each item below maps to a specific verifiable artifact — either a merged commit on `main` (for Commits 1 + 2), or a recorded GitHub Actions Environment secret update + deploy workflow run ID (for Commits 3a + 3b):

- [ ] **Commit 1 merged on `main`** — squash-merge SHA recorded in the tracking issue. Files: `journeyExecutor/pom.xml`, `…/config/BrowsingClientMetricsConfig.java`, `…/config/BrowsingClientMetricsConfigTest.java`, `…/resources/application.yml`. CI green: `mvn verify` on journeyExecutor passes including the new `@SpringBootTest`.
- [ ] **Commit 2 merged on `main`** — squash-merge SHA recorded. Files: `LookseeIaC/GCP/variables.tf`, `LookseeIaC/GCP/modules.tf`. `terraform plan` against staging + production both show "Cloud Run revision change, env vars added, values inert" (the expected behavior; see §Verification).
- [ ] **Commit 3a applied** — three GitHub Actions Environment secret updates against `staging` recorded in the tracking issue (secret names + timestamps), plus the deploy workflow run URL that picked them up, plus `gcloud run revisions describe` output showing `LOOKSEE_BROWSING_MODE=remote` on the live staging revision.
- [ ] **48 consecutive hours of green staging burn-in** — Stackdriver dashboard screenshot or query timestamps appended to the tracking issue covering all six pass criteria (minimum-traffic guard, error rate, p95, mean, smoke-check failure rate, cross-consumer guardrail) over the burn-in window.
- [ ] **Commit 3b applied** — same shape as 3a but against the `production` Environment. Tracking-issue entry includes the secret-update record + workflow run + revision describe.
- [ ] **60 consecutive minutes of green prod observation** — same dashboard-evidence convention as the staging entry, scoped to the prod observation window.
- [ ] **No rollback executed at any point** — verifiable by the tracking issue containing zero rollback-event entries (rollbacks would also produce their own secret-update records, so absence is auditable).

When all seven boxes check out, **phase 4c is done — and phase 4 is complete.** Every browser-using consumer in the umbrella's census runs against browser-service. Mention in the next LookseeCore release CHANGELOG. The 7-day calm window from the umbrella applies before declaring "done done" once production traffic is real, but is currently aspirational per the project's not-yet-in-prod posture.
