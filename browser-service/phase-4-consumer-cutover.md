# Phase 4 — Per-Consumer Cutover to Remote Browsing Mode

> **Goal:** Every browser-using Look-see consumer runs with `looksee.browsing.mode=remote` in production. Each flip is preceded by any call-site refactor that's needed to route through `capturePage` or other mode-agnostic methods, staged via a staging-environment validation before prod, observable, and rollback-safe via a one-flag revert.
>
> **Where to work:** the Look-see monorepo plus (for deployment) whatever infra repo owns the consumer manifests. Each sub-phase below describes its own feature branch.
>
> **Sibling references:** this doc mirrors [`phase-3-looksee-shim.md`](./phase-3-looksee-shim.md) and [`phase-3b-element-handle-ops.md`](./phase-3b-element-handle-ops.md) in structure. Phase 3 shipped the `RemoteBrowser` shim; phase 3b (plan doc merged, code pending) wires element-handle ops through it. Phase 4 is the actual rollout.

## Why this phase exists now

Phase 3 (LookseeCore 0.6.0, merged) and phase 3b (plan merged, code pending) are invisible to production today — every running instance of `PageBuilder`, `element-enrichment`, `journeyExecutor` still opens a local Selenium driver on boot. The shim works, the tests pass, but nothing in prod drives a browser via `browser-service` yet. Phase 4 closes that loop: one consumer at a time, config flip + rollback plan + observability.

The monorepo survey (run during planning) found three browser-using consumers and ten that never touch `Browser` at all. The ten no-op consumers pick up every LookseeCore release automatically. The three active consumers are the target of this doc.

## Scope

| In scope | Out of scope |
|---|---|
| LookseeCore 0.7.1: Micrometer instrumentation on `BrowsingClient` facade methods; structured warn logs on failure | Adding metrics beyond the facade (e.g. `RemoteBrowser`-level tracing) |
| Per-consumer call-site refactors where required (PageBuilder 4a.2, journeyExecutor 4c.1) | Deep refactors beyond what the flip needs; the element-state branch in PageBuilder is specifically called out as phase-3b-dependent |
| Deployment config plumbing: `LOOKSEE_BROWSING_*` env vars on Dockerfile + CI workflow per consumer | Spring profile (`application-prod.yml` / `application-staging.yml`) introduction — only if the plain env-var pattern runs into ergonomics issues |
| Per-sub-phase rollout cadence: staging 48h burn-in → prod flip → one-hour rollback SLA | Browser-service deployment itself — owned by `brandonkindred/browser-service` (see Prerequisites) |
| Observability: dashboard + smoke-check before each flip | Feature-flag infra (`togglz`/`launchdarkly`/…); none in use today and not adopted here |
| Rollback playbook per sub-phase | Multi-region / multi-tenant rollout (single-region assumption matches current deployment model) |

If something looks refactor-tempting beyond the per-sub-phase checklist — resist. Phase 4 is binary and reversible by design; cleanup lives in later releases.

## Locked decisions (from planning)

| Area | Decision |
|---|---|
| Sub-phase split | **4a** PageBuilder → **4b** element-enrichment → **4c** journeyExecutor, sequential, each gated on the previous being stable for 7 days in prod. |
| Phase-3b dependency | 4a / 4b / 4c all require **phase-3b code merged** — not just the plan doc (see §14.2 and §14.9 in [`phase-3b-element-handle-ops.md`](./phase-3b-element-handle-ops.md)). |
| Browser-service deployment | Hard external prerequisite. Phase 4 does not describe service-side work; it documents the handoff checklist only. |
| Cutover mechanics | Env-var flip on the consumer's deployment manifest. No feature-flag library. Rollback = unset + redeploy. |
| Observability | Dashboard + smoke-check live before every flip. 48h staging burn-in before prod. |
| Version | Each sub-phase bumps LookseeCore patch (0.7.1 / 0.7.2 / 0.7.3) for instrumentation tweaks only. No 0.8.0 unless the facade API changes. |

