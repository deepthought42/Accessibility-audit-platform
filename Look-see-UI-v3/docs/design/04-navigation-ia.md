---
id: 04
title: Navigation & information architecture
status: Draft
phase: C
addresses:
  - Orphaned routes (`/dashboard`)
  - Two divergent review routes (`/audit/:id/page` and `/audit/:id/review`)
  - `Domains` nomenclature confuses non-expert users
  - No `/home` landing; users dump into a flat audit list
  - No breadcrumbs / location awareness in nested views
  - `/how-it-works` and `/integrations` compete with workspace nav
  - No site-level concept grouping related page audits
owner: Design + Frontend
related:
  - UX_REDESIGN.md §4, §6.1
  - 02-audit-status-and-progress.md
  - 03-results-architecture.md
---

# 04 — Navigation & information architecture

## Problem

Today's route table has 11 defined routes; 3 of them are orphaned or redundant:

- `/dashboard` — defined but no nav link points to it.
- `/audit/:id/page` vs `/audit/:id/review` — two paths to the same screen with subtly different guards. The `review` path is used as a "shareable" route but has no share-token gating.
- `/integration` (singular) — listed in the router; the nav link says "Integrations" (plural). Leads to the same grayed-out "coming soon" page.

Navigation problems compound:

- The sidebar mixes **workspace** pages (Audits) with **marketing** pages (How it works) and **coming-soon** pages (Integrations) as peers.
- There is no **home** page. On login, users land on `/audit` which is a flat list of past audits — useful when you have history, useless on day 1.
- There are no **breadcrumbs** on nested pages. A user on a page audit result has no visible path back to a domain/site.
- **Domain** is the model name exposed in the UI. Non-technical users call this a **site**.
- No concept of a **project** or **team workspace** — all audits are peers. If an agency audits 10 client sites, they all mingle in one list.

## Goals

1. Collapse duplicate routes; enforce one canonical URL per destination.
2. Introduce `/home` as the post-login default.
3. Rename `Domain` → `Site` in all user-facing copy. Keep backend model names unchanged.
4. Add breadcrumbs to every nested view.
5. Restructure sidebar into **workspace** vs **learn** sections.
6. Retire `/integrations` route until at least one integration ships (or soft-hide behind a "beta" section).
7. Introduce a shareable public route `/r/:token` for external viewing.

## Non-goals

- Multi-team workspaces / org support (Phase D).
- Migrating URLs of live shared audit links — we'll honor old URLs with redirects for one year.
- Changing the backend model names.

## User stories

- *As Morgan (returning)*, I log in and immediately see what changed since my last visit — not a flat list of every audit I've ever run.
- *As Morgan (first visit)*, I go to `/home` after signup and see a clear CTA to run my first audit.
- *As an engineer sharing a result*, I paste a link in Slack and my teammate (no account) reads the report without friction.
- *As Priya*, I navigate from a page audit back up to its parent site with one click.

## Design

### 1. Route map (proposed)

**Public (no auth):**

| Path | Component | Notes |
|---|---|---|
| `/` | LandingComponent | The marketing + try-it-now page |
| `/pricing` | PricingComponent | New; billing plans |
| `/how-it-works` | HowItWorksComponent | Retained for marketing SEO |
| `/r/:shareToken` | SharedAuditViewComponent | New; gated by token; renders read-only results, no nav chrome |
| `/login` | LoginComponent | Auth0 entry |
| `/logout` | (handled by Auth0) | |
| `/404` | NotFoundComponent | Friendly 404 with links back |

**Authenticated (app shell):**

| Path | Component | Notes |
|---|---|---|
| `/home` | HomeComponent | Default after login; activity + quick audit |
| `/sites` | SiteListComponent | Renamed from `Domains` |
| `/sites/:id` | SiteDetailComponent | Aggregate scores + page list |
| `/sites/:id/settings` | SiteSettingsComponent | Per-site options |
| `/audits` | AuditListComponent | Full audit history |
| `/audits/:id` | AuditResultsComponent | Unified single-page & domain results (see spec 03) |
| `/settings` | SettingsComponent | Tabbed: Account / Billing / Team / Notifications / API |
| `/help` | HelpComponent | In-app help (merged with `/how-it-works`) |

**Retired:**

