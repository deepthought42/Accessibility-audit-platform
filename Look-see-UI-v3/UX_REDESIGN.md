# Look-see UI — End-to-End UX Redesign

> Companion to `REDESIGN_PLAN.md`. That document covers the **visual design system** (colors, type, spacing, components). This document covers the **UX architecture** — flows, onboarding friction, information hierarchy, interaction patterns, and copy. Read the two together.

**Author:** Principal UX review
**Scope:** Full product, optimized for non-expert first-time users with depth available for power users
**Status:** Proposal — no code changes made

---

## 0. Product thesis (re-stated)

Look-see is a **self-serve UX audit tool** whose value prop collapses to one sentence:

> *Paste a URL. Get a prioritized list of UX, accessibility, and content fixes. Ship them.*

Every design decision below is measured against that sentence. If a screen, click, or word doesn't move the user toward that outcome, it's cut.

The product is competing for attention with Lighthouse, axe DevTools, Wave, SiteImprove, Siteimprove, AccessiBe, and a long tail of AI audit tools. Our wedge is not "more checks" — it's **clarity of action**. The user's emotional journey should be:

1. *Curiosity* — "I wonder how my site does."
2. *Surprise* — "Oh, there's more wrong than I thought."
3. *Relief* — "But I can see exactly what to fix, and why it matters."
4. *Confidence* — "I can share this with my team and make it better."

The current UI sometimes delivers step 2 but often fumbles steps 3 and 4. This redesign is about getting all four right.

---

## 1. Primary persona — "Mixed, optimize for non-expert first"

**Primary: Morgan, the Accidental Auditor.** A product manager, marketer, founder, or small-agency owner who suspects their site has issues but doesn't know WCAG from WCAF. They have 10 minutes, not an hour. They need plain language and prioritization, not a 47-item checklist.

**Secondary: Priya, the Pro.** A designer, front-end engineer, or accessibility specialist who needs the *depth* — element selectors, WCAG references, fix snippets, and exportable reports for stakeholders.

**Design rule:** Morgan sees a clean, opinionated surface. Priya can drill into raw specifics with one or two clicks. Never make Morgan pay a complexity tax for Priya's power tools.

**Jobs to be done:**
- *Morgan:* "Show me what's broken, what matters, and what to do next."
- *Priya:* "Give me the issue, the element, the WCAG reference, and a shareable report."

---

## 2. Current state — friction diagnosis

From the code review, the top ten friction points. I've re-grouped them by the moment in the journey where they hurt.

### First impression (before signup)
1. **Landing URL input is punitive.** Hardcoded TLD regex rejects valid URLs (`.io`, `.co`, `.me`). A user's very first interaction fails with a generic error. **Severity: critical** — this is the top-of-funnel.
2. **"1 free audit for guests" dialog is pessimistic.** Verbatim: *"Unfortunately we only provide 1 free audit for visitors without an account."* That word "unfortunately" frames the product as stingy at the exact moment we're trying to convert.
3. **No trust scaffolding on the landing page.** No customer logos, sample report preview, or "10,000 sites audited" social proof. Morgan has nothing to believe in yet.

### During the audit (the scariest wait)
4. **Onboarding carousel auto-rotates every 5s regardless of user focus.** Content slides out from under the user while they're reading.
5. **No ETA, no way to cancel, no reassurance if the audit stalls.** Hard-coded 18s delays in `audit-onboarding` don't match reality for domain audits that take 5+ minutes.
6. **No status visibility for in-progress audits** once the user navigates away. The audit list doesn't show PENDING/RUNNING/FAILED states.

### Reading results (the "aha" moment that isn't)
7. **`page-audit-review` is a 1,293-LOC monolith** with panels that scroll independently and lose context.
8. **Tone drift in results copy.** "Astounding!!!!", "truly exception experience" (sic) — breaks Morgan's trust. Priya sees it as sloppy.
9. **Results are dumped, not prioritized.** Issues appear in load-order, not impact-order. Morgan doesn't know what to fix first.

### After results (the handoff)
10. **Guest users can't export.** Audit finishes, user feels the "aha," clicks Export PDF, and is hard-walled into signup with no preview of what they'd get. Conversion leaks here.

Underlying all ten: **the product talks to itself more than to the user.** Screens are organized around the backend data model (audits, domains, page-audits, stats) rather than the user's goal (find problems, fix them, prove the fix).

---

## 3. Redesign principles

These are the five rules the rest of this document is derived from. When a decision is hard, return here.

