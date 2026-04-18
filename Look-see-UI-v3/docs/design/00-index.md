# Look-see UI — Design Docs & Feature Specs

This directory contains actionable feature specs derived from the [UX Redesign](../../UX_REDESIGN.md) and the existing [Visual Redesign Plan](../../REDESIGN_PLAN.md). Each doc addresses one cluster of problems with a concrete, shippable design.

## Status legend

- **Draft** — under design review
- **Ready** — approved, not yet in development
- **In progress** — build started
- **Shipped** — live in production

---

## Specs by phase

### Phase A — Foundation & onboarding
Ship order. Each spec is independently shippable.

| # | Spec | Addresses | Status |
|---|---|---|---|
| 01 | [Design system & tokens](./01-design-system.md) | Design system issues: scattered colors, loose typography, inconsistent buttons, missing breakpoints, no dark mode foundation | Draft |
| 02 | [Audit status model & live progress feed](./02-audit-status-and-progress.md) | Critical gap: no in-progress visibility. Onboarding handoff abrupt. Low-hanging fruit: status badges in audit list. | Draft |
| 05 | [Landing & onboarding friction fixes](./05-landing-and-onboarding.md) | Low-hanging fruit: URL regex too strict, pessimistic guest dialog, auto-rotating carousel, copy rewrites | Draft |

### Phase B — Core loop

| # | Spec | Addresses | Status |
|---|---|---|---|
| 03 | [Results architecture refactor](./03-results-architecture.md) | Critical gap: 1,293-LOC page-audit-review monolith. Introduces IssueRow, IssueDetailPanel, ScreenshotCanvas. | Draft |
| 06 | [Shared UI component library](./06-shared-components.md) | Low-hanging fruit: ScoreBadge, ScoreGauge, EmptyState, SkeletonLoader, StatusChip, DataCard reusables | Draft |

### Phase C — IA & workspace

| # | Spec | Addresses | Status |
|---|---|---|---|
| 04 | [Navigation & information architecture](./04-navigation-ia.md) | Critical gap: orphaned `/dashboard`, fragmented review routes, Domains → Sites rename, `/home` as default, breadcrumbs | Draft |

---

## Cross-cutting concerns tracked across all specs

- **Accessibility:** WCAG 2.1 AA minimum on every spec. We sell accessibility; we must model it.
- **Analytics:** Every spec lists the Segment events to instrument.
- **Responsive:** Desktop-first, tablet-acceptable, mobile-read-only. No mobile-block modal in any final state.
- **Copy:** Every user-facing string passes the voice test in UX_REDESIGN.md §10.

## How to read a spec

Each spec follows the same structure:

1. **Problem** — what hurts today, cited with file:line where possible
2. **Goals / Non-goals** — crisp boundaries
3. **User stories** — Morgan (non-expert) and Priya (pro) framed
4. **Design** — the proposed interface, with wireframes
5. **Technical design** — components, state, data shape, token usage
6. **Acceptance criteria** — testable, binary
7. **Metrics** — what success looks like in the analytics pipeline
8. **Risks & open questions** — what needs product/eng input before build
9. **Phasing** — what ships first, what's deferred

If any section is empty in a given spec, it means it doesn't apply (e.g., a pure refactor may have no metrics to move).

---

## Related documents

- [UX_REDESIGN.md](../../UX_REDESIGN.md) — the end-to-end redesign these specs implement
- [REDESIGN_PLAN.md](../../REDESIGN_PLAN.md) — original visual redesign plan (colors, typography, components)
- [README.md](../../README.md) — product & deployment documentation
