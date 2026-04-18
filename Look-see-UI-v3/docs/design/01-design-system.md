---
id: 01
title: Design system & tokens
status: Draft
phase: A
addresses:
  - Hardcoded colors scattered across components
  - No responsive breakpoint coverage for tablets
  - Typography hierarchy loose; button styles inconsistent
  - No token inheritance (no Tailwind component classes)
  - No dark mode foundation
owner: Design + Frontend
related:
  - UX_REDESIGN.md §3, §8
  - REDESIGN_PLAN.md §1
---

# 01 — Design system & tokens

## Problem

The codebase has partial design tokens but no single source of truth:

- `tailwind.config.js` defines some brand colors (`brand.500 = #FF0050`, charcoal palette, shadow tokens) but components still hardcode values like `#231f20`, `#FF0050`, `#23D8A4`, `#F9BF08`, `#4f4b4c` in inline styles and SCSS files.
- `styles.scss` has 30+ custom viewport-height classes (`h-5vh` through `h-100vh`) duplicating Tailwind functionality.
- Semantic score colors (Good/Warning/Critical) are re-declared in multiple components.
- Typography: `Open Sans` and `Cera Pro` both loaded; weights applied inconsistently; no defined scale.
- Button styles inconsistent: some use `.btn-accent`, others inline `px-8 py-3` with hand-picked colors.
- Responsive: mobile (`< 640px`) and desktop (`≥ 640px`) are handled; tablet range (640–1024px) has gaps (e.g., results grid jumps from 1 to 4 columns).
- Angular Material theme is Deep Purple/Amber — conflicts with brand.
- No dark mode tokens; `dark:` classes appear in nav markup but there is no dark theme implementation.

**Impact:** every new component is an opportunity to drift further. Brand consistency erodes linearly with feature count. Designers cannot hand off because there's no canonical token name to reference.

## Goals

1. Establish **one source of truth** for tokens (colors, type, spacing, radius, shadow, motion) in `tailwind.config.js` + a thin SCSS layer for semantic aliases.
2. Define a **semantic color layer** above raw brand colors (e.g., `score.good`, `score.warning`, `score.critical`, `surface.base`, `surface.raised`, `border.subtle`).
3. Define a **typographic scale** with named roles (`display-xl`, `display`, `heading`, `subheading`, `body`, `body-sm`, `caption`, `overline`, `mono`).
4. Standardize **button variants** as a single component with well-defined variants and sizes.
5. Fill **tablet breakpoints** so layouts degrade through `sm` / `md` / `lg` / `xl` cleanly.
6. Lay **dark mode groundwork** — every token has a light and dark value, even if dark mode stays off by default.
7. **Replace the Material Deep Purple/Amber theme** with a Look-see theme derived from the same tokens.

## Non-goals

- Shipping dark mode to users (toggle is deferred until Phase D).
- Rewriting every component in one sprint — this spec defines the tokens and the button component; other components adopt over time.
- Changing the base framework (still Angular Material + Tailwind).

## User stories

- *As a designer*, I want to reference `surface.raised` in Figma and have it map to the same value the front-end developer uses, so handoffs don't lose fidelity.
- *As a front-end dev*, I want to write `<button class="btn btn-primary btn-md">` (or an Angular component) and never hand-pick colors again.
- *As Morgan (non-expert)*, I want a consistent visual language so I trust the product — subtle UI drift reads as sloppy.

## Design

### 1. Color tokens

**Raw palette** (define in `tailwind.config.js` → `theme.extend.colors`):

```js
// Brand
brand: {
  50: '#fff1f4',
  100: '#ffe1e9',
  200: '#ffb8cb',
  300: '#ff7da0',
  400: '#ff4775',
  500: '#ff0050',  // primary accent
  600: '#d40043',
  700: '#a80035',
  800: '#7a0027',
  900: '#4d0018',
  950: '#2b000d',
},

// Ink / charcoal (replaces misc hex greys)
ink: {
  50: '#f7f7f7',
  100: '#ebeaea',
  200: '#d1cfd0',
  300: '#aeabac',
  400: '#7f7c7d',
  500: '#5f5d5e',
  600: '#4f4b4c',
  700: '#3d3b3c',
  800: '#2d2a2b',
  900: '#231f20',   // primary dark
  950: '#151313',
},

// Score (semantic, mapped from named greens/ambers/reds)
score: {
  good:     '#10b981',   // emerald-500
  warning:  '#f59e0b',   // amber-500
  critical: '#ef4444',   // red-500
},
```