1. **Demonstrate value before asking for anything.** The first full audit experience — including viewing results, seeing the prioritized fix list, and reading one AI recommendation — happens before signup. Signup unlocks *saving, exporting, and re-auditing*, not *seeing*.
2. **Progressive disclosure.** Morgan sees a score, a top-3 issue list, and a "what to do next" CTA. Priya clicks one button to reveal WCAG refs, selectors, and raw JSON.
3. **Status is a first-class citizen.** Every long-running operation has a visible state, an ETA, a way to leave and come back, and a notification when it's done.
4. **Prioritize for impact, not for taxonomy.** Default view is "Top 10 fixes ranked by estimated user impact × effort," not "Visual Design → Color → Contrast → 47 items."
5. **One tone, always.** Warm, plainspoken, mildly witty, never cute. No "!!!!"'s. No "Unfortunately." Every piece of copy passes a single voice test (see §10).

---

## 4. Information architecture — proposed

### 4.1 Current IA problems
- `/dashboard` is orphaned (no nav link).
- Two different review paths (`/audit/:id/page` vs `/audit/:id/review`) with subtle guard differences.
- `/how-it-works` and `/integration` are public but sit alongside workspace pages in the nav.
- No concept of *projects* or *workspaces* — every audit is equal peers in a flat list.

### 4.2 Proposed IA

```
Public (no auth)
├── /                      Landing (try it now)
├── /r/:shareToken         Shared audit result (read-only, no nav chrome)
├── /pricing               Pricing & plans
└── /how-it-works          Educational marketing

Authenticated (app shell)
├── /home                  Workspace home — recent activity, quick audit, monitored sites
├── /sites                 Site list (formerly "Domains")
│   └── /sites/:id         Site overview (aggregate scores, page list, trends)
├── /audits                Full audit history (paginated, filterable)
│   └── /audits/:id        Audit results (unified, adapts to single-page vs domain)
├── /integrations          Integrations marketplace
├── /settings              Account, billing, team, notifications
└── /help                  In-app help (merged with how-it-works)
```

**Key moves:**
- Rename **Domain → Site** everywhere in the UI. "Domain" is an engineer's word; "site" is everyone else's.
- Collapse the two review routes into one `/audits/:id` that adapts to its shape.
- Introduce **`/home`** as the post-login default — a dashboard that answers "what happened since I was last here?" instead of dumping the user into a list.
- Move `/integrations` to a lower nav priority until integrations actually ship.
- Move legal and help to a footer, not the primary nav.

### 4.3 Proposed sidebar

```
┌──────────────────────────┐
│  ◆ Look-see              │   ← logo + wordmark, 24px
│  ────────────────────    │
│                          │
│  ⚡ New audit   [ + ]    │   ← always-visible primary action
│                          │
│  WORKSPACE               │
│    ⌂ Home                │
│    ◈ Sites               │
│    ≡ Audits              │
│                          │
│  LEARN                   │
│    ? Help                │
│    ⇄ Integrations    •   │   ← dot = "new" or "early access"
│                          │
│  ─────────────────────   │
│  [ MB ] Morgan B.    ⌃   │   ← user menu, collapses up
└──────────────────────────┘
```

Rationale: the "New audit" button is the app's single most important action. It lives outside any list, always one click away. The sidebar collapses to a 56px icon rail on narrower viewports.

---

## 5. First-run experience — the onboarding redesign

This section is the core of the brief. The current first-run has ~8 friction points in ~90 seconds. Below is the target flow.

### 5.1 Target flow (happy path, non-expert guest)

```
T+0s       Land on /          →  Immediate URL input, autofocused
T+3s       User types URL
T+4s       Click "Audit my site"  (no login yet)
T+4s       Audit starts. Full-screen progress scene.
T+4-30s    Live feed of what we're checking, actual findings streaming in
T+30s      Results slide in. Score, top 3 fixes, "see all 27 issues"
T+45s      User reads top fix. Clicks "Why this matters."
T+60s      "Save this report?" soft prompt, inline signup (Google, email)
T+90s      Signed up. Report saved. Next audit unlocked.
```

Target activation metric: **70% of URL submissions reach "results visible" state; 35% of those complete signup within 5 minutes.**

### 5.2 Landing page redesign

**Above the fold — only three things:**

