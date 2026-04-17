---
id: 06
title: Shared UI component library
status: Draft
phase: B
addresses:
  - Template duplication: score icon display repeated 20+ times
  - No reusable score badge, card, empty-state, or skeleton components
  - Component SCSS files redefine global styles
  - No shared visual pattern for status chips
  - Inline SVG score gauges with manual circumference math in multiple places
owner: Design + Frontend
related:
  - 01-design-system.md
  - 02-audit-status-and-progress.md
  - 03-results-architecture.md
  - UX_REDESIGN.md §8
---

# 06 — Shared UI component library

## Problem

The codebase has no shared component library — only ad-hoc patterns duplicated across templates. Specifically:

- **Score icon display** (check / warning / radiation icons tied to score value) appears in ≥ 20 template locations, each with its own `*ngIf` ladder.
- **Score gauge** (circular progress) is implemented via inline SVG with manual circumference math in `page-audit-review`, `audit-dashboard`, and `audit-list` — three different versions.
- **Empty states** use plain `<h1>`/`<h2>` with no illustration, inconsistent CTA style.
- **Loading states** are mostly "Please wait…" text; no skeleton loaders.
- **Status indicators** (pending/complete/failed) don't exist at all; have to infer from score presence.
- **Card styling** is re-declared per component; spacing, border, and shadow drift.

## Goals

Ship a small, well-designed component library covering **11 reusable primitives** used across all current and planned screens. Each component:

- Is an Angular standalone component under `src/app/components/shared/`.
- Consumes design tokens from spec 01 (no hex literals).
- Has documented inputs and usage examples.
- Has unit tests (Angular testing library or Jest-compat).
- Ships with a Storybook entry or a single-page demo in `/docs/design/components.html`.

## Non-goals

- A full design system framework (Material / Ionic replacement). We're using Angular Material underneath; these components are application-layer.
- Animation library. Motion uses Angular Animations API with tokens from spec 01.
- A publishable npm package. These live in the app; extraction to a library happens later if ever.

## User stories

- *As a front-end dev*, I need a score badge, I import `<looksee-score-badge>`, pass a value, done.
- *As a designer*, I need the status chip to look the same everywhere — so I specify once, and every instance follows.
- *As Morgan*, I see consistent patterns across the product — which makes it feel reliable.

## Design — the 11 components

### 1. `<looksee-button>`

Covered in spec 01 §5. Variants, sizes, loading state. Recap: `primary | accent | secondary | ghost | danger | link` × `sm | md | lg` × `[loading]`.

### 2. `<looksee-status-chip>`

Small pill indicating the state of an audit, a page, or an issue.

**Inputs:**
```ts
type ChipKind =
  | 'queued' | 'running' | 'complete' | 'failed' | 'cancelled'  // audit status
  | 'critical' | 'major' | 'minor'                              // issue severity
  | 'open' | 'in-progress' | 'done' | 'ignored'                 // issue status
  | 'beta' | 'new' | 'available' | 'waitlist';                  // generic

@Input() kind: ChipKind;
@Input() size: 'sm' | 'md' = 'md';
@Input() label?: string;   // override default label text
@Input() showDot = false;  // leading dot (used in list rows)
```

**Visual:**
```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ ● Auditing…  │    │ Complete     │    │ ⚠ Failed    │
└──────────────┘    └──────────────┘    └──────────────┘
   bg: brand.50     bg: score.good.50  bg: score.critical.50
   text: brand.700  text: score.good   text: score.critical
```

**A11y:** chip includes a visually-hidden phrase like "Status: Auditing" so screen readers are explicit.

### 3. `<looksee-score-badge>`

Small, inline score representation. Replaces the duplicated icon ladders.

**Inputs:**
```ts
@Input() value: number;                  // 0–100 or null
@Input() size: 'sm' | 'md' | 'lg' = 'md';
@Input() variant: 'pill' | 'circle' = 'pill';
@Input() showLabel = true;               // show "/ 100" suffix
@Input() muted = false;                  // for loading/placeholder
```

**Color thresholds** (from tokens):
- `value >= 80` → good
- `value >= 60` → warning
- `value < 60` → critical
- `value === null` → muted

**Sizes:**
```
sm (h-6, text-xs):   [ 72 / 100 ]
md (h-8, text-sm):   [  72  ]
lg (h-12, text-lg):  [  72  ]
```

**Circle variant:** small circular chip with just the number; used in tight spaces (audit list rows, hotspots).

### 4. `<looksee-score-gauge>`

Large circular progress gauge, used on result headers and dashboard cards.

**Inputs:**
```ts
@Input() value: number;                      // 0–100
@Input() size: 'sm' | 'md' | 'lg' | 'xl' = 'lg';
@Input() label?: string;                     // centered below value
@Input() trend?: number;                     // +/- delta from previous
@Input() projected?: number;                 // projected score (after fixes)
```

**Visual (lg, 120px):**
```
   ╭─────────────╮
  ╱   72         ╲      ← center: score, display type
 │    /100       │      ← subtext
  ╲   ↑ 4        ╱      ← trend arrow in good/critical
   ╰─────────────╯
```