| Old path | Redirects to | Rationale |
|---|---|---|
| `/dashboard` | `/home` | Orphan |
| `/audit` | `/audits` | Singular → plural |
| `/audit/:id/page` | `/audits/:id` | Duplicate of `/audit/:id/review` |
| `/audit/:id/review` | `/audits/:id` | Unified |
| `/audit/:id/domain` | `/audits/:id` | Unified; component adapts to audit type |
| `/domains` | `/sites` | Rename |
| `/integration` (singular) | `/integrations` | Fix typo |
| `/integrations` | (hidden until live) | See §6 |
| `/account` | `/settings` | Renamed |

Redirects are **permanent (301)** via Angular router `redirectTo` with `pathMatch: 'full'`. Any inbound link from email, Slack, PDF exports, or external blogs keeps working.

### 2. Sidebar structure

```
┌──────────────────────────────┐
│  ◆ Look-see                  │
│                              │
│  ┌────────────────────────┐  │
│  │ + New audit            │  │  ← always-visible primary action
│  └────────────────────────┘  │
│                              │
│  WORKSPACE                   │  ← section label (overline type)
│  ⌂  Home                     │
│  ◈  Sites          [ 3 ]     │  ← badge: count of sites with new issues
│  ≡  Audits                   │
│                              │
│  LEARN                       │
│  ?  Help                     │
│  ⇄  Integrations    beta     │  ← "beta" tag; present only when >=1 integration
│                              │
│  ─────────────────────────   │
│  [ MB ] Morgan B.       ⌃   │  ← user menu (popover)
└──────────────────────────────┘
```

**Interaction rules:**

- Active route is marked with: brand color icon + brand color text + 3px left border. Not just text color (current pattern fails AA).
- Badges show urgency (new failed audits, sites with score drops). Surfaced by backend polling or WS.
- The user menu popover has: avatar + name + email, links to Settings, Log out.
- Sidebar can collapse to a 56px icon rail via a toggle at the bottom — state persists in localStorage.
- At `< 1024px`, sidebar auto-collapses; below `< 768px`, it becomes a hamburger-triggered drawer.

### 3. Breadcrumbs

Every nested authenticated route renders a breadcrumb row above the page title.

Examples:

```
/audits/abc123                  →  Audits  /  yoursite.com/pricing
/sites/xyz/settings             →  Sites  /  acme.com  /  Settings
/sites/xyz                      →  Sites  /  acme.com
/settings                       →  Settings
/home                           →  (no breadcrumb — root)
```

**Rules:**
- Each segment except the current is a link.
- Separator is ` / ` with 1-char padding on each side, rendered in muted text.
- The trailing segment matches the page's `<h1>` where possible, for redundancy.
- Breadcrumb row also hosts page-level actions (Re-run, Settings, etc.) on the right — converges with current "audit-header" pattern.

### 4. `/home` design

```
┌──────────────────────────────────────────────────────────────────────┐
│                                                                      │
│   Good morning, Morgan.                                              │
│                                                                      │
│   ┌────────────────────────────────────────────────────────────┐     │
│   │  ⚡  Run a new audit                                       │     │
│   │                                                            │     │
│   │  [ https://…                              ] ( Audit )      │     │
│   │                                                            │     │
│   │  Audit type: ◉ Single page  ○ Whole site                   │     │
│   └────────────────────────────────────────────────────────────┘     │
│                                                                      │
│   Your sites                               [ + Add site ]  See all → │
│   ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                 │
│   │ acme.com     │ │ shop.acme    │ │ docs.acme    │                 │
│   │  82 / 100  ↑3│ │  67 / 100 ↓2 │ │  91 / 100  — │                 │
│   │ 3 new issues │ │ 11 new issues│ │ all clear    │                 │
│   └──────────────┘ └──────────────┘ └──────────────┘                 │
│                                                                      │
│   Recent audits                                         See all →    │
│   ─────────────────────────────────────────────────────────────      │
│   ● acme.com/pricing           72 / 100   2 hours ago        ⋯       │
│   ● shop.acme.com/checkout     54 / 100   yesterday          ⋯       │
│   ● acme.com/                  82 / 100   3 days ago         ⋯       │
│                                                                      │
│   ● = status chip, colored                                           │
└──────────────────────────────────────────────────────────────────────┘
```

**First-time home state:** when a user has zero sites and zero audits, the page simplifies to the quick-audit card + an empty-state illustration: *"Run your first audit to see your results here."*