```
┌─────────────────────────────────────────────────────────────────┐
│  ◆ Look-see                         Pricing   Log in   [Try it] │
│                                                                 │
│                                                                 │
│      Find what's broken on your website.                        │
│      In under a minute. Without opening DevTools.               │
│                                                                 │
│      ┌─────────────────────────────────────┬───────────────┐    │
│      │  🌐  https://your-site.com          │  Audit my site │    │
│      └─────────────────────────────────────┴───────────────┘    │
│                                                                 │
│      Free, no signup. We'll check accessibility,                │
│      content quality, visual design, and SEO.                   │
│                                                                 │
│      ▸ Trusted by teams at [logo] [logo] [logo] [logo]          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Fixes applied:**
- New headline: concrete, benefit-first, uses plain language. Old: *"Find and fix accessibility issues in seconds."* New: *"Find what's broken on your website."* (Accessibility is *one* of the things we find; leading with it narrows the addressable market.)
- Input is the hero, CTA is adjacent, not below.
- "Free, no signup" removes the #1 guest conversion barrier.
- Microcopy lists the four audit dimensions so the user knows what they'll get.
- Social proof lives above the fold, not in a separate testimonials section.
- **URL validation is permissive.** Accept anything that parses as a URL; prepend `https://` if scheme is missing; let the backend reject if unreachable. Remove the TLD regex entirely.

**Below the fold, three sections only:**
1. **Live example.** A canned audit of a well-known site rendered in an iframe-like preview, showing the results UI the user is about to experience. Sells the product by showing it.
2. **How it works** (three steps with illustrations — reuse existing assets in `/assets/illustrations/`).
3. **What we check.** The four audit dimensions explained in one paragraph each, each with a "See a sample issue →" link that scrolls to an inline example.

Cut: the second CTA section, the "aggressive hot pink" banner flagged in the existing plan, the footer-as-secondary-nav pattern.

### 5.3 The progress scene (T+4s to T+30s)

This is the most underrated screen in the product. The user has committed something (a URL) and is waiting. Every second of that wait either builds confidence or erodes it.

**Current:** a 4-slide carousel auto-rotates every 5s with generic marketing copy while 5 hardcoded "steps" tick past on fake timers.

**Proposed:** a **live audit feed** — the user watches their site being audited in real time.

```
┌───────────────────────────────────────────────────────┐
│                                                       │
│   Auditing yoursite.com                               │
│                                                       │
│   [ progress bar: 43% ]           about 20s remaining │
│                                                       │
│   ┌─────────────────────┐  ┌────────────────────────┐ │
│   │                     │  │ ✓ Captured page (2.1s)│ │
│   │                     │  │ ✓ Fetched 14 assets    │ │
│   │  [ live screenshot  │  │ ⋯ Checking contrast…   │ │
│   │    of target page ] │  │ ✓ Found 3 heading      │ │
│   │                     │  │   hierarchy issues     │ │
│   │                     │  │ ⋯ Analyzing typography │ │
│   └─────────────────────┘  └────────────────────────┘ │
│                                                       │
│   [ Leave and notify me when done ]                   │
│                                                       │
└───────────────────────────────────────────────────────┘
```

**Why this works:**
- The screenshot thumbnail turns an abstract wait into "we're looking at *your* site" — immediate trust.
- The streaming feed gives concrete evidence of work. Every checkmark is a tiny dopamine hit.
- Real findings begin appearing *before* the audit is done ("Found 3 heading hierarchy issues") — the user starts extracting value at T+10s, not T+30s.
- The "Leave and notify me when done" button acknowledges that audits take time and treats the user's time as precious. This is enabled by email capture at this step (optional) or a browser notification permission request.
- **No carousel.** If we need to educate the user while they wait, do it as an "optional" panel they can open, not as involuntary content.

**Implementation note:** the WebSocket/Pusher pipeline flagged in the review already streams `auditUpdate` events; this screen just needs to render them well. Today the data flows in but isn't surfaced. The audit service's existing event shapes (`pageFound`, `auditUpdate`) support this with minor additions.

### 5.4 The results reveal (T+30s)

When the audit finishes, the progress scene transitions (not jumps) to results. The first view is radically simpler than today's `page-audit-review`:

```
┌───────────────────────────────────────────────────────────────┐
│   yoursite.com/                         Audit complete ✓      │
│                                                               │
│   ┌────────┐   UX Health Score                                │
│   │   72   │   Room to improve. You have 3 critical issues    │
│   │   /100 │   and 24 minor ones. Start with the top 3 below. │
│   └────────┘   ◎ Accessibility 64  ◎ Content 81  ◎ Visual 78  │
│                                                               │
│   ─────────────────────────────────────────────────────────   │
│                                                               │
│   🔴  Contrast too low on primary CTA button                  │
│       Users with low vision may not see your "Sign up" button │
│       Affects: 1 element • WCAG 2.1 AA                        │
│       [ View details → ]                                      │
│                                                               │
│   🔴  Missing alt text on 8 images                            │
│       Screen readers can't describe these images to users     │
│       Affects: 8 elements • WCAG 2.1 A                        │
│       [ View details → ]                                      │
│                                                               │
│   🟡  Heading hierarchy skips H2                              │
│       Your page jumps from H1 to H3, confusing assistive tech │
│       Affects: 3 elements • WCAG 2.1 AA                       │
│       [ View details → ]                                      │
│                                                               │
│   ─────────────────────────────────────────────────────────   │
│   Show 24 more issues ▾                                       │
│                                                               │
│   [ Save report ]  [ Export PDF ]  [ Share link ]             │
└───────────────────────────────────────────────────────────────┘
```

