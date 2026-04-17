---
id: 02
title: Audit status model & live progress feed
status: Draft
phase: A
addresses:
  - No visual feedback for in-progress audits (critical gap)
  - Onboarding and results handoff feels abrupt (critical gap)
  - Auto-rotating marketing carousel during wait (low-hanging fruit)
  - Status badges missing in audit list (low-hanging fruit)
owner: Design + Frontend + Backend (audit-service)
related:
  - UX_REDESIGN.md §5.3, §6.2
  - 03-results-architecture.md
  - 06-shared-components.md (StatusChip)
---

# 02 — Audit status model & live progress feed

## Problem

Two related gaps, sharing the same underlying missing concept — **audit state machine.**

1. **In-progress audits are invisible after navigation.** `audit-list` shows finished audits only (scores 0–100 render, but no state for "not done yet"). A user who starts a domain audit (5+ minutes), navigates away, and returns has no way to know it's still running, failed, or queued.

2. **Onboarding is a fake wait.** `audit-onboarding` runs a 5-step progress animation on hardcoded timers (0s, 3s, 7s, 12s, 18s) and a 4-slide carousel auto-rotating every 5s. The animation doesn't reflect what the backend is doing; if the backend takes 30s, the user sees the animation finish at 18s and then stare at nothing. If it takes 3s, the animation is still running when results are ready.

3. **No cancellation, no ETA, no "notify me."** Audits feel like a black box.

The underlying cause: audits don't have an explicit client-side **state** the UI renders from. The data model has `AuditRecord` with scores, but state is inferred (missing scores → assume running).

## Goals

1. Introduce a first-class `AuditStatus` enum in the client data model.
2. Build a **single live progress scene** that replaces `audit-onboarding` and reuses the same component in-list.
3. Render accurate status chips in `audit-list` for every audit, at every state.
4. Stream real findings into the progress scene as they arrive (not fake steps).
5. Support **leave-and-return** — notifications when audits finish.
6. Support **cancel** — users can abort a running audit.

## Non-goals

- Re-running audits, comparing audits — covered in 03-results-architecture.md.
- Audit queue management (who runs first, priority) — out of scope; the backend decides.
- Scheduled / recurring audits — Phase D feature.

## User stories

- *As Morgan*, I start an audit, switch tabs, and come back 3 minutes later. I want to see at a glance whether it's done, still running, or failed.
- *As Morgan*, I start an audit and wait on the progress screen. I want to feel something is happening — actual findings, not fake animations.
- *As Morgan*, I start an audit and realize I entered the wrong URL. I want to cancel without having to refresh or wait.
- *As Priya*, I want to kick off an audit, get a browser notification when it's done, and open the result from the notification.

## Design

### 1. `AuditStatus` — the state machine

```
          ┌─────────┐
          │ QUEUED  │ ← client just started; backend not yet acked
          └────┬────┘
               │ acked
               ▼
          ┌─────────┐
          │ RUNNING │ ← backend actively auditing; progress updates streaming
          └────┬────┘
        ┌──────┼──────┐
   done │      │ fail │ cancel
        ▼      ▼      ▼
   ┌─────────┐ ┌───────┐ ┌───────────┐
   │COMPLETE │ │FAILED │ │ CANCELLED │
   └─────────┘ └───────┘ └───────────┘
```

**Status meanings:**

| Status | User-facing label | Chip color | Terminal? |
|---|---|---|---|
| `QUEUED` | *Queued* | ink.200 bg, ink.700 text | No |
| `RUNNING` | *Auditing…* (with % if known) | brand.50 bg, brand.700 text + spinner | No |
| `COMPLETE` | *Complete* | score.good/50 bg, score.good/700 text | Yes |
| `FAILED` | *Failed* | score.critical/50 bg, score.critical/700 text | Yes |
| `CANCELLED` | *Cancelled* | ink.100 bg, ink.600 text | Yes |