**Data sources:**
- `GET /me/sites` — sites with aggregate score and delta.
- `GET /audits?limit=5&sort=recent` — recent audits (any status).

### 5. `/sites` — renamed and productized

Current `/domains` has a placeholder "unauthenticated info cards" view and a commented-out authenticated view. Proposed:

- Grid of **SiteCard** components (see 06-shared-components.md).
- Each card: favicon, site name, current health score, 30-day sparkline, last audit timestamp, "Run audit" action.
- "Add site" is a card-shaped primary action at the end of the grid.
- Clicking a card routes to `/sites/:id`.

`/sites/:id` layout was specified in UX_REDESIGN.md §7.1 — retained unchanged.

### 6. Integrations — hide until live

Current `/integrations` is a graveyard of "coming soon" cards. Proposed:

1. **Remove the top-nav link until at least one integration ships.** Users have no reason to go there.
2. When an integration ships (e.g., Slack notifications), re-introduce the nav link with a small **beta** badge until it's out of beta.
3. The page design retains the card grid but adds real status:

| Status | Card treatment |
|---|---|
| **Available** | Full color, "Connect" button |
| **Beta** | Full color + "Beta" chip, "Connect" button |
| **Waitlist** | 60% opacity, "Notify me" button with email capture |

Never show a card with no working action.

### 7. Public share route

`/r/:shareToken` renders a **view-only**, no-nav-chrome version of the audit results. Intended for paste-in-Slack, paste-in-email, view-on-LinkedIn use.

- Token issued by `POST /audits/:id/share` (authenticated users).
- Token includes: audit id, expiry timestamp (default 30 days for free, 1 year for paid, 30 days for guests), scope (view).
- Revocation: `DELETE /share/:token`.
- Rendering: uses the same `<audit-results-shell>` component but hides the sidebar, sets a `readonly` input that suppresses actions (Mark done, Export without signup, Settings).
- Fallback: if token is expired, show a *"This report link has expired"* page with a CTA to sign up and run a fresh audit.

## Technical design

### Router config

```ts
// app-routing.module.ts (new structure, abbreviated)
const routes: Routes = [
  // Public
  { path: '', component: LandingComponent, data: { public: true } },
  { path: 'pricing', component: PricingComponent, data: { public: true } },
  { path: 'how-it-works', component: HowItWorksComponent, data: { public: true } },
  { path: 'r/:shareToken', component: SharedAuditViewComponent, data: { public: true, noChrome: true } },

  // Authenticated (inside AppShellComponent)
  {
    path: '',
    component: AppShellComponent,
    canActivate: [AuthGuard],
    children: [
      { path: 'home', component: HomeComponent },
      { path: 'sites', component: SiteListComponent },
      { path: 'sites/:id', component: SiteDetailComponent },
      { path: 'sites/:id/settings', component: SiteSettingsComponent },
      { path: 'audits', component: AuditListComponent },
      { path: 'audits/:id', component: AuditResultsComponent },
      { path: 'settings', component: SettingsComponent },
      { path: 'help', component: HelpComponent },
      { path: '', redirectTo: 'home', pathMatch: 'full' },
    ],
  },

  // Redirects
  { path: 'dashboard', redirectTo: 'home', pathMatch: 'full' },
  { path: 'audit', redirectTo: 'audits', pathMatch: 'full' },
  { path: 'audit/:id/page', redirectTo: 'audits/:id', pathMatch: 'full' },
  { path: 'audit/:id/review', redirectTo: 'audits/:id', pathMatch: 'full' },
  { path: 'audit/:id/domain', redirectTo: 'audits/:id', pathMatch: 'full' },
  { path: 'domains', redirectTo: 'sites', pathMatch: 'full' },
  { path: 'integration', redirectTo: 'integrations', pathMatch: 'full' },
  { path: 'account', redirectTo: 'settings', pathMatch: 'full' },

  // 404
  { path: '**', component: NotFoundComponent },
];
```

### Breadcrumb service

```ts
@Injectable({ providedIn: 'root' })
export class BreadcrumbService {
  crumbs$ = new BehaviorSubject<Breadcrumb[]>([]);
  set(crumbs: Breadcrumb[]) { this.crumbs$.next(crumbs); }
  clear() { this.crumbs$.next([]); }
}

// Each route component sets crumbs in ngOnInit, e.g.:
ngOnInit() {
  this.breadcrumbs.set([
    { label: 'Sites', href: '/sites' },
    { label: this.site.name, href: `/sites/${this.site.id}` },
    { label: 'Settings' },  // no href = current
  ]);
}
```

