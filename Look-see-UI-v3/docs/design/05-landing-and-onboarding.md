---
id: 05
title: Landing & onboarding friction fixes
status: Draft
phase: A
addresses:
  - URL validation regex rejects valid TLDs (.io, .co, .me)
  - Pessimistic "Unfortunately" login-required dialog
  - Auto-rotating marketing carousel during wait (see 02)
  - No trust scaffolding on landing (no logos, no sample, no proof)
  - Guest export wall — value lost before demonstrated
  - Copy tone drift ("Astounding!!!!", typos)
owner: Design + Frontend + Marketing
related:
  - UX_REDESIGN.md §5.1, §5.2, §5.5, §10
  - 02-audit-status-and-progress.md
---

# 05 — Landing & onboarding friction fixes

## Problem

The top-of-funnel has four friction points within the first 60 seconds:

1. **URL validation is punitive.** `landing.component.ts` validates URLs against a regex that hardcodes known TLDs. Valid modern URLs (`.io`, `.co`, `.me`, `.app`, `.dev`) get rejected with a generic error. This is the user's first interaction with the product and it fails.

2. **Guest conversion dialog is pessimistic.** Verbatim: *"Unfortunately we only provide 1 free audit for visitors without an account. On the bright side, we allow up to 10 free audits to users that create a FREE account."* Starts with "Unfortunately" at the moment we're trying to convert. Leads with scarcity, not value.

3. **No trust scaffolding above the fold.** Landing page goes URL input → features → footer. No customer logos, no sample report preview, no "X audits run," no testimonials. Morgan (the non-expert) has nothing to believe in.

4. **Guest export wall.** Guest users who complete an audit can see results but clicking "Export PDF" opens a login modal. Users have experienced the value *and* been blocked from taking the obvious next action.

Adjacent issues:
- Copy drift: "Astounding!!!!", "truly exception experience" (typo), "Hey there!", "Please wait while we load your audits…".
- Landing footer shows © 2021.

## Goals

1. **Permissive URL parsing** — accept anything that parses as a URL; never reject at the client level.
2. **Inline, value-first signup** — demonstrate value before asking for signup; signup appears as an inline card, not a wall.
3. **Rewrite guest/export/error dialogs** to be direct, specific, and numerate.
4. **Add trust scaffolding** above the fold on the landing page.
5. **Show, don't tell** — a live sample audit result visible from the landing page.

## Non-goals

- Pricing page design (separate spec).
- A/B testing infrastructure — assume we can toggle via feature flag and measure via Segment.
- Localization — English only.

## User stories

- *As Morgan*, I paste `acme.co` and it just works — no error, no scheme required.
- *As Morgan*, I complete a guest audit, see my score, read the top issue, and only *then* am I asked to sign up — and the ask makes sense ("save this report").
- *As a skeptic*, I land on `/` and see real customer logos and a sample report before I commit to entering a URL.
- *As Priya*, I sign up because I need historical tracking and PDF export — the upgrade path from guest is transparent.

## Design

### 1. URL input — permissive

**Validation at blur (not on submit):**

```ts
function normalizeUrl(input: string): { url: string; warning?: string } | { error: string } {
  const trimmed = input.trim();
  if (!trimmed) return { error: 'Enter a website URL.' };

  // Prepend https:// if scheme missing
  const withScheme = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;

  let parsed: URL;
  try {
    parsed = new URL(withScheme);
  } catch {
    return { error: 'That doesn\'t look like a valid website address.' };
  }

  // Reject only obviously-invalid hosts
  if (!parsed.hostname.includes('.') || parsed.hostname.endsWith('.')) {
    return { error: 'That doesn\'t look like a valid website address.' };
  }

  // Reject local/private URLs politely
  if (parsed.hostname === 'localhost' || parsed.hostname.startsWith('127.')) {
    return { error: 'We can\'t audit local addresses. Try a public URL.' };
  }

  return { url: parsed.toString() };
}
```