**Semantic aliases** (define as custom properties in `styles.scss` so dark mode can flip them in one place):

```scss
:root {
  // Surfaces
  --surface-base:    theme('colors.white');
  --surface-raised:  theme('colors.white');
  --surface-sunken:  theme('colors.ink.50');
  --surface-inverse: theme('colors.ink.900');

  // Text
  --text-primary:    theme('colors.ink.900');
  --text-secondary:  theme('colors.ink.600');
  --text-muted:      theme('colors.ink.400');
  --text-inverse:    theme('colors.white');
  --text-accent:     theme('colors.brand.600');   // AA-compliant on white
  --text-on-accent:  theme('colors.white');

  // Borders
  --border-subtle:   theme('colors.ink.100');
  --border-default:  theme('colors.ink.200');
  --border-strong:   theme('colors.ink.300');
  --border-focus:    theme('colors.brand.500');

  // Score
  --score-good:      theme('colors.score.good');
  --score-warning:   theme('colors.score.warning');
  --score-critical:  theme('colors.score.critical');

  // Focus ring
  --focus-ring: 0 0 0 3px rgba(255, 0, 80, 0.35);
}

[data-theme='dark'] {
  --surface-base:    theme('colors.ink.950');
  --surface-raised:  theme('colors.ink.900');
  --surface-sunken:  theme('colors.ink.950');
  --surface-inverse: theme('colors.white');

  --text-primary:    theme('colors.ink.50');
  --text-secondary:  theme('colors.ink.300');
  --text-muted:      theme('colors.ink.400');
  --text-accent:     theme('colors.brand.400');   // brighter for dark
  // …
}
```

**Rule:** components never reference raw palette tokens. They reference semantic tokens. Raw palette tokens are only used in the token layer itself.

**Contrast verification:** every semantic text-on-surface pair is validated for AA (4.5:1 for body, 3:1 for large text). `brand.500` (`#FF0050`) on white is 3.49:1 — fails AA for body text. `brand.600` (`#D40043`) is 4.56:1 — passes. **Therefore:** the brand pink used for text switches to `brand.600`; `brand.500` is reserved for large text, icons, and decorative accents only.

### 2. Typography scale

Consolidate on **Inter** for body (replaces Open Sans — better screen rendering, already fallback in the stack) and **Cera Pro** for display. Keep Cera Pro loaded only for display sizes to reduce payload.

| Role | Size | Line-height | Weight | Tracking | Use |
|---|---|---|---|---|---|
| `display-xl` | 48px | 1.1 | 700 (Cera Pro) | -0.02em | Landing hero only |
| `display` | 36px | 1.15 | 700 (Cera Pro) | -0.015em | Page heroes |
| `heading-lg` | 28px | 1.25 | 700 | -0.01em | Section h1 |
| `heading` | 22px | 1.3 | 600 | -0.005em | Card titles, h2 |
| `subheading` | 18px | 1.4 | 600 | 0 | h3 |
| `body-lg` | 17px | 1.55 | 400 | 0 | Large body |
| `body` | 15px | 1.55 | 400 | 0 | Default body |
| `body-sm` | 13px | 1.5 | 400 | 0 | Dense tables, helper text |
| `caption` | 12px | 1.4 | 500 | 0.01em | Labels below inputs |
| `overline` | 11px | 1.2 | 600 | 0.08em (uppercase) | Section eyebrows |
| `mono` | 13px | 1.5 | 400 | 0 | Code, selectors |

Implement as Tailwind plugin utilities:

```js
// tailwind.config.js (fragment)
plugins: [
  function({ addUtilities }) {
    addUtilities({
      '.text-display-xl': { 'font': '700 48px/1.1 "Cera Pro", Inter, sans-serif', 'letter-spacing': '-0.02em' },
      '.text-display':    { 'font': '700 36px/1.15 "Cera Pro", Inter, sans-serif', 'letter-spacing': '-0.015em' },
      '.text-heading-lg': { 'font': '700 28px/1.25 Inter, sans-serif', 'letter-spacing': '-0.01em' },
      // …
    });
  },
]
```