**Key moves:**
- Score lives with **one sentence of plain-English meaning.** No user should have to wonder "is 72 good?"
- Sub-scores are one line, not five huge cards.
- **Top 3 issues, ranked by impact × severity**, with human-language descriptions. The taxonomic breakdown ("Visual Design → Color → Contrast → Specific-check-name") is *inside* the detail view, not the list.
- "Show 24 more" is an accordion, not another page. The full list is one scroll away.
- The three action buttons at the bottom are the *only* CTAs on this screen. "Save report" triggers signup inline (see §5.5).

### 5.5 Inline signup, not a wall

Current: clicking Export PDF as a guest opens a modal demanding signup.

Proposed: an **inline prompt card** appears above the Export button once the guest user has been on the results page for >15 seconds or scrolls past the 3rd issue:

```
┌─────────────────────────────────────────────────────────┐
│  Save this report for later?                            │
│                                                         │
│  We'll keep your audit history, track your score over   │
│  time, and unlock 9 more free audits this month.        │
│                                                         │
│  [ Continue with Google ]  [ Sign up with email ]       │
│                                                         │
│  Already have an account? Log in                        │
└─────────────────────────────────────────────────────────┘
```

**Fixes applied:**
- No "Unfortunately." Signup is framed as *unlocking*, not *paywalling*.
- Concrete number of free audits ("9 more this month"), not vague "10 free audits to users."
- Two auth options visible, not hidden in a dialog.
- **Report is already saved to a share link** the user can bookmark even without signing up — so "Continue without signing up" is a real option. Reduce the sense of coercion.

### 5.6 What signup unlocks vs. what it doesn't

| Capability                 | Guest | Free | Paid |
|----------------------------|-------|------|------|
| Run single-page audit      | ✓     | ✓    | ✓    |
| View results               | ✓     | ✓    | ✓    |
| Shareable link (30 days)   | ✓     | ✓    | ✓    |
| Save audit history         | ✗     | ✓    | ✓    |
| Export PDF                 | ✗ (preview) | ✓ | ✓ |
| Re-run / compare audits    | ✗     | ✓    | ✓    |
| Domain / multi-page audit  | ✗     | limited | ✓ |
| Monitoring & scheduled runs | ✗    | ✗    | ✓    |
| Team + sharing with editors | ✗    | ✗    | ✓    |

Guests see a watermarked PDF *preview* in-browser, not a download. That demonstrates the product without giving it away.

---

## 6. Core loop — returning user experience

### 6.1 `/home` — the new default

After login, the user lands on `/home`, not `/audits`. The list is a secondary screen; the home is a state-of-the-world summary.

