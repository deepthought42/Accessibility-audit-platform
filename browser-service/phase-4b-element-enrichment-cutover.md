# Phase 4b — element-enrichment Cutover

> **Goal:** Run element-enrichment with `looksee.browsing.mode=remote` against browser-service, staged through staging burn-in → prod flip with the same observability and rollback plan as phase 4a. No call-site refactor required: element-enrichment uses only page-level ops (`navigateTo`, `close`) and element-handle ops (`removeDriftChat`, `enrichElementStates`), all of which became remote-compatible after phase-3b shipped.
>
> **Sibling references:** [`phase-4-consumer-cutover.md`](./phase-4-consumer-cutover.md) §4b is the umbrella spec. Phase-4a counterparts: [`phase-4a5-pagebuilder-staging-cutover.md`](./phase-4a5-pagebuilder-staging-cutover.md) and [`phase-4a6-pagebuilder-prod-cutover.md`](./phase-4a6-pagebuilder-prod-cutover.md). This plan mirrors their structure since the cutover shape is identical.

## Why this phase exists now

The umbrella doc identified three browser-using consumers requiring cutover: PageBuilder (phase 4a, in flight), **element-enrichment (phase 4b, this plan)**, and journeyExecutor (phase 4c, pending). Phase 4a will close out when the prod flip + 7-day calm window completes; 4b should be ready to execute the moment that gate clears.

Element-enrichment's browser usage is narrower than PageBuilder's:
- `element-enrichment/src/main/java/com/looksee/pageBuilder/AuditController.java:115` calls `browser.removeDriftChat()`.
- `element-enrichment/src/main/java/com/looksee/pageBuilder/AuditController.java:118` calls `browser_service.enrichElementStates(element_states, page_state, browser, url.getHost())`.

