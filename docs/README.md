# `/docs` — the Look-see website

A hand-written static site that introduces the project to strangers.
Served from this directory by **GitHub Pages**, no build step required.

| File | What it is |
|---|---|
| `index.html` | The entire site. Semantic, keyboard-navigable, ~27 KB. |
| `styles.css` | A single stylesheet. CSS custom properties, light + dark themes. |
| `script.js`  | Theme toggle, scroll reveal, SVG-node focusability. ~2.5 KB. |
| `.nojekyll`  | Tells GitHub Pages to skip Jekyll and serve files as-is. |

Everything is static. No build, no bundler, no framework, no trackers.

## Previewing locally

```bash
cd docs
python3 -m http.server 8000
# then open http://localhost:8000/
```

Or any other static server: `npx serve`, `caddy file-server`, `basic-http-server`, etc.

## Enabling GitHub Pages

1. Push this branch to `origin`.
2. Open the repository's **Settings → Pages**.
3. Under **Build and deployment**, set *Source* to **Deploy from a branch**.
4. Pick branch **`main`** and folder **`/docs`**. Click **Save**.
5. Wait ~30 seconds; the site will be available at
   `https://<user-or-org>.github.io/Accessibility-audit-platform/`.

To use a custom domain:

1. Create a file `docs/CNAME` containing the bare hostname (no protocol), e.g.
   `looksee.dev`.
2. Point a `CNAME` DNS record at `<user>.github.io`.
3. In **Settings → Pages**, check **Enforce HTTPS** once the certificate is issued.

## Updating content

The site is one HTML file. Edits are straightforward:

- **Copy changes** — edit `index.html` directly. Each chapter is marked by
  a `<!-- 0N -->` comment.
- **Typography or colour** — all design tokens live at the top of
  `styles.css` under `:root` (light) and `html[data-theme="night"]` (dark).
- **Stats at the top of Chapter 01** — `section#what .stats dl`. Numbers are
  hand-written so they stay honest; update them as the platform grows.
- **Architecture diagram** — inline SVG in Chapter 04. Each service is a
  `<g class="node">` with a `<rect>` and two `<text>` elements. Pub/Sub topics
  are `<g class="topic">` groups with dashed strokes.
- **In-progress list** — Chapter 05 is drawn from the commit log. When a wave
  lands or a new module opens, add a `<li class="timeline__item">`.

## Accessibility commitments

This being the website of an accessibility platform, a few things are
non-negotiable:

- **Contrast** meets WCAG 2.1 AA for every text element and AAA for body copy.
  If you add a new colour, check it against the paper and night themes before
  committing.
- **Keyboard** — every interactive element (links, theme toggle, architecture
  nodes) is reachable with Tab and has a visible focus state.
- **Reduced motion** — the scroll-reveal and any transition respect
  `prefers-reduced-motion: reduce`.
- **Semantics** — headings descend cleanly (h1 → h2 → h3 → h4). Landmarks
  (`header`, `main`, `footer`, `nav`, `aside`, `section[aria-labelledby]`)
  are real, not cosmetic.
- **No required JavaScript for content** — the site is fully readable with JS
  disabled. `script.js` adds niceties, not content.

If any of this slips, please open an issue — this page is the product's most
visible demo.

## Dependencies

- Fonts are pulled from Google Fonts at runtime: **Fraunces** (display),
  **Instrument Sans** (text), **JetBrains Mono** (code). System fallbacks are
  declared if Google Fonts is blocked.
- No other third-party scripts, analytics, or CDNs.

## License

Same as the root repository.