**Behaviors:**
- No regex allow-list of TLDs. Anything `URL()` constructor accepts is accepted.
- Scheme is auto-prepended if missing (`acme.co` → `https://acme.co`).
- Validation runs on blur (input loses focus), not on every keystroke, so users aren't yelled at while typing.
- If the server later rejects (DNS, 4xx, 5xx), the error surfaces from the backend with a specific, actionable message ("We couldn't reach this URL. Check the address and try again.").

**Input UI:**

```
┌─────────────────────────────────────────────┬───────────────┐
│ 🌐  https://your-site.com                   │  Audit my site│
└─────────────────────────────────────────────┴───────────────┘
   ↑ globe icon; scheme hint in placeholder
```

On focus, placeholder fades; cursor positioned at start. Clearing the field with `x` button.

### 2. Landing page — above the fold

See UX_REDESIGN.md §5.2 for the full layout. Key elements:

- **Headline:** *"Find what's broken on your website."*
- **Sub-headline:** *"In under a minute. Without opening DevTools."*
- **URL input + CTA** (hero-sized).
- **Micro-reassurance below the input:** *"Free, no signup. We'll check accessibility, content quality, visual design, and SEO."*
- **Trust row:** *"Trusted by teams at"* + 4 customer logos. If no logos yet, replace with *"3,247 sites audited this month"* (live count from backend).

### 3. Landing page — below the fold

Three sections only:

**Section 1 — Live sample.** An embedded preview of a canned audit result (e.g., audit of `wikipedia.org`). The actual results UI (see spec 03) rendered inline, with:
- 3 visible issues
- A hotspot on the embedded screenshot
- A "Try it on your site ↑" link that scrolls back to the input

This sells the product by showing exactly what the user will get.

**Section 2 — How it works (3 steps).**

| Step | Illustration | Headline | Body |
|---|---|---|---|
| 1 | `audit_website.jpg` | Paste a URL | *"No setup. No crawler config. Just a web address."* |
| 2 | `review_audit.png` | Read your report | *"We rank every issue by impact so you know what to fix first."* |
| 3 | `share_illustration.png` | Share with your team | *"Send a link. Export a PDF. Assign issues to teammates."* |

**Section 3 — What we check.**

Four paragraphs, one per audit dimension (Accessibility, Content, Visual, SEO). Each ends with *"See a sample issue →"* that opens an inline expandable with a real example.

**Footer:** minimal (logo, © 2026 Look-see, Privacy, Terms, Status, Contact). See spec 04 §1.

### 4. Guest flow — unblock everything

Current: guest runs 1 audit, must sign up to view PDF export, must sign up to save, must sign up to run a second.

Proposed:
- **Guest runs audits.** Per-IP/per-session limit (default: 3 audits per day) instead of "1 ever." Generous enough to sell the product.
- **Guest sees results.** Full results UI, all issues visible.
- **Guest gets a share link.** `/r/:token` with 30-day expiry, auto-generated on first audit completion. Copyable.
- **Guest export preview.** Export PDF button is enabled; click opens an in-browser preview (not a download), with a subtle watermark across the page: *"Preview — sign up to download a clean copy."*
- **Guest save.** Inline card (see §5) appears after 15s or after scrolling past the third issue. Dismissable.
- **Guest at limit.** When they try to run a 4th audit, show a card (not a modal): *"You've used today's 3 free audits. Sign up to run 7 more this month."* Not apologetic, just factual.

### 5. Inline signup card

```
┌─────────────────────────────────────────────────────────┐
│  Save this report for later?                            │
│                                                         │
│  We'll keep your audit history, track your score        │
│  over time, and unlock 7 more free audits this month.   │
│                                                         │
│  ┌───────────────────────┐  ┌────────────────────────┐  │
│  │ Continue with Google  │  │ Sign up with email     │  │
│  └───────────────────────┘  └────────────────────────┘  │
│                                                         │
│  Already have an account? Log in →                      │
└─────────────────────────────────────────────────────────┘
```

**Rules:**
- Appears inline on the results page (not modal).
- Position: below the score header, above the issue list, once trigger fires.
- Triggers (any of):
  - User has been on the results page ≥ 15s.
  - User scrolls past the 3rd issue in the list.
  - User clicks Export PDF or Save.
