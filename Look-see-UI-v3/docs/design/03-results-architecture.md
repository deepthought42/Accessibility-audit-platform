---
id: 03
title: Results architecture refactor
status: Draft
phase: B
addresses:
  - page-audit-review is 1,293-LOC unmaintainable monolith (critical gap)
  - Panels scroll independently and lose context
  - Issues shown in load order, not impact order
  - Element drill-down opens a modal that breaks context
  - Tab navigation cramped; screenshots unreadable
  - Duplicated template code between page audit and domain audit
owner: Design + Frontend
related:
  - UX_REDESIGN.md §5.4, §6.3
  - 02-audit-status-and-progress.md
  - 06-shared-components.md
---

# 03 — Results architecture refactor

## Problem

`page-audit-review.component.ts` is 1,293 lines with one component managing:
- audit record fetching
- issue list rendering
- category tabs
- score gauges (inline SVG with manual circumference math)
- screenshot display and element zoom
- element drill-down dialog
- filter / sort / search state
- WebSocket update handlers
- "mark as done" placeholder logic
- PDF/Excel export
- Segment tracking

Symptoms:
- Left issue list and right detail panel scroll independently — selecting an issue loses its row from view.
- Issues render in load order; a user scrolling for critical issues has to skim all 40.
- `element-info-dialog` is a modal — opening it covers the screenshot context the user was just reading.
- Category tabs are oversized (`h-32`) with empty `<div>` spacers where the design system should have a tab underline.
- Score display uses hand-calculated SVG circumference — every score gauge on every page has a copy-paste version.
- Domain audits duplicate much of this template inside `audit-dashboard`, with subtle divergences.
- Inline styles (`style="overflow-y:auto;height:40vh;max-height:45vh"`) scattered throughout the template.
- ~200 lines of commented-out dead code.

## Goals

1. Replace the monolith with a **composable set of 6 components** around a typed data model.
2. Default to **impact-ranked** issue ordering, with user-selectable grouping.
3. Unify single-page and domain audit views under **one component tree** that adapts to payload shape.
4. Replace the element-info modal with an **inline detail panel** that preserves context.
5. Make the **screenshot canvas** the visual anchor — clickable hotspots tied bidirectionally to the issue list.
6. Introduce **"mark done"** as a first-class verb that persists and updates the preview score.
7. Full **keyboard navigation** for power users.

## Non-goals

- Backend issue-ranking algorithm redesign — use whatever `priority` + `severity` the backend supplies; client ranks at render time with a pluggable scorer.
- Rewriting PDF/Excel export — delegate to existing `report.service.ts`, only the call-site moves.
- Inline editing of issues (adding recommendations, observations) — preserved as is, repackaged behind a cleaner action menu.

## User stories

- *As Morgan*, I open my audit results and immediately see the top 3 issues ranked by severity. I click one and read a plain-English explanation without losing the issue list.
- *As Morgan*, I look at the screenshot, see red dots over the broken parts, click a dot, and the detail panel scrolls to that issue.
- *As Morgan*, I fix an issue in my site, come back, click "Mark done," and see my projected score go up.
- *As Priya*, I press `j` and `k` to step through issues, `x` to mark done, `/` to focus the filter. I never touch the mouse.
- *As Priya*, I click an issue and see the WCAG reference, the element selector, and a copyable AI-generated fix snippet.

## Design

### 1. Component tree

```
<looksee-audit-results>                    ← route component at /audits/:id
├── <audit-header>                         ← score, sub-scores, actions
├── <audit-results-shell>                  ← 3-panel layout, adapts to audit type
│   ├── <issue-nav-panel>                  ← left rail
│   │   ├── <issue-filter-bar>
│   │   └── <issue-group-list>
│   │       └── <issue-row> *n
│   ├── <audit-canvas-panel>               ← center
│   │   ├── <page-screenshot> (single-page)
│   │   │   └── <issue-hotspot> *n
│   │   ├── <page-list>          (domain audit)
│   │   │   └── <page-row> *n
│   │   └── <no-preview-empty-state> (fallback)
│   └── <issue-detail-panel>               ← right
│       ├── <issue-detail-header>
│       ├── <issue-why-it-matters>
│       ├── <issue-standards-ref>
│       ├── <issue-fix-suggestion>
│       ├── <issue-element-preview>
│       └── <issue-action-bar>
└── <audit-actions-bar>                    ← sticky bottom: Save, Export, Share
```