**Transitions:**
- Client sets `QUEUED` optimistically when user submits URL.
- Backend ack flips to `RUNNING` (within 2s or timeout → `FAILED`).
- `auditUpdate` events carry progress percentage (0–100) and the latest finding.
- Terminal events (`auditComplete`, `auditFailed`, `auditCancelled`) flip state.

### 2. Live progress feed

Replaces the current `audit-onboarding` component entirely. Rendered at the route `/audits/:id` when `status !== COMPLETE`. Reused in-line in `audit-list` rows as an expandable "preview."

**Full-screen version (on the audit route):**

```
┌────────────────────────────────────────────────────────────────┐
│  ← Back to audits                                              │
│                                                                │
│                                                                │
│   Auditing yoursite.com/pricing                                │
│                                                                │
│   ████████████████░░░░░░░░░░░░░░░░░  43%    about 20s left    │
│                                                                │
│   ┌───────────────────────┐  ┌───────────────────────────────┐ │
│   │                       │  │  ✓  Captured page (2.1s)      │ │
│   │   [live screenshot    │  │  ✓  Fetched 14 assets         │ │
│   │    thumbnail of       │  │  ✓  Analyzed contrast         │ │
│   │    the page being     │  │  ⋯  Checking heading hierarchy│ │
│   │    audited]           │  │                               │ │
│   │                       │  │  Found so far:                │ │
│   │                       │  │  🔴 3 critical                │ │
│   │                       │  │  🟡 8 major                   │ │
│   │                       │  │  🔵 12 minor                  │ │
│   └───────────────────────┘  └───────────────────────────────┘ │
│                                                                │
│                                                                │
│   [ Cancel ]    [ Notify me when done ]                        │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

**Interaction rules:**

- **Screenshot** appears as soon as the backend captures it (first `pageFound` event for a single-page audit, or the first page captured for a domain audit).
- **Feed** prepends a line each time an `auditUpdate` event arrives. Animate with a subtle fade+slide (respecting `prefers-reduced-motion`).
- **ETA** computed from elapsed time × (100 / percent) once percent ≥ 10%. Below 10%, show "calculating…" rather than a nonsense number.
- **"Found so far" counters** update as issues arrive. If `status === RUNNING` and user clicks the counter, a drawer opens showing the partial findings. They can preview before the audit is done.
- **Cancel** calls `DELETE /audits/:id`; optimistically flip to `CANCELLED`; backend confirmation follows.
- **Notify me** requests `Notification.permission` (the browser API). If granted, shows browser notification on complete/failed. Also offers email opt-in inline ("Or email me at morgan@acme.com when it's done — [change]").
- **On completion:** the progress scene cross-fades into the results view (see 03-results-architecture.md). Don't force-navigate; the URL stays `/audits/:id`, the component detects `status === COMPLETE` and swaps layouts.

**In-list compact version (for `audit-list` rows with `RUNNING` status):**

```
┌───────────────────────────────────────────────────────────────┐
│  yoursite.com/pricing   [Auditing… 43%] ████░░░  20s    ⋯    │
│  ↳ Checking heading hierarchy                                 │
└───────────────────────────────────────────────────────────────┘
```

### 3. Audit list — status-aware

Every row shows a **StatusChip** (see 06-shared-components.md) as the leftmost column after URL. Rows with `RUNNING` status include the inline progress bar above. Rows with `FAILED` status include a retry affordance:

```
yoursite.com/checkout   [Failed]   2 hours ago   [ Retry ]  ⋯
                        ⚠ The URL timed out. Check the URL and try again.