- Dismissable with subtle `×`. Dismissal persists for the session (not shown again for 24h).
- Two auth options visible (Google + email). No stack of six social logins.
- **Unlock copy is numeric and specific.** "7 more free audits this month" beats "more free audits."

### 6. Copy rewrites

| Location | Before | After |
|---|---|---|
| Landing hero | *"Find and fix accessibility issues in seconds"* | *"Find what's broken on your website."* |
| Landing sub | *"Enter any URL and get a comprehensive audit of accessibility, content quality, and visual design — with AI-powered fix suggestions."* | *"In under a minute. Without opening DevTools."* |
| Landing CTA | *"Start Audit"* | *"Audit my site"* |
| URL helper | — | *"Free, no signup. We'll check accessibility, content, visual design, and SEO."* |
| Guest limit dialog | *"Unfortunately we only provide 1 free audit for visitors without an account."* | *"You've used today's 3 free audits. Sign up to run 7 more this month."* |
| Guest limit CTA | *"Log In"* | *"Sign up free"* / *"Log in"* (two buttons) |
| URL invalid error | *"Please enter a valid URL (e.g. https://www.example.com)"* | *"That doesn't look like a valid website address."* |
| Backend unreachable | (generic toast) | *"We couldn't reach that URL. Check the address and try again."* |
| Landing footer | *"© 2021 Look-see Inc."* | *"© 2026 Look-see, Inc."* |
| Empty audit list | *"Please wait while we load your audits…"* | — (skeleton rows replace this) |
| Results empty category | *"Astounding!!!! truly exception experience"* | *"No issues found in this category."* |
| Mobile block | *"Hey there! Look-see works best on desktop."* | *"Look-see works best on desktop. Enter your email and we'll send you a link to pick up where you left off."* + email input |
| Abandon confirmation | *"Are you sure that you want to start a new audit? Starting a new audit will cause the currently running audit to be abandoned."* | *"This will cancel the audit in progress. Start a new one anyway?"* |
| Beta modal | (lists specific bugs) | *"You're an early Look-see user. Thanks for the patience while we polish the rough edges — [hey@look-see.com] for feedback."* |

**Voice rules** (from UX_REDESIGN.md §10, restated here for spec ownership):
- No "Unfortunately," no "Please," no "Oops," no "Sorry" in transactional flows.
- No `!` except in audit-complete celebration.
- Specific numbers over vague adjectives.
- Second person always.

## Technical design

### Landing URL validation

- Replace the regex validator in `landing.component.ts` with the `normalizeUrl` function above.
- Also applied in `audit-form.component.ts` and anywhere else URL input appears. Extract to a shared pure function: `src/app/utils/url.ts`.

### Guest rate limiting

- Client tracks guest audit count in `sessionStorage` for UX (optimistic block).
- Backend is the source of truth: `POST /audits` returns `429` with `X-RateLimit-Remaining: 0` header when exceeded. Client handles 429 by showing the guest-limit card.
- Rate limit key: IP + user-agent hash, 3 per 24h window. Configurable.

### Inline signup card

- New component `<guest-signup-card>` rendered inside results view.
- State:
  ```ts
  interface GuestSignupState {
    dismissedAt?: number;   // ms timestamp
    trigger?: 'time' | 'scroll' | 'action';
  }
  ```
- Displayed when `!authenticated && !dismissedAt` and any trigger fires.
- Auth methods delegated to Auth0 Universal Login with `prompt=signup` for the sign-up button and default prompt for log-in.

### PDF export — preview mode

- `report.service.ts#exportPageReportAsPdf(...)` gains a `preview: boolean` parameter.
- In preview mode, the returned PDF blob is rendered inline via `<iframe>` or PDF.js with a CSS overlay watermark (not in the file; in the viewer only — keeps the real file unmodified).
- Export button label for guests: *"Preview PDF"*. Tooltip: *"Sign up to download."*

### Rate limit UX on 4th audit

- On `429`, don't open a modal. Render an inline card in-place of the "Audit started" success card, titled *"You've used today's audits."*

## Acceptance criteria

