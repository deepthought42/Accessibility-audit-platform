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
| Mode knob | Per-consumer override on top of the shared default, identical pattern to 4b. Add `journey_executor_browsing_mode` (string, default `"local"`); `journey_executor_cloud_run` reads `coalesce(var.journey_executor_browsing_mode, var.looksee_browsing_mode)`. Default `"local"` keeps the IaC commit inert until staging tfvars explicitly flip the per-consumer override to `"remote"`. |
| Smoke-check enable | Per-consumer `journey_executor_smoke_check_enabled` tfvar (default false). Independent burn-in observation per consumer, matching the page_builder + element_enrichment pattern. |
| Smoke-check target URL / interval / browser | Reuse the shared tfvars (`looksee_browsing_smoke_check_target_url`, `_interval`). |
| Prod calm-window | **Skipped.** Project not yet in production, so 7-day prod-stability gates from the umbrella are aspirational. Reinstate before the first real prod traffic. |
| Call-site migration | **None.** The `buildPageState(browser, ...)` path captures from the live post-journey session via `RemoteBrowser`'s phase-3b implementation of `getSource`/`getViewportScreenshot`/`getFullPageScreenshotShutterbug`/`removeDriftChat`/`removeGDPRmodals`. Migrating to `capturePage` would silently lose journey state and produce an xpath/element mismatch. journeyExecutor's structural difference from PageBuilder (capture-after-stateful-journey vs. capture-fresh) requires keeping the in-session capture. |
| Metrics common-tag wiring | New `BrowsingClientMetricsConfig` in `journeyExecutor/src/main/java/com/looksee/journeyExecutor/config/`, copy of PageBuilder's, with `consumer=journey-executor`. `@ConditionalOnBean(MeterRegistry.class)` so deployments without a registry still work. |

## Authoritative design references

- [`browser-service/phase-4-consumer-cutover.md`](./phase-4-consumer-cutover.md) §4c — umbrella spec. **Note:** §4c.1's prescription to migrate to `capturePage` is superseded by this plan; see §"Why the umbrella's 4c.1 prescription is wrong" above.
- Commit `2d22d2e` (`refactor(page-builder): migrate buildPageState → capturePage + browsing config`) — the 4a.2 swap. Referenced for the metrics common-tag config shape only; the call-site swap there is **not** copied here for the architectural reason above.
- [`phase-4b-element-enrichment-cutover.md`](./phase-4b-element-enrichment-cutover.md) — per-consumer mode-override pattern to copy.

## Prerequisites — must be GO before execution