```
┌─────────────────────────────────────────────────────────────────┐
│  Good morning, Morgan.                                          │
│                                                                 │
│  ┌──────────────────────────────────────────────┐               │
│  │  ⚡ Run a new audit                          │               │
│  │  [ https://...                       ][ Go ] │               │
│  └──────────────────────────────────────────────┘               │
│                                                                 │
│  Your sites                                       Add site →    │
│  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐       │
│  │ acme.com       │ │ shop.acme.com  │ │ docs.acme.com  │       │
│  │ 82 / 100  ↑3   │ │ 67 / 100  ↓2   │ │ 91 / 100  —    │       │
│  │ 3 new issues   │ │ 11 new issues  │ │ all clear      │       │
│  └────────────────┘ └────────────────┘ └────────────────┘       │
│                                                                 │
│  Recent audits                                    See all →     │
│  ─────────────────────────────────────────────────────────────  │
│  acme.com/pricing           72 / 100   2 hours ago       ⋯      │
│  shop.acme.com/checkout     54 / 100   yesterday         ⋯      │
│  acme.com/                  82 / 100   3 days ago        ⋯      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

This answers "what's changed since I was last here?" in three seconds. The new-audit input is always at the top, because that's the primary action.

### 6.2 `/audits` — the list, done right

The current audit list is a cramped 12-column grid with no pagination, no filtering, inconsistent status handling, and an empty state that says "Enter a URL above" when the form is below.

**Proposed:**

- Default sort: recent first. Secondary sort options: score (low to high for triage), URL.
- **Status column is first-class.** Pending / Running (with live % from WebSocket) / Complete / Failed — with clickable retry for failed.
- **Bulk actions.** Select multiple audits to compare side-by-side, export together, or delete.
- **Filter chips** above the table: Site, Date range, Status, Score range.
- **Skeleton rows during load** (the existing plan calls this out; keep it).
- **Empty state says "Run your first audit"** with the input *right there*, not above.
- **Row action menu**: View, Re-run, Compare with previous, Share, Delete.

Remove the three-column-of-icons score pattern. Each row gets one badge showing the overall score, color-coded, and the row expands to show sub-scores on click.

### 6.3 `/audits/:id` — unified results (replaces `page-audit-review`)

One route, one component, shaped by whether the target is a single page or a site.

**Single-page audit layout:**

```
┌─────────────────────────────────────────────────────────────────┐
│  ← Back to audits         yoursite.com/pricing     Share  Export │
│                                                                 │
│  ┌────────┐  UX Health 72                                        │
│  │   72   │  ◎ Access 64  ◎ Content 81  ◎ Visual 78  ◎ SEO 70   │
│  └────────┘  Audited 2 min ago by Morgan · Re-run               │
│                                                                 │
│  ┌─────────────────────────┬────────────────────────────────┐   │
│  │  [ Top issues (27) ]   ││  ┌──────────────────────────┐  │   │
│  │  Filter: All | A11y |  ││  │                          │  │   │
│  │         Content | Vis  ││  │  [ page screenshot with  │  │   │
│  │  Sort:  Impact ▾       ││  │    issue hotspots ]      │  │   │
│  │                        ││  │                          │  │   │
│  │  ▼ Critical (3)        ││  │                          │  │   │
│  │    • Contrast on CTA   ││  │                          │  │   │
│  │    • Missing alt text  ││  │                          │  │   │
│  │    • Form label missing││  └──────────────────────────┘  │   │
│  │                        ││                                │   │
│  │  ▼ Major (8)           ││  ┌──────────────────────────┐  │   │
│  │    …                   ││  │ Selected: Contrast on CTA│  │   │
│  │                        ││  │ ──────────────────────── │  │   │
│  │  ▸ Minor (16)          ││  │ Why it matters           │  │   │
│  │                        ││  │ Users with low vision…   │  │   │
│  │                        ││  │                          │  │   │
│  │                        ││  │ WCAG 2.1 AA · 1.4.3      │  │   │
│  │                        ││  │                          │  │   │
│  │                        ││  │ How to fix               │  │   │
│  │                        ││  │ Change background from…  │  │   │
│  │                        ││  │                          │  │   │
│  │                        ││  │ [ Copy fix ] [ Mark done]│  │   │
│  │                        ││  └──────────────────────────┘  │   │
│  └─────────────────────────┴────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Key design decisions:**

- **Three panels, each independently scrollable, with sticky headers.** The current design has panels that scroll independently but lose context — fix by making the issue row always in view in the left rail when the detail panel shows it.
- **Grouped by severity by default**, because that's what Morgan needs. Power users can re-group by category via the Sort/Group control.
- **Screenshot with hotspots.** Each visual issue gets a numbered dot on the screenshot. Clicking the dot selects the issue in the left rail and scrolls it into view. Clicking an issue in the rail highlights the dot. This is the tight loop the current `element-info-dialog` approximates clumsily.
- **Detail panel** always has the same four sections: *Why it matters* (Morgan-facing plain English), *WCAG / standards reference* (Priya-facing), *How to fix* (AI-generated, copyable), *Action* (Mark done / Assign / Ignore). Anything else is collapsed.
- **"Mark done" is a first-class verb.** Users can check off fixes as they ship them. The score then re-calculates in a next-audit preview. This is the state change the current product is missing.
- **Keyboard nav.** `j`/`k` to move between issues, `x` to mark done, `/` to focus filter, `?` for shortcuts cheat sheet. This serves Priya without bothering Morgan.

**Site-audit layout** uses the same shell but replaces the screenshot panel with a page list (what the current `audit-dashboard` does), and replaces the issue list with an aggregated issue view grouped by "issues affecting the most pages."

### 6.4 Screenshot + element interaction

Currently, `element-info-dialog` opens as a modal to show element details. Modals are context-breaking. Replace with:

- **Hover a screenshot hotspot** → tooltip with issue title and severity chip.
- **Click a hotspot** → selects the issue; detail panel updates.
- **Hover an issue in the left rail** → hotspot on the screenshot gets an outline + label.
- **Double-click a hotspot** → expands to a zoomed-in crop of that element, with the element's DOM selector copyable.