```

### 4. Notification design

**Browser notification on completion:**

> **Audit complete — yoursite.com/pricing**
> Score 72 · 27 issues found
> *Click to view results*

**Email on completion (fallback if browser notifications declined):**

Subject: `Your Look-see audit of yoursite.com/pricing is ready`

Body (plain HTML email, branded):

> We've finished auditing **yoursite.com/pricing**. Your UX Health Score is **72/100** — we found 27 issues, including 3 critical.
>
> [View the report →]
>
> This report link expires in 30 days. Sign up to save it permanently.

### 5. Cancellation UX

When the user clicks **Cancel** on the progress scene:

1. A confirmation dialog:
   > *Cancel this audit?*
   > *We'll stop the analysis. Findings collected so far won't be saved.*
   > `[ Keep auditing ] [ Cancel audit ]`
2. On confirm: optimistic `CANCELLED` state, call `DELETE /audits/:id`, route to `/audits` with a toast:
   > *Audit cancelled.*

**No confirmation** when cancelling an audit < 5s old (it's trivially reversible — just run again).

## Technical design

### Client data model

```ts
// src/app/models/audit-status.enum.ts
export enum AuditStatus {
  QUEUED = 'QUEUED',
  RUNNING = 'RUNNING',
  COMPLETE = 'COMPLETE',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
}