**Implementation:** SVG with a single CSS-animated arc. Replace the three hand-math versions with this one component.

**A11y:** `role="img"` with `aria-label` like "UX Health score 72 out of 100, up 4 from previous audit."

### 5. `<looksee-score-delta-arrow>`

Tiny composable used inside other components, but exposed for any "score went up/down" display.

```
↑ 4    up, good (score went up)
↓ 2    down, critical
—      no change, muted
```

### 6. `<looksee-data-card>`

Generic card wrapper. Replaces every component's hand-rolled card.

**Inputs:**
```ts
@Input() title?: string;
@Input() subtitle?: string;
@Input() padding: 'none' | 'sm' | 'md' | 'lg' = 'md';
@Input() elevation: 'flat' | 'raised' | 'floating' = 'raised';
@Input() interactive = false;              // adds hover lift + pointer cursor
@Input() href?: string;                    // if set, renders as <a>
```

**Slots** (Angular content projection):
- default content
- `[card-actions]` — top-right action bar
- `[card-footer]` — bottom border-separated footer

**Visual variants:**
```
flat:      white bg, 1px border subtle, no shadow
raised:    white bg, shadow-sm (default)
floating:  white bg, shadow-md, lifts to shadow-lg on hover if interactive
```

### 7. `<looksee-empty-state>`

Friendly empty state with illustration + headline + body + primary/secondary actions.

**Inputs:**
```ts
@Input() illustration: 'audits' | 'sites' | 'issues' | 'search' | 'generic';
@Input() title: string;
@Input() body?: string;
@Input() primaryAction?: { label: string; href?: string; onClick?: () => void };
@Input() secondaryAction?: { label: string; href?: string; onClick?: () => void };
@Input() compact = false;     // reduces padding and illustration size
```

**Illustrations** pulled from `src/assets/illustrations/` (many already exist — `audit_website.jpg`, `review_audit.png`, `look-see_target_check.png`).

**Layout:**
```
        [illustration, 160px]

        Title — heading-lg, centered

        Body — body, max-w-md, muted

        [ Primary action ]   Secondary action →
```

### 8. `<looksee-skeleton>`

Animated loading placeholder. Used in lists, cards, and headers while data loads.

**Inputs:**
```ts
@Input() variant: 'text' | 'title' | 'circle' | 'rect' | 'row';
@Input() width?: string;    // e.g., '60%', '120px'
@Input() height?: string;
@Input() rows?: number;     // for 'row' variant; renders a list
```

**Behavior:** subtle shimmer animation; respects `prefers-reduced-motion` by switching to static muted-gray.

**Usage patterns:**
```html
<looksee-skeleton variant="title" width="240px"></looksee-skeleton>
<looksee-skeleton variant="text" [rows]="3"></looksee-skeleton>

<!-- Typical audit-list loading state: -->
<looksee-skeleton variant="row" [rows]="5"></looksee-skeleton>
```

### 9. `<looksee-site-card>`

Card used on `/home` and `/sites` to represent a site at a glance.

**Inputs:**
```ts
@Input() site: Site;    // includes url, score, trend, issuesCount, lastAuditAt, favicon
```

**Visual:**
```
┌────────────────────────────────┐
│  [favicon] acme.com           ⋯│
│                                │
│    ╭────╮                      │
│    │ 82 │   ↑ 3 since last     │
│    ╰────╯                      │
│                                │
│  3 new issues · audited 2h ago │
└────────────────────────────────┘
```

Interactive card; click routes to `/sites/:id`. Includes a trailing `⋯` menu for per-site actions (run audit, settings, remove).

### 10. `<looksee-kbd>`

Tiny keyboard-key display for shortcut hints.

**Inputs:**
```ts
@Input() keys: string[];    // e.g., ['j'], ['Ctrl', 'K']
```

**Visual:**
```
[ j ]        [ Ctrl ] + [ K ]
```

Uses `<kbd>` element with tokenized styling: `ink.100` bg, `ink.300` border-bottom 2px (tactile feel), `mono` type, 11–13px.

### 11. `<looksee-toast>` / ToastService

Replace the current `message.service` single-method approach with a typed toast service driving a single `<looksee-toast-host>` in the app shell.

**API:**
```ts
toast.success('Audit saved.');
toast.error('We couldn\'t save your audit. Try again.');
toast.info('Running the audit now.', { duration: 6000 });
toast.promise(promise, {
  loading: 'Saving…',
  success: 'Saved.',
  error: 'Something went wrong.',
});
```

**Visual:**
- Bottom-right stack, slide-in from right.
- Auto-dismiss in 5s (success), 8s (error), 4s (info). Hover pauses auto-dismiss.
- Each toast is a `<div role="status" aria-live="polite">` (or `assertive` for errors).
- Max 3 visible at once; overflow queued.

---

## Component dependencies

```
looksee-button            ← (standalone)
looksee-kbd               ← (standalone)
looksee-status-chip       ← (standalone)
looksee-score-delta-arrow ← (standalone)
looksee-skeleton          ← (standalone)

looksee-score-badge       ← looksee-skeleton (when value is null)
looksee-score-gauge       ← looksee-score-delta-arrow
looksee-data-card         ← (standalone)
looksee-empty-state       ← looksee-button
looksee-site-card         ← looksee-data-card, looksee-score-badge, looksee-score-delta-arrow
looksee-toast             ← (service + host)
```