No modal needed. Everything stays on one page.

### 6.5 Sharing & collaboration

Current product has an analytics event `'Clicked share button'` but no clear collaboration model. Proposed:

- Every audit has a **public share URL** (`/r/:shareToken`) that's view-only, no login required, no nav chrome. Rendered as a single-page, print-friendly version of the results.
- **Expires after 30 days** for guests, 1 year for free users, unlimited for paid.
- **Comments** on issues (paid feature) — team members can discuss and resolve issues inline.
- **Assign issues** to teammates (paid feature) — issue becomes a todo with an owner.
- Export formats: PDF (stakeholder-ready), CSV (engineering triage), Jira/Linear (once integrations ship).

---

## 7. Screen-by-screen specs

Short treatments of the remaining pages.

### 7.1 `/sites/:id` — Site overview

**Current `audit-dashboard` problems (from review):** 5-column score card grid too cramped, "Coming soon" labels in the middle of scores, page list duplicates the audit list template.

**Proposed structure:**

```
┌─────────────────────────────────────────────────────────────┐
│  acme.com                    [ Run full audit ] [ Settings ]│
│                                                             │
│  ┌──────┐  Site health  76                                  │
│  │  76  │  ↑ 4 points since last audit (7 days ago)         │
│  └──────┘  ◎ Access 64  ◎ Content 81  ◎ Visual 78  ◎ SEO 81│
│                                                             │
│  ▸ Score trend (last 30 days)  [sparkline chart]            │
│                                                             │
│  Pages audited (23)                  Sort: Lowest score ▾   │
│  ─────────────────────────────────────────────────────────  │
│  /pricing          54   ■■■■■■■■■■□□□□  17 issues   → view  │
│  /checkout         62   ■■■■■■■■■■■■□□  12 issues   → view  │
│  /login            71   ■■■■■■■■■■■■■■□   8 issues  → view  │
│  /                 82   ■■■■■■■■■■■■■■■   3 issues  → view  │
│  …                                                          │
└─────────────────────────────────────────────────────────────┘
```

- **"Coming soon" sub-scores are removed until they work.** Showing an empty promise in the middle of a working UI is worse than not showing anything. Ship one new sub-score at a time with a "New" badge.
- Sort default: lowest score first (triage mode — show me where to work).
- Each page row links to its `/audits/:auditId` result.

### 7.2 `/sites` — Site list

A lightweight dashboard of every site the user has added. Card-based, each card shows health score, trend, and last audit time. An "Add site" card uses the same pattern. Clicking a card routes to `/sites/:id`.

### 7.3 `/integrations`

**Current:** 6 grayed-out cards, all "Coming soon," no hover state.

**Proposed:** either **ship something real** (Slack notifications for completed audits is the lowest-effort win, Jira export is next), **or don't show the page at all** until at least one integration works.

If the page stays, each card gets:
- Clear status chip: **Available** / **Beta** / **Request access**
- For "Request access" cards, a working email capture with confirmation.
- For "Available" cards, a "Connect" button that starts the OAuth flow.

Never show dead UI.

### 7.4 `/settings`

Tabbed page: **Account** (name, email, password, delete account) • **Billing** (current plan, upgrade, invoices) • **Team** (paid: invite members, manage roles) • **Notifications** (email, Slack, browser) • **API keys** (paid).

Current `user-profile` is a minimal stub; this is greenfield.

### 7.5 `/help`

Merge `/how-it-works` into `/help`. In-app help uses a command palette (`⌘K`) as the primary discovery surface for power users, with a flat searchable article list as the fallback.

### 7.6 Footer

Minimal site-wide footer: logo, © 2026 Look-see, Privacy, Terms, Status, Contact. Kill the 2021 copyright.

---

## 8. Component library — what to build first

Assuming the existing `REDESIGN_PLAN.md`'s Phase 1 design system lands, here are the behavior-focused components this UX redesign needs:

1. **`URLInput`** — permissive URL input with scheme auto-prepend, validation on blur (not on submit), clear error states, and a "try this URL" suggestion if validation fails.
2. **`AuditProgress`** — full-screen progress scene driven by WebSocket events; includes screenshot preview, feed, ETA, and "notify me" affordance.
3. **`IssueRow`** — severity chip, title, one-line why-it-matters, affected-count, click to select. Used in results and site overview.
4. **`IssueDetailPanel`** — four-section layout (Why / WCAG / Fix / Action); keyboard-navigable.
5. **`ScreenshotCanvas`** — clickable, pannable, zoomable screenshot with issue hotspots. This is the single most technically interesting component and the biggest visual-clarity unlock.
6. **`ScoreGauge`** — circular score with semantic color, sized `sm`/`md`/`lg`/`xl`. Replaces three different inline SVG implementations.
7. **`TrendChart`** — score sparkline with deltas. Used on home, site overview, and site list cards.
8. **`EmptyState`** — illustration + headline + body + CTA; matched to context (no audits / no sites / no results / no issues).
9. **`ShareLink`** — inline copyable URL with expiry indicator.
10. **`SkeletonLoader`** — for audit list, site list, home, results.