## Consumer census

Source: monorepo grep sweep during planning. Compare against `phase-3b-element-handle-ops.md` Step 0 sweep before each sub-phase begins — any drift flags a new finding.

### Bucket A — cutover targets (3 consumers)

| Consumer | Sub-phase | Browser call-site count | Pre-flip code change |
|---|---|---|---|
| PageBuilder | 4a | 2 (`getConnection` + `buildPageState`, then `getDomElementStates`) | Migrate `buildPageState` call to `capturePage`; keep `getDomElementStates` branch (needs phase-3b code merged first). |
| element-enrichment | 4b | 3 (`getConnection` + `navigateTo` + `removeDriftChat` + `enrichElementStates`) | None — phase-3b code wires every one; flip is config-only. |
| journeyExecutor | 4c | 4 (`getConnection` + `performJourneyStepsInBrowser` which calls `StepExecutor` + raw `getDriver().getCurrentUrl()` + `buildPage`) | Migrate `buildPage` call to `capturePage`. Phase-3b Step 7.4 handles the `getDriver()` line. StepExecutor refactor is phase-3b Step 7. |

### Bucket B — no-op consumers (10 services)

`AuditManager`, `audit-service`, `CrawlerAPI`, `contentAudit`, `informationArchitectureAudit`, `visualDesignAudit`, `journeyExpander`, `journeyErrors`, `journey-map-cleanup`, `look-see-front-end-broadcaster` — none call `BrowserService.getConnection` / `capturePage` / `BrowserConnectionHelper.getConnection`. Nothing to cut over; they transparently consume LookseeCore 0.7.x. Before each phase-4 release, re-run the grep sweep and confirm none of these grew a browser call site without notice.

## Prerequisites

### Hard external dependencies

1. **browser-service deployment.** Phase 4 cannot start 4a until `brandonkindred/browser-service` ticks:
   - [ ] Published Docker image (tagged release).
   - [ ] Staging URL with documented `/healthz` + `/readyz` endpoints.
   - [ ] Prod URL behind the same health contract.
   - [ ] Documented auth model (see §14.7) or a decision that prod is internal-network-only for now.
   - [ ] Documented rate limits + expected RPS capacity.
   - [ ] Cost alert configured (see §14.8).

   Gate this on a checklist in the browser-service repo, not on this doc. Phase 4 just refuses to start until the checklist is green.

2. **Phase-3b code merged.** The shim's element-handle ops must be real, not `UnsupportedOperationException`. 4a recommendation (see §14.2) is to wait on phase-3b code rather than defer PageBuilder's element-state branch.

3. **LookseeCore release published.** 0.7.0 (phase 3b) on Maven Central; 0.7.1 (facade instrumentation from Step 4a.1) published before 4a.5.

### Consumer-side prereqs

- Consumer Dockerfile accepts `LOOKSEE_BROWSING_*` env vars. Confirm by re-reading each `Dockerfile` before the sub-phase starts; most don't take env-var-driven config today.
- Consumer's GitHub Actions workflow (or equivalent) injects env vars on deploy. Same review.
- Sentry / Cloud Logging pipelines catch failures (existing; verify unchanged).

### Observability prereqs

- A dashboard showing `browser_service_calls_total{consumer, operation, outcome}` + p50/p95/p99 latency + error rate. Live before any flip.
- A smoke-check cron firing `browserService.capturePage(new URL("https://example.com"), CHROME, 1L)` every 60s against the deployed browser-service. Doubles as a watchdog during rollout; reports success via Micrometer counter, failure via Sentry.
- Alert rule: error rate >1% over 5 minutes pages oncall. Document who oncall is.

## Step 0 — Baseline + grep sanity (run once before each sub-phase)