Each of these is a small, tested Angular standalone component. No component exceeds **250 lines** (enforced by ESLint).

### 2. Layout: 3-panel desktop, tabs on tablet, scroll on mobile

**Desktop (≥ 1024px):**

```
┌──────────────────────────────────────────────────────────────────────┐
│  ← Back    yoursite.com/pricing      [Re-run] [Share] [Export ▾]    │
├──────────────────────────────────────────────────────────────────────┤
│ ┌──────┐ UX Health 72                                                │
│ │  72  │ ◎ Access 64  ◎ Content 81  ◎ Visual 78  ◎ SEO 70           │
│ └──────┘ Audited 2 min ago · 27 issues                               │
├──────────────────────────────────────────────────────────────────────┤
│                     │                       │                        │
│  issue-nav-panel    │   audit-canvas-panel  │   issue-detail-panel   │
│                     │                       │                        │
│  [filter: All ▾]    │   [page screenshot    │   Selected:            │
│  [sort: Impact ▾]   │    with hotspots]     │   Contrast on CTA      │
│                     │                       │   ─────────────        │
│  ▼ Critical (3)     │                       │   Why it matters       │
│  ◉ Contrast on CTA  │                       │   …                    │
│  ○ Missing alt text │                       │   WCAG 2.1 AA · 1.4.3 │
│  ○ Form label       │                       │                        │
│                     │                       │   How to fix           │
│  ▼ Major (8)        │                       │   Change background…  │
│  ○ …                │                       │                        │
│                     │                       │   [Copy fix] [Done]   │
│  ▸ Minor (16)       │                       │                        │
│                     │                       │                        │
│  width: 320px       │   fluid, min 400px    │   width: 360px         │
│  scroll: yes        │   scroll: no          │   scroll: yes          │
└─────────────────────┴───────────────────────┴────────────────────────┘
```

**Tablet (768–1023px):** canvas panel and detail panel collapse into tabs above a single content area; issue nav stays as left rail (240px).

**Mobile (< 768px):** everything stacks. Single-column scrollable list with screenshot above. Detail opens as a full-screen drawer. This is the mobile view-only mode — no mark-done, no filter, no re-run.

### 3. Issue ranking & grouping

**Default sort: impact score.** Computed client-side:

```
impact_score = severity_weight × affected_element_count × visibility_weight
  where
    severity_weight  = CRITICAL: 3, MAJOR: 2, MINOR: 1
    visibility_weight = aboveFold ? 1.2 : 1.0
```

Grouping options (one active at a time):

- **Severity** (default) — Critical → Major → Minor groups.
- **Category** — Accessibility / Content / Visual / SEO.
- **WCAG level** — A / AA / AAA / Non-WCAG.
- **Status** — Open / In Progress / Done / Ignored.

Group headers collapse/expand with a disclosure icon. Collapsed state is persisted per user in localStorage, keyed by audit id.

### 4. Screenshot with hotspots

`<page-screenshot>` renders the captured full-page screenshot with numbered circular hotspots for each issue that has a positioned element.

**Hotspot states:**

| State | Appearance |
|---|---|
| Default | 24px circle, severity color bg, white number, 2px white ring |
| Hovered | 28px, drop-shadow lg, tooltip with issue title |
| Selected | 32px, brand border ring, persistent label |
| Marked done | 50% opacity, strike-through number |

**Interactions:**
- Click a hotspot → selects issue; detail panel updates; row scrolls into view in nav panel.
- Hover an issue row → corresponding hotspot pulses once.
- Hover a hotspot → tooltip with issue title + severity.
- Double-click or pinch-zoom → zooms into a 3× cropped view of the element. Escape exits zoom.
- If screenshot is missing (audit failed before capture), show an `<no-preview-empty-state>` with a retry CTA.

**Implementation:** hotspots are absolutely positioned inside a container that scales with the screenshot. Coordinates come from the backend's captured bounding box for each element; normalized to 0–1 so the UI can re-scale without drift.

### 5. Detail panel — 4 sections, consistent

Every issue's detail panel has the same four sections, in the same order:

1. **Why it matters** (1–2 sentences, non-technical, Morgan-facing). Example: *"Users with low vision may not see your 'Sign up' button. About 1 in 12 men and 1 in 200 women have some form of color blindness."*
2. **Standards reference** (WCAG criterion number + link to the spec, and any ADA/Section 508 note). Priya-facing, collapsible on small screens.
3. **How to fix** (AI-generated suggestion + copy-to-clipboard + if code fix, a before/after diff in a code block).
4. **Affected element(s)** (screenshot crop + CSS selector + copyable; if multiple, paginated: "Element 1 of 8 →").