These map cleanly onto the REDESIGN_PLAN's Phase 1 foundation but add the *behavior* layer that plan focuses less on.

---

## 9. Accessibility — eat our own cooking

We sell accessibility. Our product must model it.

- **WCAG 2.1 AA baseline, AAA where possible.** Every shipped component passes axe + manual screen reader testing.
- **Full keyboard nav.** Every interactive element reachable by tab; skip-to-content link; focus rings visible and high-contrast.
- **Semantic HTML** throughout. The current templates lean on `<div>` for interactive elements (flagged in the existing plan). Fix systematically.
- **ARIA live regions** for audit progress updates (screen reader users hear streaming findings instead of silence).
- **Reduced-motion respect.** The progress scene's streaming animations honor `prefers-reduced-motion: reduce` by switching to static updates.
- **Color contrast** on every state. The current brand pink `#FF0050` fails AA on white for normal text; use it only for large text or decorative, and darken to `#D40043` (or similar) for body-text accent uses.
- **Dark mode.** Flagged but not implemented. Add as part of the token system from day one so we're not retrofitting.

---

## 10. Copy & voice

### 10.1 Voice test

Every string passes this three-question check:
1. Would Morgan, a smart non-expert, understand this without a glossary?
2. Is it specific (a number, a name) rather than vague?
3. Does it sound like a helpful colleague, not a vending machine or a cheerleader?

### 10.2 Rewrites — before / after

| Context | Before | After |
|---|---|---|
| Guest dialog | *"Unfortunately we only provide 1 free audit for visitors without an account. On the bright side, we allow up to 10 free audits…"* | *"Create a free account to run 9 more audits this month — and save this one."* |
| Abandon confirmation | *"Are you sure that you want to start a new audit? Starting a new audit will cause the currently running audit to be abandoned."* | *"This will cancel the audit in progress. Start a new one anyway?"* |
| Results praise | *"Astounding!!!! truly exception experience"* | *"No issues found in this category. "* |
| Loading | *"Please wait while we load your audits…"* | *"Loading your audits…"* (skeletons do the real work) |
| Landing headline | *"Find and fix accessibility issues in seconds"* | *"Find what's broken on your website — in under a minute."* |
| Empty audit list | *"Enter a website URL above to run your first UX audit."* | *"Run your first audit."* (with the input directly below) |
| Mobile block modal | *"Hey there! Look-see works best on desktop."* | *"Look-see works best on desktop. We'll email you a link so you can pick up where you left off."* + email capture |

### 10.3 Tone principles

- **No exclamation points** except in genuine celebration (audit complete, first issue fixed).
- **No "Oops"** or other cute error words. Be direct: *"We couldn't reach that URL. Check the address and try again."*
- **No "Unfortunately," "We're sorry," "Please forgive,"** or other apologetic hedges in transactional flows.
- **Numbers over adjectives.** "9 more audits this month" beats "plenty of free audits."
- **Second person.** "You have 3 critical issues." Not "There are 3 critical issues" and definitely not "The user has 3 critical issues."

---

## 11. Responsive strategy

The current product has a partial mobile block modal and no serious tablet/mobile experience. Given that audit results are inherently visual and information-dense, **full mobile parity isn't the goal** — *mobile-useful* is.

- **Mobile (< 640px):** View-only experience. Users can read shared audit links, view the top-3 issues, read details, share. Cannot initiate new audits or compare. Entry points that require the full UI show a "Best on desktop — email me a link" card, with email capture and deep link.
- **Tablet (640–1024px):** Single-panel responsive layout. The three-panel results view collapses to tabs (Issues / Screenshot / Detail) with a drawer pattern for the detail panel.
- **Desktop (> 1024px):** Full three-panel layout as specified.

The mobile-block modal becomes unnecessary — every screen degrades gracefully instead.

---

## 12. Metrics — what to measure after launch

This redesign is only worth the cost if we can tell whether it worked. Instrument these events from day one (piggyback on the existing Segment integration):