```bash
cd /path/to/Look-see
git checkout main && git pull --ff-only

# Confirm LookseeCore is at expected version.
grep "<looksee.version>" LookseeCore/pom.xml

# Re-run the phase-3b consumer census; no new findings expected.
EXCLUDE='LookseeCore/(looksee-browser|looksee-core/src/test)/|/src/test/'
grep -rn "browserService\\.getConnection\\|browser_service\\.getConnection" --include="*.java" \
  | grep -Ev "$EXCLUDE"
# Expected: exactly the three lines in the Bucket A table above. Any new
# consumer that grew a getConnection call since the last sub-phase is a new
# finding — either bring it into the phase-4 scope or defer it.

# Confirm no-op bucket is still no-op (add-only regression check).
for svc in AuditManager audit-service CrawlerAPI contentAudit informationArchitectureAudit visualDesignAudit journeyExpander journeyErrors journey-map-cleanup look-see-front-end-broadcaster; do
  echo "=== $svc ==="
  grep -rn "getConnection\\|capturePage\\|new Browser(" "$svc/src/main" 2>/dev/null || echo "  (clean)"
done
```

Any surprise here blocks the sub-phase until reconciled.

## Phase 4a — PageBuilder cutover

PageBuilder is first because (a) its flow is the simplest (single-page capture + element extraction), (b) the capture path is already mode-agnostic via `capturePage`, and (c) it has the cleanest deployment manifest to parameterize.

### 4a.0 — Branch + sanity

```bash
git checkout -b phase-4a/page-builder-cutover
cd LookseeCore && mvn -q verify   # 0.7.0 green baseline
```

Run the Step 0 grep sweep. Expect the PageBuilder hit at `PageBuilder/src/main/java/com/looksee/pageBuilder/AuditController.java:245`.

### 4a.1 — LookseeCore 0.7.1: instrument the facade

**File:** `LookseeCore/looksee-browsing-client/src/main/java/com/looksee/browsing/client/BrowsingClient.java`

Inject an optional `MeterRegistry`; use `io.micrometer.core.instrument.Timer` per facade method. Rough shape:

```java
@Autowired(required = false)
private MeterRegistry meterRegistry;

private <T> T recordCall(String operation, SupplierWithException<T> body) {
    long start = System.nanoTime();
    String outcome = "success";
    try {
        return body.get();
    } catch (BrowsingClientException e) {
        outcome = "failure";
        log.warn("BrowsingClient.{} failed: {}", operation, e.getMessage());
        throw e;
    } finally {
        if (meterRegistry != null) {
            meterRegistry.timer("browser_service_calls",
                "operation", operation, "outcome", outcome)
                .record(System.nanoTime() - start, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }
}
```

Wrap every public facade method. The `meterRegistry == null` guard means consumers without a `MeterRegistry` bean don't break on the 0.7.1 upgrade — see §14.4.

**Commit:** `feat(browsing-client): instrument facade methods with Micrometer timers`

### 4a.2 — PageBuilder call-site migration

**File:** `PageBuilder/src/main/java/com/looksee/pageBuilder/AuditController.java`

Before (lines ~244–263):

```java
browser = browser_service.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
page_state = browser_service.buildPageState(url, browser, is_secure, http_status, url_msg.getAuditId());

PageState page_state_record = audit_record_service.findPageWithKey(url_msg.getAuditId(), page_state.getKey());
if (page_state_record == null || element_state_service.getAllExistingKeys(page_state_record.getId()).isEmpty()) {
    List<String> xpaths = browser_service.extractAllUniqueElementXpaths(page_state.getSrc());
    List<ElementState> element_states = browser_service.getDomElementStates(page_state, xpaths, browser, url_msg.getAuditId());
    page_state.setElements(element_states);
    page_state = page_state_service.save(page_state);
}
// ... finally { if (browser != null) browser.close(); }
```

After (simplified — `capturePage` replaces both `getConnection` + `buildPageState`, and opens its own browser internally):