Below the four sections: the **action bar**.

```
[ Mark done ] [ Assign ▾ ] [ Ignore ▾ ]        View in page ↗
```

- **Mark done** — flips status to `DONE`, animates a checkmark, updates the "projected score" in the header.
- **Assign** — paid feature, opens teammate picker (Phase D).
- **Ignore** — with reason picker: "Not applicable", "Accepted risk", "Won't fix". Ignored issues move to a collapsed section.
- **View in page** — opens the live site in a new tab, scrolled to the element (using `#element-id` or a URL fragment we compose).

### 6. Mark-done & projected score

When a user marks an issue done, the client:
1. Optimistically updates issue status.
2. Recomputes the projected score using the same algorithm the backend used, minus this issue's contribution.
3. Shows the projected score next to the current score in the header:
   ```
   72 → 78 (projected after 3 fixes)
   ```
4. Persists to backend via `PATCH /audits/:id/issues/:issueId { status: 'DONE' }`.

**Caveat copy:** *"Run a new audit to confirm your score."* Sits next to the projected score. Honest about the estimate.

### 7. Keyboard shortcuts

| Key | Action |
|---|---|
| `j` | Next issue |
| `k` | Previous issue |
| `x` | Mark current issue done |
| `i` | Ignore current issue |
| `/` | Focus filter |
| `g` | Open grouping menu |
| `z` | Zoom to current issue's element on screenshot |
| `s` | Focus share action |
| `e` | Focus export action |
| `?` | Show shortcuts cheat sheet overlay |
| `Esc` | Close drawers, cheat sheet, zoom |

Cheat sheet overlay rendered by `<looksee-kbd-cheatsheet>`, triggered globally. Accessible, focusable, dismissable.

## Technical design

### State management

Introduce a service `AuditResultsStore` (BehaviorSubject-backed) scoped to the route component:

```ts
interface AuditResultsState {
  record: AuditRecord;
  issues: Issue[];
  selectedIssueId: string | null;
  grouping: 'severity' | 'category' | 'wcag' | 'status';
  filter: { categories: Set<Category>; severities: Set<Severity>; query: string };
  ignoredReasons: Record<IssueId, string>;
  collapsedGroups: Set<string>;
  projectedScore: number | null;
}
```

Components subscribe to slices via selectors; dispatch via typed action methods on the store (no raw `next()` from outside).

### Data model (additions to existing)

```ts
interface Issue {
  id: string;
  auditId: string;
  category: 'ACCESSIBILITY' | 'CONTENT' | 'VISUAL' | 'SEO';
  severity: 'CRITICAL' | 'MAJOR' | 'MINOR';
  status: 'OPEN' | 'IN_PROGRESS' | 'DONE' | 'IGNORED';
  title: string;                    // 1 line, plain English
  whyItMatters: string;             // 1–2 sentences
  wcag?: { criterion: string; level: 'A' | 'AA' | 'AAA'; url: string };
  fixSuggestion: {
    description: string;
    codeBefore?: string;
    codeAfter?: string;
    language?: string;
  };
  affectedElements: AffectedElement[];
  impactScore: number;              // precomputed by backend, 0–100
  aboveFold: boolean;
}

interface AffectedElement {
  id: string;
  selector: string;
  screenshotCropUrl: string;
  boundingBox: { x: number; y: number; width: number; height: number }; // normalized 0–1
  snippetHtml?: string;
}
```

### Route component

```ts
@Component({ standalone: true, selector: 'looksee-audit-results', ... })
export class AuditResultsComponent {
  constructor(private route: ActivatedRoute, private store: AuditResultsStore) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.store.load(id); // fetches audit record + issues, subscribes to WS updates
  }
}
```

### Removal

- Delete `page-audit-review.component.ts/html/scss`.
- Delete `element-info-dialog.component.*`.
- Delete duplicated score-gauge SVG code in `audit-dashboard.component.html`.
- Delete the ~200 lines of commented-out HTML.

### A11y