**Activation funnel (the most important):**
1. `Landing: URL submitted`
2. `Audit: started` (split by guest vs. authed)
3. `Audit: first-finding-surfaced` (new — t < audit complete)
4. `Results: viewed`
5. `Results: issue-detail-opened` (proves the user engaged with a finding)
6. `Results: action-taken` (save, export, share, mark-done)
7. `Auth: signup-completed`

Track time between each step. The 1→4 conversion is the north-star activation rate.

**Value moments:**
- `Results: copy-fix-suggestion`
- `Results: mark-issue-done`
- `Audit: re-run-after-fixes`
- `Share: link-viewed` (by recipient)

**Friction signals:**
- `Landing: URL-validation-failed` (should go to ~0 after regex removal)
- `Audit: abandoned-during-progress`
- `Results: scroll-without-click` (dwell without engagement)
- `Signup: dialog-dismissed`

---

## 13. Implementation sequencing

This redesign is large. Ship it in four phases, each with independent user value.

### Phase A — Foundation & onboarding (2–3 sprints)

Biggest conversion lift. Lowest technical lift.

1. Fix landing URL validation (permissive parsing, remove TLD regex).
2. Rewrite landing hero copy and CTA.
3. Replace onboarding carousel with live audit feed (reuse existing WebSocket events).
4. Inline signup prompt (replace hard-wall dialog).
5. Guest share link with 30-day expiry.
6. Copy pass on all dialog and error strings.

### Phase B — Results redesign (3–4 sprints)

The core loop.

7. Unify `/audit/:id/page` and `/audit/:id/review` routes.
8. Build `IssueRow`, `IssueDetailPanel`, `ScreenshotCanvas` components.
9. Default issue sort by severity × impact.
10. "Mark done" action and post-fix re-audit preview.
11. Keyboard nav + shortcut cheat sheet.
12. Replace `element-info-dialog` modal with inline detail panel.

### Phase C — IA & workspace (3 sprints)

13. Rename Domains → Sites throughout.
14. Build `/home` as post-login default.
15. Build `/sites/:id` redesign.
16. Redesign `/audits` list with status column, filters, bulk actions, pagination.
17. Consolidate `/how-it-works` into `/help`.

### Phase D — Polish & power (ongoing)

18. Responsive & mobile view-only.
19. Dark mode.
20. Ship at least one real integration (Slack notifications).
21. Team collaboration features (comments, assignments) — paid tier.
22. Monitoring & scheduled audits — paid tier.

---

## 14. Risks & open questions

These need product-side answers before implementation:

1. **Pricing model.** The "9 free audits this month" number is placeholder. Real limits need to come from billing/Stripe configuration. Current `start-audit-login-required-dialog` hardcodes "10 free audits" — that needs to be a config value.
2. **Backend streaming support.** The "live findings" progress scene assumes findings can be surfaced as they're computed. Today, `auditUpdate` Pusher events carry progress metadata but not partial findings. Needs backend work in `audit-service` + `AuditManager`.
3. **Share token architecture.** Public share links (`/r/:token`) need a tokenized, time-limited, revocable scheme. Likely a new backend endpoint + token table.
4. **Re-run / compare flow.** "Mark done" previewing the next score assumes we can simulate the fix. Either we actually re-run the audit (costly) or we provide a projected score with a caveat ("Estimated after fixes — run a new audit to confirm"). The latter is cheaper and honest.
5. **Multi-page audit data shape.** The unified `/audits/:id` route needs the detail component to handle both single-page and site audits cleanly. This is a template architecture question best resolved during Phase B.
6. **Copy ownership.** This redesign rewrites ~30 user-facing strings. Decide if copy lives in code, in i18n files (opening the door to localization), or in a CMS.
7. **Analytics consent.** The review notes Segment fires without a consent banner. Before this redesign ships to EU users, GDPR consent needs to gate Segment initialization — this is a compliance risk today, not a design question, but the redesign is the right moment to fix it.

---

## 15. Summary — what changes, in one screen

```
Before                              After
──────                              ─────
URL regex rejects .io domains  →   Permissive URL parsing
"Unfortunately" gate dialog    →   Inline signup after value shown
4-slide marketing carousel     →   Live audit feed with findings streaming
1293-LOC results monolith      →   3-panel results with 4 focused components
Issues in load order           →   Issues ranked by impact × severity
Separate modal for elements    →   Inline detail panel, same screen
Domains / How it works in nav  →   Sites / Help, with orphans removed
Audits-first default           →   Home-first with state-of-world summary
"Coming soon" grayed cards     →   Real integrations or no page at all
Copyright 2021                 →   Copyright 2026, clean footer
```

Every change above is traceable to one of the five redesign principles in §3. If any of them can't be, cut them.

---

*End of redesign document.*