```java
page_state = browser_service.capturePage(url, BrowserType.CHROME, url_msg.getAuditId());

PageState page_state_record = audit_record_service.findPageWithKey(url_msg.getAuditId(), page_state.getKey());
if (page_state_record == null || element_state_service.getAllExistingKeys(page_state_record.getId()).isEmpty()) {
    // Element-state extraction still needs a live browser — retained until
    // PageBuilder migrates to a server-side extraction endpoint in a later phase.
    browser = browser_service.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
    try {
        browser.navigateTo(url.toString());
        List<String> xpaths = browser_service.extractAllUniqueElementXpaths(page_state.getSrc());
        List<ElementState> element_states = browser_service.getDomElementStates(page_state, xpaths, browser, url_msg.getAuditId());
        page_state.setElements(element_states);
        page_state = page_state_service.save(page_state);
    } finally {
        browser.close();
    }
}
```

The page-capture path now uses the one-shot, which works identically in local and remote mode. The element-state branch stays on a live `Browser` — in remote mode that's a `RemoteBrowser`, which requires phase-3b code merged for `getDomElementStates` to work. Do not start 4a.5 until phase-3b code lands.

**Commit:** `refactor(page-builder): migrate buildPageState call site to capturePage`

### 4a.3 — PageBuilder deployment config

**Files:**
- `PageBuilder/src/main/resources/application.yml` — add explicit defaults so ops engineers can see what's tunable:

  ```yaml
  looksee:
    browsing:
      mode: local       # overridden by LOOKSEE_BROWSING_MODE env var
      service-url: ""   # required when mode=remote
      connect-timeout: 5s
      read-timeout: 120s
  ```

- `PageBuilder/Dockerfile` — add `ENV` stanzas or rely on runtime env injection (document which). Spring Boot reads env automatically via Relaxed Binding, so no Dockerfile change is strictly needed if the deployment platform injects env vars.
- `PageBuilder/.github/workflows/*.yml` (whichever handles deploy) — extend deploy step to pass `LOOKSEE_BROWSING_*` env vars from GitHub Actions secrets / variables.

Default value is `local`, so this commit alone does **not** flip anything. It only makes the flip possible.

**Commit:** `chore(page-builder): parameterize browsing mode via env vars`

### 4a.4 — Dashboard + smoke-check

Two infra artifacts, documented here but shipped in whatever infra repo owns observability:

1. **Dashboard panel(s)** — query `browser_service_calls{consumer="page-builder"}`. Panels: total RPS by operation, p50/p95/p99 latency by operation, error rate, distinct failure types (from Sentry grouping).

2. **Smoke-check cron** — a minimal Spring Boot job:

   ```java
   @Component
   @ConditionalOnProperty(name = "looksee.browsing.smoke-check.enabled", havingValue = "true")
   public class CapturePageSmokeCheck {
       @Scheduled(fixedRate = 60_000)
       public void probe() { /* browserService.capturePage(https://example.com, CHROME, -1L) + counter */ }
   }
   ```

   Deploy alongside the first consumer that flips to remote mode. Report to the same dashboard.

Both must be live before 4a.5 flips staging. No code commit here — the artifacts live outside this repo — but document what was put in place.

### 4a.5 — Flip staging

Config-only. Set in the staging deployment manifest:

```
LOOKSEE_BROWSING_MODE=remote
LOOKSEE_BROWSING_SERVICE_URL=https://browser-service-staging.internal/v1
```

Redeploy. Smoke-check should flip to hitting the staging browser-service; dashboard should show `outcome=success` within five minutes.

Burn-in requirement: **48 hours** green with the following success criteria:
- Error rate <1% averaged over any 15-minute window.
- p95 latency within 2× the local-mode baseline (captured from the same dashboard before the flip).
- No Sentry regression in PageBuilder-tagged errors.

Failure at any point during burn-in → rollback (see playbook below) → investigate → fix → restart burn-in.

### 4a.6 — Flip prod

After successful 48h staging burn-in, flip the prod manifest. Same env vars with the prod `SERVICE_URL`. Require:
- Oncall on pager during the rolling deploy.
- Rollback dry-run performed within the last 7 days (unset env var → redeploy → confirm mode reverts).
- Cost alert armed (see §14.8).