Both are already remote-compatible after phase-3b (PR #38 merged), so 4b is **deployment-only** — no Java change, no call-site refactor.

## Scope

| In scope | Out of scope |
|---|---|
| element-enrichment `application.yml`: bind `LOOKSEE_BROWSING_*` env vars (mirrors PageBuilder's 4a.3 single-file change) | Any Java change in element-enrichment, LookseeCore, or LookseeIaC modules |
| Deployment-config wiring for element-enrichment (LookseeIaC Cloud Run module + tfvars), reusing the shared `looksee_browsing_mode` knob and the `plain_environment_variables` parameter from 4a.5 | journeyExecutor work — that's phase 4c, blocked on 4b stable for 7 days |
| 48-hour staging burn-in observation procedure (mirrors 4a.5) | Adding new metrics — instrumentation already covers `consumer=element-enrichment` via per-consumer `MeterFilter.commonTags` |
| 1-hour prod observation + 7-day calm window before 4c (mirrors 4a.6) | Browser-service deployment / prod readiness — owned upstream |
| Rollback playbook (one-edit revert, identical shape to 4a.5/4a.6) | Per-consumer cost alert authoring — already-armed prereq from umbrella §14.8 |

## Locked decisions

| Area | Decision |
|---|---|
| Mode knob | Reuse the shared `looksee_browsing_mode` tfvar shipped in 4a.5. One variable governs every consumer. A 4a/4b combined rollback is one edit. |
| Smoke-check enable | Per-consumer `element_enrichment_smoke_check_enabled` tfvar (default false). Independent burn-in observation per consumer, identical to the `page_builder_smoke_check_enabled` pattern. |
| Smoke-check target URL / interval / browser | Reuse the existing shared tfvars (`looksee_browsing_smoke_check_target_url`, `_interval`). Keep the surface flat — these aren't consumer-specific. |
| Burn-in window | 48 hours staging (umbrella §4b.2 inherits §4a.5). 1 hour prod observation + 7-day calm before 4c. |
| Call-site migration | None. Both element-enrichment call sites (`AuditController.java:115`, `:118`) are already remote-safe after phase-3b. |

## Authoritative design reference

[`browser-service/phase-4-consumer-cutover.md`](./phase-4-consumer-cutover.md) §4b — umbrella spec.

## Prerequisites — must be GO before execution

- [x] Phase-3b code merged to main (PR #38, commit `748d42a`). Without it, `removeDriftChat` + `enrichElementStates` throw on remote.
- [x] element-enrichment pinned to LookseeCore ≥0.8.2 (`element-enrichment/pom.xml` line 49: `<version>0.8.2</version>`).
- [ ] **4a stable in prod for 7 days** (phase 4a.6 calm window elapsed without rollback or anomaly). This is the hard gate; the consumer-side wiring (Step 1 below) can land before this clears since it's inert without the tfvar flip.
- [ ] **Smoke-check still green** for `consumer=page-builder` at the time of 4b kickoff. Indicates browser-service is healthy enough to bring on a second consumer.
- [ ] **Open question (must be resolved before Step 2):** element-enrichment has no Cloud Run module in `LookseeIaC/GCP/modules.tf`. PageBuilder, audit-manager, audit-service, journey-executor, journey-expander, content-audit, visual-design-audit, and information-architecture-audit all have modules; element-enrichment does not. Either:
  - (a) it's deployed via a different IaC mechanism (separate repo, manual Cloud Run deploy, Cloud Build trigger) that needs identifying, or
  - (b) it isn't productionized yet in this IaC topology — meaning 4b.2 deployment work is premature until it is.

The user/team must clarify before Step 2 lands. Step 1 (consumer-side `application.yml`) is unblocked regardless.

## Execution plan — 3 commits across 1–2 repos

### Commit 1 (element-enrichment) — `feat(config): wire looksee browsing env vars (phase-4b)`

Edit `element-enrichment/src/main/resources/application.yml`. Append to the file (mirrors PageBuilder's 4a.3 + 4a.5 combined yaml shape, since element-enrichment's existing yaml has no `looksee:` block today):

```yaml
# LookseeCore browsing configuration. Default mode is `local` — every instance
# opens a local Selenium driver. Flip to `remote` to route browser ops through
# brandonkindred/browser-service; requires LOOKSEE_BROWSING_SERVICE_URL set.
# See browser-service/phase-4b-element-enrichment-cutover.md.
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

Defaults preserve current behavior. No Dockerfile change (Cloud Run reads env at runtime, not build-time, same finding as 4a.3 for PageBuilder).

PR title: **"feat(config): wire looksee browsing env vars for phase-4b cutover"**.

### Commit 2 (LookseeIaC) — `feat(element-enrichment): add cloud_run module + cutover env vars`

**Contingent on resolving the open question above.** If element-enrichment has no Cloud Run module today, this commit creates one mirroring `module "page_builder_cloud_run"` (same pattern, adjusted for element-enrichment's resource sizing, pubsub topics, and image variable). Plus:

- New tfvar `element_enrichment_smoke_check_enabled` (bool, default `false`).
- New tfvar `element_enrichment_image` (string, default `"docker.io/deepthought42/element-enrichment:latest"` — adjust to actual image name).
- Wire `LOOKSEE_BROWSING_*` via `plain_environment_variables` exactly as 4a.5 did for page-builder.

If element-enrichment is deployed via a different mechanism, this commit goes wherever that lives. Either way, the substantive constraint is identical: the same `looksee_browsing_mode` tfvar from 4a.5 governs both consumers.

PR title: **"feat(element-enrichment): cutover env vars (phase-4b)"**.

### Commit 3 (LookseeIaC, **staging then prod tfvars**) — `chore(<env>): flip element-enrichment to remote browsing`

Two **separate** PRs, identical-shape, staggered by the staging burn-in:

#### Commit 3a — staging tfvars

Once Commit 2 has landed and browser-service-staging is reachable:

```hcl
# Staging tfvars override
element_enrichment_smoke_check_enabled = true
# looksee_browsing_mode and looksee_browsing_service_url are already
# set in staging from 4a.5 — no change needed there. Both consumers
# will share mode=remote against the staging browser-service URL.
```

Apply via the staging Terraform workflow. Cloud Run rolling redeploy; observation begins.

48-hour burn-in pass criteria (umbrella §4a.5, identical):
- Error rate <1% averaged over any 15-minute window (`browser_service_calls{outcome="failure",consumer="element-enrichment"}`).
- p95 latency within 2× the local-mode baseline for element-enrichment specifically (capture from the 24 hours pre-flip).
- No new Sentry regressions tagged `service:element-enrichment`.
- Smoke-check failure rate <1%.
- **Cross-consumer guardrail:** `consumer=page-builder` metrics stay green throughout — bringing on a second consumer must not destabilize the first.

#### Commit 3b — prod tfvars (gated on 3a 48h green + the same human gates as 4a.6)

```hcl
# Prod tfvars override
element_enrichment_smoke_check_enabled = true
```

Same six prereqs as 4a.6: 4b staging burn-in signed off, oncall scheduled, rollback dry-run within 7 days, cost alert armed, browser-service prod reachable, dashboard renders prod `consumer=element-enrichment` tag.

1-hour prod observation. If green, declare 4b stable. 7-day calm window before 4c.

PR titles: **"chore(staging): flip element-enrichment to remote browsing (phase-4b)"** and **"chore(prod): flip element-enrichment to remote browsing (phase-4b)"**.

## Verification

### Per-commit

- Commit 1: `cd element-enrichment && mvn compile` — no behavior change with default env vars. The `LookseeBrowsingProperties` bean is present transitively via the `A11yCore` 0.8.2 pin; no extra dependency add needed.
- Commit 2: `terraform plan` — shows new module + env vars added; non-staging environments unchanged because tfvar defaults preserve current behavior.
- Commit 3a: `terraform plan` against staging shows `LOOKSEE_BROWSING_SMOKE_CHECK_ENABLED=true` flip on element-enrichment revision. (`LOOKSEE_BROWSING_MODE=remote` is already in place from 4a.5.)
- Commit 3b: same against prod once 3a is signed off.

### Post-deploy (per environment)

- Cloud Run element-enrichment revision rolls out with new env vars.
- Boot log shows `CapturePageSmokeCheck started: interval=60000ms target=https://example.com`.
- First probe fires immediately (initial-delay 0 from the 4a.4 fix).
- Dashboard renders `browser_service_smoke_checks{outcome=success,consumer=element-enrichment}` ticking once per 60s.
- Real element-enrichment traffic produces `browser_service_calls{outcome=success,consumer=element-enrichment}` increasing in step with `removeDriftChat` + `enrichElementStates` invocations.

## Rollback playbook

Identical shape to 4a.5/4a.6: edit the relevant tfvar override (set `element_enrichment_smoke_check_enabled=false` and/or `looksee_browsing_mode=local`), `terraform apply`, ≤10-minute Cloud Run rolling redeploy reverts.

**Important:** since `looksee_browsing_mode` is shared across consumers, flipping it to `local` rolls back **both** PageBuilder and element-enrichment simultaneously. If only element-enrichment needs to roll back (page-builder is healthy), flip `element_enrichment_smoke_check_enabled=false` to silence the watchdog and remove the consumer's env-var override more surgically — or split `looksee_browsing_mode` into per-consumer variables before doing element-enrichment-only rollbacks. The latter is a meaningful design choice; defer until needed.

## Critical files

- `element-enrichment/src/main/resources/application.yml` — Commit 1
- `LookseeIaC/GCP/modules.tf` — Commit 2 (new `element_enrichment_cloud_run` module, contingent on resolving the open question)
- `LookseeIaC/GCP/variables.tf` — Commit 2 (new tfvars)
- `LookseeIaC/<staging tfvars>` — Commit 3a
- `LookseeIaC/<prod tfvars>` — Commit 3b

## Issue + PRs

One tracking issue covering all three commits, since the burn-in is the unit of completion. Each PR body links to this plan doc. Closing the tracking issue happens on Commit 3b merge + 1-hour observation green + 7-day calm passed (same definition of done as 4a).

## Definition of done

- [x] Commit 1 merged (consumer-side wiring).
- [x] Commit 2 merged (LookseeIaC element-enrichment module + tfvars).
- [x] Commit 3a merged + applied (staging flip).
- [x] 48 consecutive hours of green staging burn-in.
- [x] Commit 3b merged + applied (prod flip).
- [x] 60 consecutive minutes of green prod observation.
- [x] No rollback executed at any point.
- [x] 7-day calm window elapsed without anomaly.

When all eight boxes check out, **phase 4b is done** and 4c (journeyExecutor cutover, requires one call-site refactor at `journeyExecutor/.../AuditController.java:234`) can begin planning.