**URL input:**
- [ ] `acme.co`, `https://acme.io`, `http://acme.dev`, `www.acme.me` all accepted.
- [ ] `foo`, `foo.`, `localhost`, `127.0.0.1` rejected with specific messages.
- [ ] Validation fires on blur, not on each keystroke.
- [ ] Scheme auto-prepended when missing.

**Landing page:**
- [ ] Above-the-fold includes headline, sub, input, CTA, trust row.
- [ ] Below-the-fold: live sample, 3-step how-it-works, what-we-check, minimal footer.
- [ ] Footer copyright reads current year.
- [ ] No reference to `©2021`.

**Guest flow:**
- [ ] Guest can run up to 3 audits per rolling 24h window.
- [ ] Every guest audit generates a `/r/:token` link (30-day expiry).
- [ ] Export PDF as guest opens in-browser preview with watermark; does not trigger signup modal.
- [ ] Inline signup card appears at the earliest of: 15s dwell, scroll past 3rd issue, action click.
- [ ] Card is dismissable; dismissal persists for 24h in localStorage.

**Copy:**
- [ ] All 12 strings in §6 replaced verbatim.
- [ ] No "Unfortunately," "Oops," or "!!!!"' remain in user-facing templates (grep check).
- [ ] Mobile block modal includes email capture input and success state.

**A11y:**
- [ ] URL input has visible label (visually-hidden is acceptable); error announced via `aria-describedby`.
- [ ] Inline signup card is a `role="region" aria-labelledby="..."` with focusable dismiss button.
- [ ] Landing page `<h1>` is the hero; proper heading hierarchy downstream.

## Metrics

- `Landing: hero-viewed`
- `Landing: url-submitted` — top-of-funnel measure
- `Landing: url-normalized` (properties: `scheme_added`, `reason`) — measures how often we rescue a URL
- `Landing: url-rejected` (properties: `reason`) — should go near zero after launch
- `Landing: sample-issue-expanded` — engagement with the "See a sample issue" affordance
- `Guest: audit-completed`
- `Guest: pdf-preview-opened`
- `Guest: signup-card-shown` (properties: `trigger`)
- `Guest: signup-card-dismissed`
- `Guest: signup-completed` (properties: `from: guest-card|export|save|limit`)
- `Guest: daily-limit-reached`

**Success metrics:**
- Landing-to-audit-submitted conversion ≥ 35% (baseline TBD).
- `url-rejected` events drop to < 2% of submissions (baseline: ~8% per the current regex).
- Guest → signup conversion within 10 minutes of first audit ≥ 25% (baseline TBD).
- Reduction in support tickets about "URL not accepted" to zero.

## Risks & open questions

1. **Guest rate limit gaming.** IP+UA hashing can be bypassed with rotating proxies. Accept some leakage; monitor for abuse; add CAPTCHA only if traffic justifies.
2. **Social proof accuracy.** "Trusted by teams at" requires permission to use customer logos. If none are available, substitute with the "X sites audited this month" count — ensure the number is real, not fabricated.
3. **Sample audit data.** The live sample on the landing needs a canned, cached result that doesn't re-run every page view. Maintain a manually-curated set of "showcase audits" in the backend.
4. **PDF preview rendering.** Browsers handle PDF inline rendering differently. Consider a server-generated HTML-preview fallback.
5. **Signup card frequency.** 24h dismissal may be too aggressive or too soft — A/B test after launch.
6. **Legal review of rate limit copy.** "3 free audits" is a commitment; check with product/legal that it matches plans in Stripe config.

## Phasing

**Milestone 1 (smallest, highest-leverage):**
- URL validation fix.
- Copy rewrites (all 12 strings).
- Footer year update.
- **These three changes can ship in a single day and materially improve conversion.**

**Milestone 2:**
- Inline signup card (replaces hard-wall modal).
- Guest per-session limit increased to 3, with polite limit card.
- PDF preview mode.

**Milestone 3:**
- Landing page rebuild (above and below the fold).
- Live sample audit component.
- Trust row with real logos or live counter.

**Milestone 4:**
- Mobile-block replacement with email capture.
- Localization groundwork (extract copy to i18n files).