Observe for 1 hour; if error rate <1% and p95 is within spec, declare 4a stable. Watch for 7 days before starting 4b.

### 4a rollback playbook

Any of: error rate >5% for >2 minutes, p95 >3× baseline, any Sentry new-regression, browser-service returning 5xx.

1. Unset `LOOKSEE_BROWSING_MODE` (or set to `local`) in the consumer deployment manifest.
2. Redeploy. Rolling-deploy completes in ≤10 minutes.
3. Confirm via smoke-check + dashboard that traffic reverted.
4. File an incident report with the specific failure mode. Do not re-flip until root cause is understood and fixed in either browser-service or the consumer.

## Phase 4b — element-enrichment cutover

### 4b.0 — Prerequisites

- **Phase-3b code merged to main.** Without it, `removeDriftChat` + `enrichElementStates` both throw.
- 4a stable in prod for 7 days (no regression in error rate or latency).
- Smoke-check still green.

Re-run Step 0 grep sanity. Expected: `element-enrichment/src/main/java/com/looksee/pageBuilder/AuditController.java:113` hit unchanged since planning.

### 4b.1 — Deployment config

No call-site refactor. element-enrichment's browser flow uses only page-level ops (`navigateTo`, `close`) and element-handle ops (`removeDriftChat`, `enrichElementStates`) — all remote-compatible after phase-3b ships.

Same three files as 4a.3, adapted:
- `element-enrichment/src/main/resources/application.yml`
- `element-enrichment/Dockerfile`
- `element-enrichment/.github/workflows/*.yml` if present

**Commit:** `chore(element-enrichment): parameterize browsing mode via env vars`

### 4b.2 — Flip staging → prod

Same cadence as 4a.5 / 4a.6: 48h staging burn-in, then prod flip with rollback plan. Reuse the same dashboard + smoke-check (now observing both `consumer=page-builder` and `consumer=element-enrichment`).

## Phase 4c — journeyExecutor cutover

### 4c.0 — Prerequisites

- 4b stable in prod for 7 days.
- Phase-3b code merged (same requirement as 4b; reconfirm in case of drift).

Re-run Step 0 sweep. Expected: `journeyExecutor/.../AuditController.java:214` hit; also line 234 (`buildPage`) and 560 (`step_executor.execute`).

### 4c.1 — Call-site migration

One refactor: migrate `buildPage(browser, ...)` at `journeyExecutor/src/main/java/com/looksee/journeyExecutor/AuditController.java:234` to `capturePage(...)` (same pattern as 4a.2). The `getDriver().getCurrentUrl()` at line 562 is already replaced by `browser.getCurrentUrl()` as part of phase-3b Step 7.4.

**Commit:** `refactor(journey-executor): migrate buildPage call site to capturePage`

### 4c.2 — Deployment config + flip

Same as 4b. 48h staging burn-in → prod flip.

### 4c final gate

After 4c prod-stable for 7 days, every browser-using consumer is remote. Declare phase 4 complete. Note in the next LookseeCore release CHANGELOG.

## Step 5 — Verification + definition of done

Per sub-phase:
- [ ] Staging: 48h green burn-in (error rate <1%, p95 <2× baseline).
- [ ] Prod flip: 1 hour green observation post-flip.
- [ ] Rollback dry-run within the last 7 days.
- [ ] Dashboard + smoke-check live for this consumer.
- [ ] Sentry shows no new failure classes from the switch.
- [ ] Cost alert armed.

Overall (after 4c):
- [ ] `PageBuilder`, `element-enrichment`, `journeyExecutor` all running `LOOKSEE_BROWSING_MODE=remote` in prod.
- [ ] Bucket B (10 services) confirmed unchanged — grep sweep still clean.
- [ ] LookseeCore default `looksee.browsing.mode` remains `local` so a newly-onboarded consumer still works out of the box.
- [ ] All three consumers' `application.yml` has an explicit `looksee.browsing.mode` block (easier for future ops to find than tribal knowledge).

