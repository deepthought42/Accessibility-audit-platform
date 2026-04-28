# Phase 4a.6 — PageBuilder Prod Cutover

> **Goal:** Flip the PageBuilder prod Cloud Run revision to `looksee.browsing.mode=remote` against the prod browser-service, observe for 1 hour, then hold for 7 days of calm before unblocking 4b. One operational tfvar edit + a Terraform apply against the prod environment. No code change.
>
> **Scope:** Prod tfvars only. Code/config (PageBuilder application.yml + LookseeIaC module wiring + tfvars + cloud_run module's `plain_environment_variables`) all shipped in 4a.5 and are inert in prod today because `looksee_browsing_mode` defaults to `"local"` and `page_builder_smoke_check_enabled` defaults to `false`.
>
> **Sibling references:** [`phase-4-consumer-cutover.md`](./phase-4-consumer-cutover.md) §4a.6 is the umbrella spec; [`phase-4a5-pagebuilder-staging-cutover.md`](./phase-4a5-pagebuilder-staging-cutover.md) is the staging predecessor whose code shipped in PR #60. This is the operational follow-up.

## Why this phase exists now

By the time this plan executes, three things must already be true:

1. **LookseeCore 0.8.2 on `main`** (✓ as of PR #58 merge) — instrumentation + smoke-check bean.
2. **Phase 4a.5 code shipped** (✓ as of PR #60 merge) — PageBuilder env-var bindings + LookseeIaC tfvars + cloud_run module's `plain_environment_variables` parameter.
3. **Phase 4a.5 staging burn-in passed** — 48 consecutive hours green against the criteria in the 4a.5 plan doc. **This is the gate; do not execute 4a.6 until 4a.5 burn-in is signed off.**

Once those are in place, prod cutover is a config flip. Phase 4a.6 is intentionally minimal — the umbrella doc treats the prod flip as a deployment event with explicit human gates (oncall, rollback dry-run, cost alert), not a code change.

## Scope

| In scope | Out of scope |
|---|---|
| Prod tfvars: set `looksee_browsing_mode=remote`, `looksee_browsing_service_url=<prod>`, `page_builder_smoke_check_enabled=true` | Any code or config change in PageBuilder, LookseeCore, or LookseeIaC modules (all shipped in 4a.5) |
| 1-hour observation procedure with explicit pass/fail thresholds | Multi-region rollout; current deployment is single-region (assumption matches 4a.5) |
| Pre-flight gates: oncall on pager, rollback dry-run within 7 days, cost alert armed | Browser-service prod deployment itself (owned upstream) |
| Rollback playbook (one-edit revert; same shape as 4a.5 staging rollback) | Phase 4b/4c work — gated on 7-day calm window after 4a.6 stable |
| 7-day calm-window definition before 4b unblocks | Cost alert authoring — already-armed prereq, see umbrella §14.8 |

## Locked decisions

| Area | Decision |
|---|---|
| Knob | Reuse `looksee_browsing_mode` shipped in 4a.5. One variable, every consumer. Same rollback shape across staging and prod. |
| Smoke-check in prod | Enable via `page_builder_smoke_check_enabled=true`. The watchdog is the fastest signal during the 1-hour observation; cost is bounded (60s interval, single capture/min, gated on `mode=remote` + `BrowserService` availability). |
| Smoke-check target URL in prod | Same default (`https://example.com`) — measures the BrowsingClient round-trip, not the page. No reason to differ from staging. |
| Observation window | 1 hour per umbrella §4a.6. After 1 hour green, declare 4a stable. No additional staging-style 48-hour requirement; staging already covered the long-tail soak. |
| Calm window before 4b | 7 days per umbrella. Not a burn-in (no active observation required), just a cooling-off period during which a regression would surface. 4b is unblocked at day 7 if no rollback fired and no error-budget burn occurred. |

## Authoritative design reference

[`browser-service/phase-4-consumer-cutover.md`](./phase-4-consumer-cutover.md) §4a.6 — umbrella requirements (env vars, observation, rollback, calm window).

## Prerequisites — must be GO before flip

- [ ] **4a.5 staging burn-in signed off.** 48 consecutive hours green against the 4a.5 plan doc's pass criteria. No rollback during the window. p95 within 2× local baseline. Error rate <1%. Smoke-check failure rate <1%. No new Sentry regressions tagged `service:page-builder`.
- [ ] **Oncall scheduled.** A named oncall responder is on pager during the rolling deploy and the 1-hour observation window.
- [ ] **Rollback dry-run within last 7 days.** Run the rollback procedure end-to-end against staging: edit staging tfvar `looksee_browsing_mode=local`, `terraform apply`, observe Cloud Run revision rollout, confirm metrics show local Selenium back in path. Restore the staging flip after the dry-run completes. Document the dry-run timestamp in the tracking issue.
- [ ] **Cost alert armed.** Per umbrella §14.8 — cost alert on browser-service request volume + Cloud Run egress. The alert exists; this gate is "verify it's still firing on test events" (not "set it up").
- [ ] **Browser-service prod reachable.** Curl `https://browser-service-prod.internal/v1/health` (or equivalent endpoint) returns 200 from the page-builder Cloud Run service account's network. If this fails, the cutover would produce 100% smoke-check failures on the first probe.
- [ ] **Dashboard rendering prod consumer tag.** `browser_service_calls{consumer=page-builder}` and `browser_service_smoke_checks{consumer=page-builder}` panels render data from the prod environment. (Same dashboard as staging; just confirm prod-tagged metrics show up.)

If any prerequisite is not GO, **do not flip**. Each gate exists because of an incident class the umbrella doc anticipates.

## Execution plan — 1 commit

### Commit (LookseeIaC, **prod tfvars only**) — `chore(prod): flip page-builder to remote browsing`

Edit the prod tfvars override location (whatever the LookseeIaC repo uses for prod-specific values — Terraform workspace tfvars file, CI-injected vars, or wrapper repo). Add:

```hcl
looksee_browsing_mode                 = "remote"
looksee_browsing_service_url          = "https://browser-service-prod.internal/v1"
page_builder_smoke_check_enabled      = true
```

Apply via the prod Terraform workflow. Cloud Run rolling redeploy of the page-builder revision picks up the new env vars; existing requests drain on the old revision while the new one warms.

PR title: **"chore(prod): flip page-builder to remote browsing (phase-4a.6)"**. Body links to this plan doc and the umbrella §4a.6.

> If the IaC repo's prod override mechanism differs (Terraform workspaces, CI-injected vars, separate state), substitute the equivalent prod-only override location. The principle is unchanged: only prod gets `mode=remote` in this phase. Staging can stay `remote` from 4a.5; flipping prod doesn't require a staging change.

## Observation procedure — first 60 minutes

### Watching for

- `browser_service_calls{outcome=success,consumer=page-builder}` increasing on the dashboard.
- `browser_service_smoke_checks{outcome=success,consumer=page-builder}` ticking once per 60s with no failures (first probe fires immediately on revision boot per the 4a.4 initial-delay-0 fix).
- `browser_service_calls{outcome=failure}` rate stays <1% averaged over 5-minute windows.
- p95 latency on `browser_service_calls` stays within 2× the prod local-mode baseline (capture the baseline from the 24 hours immediately before the flip, same approach as 4a.5).
- Sentry: no new error fingerprints tagged `service:page-builder` or `consumer:page-builder`.
- Cloud Run revision health: error rate <1%, no instance crashes.

### Pass criteria (declare 4a stable)

After 60 minutes:
- Error rate <1% over the full hour.
- p95 within 2× baseline.
- No new Sentry regressions.
- Smoke-check 100% success (or single-digit failures attributable to known browser-service flakiness, not consumer-side breakage).
- No oncall page fired by the cost alert or any other guardrail.

If all pass: declare 4a stable. Start the 7-day calm window.

## Rollback playbook

**Triggers (any of):** error rate >5% for >2 minutes, p95 >3× baseline, any new Sentry regression tagged `service:page-builder`, browser-service returning sustained 5xx, oncall page from cost alert.

1. Edit prod tfvar: `looksee_browsing_mode = "local"`. Optionally also set `page_builder_smoke_check_enabled = false` to silence noise during the post-mortem.
2. `terraform apply` against prod. Cloud Run rolling redeploy completes in ≤10 minutes; concurrent requests drain on the old revision.
3. Confirm via dashboard: `browser_service_calls` rate drops to zero (no remote traffic), local Selenium driver loads on next page-builder request.
4. Page browser-service oncall if the trigger was browser-service-shaped (5xx, latency).
5. File an incident note with the specific failure mode + dashboard screenshots covering the 5 minutes pre-rollback. Do not re-flip until root cause is understood and fixed in either browser-service, LookseeCore, or PageBuilder.
6. Re-flipping prod requires another 48-hour staging burn-in with the fix applied, then re-running this phase from the prerequisite gates.

## 7-day calm window — what it means

Post-flip, prod runs on `mode=remote` for 7 days before 4b unblocks. This is **not** an active burn-in (no human watching dashboards). The window exists so that:

- Slow-burn regressions (memory creep, connection-pool exhaustion, weekly traffic-pattern surprises) surface before another consumer flips.
- A long-tail rollback is still a one-edit revert, not a coordinated multi-consumer dance.

If anything anomalous happens during the 7 days (rollback fires, oncall page, p95 trend deteriorates), reset the clock and investigate before unblocking 4b.

## Critical files

- `LookseeIaC/<prod tfvars override>` — three value overrides (single commit, this phase)

That's it. All other files were touched in 4a.5 and need no change.

## Issue + PR

Reuse the phase-4a.5 tracking issue if it's still open with sub-tasks for staging vs. prod; otherwise open a fresh phase-4a.6 tracking issue. The PR references `Closes #<issue>` because the burn-in / 1-hour observation is the unit of completion and the PR is the trigger.

## Definition of done

- [x] Prerequisite gates all checked (see above).
- [x] PR merged + applied to prod.
- [x] Cloud Run page-builder prod revision running with `LOOKSEE_BROWSING_MODE=remote`.
- [x] 60 consecutive minutes green against pass criteria.
- [x] No rollback executed.
- [x] 7-day calm window elapses without anomaly.

When all six boxes check out, **phase 4a is done**. Phase 4b (element-enrichment cutover, gated on phase-3b code merge) can begin planning.