Base layer resets for `h1–h6` map to these utilities so plain markdown-ish HTML looks right out of the box.

### 3. Spacing, radius, shadow, motion

**Spacing scale:** adopt the Tailwind 4-px grid verbatim. Forbid arbitrary spacing (`pl-6` is fine; `pl-[23px]` is not). Add a lint rule.

**Radius tokens:**
```js
borderRadius: {
  'xs': '2px',
  'sm': '4px',
  'md': '6px',
  'lg': '8px',
  'xl': '12px',
  '2xl': '16px',
  'full': '9999px',
}
```
Guidance: buttons = `md`, cards = `lg`, modals = `xl`, avatars/chips = `full`.

**Shadow tokens:**
```js
boxShadow: {
  'xs':    '0 1px 2px 0 rgb(0 0 0 / 0.04)',
  'sm':    '0 1px 3px 0 rgb(0 0 0 / 0.06), 0 1px 2px -1px rgb(0 0 0 / 0.04)',
  'md':    '0 4px 6px -1px rgb(0 0 0 / 0.08), 0 2px 4px -2px rgb(0 0 0 / 0.04)',
  'lg':    '0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.06)',
  'xl':    '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.04)',
  'focus': 'var(--focus-ring)',
}
```

**Motion tokens:**
```js
transitionDuration: {
  fast:   '120ms',
  base:   '180ms',
  slow:   '280ms',
}
transitionTimingFunction: {
  'ease-out-soft': 'cubic-bezier(0.22, 1, 0.36, 1)',
  'ease-in-out-soft': 'cubic-bezier(0.65, 0, 0.35, 1)',
}
```

All interactive elements animate on `base` duration with `ease-out-soft`. Honor `prefers-reduced-motion: reduce` globally:

```scss
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

### 4. Responsive breakpoints

Extend Tailwind defaults — no new breakpoints, but **every layout must be tested at all four.**

| Token | Min-width | Device class |
|---|---|---|
| `sm` | 640px | Large phone / small tablet portrait |
| `md` | 768px | Tablet portrait |
| `lg` | 1024px | Tablet landscape / small laptop |
| `xl` | 1280px | Desktop |
| `2xl` | 1536px | Large desktop |

**Rule:** no Tailwind utility jumps >1 breakpoint without passing through `md`. E.g., `grid-cols-1 md:grid-cols-2 xl:grid-cols-4` is legal; `grid-cols-1 xl:grid-cols-4` is a bug.

### 5. Button system

Replace ad-hoc buttons with one Angular component, three variants × three sizes × two emphasis modes.

```html
<looksee-button variant="primary" size="md">Start audit</looksee-button>
<looksee-button variant="secondary" size="sm">Cancel</looksee-button>
<looksee-button variant="ghost" size="md" icon="plus">New site</looksee-button>
<looksee-button variant="danger" size="md">Delete</looksee-button>
<looksee-button variant="primary" size="md" [loading]="true">Saving…</looksee-button>
```

| Variant | Use | Background | Text | Border |
|---|---|---|---|---|
| `primary` | Main CTA — one per screen | `ink.900` | white | — |
| `accent` | Conversion CTA (Start audit, Sign up) | `brand.600` | white | — |
| `secondary` | Secondary actions | `white` | `ink.900` | `ink.200` |
| `ghost` | Low-emphasis inline | transparent | `ink.700` | — (hover: `ink.50` bg) |
| `danger` | Destructive | `white` | `score.critical` | `score.critical` |
| `link` | Inline anchor-like | transparent | `brand.600` | underline on hover |

| Size | Height | Padding-x | Text |
|---|---|---|---|
| `sm` | 32px | 12px | `body-sm` |
| `md` | 40px | 16px | `body` |
| `lg` | 48px | 20px | `body-lg` |

**States:** hover (darkens by one step), active (darkens two steps, subtle press via `translateY(1px)`), disabled (50% opacity, no pointer), focus (`shadow-focus` ring).

**Loading:** spinner replaces icon, label dims to 70% opacity, button becomes non-interactive.

### 6. Angular Material theme override

Material uses SCSS mixins. Define a single theme file that maps Material's `$primary`, `$accent`, `$warn` palettes to our tokens:

```scss
// src/theme/material-overrides.scss
@use '@angular/material' as mat;

