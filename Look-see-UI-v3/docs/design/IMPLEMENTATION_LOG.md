# Implementation log — Phase A foundation

Record of what was actually shipped vs. what each spec calls for. Treat each
"Shipped" item as done; "Follow-up" items are the remaining scope from that
spec.

---

## Spec 01 — Design system & tokens

**Shipped (M1 + M2):**

- `src/theme/tokens.scss` — 40+ semantic CSS custom properties (surfaces,
  text, borders, score, brand chips, focus ring, elevation, radius, motion)
  with full `[data-theme='dark']` overrides.
- `src/theme/typography.scss` — @font-face + 12 named typography utilities
  (`text-display-xl`, `text-display`, `text-heading-lg`, `text-heading`,
  `text-subheading`, `text-body-lg`, `text-body-token`, `text-body-sm-token`,
  `text-caption`, `text-overline`, `text-mono`).
- `src/theme/material-overrides.scss` — Angular Material's buttons, dialogs,
  progress, tooltip, snackbar, form-fields rebound to Look-see tokens.
- `tailwind.config.js` — added `ink` palette (alongside legacy `charcoal`),
  extended score palette with semantic `bg`/`text` variants, added
  `surface.*` semantic color references, added radius/shadow/motion tokens,
  enabled `darkMode: ['class', '[data-theme="dark"]']`.
- `src/styles.scss` — imports new theme files; adds global
  `prefers-reduced-motion` opt-out; retired duplicate @font-face and
  inline Material overrides.
- `src/app/components/shared/button/` — `LookseeButtonComponent` (standalone):
  6 variants × 3 sizes × loading state, token-driven, focus-visible ring.
  Registered in AppModule imports.

**Follow-up (out of scope for this pass):**

- ESLint rule `no-hex-in-components` — deferred. Existing code has hundreds of
  hex literals; enforcing would fail lint immediately. Recommended approach:
  add as `warn` first, baseline the current count, ratchet to `error` as
  components migrate.
- Base `h1`-`h6` remap to new typography utilities — left as-is to avoid
  visual churn in this milestone. Migrate per-component as touched.
- Storybook / `/docs/design/components.html` demo page — not built.
- Dark-mode toggle UI — groundwork in tokens only; no toggle shipped.
- Full Material theme rebuild via `mat.define-theme()` — shipped as overrides
  instead (lower risk).

---

## Spec 02 — Audit status & live progress

**Shipped (M1 client-only slice):**

- `src/app/models/audit-status.ts` — `AuditStatus` enum (QUEUED / RUNNING /
  COMPLETE / FAILED / CANCELLED), `auditStatusChipKind()` mapper, terminal-
  status predicate, and `normaliseAuditStatus()` that maps raw backend status
  strings (including lowercase / legacy variants) into the enum. Fallback
  resolves from score-presence when unknown.
- `audit-list.component.ts` — imports the enum; adds `statusFor()` that reads
  `record.status` and presence of scores to derive the correct chip kind.
- `audit-list.component.html` — renders `<looksee-status-chip>` per row;
  skeleton rows via `<looksee-skeleton variant="row">`; empty state via
  `<looksee-empty-state>` (also fixed a pre-existing bug where the empty
  state guard `!audit_records` was always falsy).

**Follow-up:**

- Full `AuditProgressScene` component (screenshot + streaming feed + ETA +
  cancel + notify-me) — not built. This requires backend `auditUpdate` payload
  changes (progressPercent, findingsPreview, latestFindingLabel,
  screenshotUrl) which are out of scope for a front-end-only pass.
- `cancelAudit()` / `retryAudit()` / `subscribeToCompletion()` service
  methods — not added.
- Retiring `audit-onboarding.component` — kept intact; flip once progress
  scene ships.
- WebSocket merge-on-reconnect replay — not implemented.

---

## Spec 03 — Results architecture refactor

**Deferred entirely.** Full implementation (6 new components + store + data-
model additions + keyboard shortcuts + mark-done + projected score) is too
large for a single session and depends on backend data-contract decisions
listed in the spec's Risks section.

**Recommended next steps:** ship behind `results_v2` feature flag as the spec
outlines. Start with the component tree skeleton (`AuditResultsShell`,
`IssueNavPanel`, `ScreenshotCanvas`, `IssueDetailPanel`) rendering today's
data shape, then iterate.

---

## Spec 04 — Navigation & IA

**Shipped (M1 slice):**

- `src/app/app-routing.module.ts` — added redirect aliases for forward-
  compatible paths (`/home`, `/sites`, `/audits`, `/audits/:id`, `/settings`,
  `/integrations`, `/help`) that map to existing components; redirected the
  orphaned `/dashboard` → `/audit`; added wildcard 404 route.
- `src/app/components/not-found/` — `NotFoundComponent` with friendly 404
  page pointing back to home / audits / how-it-works.
- `src/app/services/breadcrumb/breadcrumb.service.ts` — `BreadcrumbService`
  with `set()` / `clear()` / `snapshot()` and `crumbs$` observable.
- `src/app/components/shared/breadcrumb-bar/` —
  `LookseeBreadcrumbBarComponent` (standalone), renders nothing when no
  crumbs; proper `nav aria-label="Breadcrumb"` + `aria-current="page"` on
  trailing segment.

**Follow-up:**

- Full canonical rename — Domain → Site, `/audit` → `/audits`, etc. requires
  updating 19 `router.navigate` / `routerLink` call-sites + all UI copy. Not
  attempted to avoid regressions without tests.