- Issue rows are `<li>` with `role="option"` inside an `<ul role="listbox">`. Selection follows listbox pattern.
- Detail panel is a `<section aria-labelledby="issue-title">`.
- Hotspots are `<button>` elements with `aria-label="Issue 3: Contrast on CTA, critical"`.
- Cheat sheet is a `role="dialog"` with focus trap.
- Live score updates announce via ARIA live region after mark-done.

## Acceptance criteria

**Structure:**
- [ ] `page-audit-review` component deleted; replaced by the component tree in §1.
- [ ] `element-info-dialog` deleted; its functionality served by `<issue-detail-panel>`.
- [ ] No component in the new tree exceeds 250 lines.
- [ ] No inline `style="…"` attributes in the new templates.

**Layout:**
- [ ] 3-panel layout at ≥ 1024px; tab layout at 768–1023px; stacked on < 768px.
- [ ] Left and right panels scroll independently; selecting an issue in the list auto-scrolls it into view.

**Issue ranking:**
- [ ] Default sort: impact score descending, grouped by severity.
- [ ] Grouping dropdown changes between Severity / Category / WCAG / Status.
- [ ] Collapsed-group state persists in localStorage.

**Screenshot:**
- [ ] Hotspots render at correct positions across viewport sizes.
- [ ] Clicking a hotspot selects the issue and scrolls it into view.
- [ ] Hovering an issue row highlights the corresponding hotspot.
- [ ] Double-click zooms into the element; Escape exits zoom.

**Detail panel:**
- [ ] Every issue shows Why / Standards / Fix / Affected sections, in order.
- [ ] Copy-fix button copies the fix snippet and announces "Copied" to assistive tech.
- [ ] Mark-done updates status, decrements group count, and updates projected score.
- [ ] Ignore with reason moves the issue to a collapsed "Ignored" group.

**Keyboard:**
- [ ] All shortcuts in §7 work; cheat sheet accessible via `?`.
- [ ] Focus management preserved when opening/closing zoom and drawers.

**A11y:**
- [ ] Axe passes with 0 serious violations on results page.
- [ ] VoiceOver / NVDA smoke test passes (manual).

## Metrics

- `Results: viewed` — user landed on the results page (count: 1 per audit-view).
- `Results: issue-detail-opened` (properties: `severity`, `category`) — proves engagement depth.
- `Results: issue-marked-done`
- `Results: issue-ignored` (properties: `reason`)
- `Results: fix-copied`
- `Results: hotspot-clicked` — verifies the canvas loop works.
- `Results: keyboard-shortcut-used` (properties: `key`) — measures power-user adoption.

**Success metrics:**
- Time-to-first-detail-open (from results load) < 30s for 60% of sessions.
- Mark-done rate among completed audits > 15% (a proxy for real value delivery).
- Average issues-viewed-per-session ≥ 3 (vs. unknown today).

## Risks & open questions

1. **Backend issue data shape.** The `Issue` interface above assumes structured fields that may not match today's payload (which carries `ADAComplianceNote`, `UXIssueMessage`, etc., with variable fidelity). Needs a backend data-contract review before Phase B.
2. **Hotspot coordinates.** Today's backend captures element screenshots but may not export normalized bounding boxes for the full-page screenshot. May need a rendering pass to recompute coordinates.
3. **Projected score accuracy.** Client-side recomputation may drift from backend algorithm over time. Mitigation: backend endpoint `GET /audits/:id/preview-score?ignored=[]&done=[]` as the source of truth, cached by the client.
4. **Mark-done for ignored issues.** If backend re-runs future audits and finds the "same" issue, do we auto-mark done from history? Recommend: yes, via a stable `issueFingerprint` (selector + rule id). Design TBD.
5. **Domain audit canvas.** Replacing screenshot with a page list is clear, but what's the default sort for pages? Recommend: lowest-score first (triage mode).
6. **Store library.** BehaviorSubject-based store is lightweight; consider NgRx Signals or akita if complexity grows. Keep simple for now.

## Phasing

**Milestone 1 (behind feature flag):**
- New component tree shipped alongside legacy `page-audit-review`.
- Feature flag `results_v2` toggles between them.
- Internal dogfooding only.

**Milestone 2:**
- Flag on for 10% of users; measure engagement metrics vs. control.
- Fix any regressions.

**Milestone 3:**
- Flag on for all; legacy component marked deprecated.
- Delete legacy after 2 releases with no rollback.

**Milestone 4 (Phase C):**
- Team features (assign, comments on issues).
- Compare with previous audit.