- [x] Phase-3b code merged to main (PR #38, commit `748d42a`). Without it, `step_executor.execute(browser, step)` and the element-state extraction branch throw on remote. Confirmed; same prereq satisfied 4b shipped against.
- [x] journeyExecutor pinned to LookseeCore 0.8.2 (`journeyExecutor/pom.xml:15`: `<core.version>0.8.2</core.version>`).
- [x] `journey_executor_cloud_run` module exists in `LookseeIaC/GCP/modules.tf:318`.
- [x] `plain_environment_variables` parameter exists on `modules/cloud_run` (shipped in 4a.5 PR #60). No module-level work needed in 4c.
- [ ] Phase 4b consumer-side wiring + IaC merged (Commit 1 + Commit 2 of 4b plan executed). The shared `looksee_browsing_mode` knob and the per-consumer mode-override pattern need to exist in IaC before 4c reuses them. Once 4b's Commit 2 lands, 4c's IaC commit is additive and small.

## Execution plan — 3 commits across 2 repos

### Commit 1 (journeyExecutor) — `feat(config): consumer=journey-executor metrics common-tag + browsing env vars`

**No call-site refactor.** This commit is config-only: a Micrometer common-tag config and an `application.yml` env-var binding.

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
  description = "Per-consumer override for journey-executor's looksee.browsing.mode. Defaults to 'local' so this commit lands inert; staging/prod tfvars set 'remote' explicitly."
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
  "LOOKSEE_BROWSING_MODE"                   = coalesce(var.journey_executor_browsing_mode, var.looksee_browsing_mode)
  "LOOKSEE_BROWSING_SERVICE_URL"            = var.looksee_browsing_service_url
  "LOOKSEE_BROWSING_SMOKE_CHECK_ENABLED"    = tostring(var.journey_executor_smoke_check_enabled)
  "LOOKSEE_BROWSING_SMOKE_CHECK_INTERVAL"   = var.looksee_browsing_smoke_check_interval
  "LOOKSEE_BROWSING_SMOKE_CHECK_TARGET_URL" = var.looksee_browsing_smoke_check_target_url
}
```

The `coalesce(per_consumer, global)` ordering matches the 4b pattern: explicit per-consumer override wins; otherwise inherit the global. Default `"local"` on the per-consumer override means this commit lands inert in every environment, including staging where `var.looksee_browsing_mode=remote` from 4a.5.

PR title: **"feat(journey-executor): cutover env vars (phase-4c)"**.

### Commit 3 (LookseeIaC, **staging then prod tfvars**) — `chore(<env>): flip journey-executor to remote browsing`

Two **separate** PRs, identical-shape, staggered by the staging burn-in.

#### Commit 3a — staging tfvars

Once Commit 2 has landed and browser-service-staging is reachable:

```hcl
# Staging tfvars override
journey_executor_browsing_mode       = "remote"
journey_executor_smoke_check_enabled = true
```

Apply via the staging Terraform workflow. Cloud Run rolling redeploy of the journey-executor revision picks up the new env vars.

48-hour burn-in pass criteria (umbrella §4a.5, identical to 4b — all expressions reference the timer's exported Prometheus series per umbrella §Observability prereqs):
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
  (Falls back to `browser_service_calls_seconds_max` if the registry doesn't publish a percentile histogram.)
- No new Sentry regressions tagged `service:journey-executor`.
- Smoke-check failure rate <1%: `sum(rate(browser_service_smoke_checks_total{consumer="journey-executor", outcome="failure"}[15m])) / sum(rate(browser_service_smoke_checks_total{consumer="journey-executor"}[15m]))`.
- **Cross-consumer guardrail:** `consumer=page-builder` and `consumer=element-enrichment` metrics stay green throughout — bringing on the third consumer must not destabilize the first two.

#### Commit 3b — prod tfvars

```hcl
# Prod tfvars override
journey_executor_browsing_mode       = "remote"
journey_executor_smoke_check_enabled = true
```

Same six prereqs as 4a.6: staging burn-in signed off, oncall scheduled, rollback dry-run within 7 days, cost alert armed, browser-service prod reachable, dashboard renders prod `consumer=journey-executor` tag.

1-hour prod observation. If green, declare 4c stable.

PR titles: **"chore(staging): flip journey-executor to remote browsing (phase-4c)"** and **"chore(prod): flip journey-executor to remote browsing (phase-4c)"**.

## Verification

### Per-commit

- Commit 1: `cd journeyExecutor && mvn compile`. The new `BrowsingClientMetricsConfig` should pass component-scan; `application.yml` is a resource file, no behavior change with default env vars.
- Commit 2: `terraform plan` — shows new env vars added to journey-executor revision; non-staging environments unchanged because per-consumer mode override defaults to `"local"`.
- Commit 3a/3b: `terraform plan` against the target environment shows `LOOKSEE_BROWSING_MODE` flipping `local → remote` and `LOOKSEE_BROWSING_SMOKE_CHECK_ENABLED=true`.

### Post-deploy (per environment)

- Cloud Run journey-executor revision rolls out with new env vars.
- Boot log shows `CapturePageSmokeCheck started: interval=60000ms target=https://example.com` (only when smoke-check enabled).
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
- **Coarse / multi-consumer**: set the global `looksee_browsing_mode = "local"`. Any consumer that did **not** explicitly override its mode falls back to local; consumers pinned via per-consumer override are unaffected. For each affected consumer that inherits the global and has its watchdog on, also flip its `<consumer>_smoke_check_enabled = false` for the same crash-loop reason.

## Critical files

- `journeyExecutor/src/main/java/com/looksee/journeyExecutor/config/BrowsingClientMetricsConfig.java` — Commit 1 (new file)
- `journeyExecutor/src/main/resources/application.yml` — Commit 1
- `LookseeIaC/GCP/variables.tf` — Commit 2 (two new tfvars)
- `LookseeIaC/GCP/modules.tf` — Commit 2 (`journey_executor_cloud_run` block, add sibling `plain_environment_variables`)
- `LookseeIaC/<staging tfvars>` — Commit 3a
- `LookseeIaC/<prod tfvars>` — Commit 3b
- **Not touched: `AuditController.java`.** The umbrella's prescribed swap is unsafe here.

## Issue + PRs

One tracking issue covering all three commits, since the burn-in is the unit of completion. Each PR body links to this plan doc. Closing the tracking issue happens on Commit 3b merge + 1-hour observation green.

## Definition of done

- [x] Commit 1 merged (metrics common-tag config + application.yml env-var bindings).
- [x] Commit 2 merged (LookseeIaC tfvars + module wiring).
- [x] Commit 3a merged + applied (staging flip).
- [x] 48 consecutive hours of green staging burn-in.
- [x] Commit 3b merged + applied (prod flip).
- [x] 60 consecutive minutes of green prod observation.
- [x] No rollback executed at any point.

When all eight boxes check out, **phase 4c is done — and phase 4 is complete.** Every browser-using consumer in the umbrella's census runs against browser-service. Mention in the next LookseeCore release CHANGELOG. The 7-day calm window from the umbrella applies before declaring "done done" once production traffic is real, but is currently aspirational per the project's not-yet-in-prod posture.