All are **standalone Angular components** (Angular 17 pattern). No shared NgModule.

## Technical design

### File layout

```
src/app/components/shared/
├── button/
│   ├── button.component.ts
│   ├── button.component.html
│   ├── button.component.scss
│   └── button.component.spec.ts
├── status-chip/
├── score-badge/
├── score-gauge/
├── score-delta-arrow/
├── data-card/
├── empty-state/
├── skeleton/
├── site-card/
├── kbd/
├── toast/
│   ├── toast.service.ts
│   ├── toast-host.component.ts
│   └── toast.component.ts
└── index.ts    // barrel re-export for convenience
```

### Storybook-lite docs

If Storybook adds too much build complexity, ship a static `/docs/design/components.html` page that imports the built components and shows examples. This is enough for internal handoff at this stage.

### Testing

Each component: unit tests for rendering + inputs; a11y test using axe-core via `@testing-library/angular`. Target 80% coverage on the shared library (higher than the app average because these are reused).

### Tokens used by each component

| Component | Tokens |
|---|---|
| button | `--text-primary`, `--text-on-accent`, `brand.*`, `ink.*`, radius.md |
| status-chip | `--score-*`, `brand.*`, radius.full |
| score-badge | `--score-*`, typography body-sm/body |
| score-gauge | `--score-*`, typography display |
| score-delta-arrow | `--score-good`, `--score-critical` |
| data-card | `--surface-raised`, `--border-subtle`, shadow.sm/md |
| empty-state | `--text-primary`, `--text-secondary`, typography heading-lg/body |
| skeleton | `ink.100` → `ink.200` shimmer gradient |
| site-card | all card + score tokens |
| kbd | `ink.100`, `ink.300`, typography mono |
| toast | `--surface-raised`, `--score-good/critical`, shadow.lg |

## Acceptance criteria

- [ ] All 11 components exist under `src/app/components/shared/`, with the file layout in §Technical design.
- [ ] Every component is standalone (no NgModule).
- [ ] No hex literals in any shared component (ESLint rule from spec 01 passes).
- [ ] All components accept `--data-theme='dark'` tokens (verified in a dark-mode preview page).
- [ ] All components have at least one unit test; axe-core passes for each.
- [ ] `index.ts` barrel exports every component.
- [ ] Documentation page (Storybook or `/docs/design/components.html`) exists with:
  - Usage code snippet per component.
  - Visual examples at all defined variants/sizes.
- [ ] At least three existing pages migrated to use the new components as a proof of adoption:
  - `audit-list` → `status-chip`, `score-badge`, `skeleton`, `empty-state`
  - `page-audit-review` (or its replacement) → `score-gauge`, `kbd`, `empty-state`
  - `landing` → `button`, `empty-state` (for the "what we check" section)

## Metrics

Not directly user-facing. Track as engineering-health KPIs:
- Count of hand-rolled card patterns in `src/app/components/*/` (baseline → target: 0).
- Lines of SCSS in `src/app/components/*/` (expected to *drop* as shared styles consolidate).
- Frequency of component reuse (grep for `<looksee-*>` — trend should rise monthly).

## Risks & open questions

1. **Angular Material overlap.** Material already provides `MatChip`, `MatCard`, `MatProgressSpinner`. Decision: wrap Material where we can for keyboard / a11y compliance (e.g., `looksee-button` wraps `matButton`), but bring the brand layer on top. Revisit per-component.
2. **Illustration assets.** Empty state needs 5+ illustration variants. Audit what's in `src/assets/illustrations/` already; commission or source the rest. Product decision.
3. **Storybook vs static docs.** Storybook adds ~2–3 days of setup + ongoing maintenance. For a small team, static docs may be enough. Recommend: static for now, Storybook when the library exceeds 20 components.
4. **Dark-mode contrast validation.** Every token pair needs manual verification in both themes. Build a tiny contrast-check script.
5. **Toast API ergonomics.** `toast.promise(...)` is nice but couples toasts to promises, not observables. Consider adding an RxJS-friendly variant: `toast.observable(obs$, messages)`.
6. **Kbd on mobile.** Keyboard hints are useless on touch; components consuming `<looksee-kbd>` should conditionally hide it at `< md` breakpoint, or behind a "desktop-only help" expander.

## Phasing

**Milestone 1 (parallel with spec 01 Milestone 2):**
- `button`, `status-chip`, `score-badge`, `skeleton`.
- These four unblock spec 02 (status chips in audit list) and spec 05 (signup card).

**Milestone 2:**
- `score-gauge`, `score-delta-arrow`, `empty-state`, `data-card`.
- Unblocks spec 03 and the `/home` page.

**Milestone 3:**
- `site-card`, `kbd`, `toast`.

**Milestone 4:**
- Storybook (if we commit to it), or static docs polish.
- Consumer migrations across the app opportunistically.
