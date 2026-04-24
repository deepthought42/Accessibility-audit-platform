# Phase 3c — Close the Internal Remote-Compat Gap in LookseeCore

> **Goal:** Two LookseeCore-internal code paths — `PageStateAdapter`'s `Browser`-taking overloads and `BrowserUtils`'s URL-sanitization helpers — stop reaching through `browser.getDriver()` so they work in both modes. With this change, **every high-value consumer call path is mode-agnostic**, unblocking phase 4a.2's PageBuilder `buildPageState` → `capturePage` migration and every downstream cutover in phase 4.
>
> **Where to work:** locally in the Look-see monorepo against LookseeCore. Suggest a feature branch: `git checkout -b phase-3c/internal-remote-compat`.
>
> **Sibling references:** this doc mirrors [`phase-3-looksee-shim.md`](./phase-3-looksee-shim.md), [`phase-3b-element-handle-ops.md`](./phase-3b-element-handle-ops.md), and [`phase-4-consumer-cutover.md`](./phase-4-consumer-cutover.md) in structure. The scope for 3c was enumerated in phase-3b §14.9 as the inherit list.

## Why this phase exists now

Phase 3 shipped `RemoteBrowser`. Phase 3b (just merged in #38, LookseeCore 0.7.0) wired every element-handle op through it, so every method on `Browser` that takes or returns a `WebElement` now works in remote mode. But phase-3b §14.9 explicitly enumerated a set of **LookseeCore-internal** code paths that were still remote-incompatible because they reach through `browser.getDriver()` for driver-level state (`getPageSource`, `getTitle`, `window().getSize()`, `getCurrentUrl`).

These paths don't break anything today because nothing routes a `RemoteBrowser` through them. But the moment phase 4a.2 migrates PageBuilder's `buildPageState` call site, `PageStateAdapter.toPageState(Browser, long, String)` runs with a `RemoteBrowser` in its `browser` slot — and throws `UnsupportedOperationException("RemoteBrowser has no local WebDriver")` on line 94 at the first `browser.getDriver().getPageSource()` call.

Phase 3c closes the gap for the subset that actually blocks phase 4. The rest of the §14.9 list — `BrowserService` form extraction, the `Table` helper, and the `RemoteWebElement` unsupported methods — is **explicitly out of scope here** and documented as phase-3d work, with clear triggers for when it becomes necessary.

## Scope

| In scope | Out of scope |
|---|---|
| New `Browser.getTitle()` method with a local default and a `RemoteBrowser` override | New `/v1/sessions/{id}/title` endpoint — we derive title from the existing `getSource()` response |
| Migrate `PageStateAdapter.toPageState(Browser, long, String)` — 4 call sites — off `browser.getDriver()` | Adding a third `PageStateAdapter.toPageState` overload |
| Migrate `PageStateAdapter.toPageState(URL, Browser, boolean, int, long)` — 4 call sites — off `browser.getDriver()` | Restructuring `PageStateAdapter`'s class shape |
| Migrate `BrowserUtils` lines 1377 + 1465 — 2 call sites — off `browser.getDriver().getCurrentUrl()` | `BrowserService` form extraction (lines 454, 1953, 3291, 3292, 3401) — phase 3d |
| Extend the existing `BrowserServiceModeForkTest` and `PageStateAdapterTest` suites to cover remote-mode parity | `RemoteWebElement` unsupported methods (click, sendKeys, getText, etc.) — phase 3d |
| Version bump 0.7.0 → 0.7.1 + CHANGELOG | `Table` helper (findElements on parent element) — phase 3d |
| No new Maven dependencies | Any change to `Browser.java` beyond adding `getTitle()` |

If something looks refactor-tempting inside `PageStateAdapter` or `BrowserUtils` beyond the §14.9 subset, resist. Phase 3c is deliberately tight; phase 3d takes the remaining items.

## Locked decisions (from planning)

| Area | Decision |
|---|---|
| `Browser.getTitle()` remote implementation | Jsoup-parse the result of `getSource()` and return `Document.title()`. **No new endpoint, no new facade method, no new dependency.** Slightly wasteful on round-trip count (a caller that then also calls `getSource()` pays twice) — see §14.1. |
| Existing `Browser.getSource()` reused | `browser.getSource()` already exists and is overridden in RemoteBrowser (phase 3). PageStateAdapter uses `browser.getDriver().getPageSource()` instead — one-line migration to `browser.getSource()`. No new Browser method needed for source. |
| Existing `Browser.getViewportSize()` reused | `Browser.getViewportSize()` is a Lombok-generated getter that RemoteBrowser overrides to query the server (phase 3). PageStateAdapter uses `browser.getDriver().manage().window().getSize()` — one-line migration to `browser.getViewportSize()`. No new Browser method needed for size. |
| Existing `Browser.getCurrentUrl()` reused | Added in phase 3b. Both PageStateAdapter (line 86) and BrowserUtils (lines 1377, 1465) migrate to it. No new Browser method needed for URL. |
| Version bump | 0.7.0 → **0.7.1** (patch). 3c is an internal remote-compat fix with one additive `Browser` method; consumers see no API removal, no behavior change in local mode. Same sizing as the 4a.1 0.6.1 patch. |
| Phase 3d split | Everything in phase-3b §14.9 that isn't in this phase's scope — `BrowserService` form extraction, `Table` helper, `RemoteWebElement` unsupported methods — becomes phase 3d. Sequencing: 3d starts only when a consumer actually needs it (currently none do). |

## Prerequisites

```bash
java -version     # 17.x (LookseeCore stays on Java 17)
mvn -v            # 3.9+
git status        # clean working tree on main
```

- Phase 3b code merged (LookseeCore 0.7.0 on `main`) — confirmed.
- No concurrent `Browser.java` changes in flight — check `git log LookseeCore/looksee-browser/src/main/java/com/looksee/browser/Browser.java` before starting.

## Step 0 — Branch + sanity

```bash
cd /path/to/Look-see
git checkout -b phase-3c/internal-remote-compat

cd LookseeCore && mvn -q verify   # 0.7.0 green baseline (155 looksee-core tests, 31 browsing-client tests)
```

Then run the narrowed grep sanity to confirm the §14.9 subset is still exactly as enumerated (nothing drifted in since phase 3b):

```bash
cd /path/to/Look-see

# PageStateAdapter reach-throughs — expect exactly 7 hits (lines 86, 94, 106, 126, 177, 194, 216).
grep -n "browser\\.getDriver()" LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/PageStateAdapter.java

# BrowserUtils reach-throughs — expect 2 hits (lines 1377, 1465).
grep -n "browser\\.getDriver()\\.getCurrentUrl" LookseeCore/looksee-core/src/main/java/com/looksee/utils/BrowserUtils.java
```

If either grep returns more or fewer hits than expected, update §14.9 in the same commit that addresses the divergence.

## Step 1 — Add `Browser.getTitle()`

**File:** `LookseeCore/looksee-browser/src/main/java/com/looksee/browser/Browser.java`

One new method, local default mirrors current `driver.getTitle()` semantics. Inline:

```java
/**
 * Returns the current document title. Local-mode delegates to
 * {@link WebDriver#getTitle()}; {@link com.looksee.services.browser.RemoteBrowser}
 * overrides this to derive the title from the remote {@link #getSource()}
 * response via Jsoup — no new browser-service endpoint required.
 */
public String getTitle() {
    return driver.getTitle();
}
```

Place it in the same "High-level ops routed through the Browser abstraction" section at the bottom of `Browser.java` where `performClick`, `performAction`, and `getCurrentUrl` live (phase 3b additions).

**File:** `LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/RemoteBrowser.java`

Override — parse title from `getSource()` via Jsoup:

```java
@Override
public String getTitle() {
    // No dedicated /title endpoint in the OpenAPI spec; deriving from the
    // source avoids a spec change. A caller that needs both title and source
    // should call getSource() once and Jsoup-parse locally; this method is
    // for the title-only path.
    return org.jsoup.Jsoup.parse(client.getSource(sessionId)).title();
}
```

Jsoup is already a LookseeCore dep (used by `PageStateAdapter` itself) — no new dep.

**Commit:** `feat(core): add Browser.getTitle() with local default + RemoteBrowser override`

## Step 2 — Migrate `PageStateAdapter` Browser-taking overloads

**File:** `LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/PageStateAdapter.java`

Two overloads, seven call-site migrations total. Pattern-for-pattern replacements:

| Line(s) | Before | After |
|---|---|---|
| 86 | `browser.getDriver().getCurrentUrl()` | `browser.getCurrentUrl()` |
| 94, 177 | `browser.getDriver().getPageSource()` | `browser.getSource()` |
| 106, 194 | `browser.getDriver().getTitle()` | `browser.getTitle()` |
| 126, 216 | `browser.getDriver().manage().window().getSize()` | `browser.getViewportSize()` |

No imports to remove (WebDriver / Dimension are still used elsewhere in the file by type declarations). No behavior change in local mode — every `Browser` method that replaces a `driver.X()` call either delegates to `driver.X()` (the new/existing ones) or was overridden to the equivalent remote call in phase 3.

**Commit:** `refactor(core): route PageStateAdapter Browser overloads through Browser methods`

## Step 3 — Migrate `BrowserUtils`

**File:** `LookseeCore/looksee-core/src/main/java/com/looksee/utils/BrowserUtils.java`

Two single-line edits:

- Line 1377: `browser.getDriver().getCurrentUrl()` → `browser.getCurrentUrl()`
- Line 1465: `browser.getDriver().getCurrentUrl()` → `browser.getCurrentUrl()`

Same rationale as Step 2. Local-mode behavior is byte-identical (local `Browser.getCurrentUrl()` body is `return driver.getCurrentUrl()`).

**Commit:** `refactor(core): route BrowserUtils through browser.getCurrentUrl()`

## Step 4 — Tests

### 4.1 `PageStateAdapterRemoteModeTest` — new

`LookseeCore/looksee-core/src/test/java/com/looksee/services/browser/PageStateAdapterRemoteModeTest.java`

Drives `PageStateAdapter.toPageState(Browser, long, String)` and `toPageState(URL, Browser, boolean, int, long)` with a mocked `RemoteBrowser`. Assertions:

1. Neither overload throws `UnsupportedOperationException("RemoteBrowser has no local WebDriver")`.
2. `client.getCurrentUrl` / `client.getSource` / `client.getViewport` (and the Jsoup title derivation via a second `getSource` call, or one call and parse twice depending on implementation efficiency) are invoked.
3. The resulting `PageState` has non-null fields for URL, source, title, viewport size, and screenshot URLs.

### 4.2 `BrowserTest.getTitle_delegatesToDriver` — add to existing test

`LookseeCore/looksee-browser/src/test/java/...` (or wherever `Browser` unit tests live; add one if the suite is empty). Verifies local-mode `getTitle()` returns `driver.getTitle()`.

### 4.3 `RemoteBrowserElementOpsTest.getTitle_parsesFromSource` — add to existing test

Adds a single test case to the phase-3b `RemoteBrowserElementOpsTest`:

```java
@Test
void getTitle_parsesFromSourceViaJsoup() {
    when(client.getSource("session-1")).thenReturn(
        "<!doctype html><html><head><title>Hello World</title></head></html>");
    assertEquals("Hello World", remote.getTitle());
}
```

### 4.4 Regression guarantees

- `PageStateAdapterTest` (if present — verify during execution) passes unchanged.
- Existing `BrowserServiceLocalModeTest`, `BrowserServiceModeForkTest`, `BrowserServiceTest` all pass unchanged.
- Full `mvn clean verify` is green with the same test count as 0.7.0 plus the new 3c tests.

**Commit:** `test(core): cover remote-mode PageStateAdapter + Browser.getTitle`

## Step 5 — Version bump 0.7.0 → 0.7.1

Same pattern as phase 3 / 3b / 4a.1 version bumps:

- `mvn versions:set -DnewVersion=0.7.1 -DgenerateBackupPoms=false -DprocessAllModules=true` from LookseeCore root.
- Manually update `<looksee.version>0.7.0</looksee.version>` in the root pom's `<properties>`.
- Update `LOOKSEE_CORE_VERSION` at repo root to `0.7.1`.
- Bump all 13 consumer service poms (seven `<core.version>`, two `<looksee-core.version>`, four inline `<version>`) to 0.7.1.
- CHANGELOG entry under `## [0.7.1]`:
  - Added: `Browser.getTitle()` with local default + RemoteBrowser override via Jsoup-from-source.
  - Changed: PageStateAdapter + BrowserUtils no longer reach through `browser.getDriver()`. Remote-mode consumers can now call these paths without hitting `UnsupportedOperationException`.
  - Deferred (phase 3d): enumerated in §14 below.

**Commit:** `chore: bump LookseeCore to 0.7.1 + CHANGELOG + consumer pins`

## Step 6 — Verification

1. **Clean build:**
   ```bash
   cd LookseeCore && mvn -q clean verify
   ```
   Expect: `BUILD SUCCESS`, all 11 modules green. Test counts: looksee-core ≥ 158 (was 155 in 0.7.0; +3 for the new remote-mode adapter + getTitle tests), looksee-browsing-client unchanged at 31.

2. **Scope-preservation check:**
   ```bash
   git diff --stat main..HEAD -- LookseeCore/looksee-browser/
   # Expect: only Browser.java (one new getTitle method) + pom.xml parent-version bump.
   ```

3. **Post-3c sweep sanity:**
   ```bash
   grep -n "browser\.getDriver()" LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/PageStateAdapter.java
   grep -n "browser\.getDriver()" LookseeCore/looksee-core/src/main/java/com/looksee/utils/BrowserUtils.java
   ```
   Expect: empty for both. If anything remains, either migrate it in the same PR or document the reason inline.

4. **Optional manual remote-mode verification.** Same script as phase 3b §11 — spin up browser-service in Docker, flip a scratch consumer to `mode=remote`, call `pageStateAdapter.toPageState(browser, ...)` with a `RemoteBrowser`. Confirm `PageState` is populated and the title matches what `<title>` in the page source reports.

## Definition of done

- [ ] `Browser.getTitle()` exists with a local default body; `RemoteBrowser` overrides it via Jsoup-parse of `getSource()`.
- [ ] `PageStateAdapter.toPageState(Browser, long, String)` has zero `browser.getDriver()` calls.
- [ ] `PageStateAdapter.toPageState(URL, Browser, boolean, int, long)` has zero `browser.getDriver()` calls.
- [ ] `BrowserUtils` lines 1377 + 1465 both use `browser.getCurrentUrl()`.
- [ ] `PageStateAdapterRemoteModeTest` exists and passes.
- [ ] Every pre-0.7.1 test passes unchanged (local-mode byte-identical regression).
- [ ] `A11yParent` at 0.7.1 across every pom; all 13 consumer service poms pinned to 0.7.1.
- [ ] PR opened against `main` with title **"Phase 3c: close the internal remote-compat gap"**.

## Push and open PR

```bash
git push -u origin phase-3c/internal-remote-compat
```

PR body links to this doc + phase-3b-element-handle-ops.md §14.9. Explicit note: default mode stays `local`; this PR ships no behavior change for consumers that don't opt in. Flags that phase 4a.2 (PageBuilder `buildPageState` → `capturePage` migration) is unblocked as of this merge.

## 14. Open items flagged for reviewer

1. **`Browser.getTitle()` remote-mode efficiency.** Parsing title from a full-source Jsoup parse is wasteful if a caller then also wants `getSource()` — the source is fetched twice (once for title, once for source). Currently the only caller is `PageStateAdapter.toPageState(Browser, long, String)` at line 194, which does: source-then-title — so they *could* share the parse. The current refactor leaves them separate for simplicity. If profiling during phase-4 cutover shows the duplicate fetch matters, add a `Browser.getTitleAndSource()` tuple-returning method or refactor `PageStateAdapter` to parse title + source once itself. Cheap to revisit.

2. **RemoteBrowser.getTitle() second round-trip.** Each call to `remote.getTitle()` costs one `GET /v1/sessions/{id}/source` — not cheap if an audit calls it repeatedly. `PageStateAdapter` calls it once per adapt; acceptable. If any new caller calls it in a tight loop, cache or restructure.

3. **Phase 3d inherit list.** Everything in phase-3b §14.9 that's **not** in this phase:
   - `BrowserService` line 454 — `browser.getDriver().getCurrentUrl()` inside `enrichElementStates`. Trivially migrates to `browser.getCurrentUrl()` — could ship with 3c if time permits; left out to keep this phase scoped to the PageBuilder-blocker subset.
   - `BrowserService` lines 1953, 3291, 3292, 3401 — form extraction + element-state build via `driver.findElement(s)`. Needs either a new `Browser.findElements(WebElement parent, String xpath)` method + server endpoint, or a full rewrite of those paths to use the existing `RemoteWebElement` surface. Neither is small.
   - `com.looksee.browsing.table.Table` lines 44, 51, 52 — `element.findElements(By.xpath(...))` on a passed-in WebElement. Same constraint as above.
   - `RemoteWebElement` unsupported methods (13 total) — listed verbatim in phase-3b §14.2. Consumer census today has zero callers; ship on demand.

4. **When phase 3d triggers.** Either (a) a consumer call path exercises a `Browser` method that still reaches `getDriver()`, or (b) a phase-4 cutover surfaces a bug that traces back to a §14.9 reach-through. Until one of those happens, 3d stays unscheduled.

5. **Phase 4a.2 can now proceed.** PageBuilder's `buildPageState` → `capturePage` migration was reverted in phase-4a PR #36 because element extraction in a second browser session caused DOM-consistency issues. With phase 3c merged, the old single-browser flow (`getConnection` + `buildPageState` + `getDomElementStates` + `close`) works transparently in remote mode — no DOM split needed. Phase-4 doc §14.2 option (b) is now actionable: one commit in PageBuilder's `AuditController.java` to re-attempt the migration, this time by doing the whole flow against one `RemoteBrowser` session instead of splitting.