## Push and open PRs

Each sub-phase opens its own PR (code changes + config) against `main`. Title pattern: **"Phase 4a: PageBuilder — migrate to capturePage + parameterize browsing mode"**. Body includes:

- Link to this doc.
- Which sub-phase + which step.
- Dashboard / smoke-check URLs.
- Rollback plan link.
- Explicit note: default mode stays `local`; this PR alone does not flip anything.

The flag flips themselves (4a.5, 4a.6, 4b.2, 4c.2) are config-only and happen in the infra repo / deployment manifest, not in this repo. Note the date + flipper in a CHANGELOG entry on the consumer.

## 14. Open items flagged for reviewer

1. **browser-service deployment is owned by the other repo.** Phase 4 cannot start 4a until browser-service ticks the prerequisite checklist in §Prerequisites. The phase-4 doc includes the checklist but does not describe the service-side work; `brandonkindred/browser-service` owns it.

2. **PageBuilder's `getDomElementStates` branch depends on phase-3b code.** It calls `browser.findElement` + `browser.extractAttributes` through `browser_service.getDomElementStates`. Options:
   - **(a)** Skip the branch in remote mode with a mode-gated early return. **Not recommended** — silently masks missing element data.
   - **(b)** Wait for phase-3b **code** (not just plan doc) to merge before starting 4a.5. **Recommended.**
   - **(c)** Scope 4a to capture-only and defer element-state extraction to "4a-bis" once phase-3b lands. Equivalent in effect to (b), more ceremony.

3. **No feature-flag infra.** Grep shows no `togglz`/`launchdarkly`/`unleash`/`split.io`. Rollout is binary per-consumer. The 48h staging burn-in + observable prod flip is the best we have; adopt a feature-flag library only if phase 4 reveals a case for it.

4. **Micrometer registry not present in every consumer.** `LookseeCore` pins Micrometer 1.10.13 but no `MeterRegistry` bean is auto-wired in consumers by default. The 4a.1 instrumentation uses `@Autowired(required = false)` + `meterRegistry != null` guard so consumers without a registry compile and run unchanged on 0.7.1. Consumers that want the dashboard must add a `MeterRegistry` bean themselves (one-liner).

5. **Spring profile structure missing.** Consumers use the `test` profile in CI; there's no `staging` / `prod`. Phase 4 uses env-var flipping, which works without profiles. Consider adopting `application-staging.yml` / `application-prod.yml` only if the flat env-var pattern runs into ergonomics issues. Not a blocker.

6. **Security finding (unrelated but found during exploration):** `element-enrichment/Dockerfile` bakes GCP + Gmail credentials into the image. Flag for a separate security PR; do **not** conflate with phase 4.

7. **browser-service authentication.** The OpenAPI spec does not currently require auth headers. If browser-service ends up behind auth in prod (recommended), `BrowsingClient` needs a pluggable auth interceptor — cheap to add now, painful once there are three deployed consumers. Phase-4a-pre-flight item: confirm auth model with browser-service owner before 4a.5.

8. **Cost observability.** browser-service spins up real browsers; cutover multiplies instance-hours. Set a Cloud Billing budget alert per sub-phase with a realistic ceiling (staging: $50/week, prod: 2× the pre-flip local Selenium cost). Arm before each prod flip, not after.

9. **Call-site migration pattern is repeatable.** 4a.2 and 4c.1 do the same basic refactor: `getConnection` + `buildPageState|buildPage` → `capturePage`. If future consumers want remote mode, they follow the same template. Document it as a short note in `LookseeCore/CHANGELOG.md` under 0.7.1 so future migrators find it.

10. **Post-phase-4 cleanup candidates.** Once every browser-using consumer is on remote, `LookseeCore/looksee-browser/` is only compiled for the library's default-local mode. Phase 5 (already flagged in phase-3 doc §14) can start retiring it once the last consumer is remote-stable for 30 days.