// src/app/models/audit-record.ts  (augment)
export interface AuditRecord {
  id: string;
  url: string;
  type: 'PAGE' | 'DOMAIN';
  status: AuditStatus;
  statusMessage?: string;        // free-text reason for FAILED
  progressPercent?: number;      // 0–100, undefined if QUEUED
  startedAt: string;
  completedAt?: string;
  screenshotUrl?: string;        // present as soon as captured
  // … existing score fields
  findingsPreview?: {            // live counters during RUNNING
    critical: number;
    major: number;
    minor: number;
  };
  latestFindingLabel?: string;   // "Checking heading hierarchy" — for in-list display
}
```

### Server-side additions (audit-service / AuditManager)

- Ensure `AuditRecord` persists `status`, `progressPercent`, `statusMessage`, `startedAt`, `completedAt`.
- Extend the Pusher `auditUpdate` event payload:
  ```json
  {
    "auditId": "…",
    "status": "RUNNING",
    "progressPercent": 43,
    "latestFindingLabel": "Checking heading hierarchy",
    "findingsPreview": { "critical": 3, "major": 8, "minor": 12 },
    "screenshotUrl": "https://…"
  }
  ```
- Emit terminal events:
  - `auditComplete` — final scores + status.
  - `auditFailed` — `statusMessage`, optional retryable flag.
  - `auditCancelled` — from client cancel or admin abort.
- `DELETE /audits/:id` for cancel. Idempotent.
- `POST /audits/:id/retry` for failed-audit retry (creates a new audit preserving URL/type).
- `POST /audits/:id/subscribe` for email-on-complete opt-in (body: `{ email }`).

### Client service updates

`audit.service.ts`:
- `cancelAudit(id: string): Observable<void>`
- `retryAudit(id: string): Observable<AuditRecord>`
- `subscribeToCompletion(id: string, email?: string): Observable<void>`

`web-socket.service.ts`:
- Ensure `auditUpdate` handlers merge partial payloads into an in-memory audit record (no clobbering).
- Add handlers for `auditComplete`, `auditFailed`, `auditCancelled`.

### Components

**New:**
- `AuditProgressSceneComponent` — full-screen progress view. Inputs: `auditRecord$`, `onCancel`, `onNotifyMe`. Replaces `audit-onboarding`.
- `AuditProgressInlineComponent` — compact version for list rows. Same inputs, compact template.
- `FindingsPreviewCountersComponent` — three-counter display with severity chips.
- `StatusChipComponent` — see 06-shared-components.md.

**Retired:**
- `audit-onboarding/*` (the timer-driven fake-progress component and its carousel).

**Updated:**
- `AuditListComponent` — uses `StatusChip`, embeds `AuditProgressInline` for running rows, adds retry handler.
- `PageAuditReviewComponent` — becomes status-aware; swaps its template between progress and results based on `auditRecord.status`.

### Reduced motion

All animations (slide-in feed lines, spinning dots, progress bar fill) respect `prefers-reduced-motion`. With reduce, feed lines appear without animation; progress bar snaps to new values.

## Acceptance criteria

**State machine:**
- [ ] `AuditStatus` enum exists and is set correctly for every audit record returned by the API.
- [ ] Transitions from `QUEUED → RUNNING → {COMPLETE|FAILED|CANCELLED}` are verified end-to-end in an integration test.
- [ ] Orphaned audits (no ack from backend within 30s) auto-transition to `FAILED` client-side with a retry affordance.

**Progress scene:**
- [ ] Screenshot thumbnail appears within 3s of submission for URLs that respond within 2s.
- [ ] Feed updates in real time (no polling); verified with WebSocket mock.
- [ ] Progress bar fills 0→100% with accurate ETA after 10% threshold.
- [ ] Cancel button works; calls `DELETE /audits/:id`; returns to list with toast.
- [ ] "Notify me" requests browser notification permission; shows inline email fallback if declined.
- [ ] On completion, progress scene transitions to results view without URL change or full-page reload.

**Audit list:**
- [ ] Every row shows a `StatusChip` matching the audit's current status.
- [ ] `RUNNING` rows include compact progress + live label.
- [ ] `FAILED` rows show `statusMessage` and a `[Retry]` button.
- [ ] Updates propagate to list rows in real time if the user is on `/audits` during completion.

**A11y:**
- [ ] Progress updates announced via ARIA live region (`role="status"`, `aria-live="polite"`).
- [ ] Status chip includes visually-hidden descriptive text for screen readers.
- [ ] Cancel and Retry buttons keyboard-reachable with visible focus rings.

## Metrics

Track in Segment:
- `Audit: started` (properties: `type`, `authenticated`, `referrer_screen`)
- `Audit: first-finding-surfaced` (new) — time from `started` to first `auditUpdate` with a finding
- `Audit: completed` (properties: `duration_ms`, `issues_found`, `score`)
- `Audit: failed` (properties: `reason`, `duration_ms`)
- `Audit: cancelled` (properties: `progress_percent_at_cancel`)
- `Audit: retry-clicked`
- `Audit: notify-me-opted-in` (properties: `channel: browser|email`)

**Success metrics:**
- ≥ 95% of submitted audits reach a terminal state within 5 minutes (today: unknown; suspected < 85%).
- Progress scene abandonment rate (user navigates away before terminal) < 30%.
- `notify-me` opt-in rate ≥ 40% among users who navigate away.

## Risks & open questions

1. **Backend work required.** This spec assumes `audit-service` can emit percentage progress and partial findings. Today `auditUpdate` carries coarse progress only. Needs a backend design doc to spec the stream contract.
2. **Cancel semantics.** Should `CANCELLED` audits be visible in the list permanently, or auto-hidden after 24h? Recommend: show for 24h, then collapse into a "Recently cancelled" section.
3. **Notification permissions UX.** iOS Safari doesn't support Web Push for non-PWA sites. Email fallback is the answer but adds a mail-delivery dependency. Confirm SES/SendGrid is in the stack.
4. **Partial findings visibility.** Showing counters during `RUNNING` may mislead users into thinking the audit is done. Mitigation: counter label says "Found so far" and is clearly incrementing. Usability test this.
5. **Failure recovery.** If the WebSocket disconnects mid-audit, how do we recover state? Proposal: on reconnect, the client re-fetches the audit record and replays any missed updates from a backend-provided `since` cursor. Needs backend support.
6. **Multi-page domain audits.** For a domain audit with 50 pages, should the feed group findings by page or chronologically? Recommend: group by page, with expandable sections. Needs usability test.

## Phasing

**Milestone 1:** Status enum + `StatusChip` + audit-list status chips. No real-time work. Status resolved from scores/errors at read time.

**Milestone 2:** WebSocket event schema extended server-side; client merges partial payloads. Full-screen `AuditProgressScene` with screenshot, feed, progress bar. Retires `audit-onboarding`.

**Milestone 3:** Cancel, retry, notify-me. Email fallback. In-list progress for running rows.

**Milestone 4 (Phase D):** Scheduled audits → audit list rows with `SCHEDULED` status (extends the same state machine).