`<looksee-breadcrumb-bar>` reads the service and renders.

### Copy change: `Domain` → `Site`

- **User-facing labels:** 100% of occurrences of "Domain" in templates, buttons, table headers, help text → "Site."
- **Data model class names:** unchanged (`Domain`, `DomainSettings`, etc.). Localized in model files; UI maps label on render.
- **Analytics event names:** keep historical event names for continuity; add new events alongside as needed.
- **URLs:** the user-facing path is `/sites`. API calls to `/domains` remain until backend rename (out of scope).

### App shell

Introduce an `AppShellComponent` that owns: sidebar, breadcrumb bar, toast host, mobile-drawer state. Child routes render inside a `<router-outlet>` within its main content area.

Public routes (landing, pricing, help, shared audit) do **not** use the app shell — they have their own layout with a marketing nav bar or no chrome.

## Acceptance criteria

- [ ] All orphan routes redirected via `redirectTo`.
- [ ] Zero references to `/audit/:id/page` or `/audit/:id/review` in templates or code (except the router redirect entries).
- [ ] Every user-facing instance of "Domain" replaced with "Site" (verified by grep of template files).
- [ ] `/home` renders correctly for zero-state and populated-state users.
- [ ] Sidebar is divided into Workspace / Learn sections with correct active-state styling.
- [ ] Breadcrumbs render on every nested route; root route (`/home`) has no breadcrumb.
- [ ] `/r/:shareToken` renders results without sidebar or authenticated-only actions.
- [ ] Old share links (via expired routes like `/audit/:id/review`) redirect to new equivalent.
- [ ] 404 page exists and renders friendly message + links.
- [ ] A11y: keyboard-navigable skip link jumps past sidebar to main content; tab order logical.

## Metrics

- `Nav: sidebar-item-clicked` (properties: `item`) — distribution shows which nav items matter.
- `Nav: breadcrumb-clicked` (properties: `depth`) — verifies breadcrumbs are used.
- `Nav: new-audit-button-clicked` — the always-visible CTA in the sidebar; baseline for later optimization.
- `Home: quick-audit-submitted` — proves `/home` is a useful primary surface.
- `Share: token-created`, `Share: token-viewed` — measures shareable-link adoption.

**Success metrics:**
- `/home` returning-user engagement (≥ 1 action) > 80% of sessions.
- Reduction in 404s from external links (tracking pre/post redirect rollout).
- Domain → Site rename causes zero ticket volume (copy change is invisible).

## Risks & open questions

1. **SEO impact from route changes.** `/domains` and `/how-it-works` may have inbound links. Use `301 Moved Permanently` redirects (not 302) and monitor Search Console.
2. **Shared link token architecture.** New endpoint + table needed server-side. Coordinate with backend team; token design should prevent enumeration (UUIDv4 + HMAC).
3. **Breadcrumb fragility.** Components setting breadcrumbs in `ngOnInit` can race with async data loads. Recommend each component pushes two updates: initial (with placeholders like "Loading…") and final (with resolved names).
4. **Rename scope.** "Domain" also appears in settings screens, emails, exports, and marketing pages. Need a comprehensive audit; some marketing copy may deliberately use "domain" for SEO — product decision.
5. **Sidebar collapse state.** Persists to localStorage — but if user has multiple devices, their preference doesn't roam. Minor issue; defer.
6. **Integrations retirement.** Do we keep `/integrations` route accessible (not linked from nav) for users bookmarking, or hard-redirect to `/home`? Recommend: redirect for now; restore when ready.

## Phasing

**Milestone 1 (Phase A pairing):**
- Redirects in place; old links keep working.
- Breadcrumb service + component shipped, used on 2–3 existing pages.

**Milestone 2:**
- Sidebar rebuilt with Workspace/Learn structure.
- Domain → Site rename swept through templates.
- `/home` component built and routed as default.

**Milestone 3:**
- `/sites` and `/sites/:id` rebuild (paired with SiteCard in spec 06).
- `/settings` tabbed view.

**Milestone 4:**
- `/r/:shareToken` public share route.
- Integrations nav re-added when first integration ships.