- `/home` component — redirects to `/audit` for now.
- Sidebar redesign with Workspace / Learn sections.
- Public `/r/:shareToken` route — needs backend share-token endpoint.
- Wiring `<looksee-breadcrumb-bar>` into route components — need to place in
  `AppComponent` layout shell; not added yet.

---

## Spec 05 — Landing & onboarding friction fixes

**Shipped (M1 — the three one-day wins):**

- `src/app/utils/url.ts` — `normaliseUrl()` permissive URL parser. Auto-
  prepends `https://` when scheme is missing; accepts any TLD the URL
  constructor accepts; rejects only truly invalid hostnames and polite
  rejections for local / private ranges.
- `landing.component.ts` — replaced hardcoded-TLD regex with `normaliseUrl()`;
  rewrote error copy ("We couldn't reach that URL…" instead of "our servers
  decided to take a coffee break…").
- `audit-form.component.ts` — same URL validator swap; rewrote error copy.
- Copy rewrites:
  - `start-audit-login-required-dialog.html` — removed "Unfortunately";
    reframed as unlock-message: *"Save this audit for later?"* + *"unlock up
    to 10 audits per month."*
  - `start-audit-confirm-dialog.html` — *"Cancel the audit in progress?"* +
    *"Findings collected so far won't be saved."* (replaces "Are you sure
    that you want to start a new audit?…")
- Footer year — already dynamic via `new Date().getFullYear()`; no change
  needed.

**Follow-up:**

- Inline signup card component (triggers on 15s dwell / scroll past 3rd
  issue) — not built.
- Guest per-day rate limit (client-side backoff + friendly 429 card) —
  not built.
- PDF preview mode with watermark — not built.
- Landing page above-/below-the-fold rebuild (trust row, live sample,
  three-step how-it-works) — not built.
- Mobile-block modal email-capture replacement — not built.

---

## Spec 06 — Shared UI components

**Shipped:**

- `LookseeButtonComponent` (part of Spec 01).
- `LookseeStatusChipComponent` — 16 `kind` values covering audit status,
  issue severity, issue status, and generic (beta/new/available/waitlist);
  `sm`/`md` sizes; optional leading dot; SR-only accessible status text.
- `LookseeSkeletonComponent` — 5 variants (text/title/circle/rect/row);
  respects `prefers-reduced-motion`.
- `LookseeEmptyStateComponent` — illustration + title + body + primary /
  secondary actions; `compact` size; uses existing `src/assets/` art.
- `LookseeBreadcrumbBarComponent` (part of Spec 04).

**Follow-up:**

- `LookseeScoreBadgeComponent` upgrade — existing `app-score-badge` still has
  hardcoded hex. Migration left for the next pass.
- `LookseeScoreGaugeComponent` upgrade — same situation; current component
  uses hand-math SVG.
- `LookseeScoreDeltaArrowComponent` — not built.
- `LookseeDataCardComponent` — not built.
- `LookseeSiteCardComponent` — not built.
- `LookseeKbdComponent` — not built.
- `LookseeToastComponent` + `ToastService` — not built.
- Unit tests for each component — not written.
- Storybook / demo page — not built.

---

## Summary — what to validate on first `ng build`

Because this environment does not have `node_modules` installed, no compile /
tsc / ng-build was run. On first run after `npm install`:

1. **Expect no breaking change to existing behaviour.** All new components
   are standalone and imported via AppModule's `imports` array — no existing
   declarations were removed.
2. **Watch for template selector resolution:** `<looksee-status-chip>`,
   `<looksee-skeleton>`, `<looksee-empty-state>`, `<looksee-button>`, and
   `<looksee-breadcrumb-bar>` are now usable from any template declared in
   AppModule.
3. **Route redirects may shift behaviour** for any internal test that hits
   `/dashboard` (now redirects to `/audit`) or bookmarked URLs like
   `/integration` (still works; `/integrations` plural also works via
   redirect).
4. **Material Deep Purple theme still loads** (pre-built CSS). The overrides
   layer on top. If visual regressions appear, the culprit is likely the
   `--mdc-*` CSS variable overrides in `material-overrides.scss`.
5. **ESLint may warn** on the deprecated `isValidURL()` shims in `landing`
   and `audit-form`. Remove these once any remaining callers are migrated
   (grep shows no non-self callers — safe to delete after one release).

## Files created

```
src/theme/
  tokens.scss
  typography.scss
  material-overrides.scss

src/app/utils/
  url.ts

src/app/models/
  audit-status.ts

src/app/services/breadcrumb/
  breadcrumb.service.ts

src/app/components/
  not-found/
    not-found.component.ts
    not-found.component.html
    not-found.component.scss
  shared/
    button/
      button.component.ts
      button.component.html
      button.component.scss
    status-chip/
      status-chip.component.ts
      status-chip.component.html
      status-chip.component.scss
    skeleton/
      skeleton.component.ts
      skeleton.component.html
      skeleton.component.scss
    empty-state/
      empty-state.component.ts
      empty-state.component.html
      empty-state.component.scss
    breadcrumb-bar/
      breadcrumb-bar.component.ts
      breadcrumb-bar.component.html
      breadcrumb-bar.component.scss
```

## Files modified

```
tailwind.config.js
src/styles.scss
src/app/app.module.ts
src/app/app-routing.module.ts
src/app/components/landing/landing.component.ts
src/app/components/audit-form/audit-form.component.ts
src/app/components/audit-list/audit-list.component.ts
src/app/components/audit-list/audit-list.component.html
src/app/components/start-audit-login-required-dialog/start-audit-login-required-dialog.html
src/app/components/start-audit-confirm-dialog/start-audit-confirm-dialog.html
```

No existing files were deleted.