$looksee-primary: mat.define-palette((
  50: #f7f7f7, 100: #ebeaea, …, 900: #231f20,
  contrast: (50: #231f20, …, 900: #ffffff)
));
$looksee-accent:  mat.define-palette((…brand palette…));
$looksee-warn:    mat.define-palette((…score.critical palette…));

$looksee-theme: mat.define-light-theme((
  color: (primary: $looksee-primary, accent: $looksee-accent, warn: $looksee-warn),
  typography: mat.define-typography-config($font-family: '"Inter", sans-serif'),
));

@include mat.all-component-themes($looksee-theme);
```

## Technical design

### File layout

```
src/
├── theme/
│   ├── tokens.scss             // CSS custom properties for semantic tokens
│   ├── typography.scss         // @font-face + utility definitions
│   └── material-overrides.scss // Material theme bound to our tokens
├── styles.scss                 // imports the above, minimal else
└── app/components/shared/
    ├── button/                 // looksee-button component
    ├── ...
tailwind.config.js              // raw palette + utility plugins
```

### Migration strategy

1. **Add** the new tokens alongside existing hex values. Don't remove anything yet.
2. **Build** `LookseeButtonComponent` and Material override. Ship.
3. **Migrate** components opportunistically — any component touched for another reason gets migrated.
4. **Lint rule:** `no-hex-in-components` ESLint plugin that errors on hex literals in `.html`, `.ts`, `.scss` under `src/app/components/`. Allow in `src/theme/` and `tailwind.config.js` only.
5. **Remove** old custom vh classes from `styles.scss` once no component references them.
6. **Deprecate** Open Sans (keep load for two releases, then remove).

### Dark mode groundwork

Add `data-theme` attribute on `<html>`, toggled by a service `ThemeService` that persists choice to `localStorage`. Default is `light`. Dark CSS variables are defined but the toggle UI ships in Phase D.

### Analytics

No events added by this spec.

## Acceptance criteria

- [ ] `tailwind.config.js` has `brand`, `ink`, and `score` palettes with 50–950 scales.
- [ ] `src/theme/tokens.scss` defines ≥ 20 semantic CSS custom properties with `[data-theme='dark']` overrides for each.
- [ ] Typography plugin ships 10 named utilities; all `h1`–`h6` base styles map to them.
- [ ] `LookseeButtonComponent` implemented with 6 variants × 3 sizes; passes AA contrast in Storybook or equivalent visual regression.
- [ ] Angular Material primary/accent/warn palettes bound to brand tokens; one end-to-end Material component (e.g., `MatDialog`) reflects the change.
- [ ] `prefers-reduced-motion` global opt-out works (manual QA).
- [ ] ESLint rule `no-hex-in-components` configured and passing (may require legacy exceptions initially).
- [ ] Design token reference page shipped in Storybook or `/docs/design/tokens.html`.

## Metrics

Not directly user-facing. Track as engineering-health KPIs:
- Count of raw hex literals in `src/app/components/**` (baseline → target: 0 except in legacy SVGs).
- Count of inline `style="…"` attributes in templates (baseline → target: ≤ 10% of current).
- Lighthouse accessibility score for every page (target: ≥ 95).

## Risks & open questions

1. **Font licensing.** Confirm we have a production license for Cera Pro on the current domain(s). If not, replace with a similar-feel free alternative (e.g., General Sans, Satoshi) before Phase A ships.
2. **Material version.** Angular Material 17 theming uses `define-theme` (M3) pattern; verify mixin API used here matches the version pinned in `package.json`.
3. **Storybook vs. ad-hoc gallery.** Storybook is the industry default but adds build complexity. A single `/docs/design/tokens.html` page may be enough initially. Product decision.
4. **Dark mode rollout.** Do we ship the toggle in Phase D, or gate behind a feature flag for beta users first? Recommend flag.
5. **Custom vh classes usage audit.** Need to grep `h-5vh`..`h-100vh` to confirm removal is safe before deleting.

## Phasing

**Milestone 1 (week 1):** Tokens + typography + ESLint rule. No UI change visible.
**Milestone 2 (week 2):** Button component + Material theme override. Migrate nav-bar and landing CTAs as reference implementations.
**Milestone 3 (week 3–4):** Opportunistic migration of high-traffic components during Phase B work.
**Milestone 4 (Phase D):** Dark mode toggle.
