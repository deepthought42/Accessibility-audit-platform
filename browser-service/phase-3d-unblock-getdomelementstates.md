# Phase 3d — Unblock `getDomElementStates` for Remote Mode

> **Goal:** `BrowserService.getDomElementStates(PageState, xpaths, Browser, long)` — the branch PageBuilder calls when audit-record element data is missing — runs to completion against a `RemoteBrowser`. This closes the last blocker for phase 4a.2 (PageBuilder `buildPageState` → `capturePage` migration) and lets the single-session flow finally re-land.
>
> **Where to work:** locally in the Look-see monorepo against LookseeCore. Suggest a feature branch: `git checkout -b phase-3d/unblock-getdomelementstates`.
>
> **Sibling references:** this doc mirrors [`phase-3-looksee-shim.md`](./phase-3-looksee-shim.md), [`phase-3b-element-handle-ops.md`](./phase-3b-element-handle-ops.md), [`phase-3c-internal-remote-compat.md`](./phase-3c-internal-remote-compat.md), [`phase-4-consumer-cutover.md`](./phase-4-consumer-cutover.md) in structure. The scope was enumerated in phase-3b §14.9 as the inherit list and narrowed in phase-3c §14.3 as the phase-3d triggers-on-demand set.

## Why this phase exists now

Phase 3c (LookseeCore 0.7.1, merged in PR #42) migrated `PageStateAdapter` and `BrowserUtils` off `browser.getDriver()`. That made `PageStateAdapter.toPageState(Browser, …)` remote-safe, which means `browser_service.buildPageState(url, browser, …)` (the first call in PageBuilder's audit flow) works end-to-end in remote mode.

**But** the second call in PageBuilder's flow — `browser_service.getDomElementStates(pageState, xpaths, browser, auditRecordId)` — still has two remote-incompatible sites:

| Site | Call | Impact on RemoteBrowser |
|---|---|---|
| `BrowserService.java:454` | `browser.getDriver().getCurrentUrl()` | Throws `UnsupportedOperationException("RemoteBrowser has no local WebDriver")`. |
| `BrowserService.java:471` | `web_element.getTagName()` on a `RemoteWebElement` | Throws with a phase-3c marker — the WebElement surface for `getTagName` was explicitly deferred. |
| `BrowserService.java:547` (`isImageElement` helper) | `web_element.getTagName()` same as above | Same throw. |

Until this phase ships, PageBuilder's 4a.2 re-migration (reverted in PR #36 for DOM-consistency reasons) is still not safe in remote mode — `getDomElementStates` would throw on the first RemoteBrowser audit. Phase 3d unblocks that.

## Scope

| In scope | Out of scope |
|---|---|
| Migrate `BrowserService.java:454` `browser.getDriver().getCurrentUrl()` → `browser.getCurrentUrl()` | Migrate every other `browser.getDriver()` reach-through in `BrowserService` (§14.9 list) — those don't block 4a.2 |
| Derive tag name from the iteration xpath (not from `WebElement.getTagName()`) in the two sites that consume it | Add `tag_name` to the browser-service OpenAPI contract — that's a server-repo change, out of this repo's reach |
| Extend `RemoteWebElement.getTagName()` so it's no longer a flat throw — reads the cached `attributes` map for a `"tag_name"` key if present, otherwise throws with a clearer message pointing at the workaround | Implementing a server-side `tag_name` attribute synthesis — that would ship in browser-service |
| Tests: a new `BrowserServiceGetDomElementStatesRemoteModeTest` + existing remote-mode test coverage update | Full test coverage of the `enrichElementStates` and form-extraction paths — those stay phase 3e / 3f candidates |
| LookseeCore 0.7.1 → **0.7.2** patch bump + 13 consumer pom pins | Any change to local-mode behavior of `BrowserService` / `Browser` / `RemoteBrowser` |

If something looks refactor-tempting inside `BrowserService` beyond lines 454, 471, and 547, resist. Phase 3d is deliberately tight; the rest of §14.9 becomes phase 3e if a consumer ever needs it.

## Locked decisions (from planning)

| Area | Decision |
|---|---|
| Tag name source | **Parse from the iteration xpath.** `getDomElementStates` already has the xpath in its loop variable; the tag is always the last path segment's name. Pure local logic, no new API, mode-agnostic. |
| `RemoteWebElement.getTagName()` treatment | **Pass-through of cached `attributes.get("tag_name")`** when present, otherwise throws `UnsupportedOperationException("phase 3d: server did not include tag_name in attributes; derive from xpath or add browser-service support")`. Current browser-service doesn't send it; keeps the method honest instead of unimplemented, and gives future callers a clear "add it server-side if you need it" signal. |
| Other §14.9 reach-throughs | **Defer to phase 3e (triggered on demand).** enrichElementStates, form extraction, and the `Table` helper each have at least one `driver.findElement(s)` call that would need a new `Browser` method (e.g. `Browser.findElements(WebElement parent, String xpath)`) backed by a new OpenAPI endpoint. No current consumer is blocked by them — PageBuilder's 4a.2 only needs `getDomElementStates`. Enumerate in §14 for phase 3e to inherit. |
| Version | 0.7.1 → **0.7.2** (patch). Same sizing rationale as 3c: internal remote-compat fix, no API removal, no local-mode behavior change. |

## Prerequisites

```bash
java -version     # 17.x
mvn -v            # 3.9+
git status        # clean working tree on main
```

- Phase 3c code merged (LookseeCore 0.7.1 on `main`) — confirmed.
- `getDomElementStates` at line 439 still matches the pre-planning enumeration (3 reach-throughs at lines 454, 471, 547). Re-verify with Step 0 grep.

## Step 0 — Branch + sanity

```bash
cd /path/to/Look-see
git checkout -b phase-3d/unblock-getdomelementstates

cd LookseeCore && mvn -q verify   # 0.7.1 green baseline (161 looksee-core tests, 31 browsing-client tests)
```

Confirm the §14.9 subset this phase scopes is still exactly these three sites:

```bash
# Expect: exactly 3 hits (lines 454, 471, 547 in the current BrowserService.java)
grep -n "browser\\.getDriver()\\.getCurrentUrl\\|web_element\\.getTagName" \
  LookseeCore/looksee-core/src/main/java/com/looksee/services/BrowserService.java | head
```

If the grep returns more or fewer hits than expected, update §14 in the same commit that addresses the divergence.

## Step 1 — Migrate `BrowserService.java:454`

**File:** `LookseeCore/looksee-core/src/main/java/com/looksee/services/BrowserService.java`

Single-line swap at line 454:

```java
// Before:
String host = (new URL(browser.getDriver().getCurrentUrl())).getHost();

// After:
String host = (new URL(browser.getCurrentUrl())).getHost();
```

Same pattern as phase 3c's `BrowserUtils` fix. Local-mode byte-identical; remote mode uses `PageStatus.current_url` via the phase-3b-shipped `RemoteBrowser.getCurrentUrl()`.

**Commit:** `refactor(core): route BrowserService.getDomElementStates host lookup through browser.getCurrentUrl()`

## Step 2 — Replace `web_element.getTagName()` with xpath-derived tag

**File:** `LookseeCore/looksee-core/src/main/java/com/looksee/services/BrowserService.java`

Add a private static helper near the other BrowserService xpath helpers:

```java
/**
 * Extracts the HTML tag from the last segment of an xpath. Mode-agnostic —
 * doesn't round-trip to a driver or server. Reads the text between the
 * final "/" and the first "[" or end-of-string. Matches the shape of
 * xpaths produced by {@code BrowserService.extractAllUniqueElementXpaths}
 * and the ones stored in {@code ElementState.getXpath()}.
 *
 * <p>If the xpath is malformed or ends in a non-tag token, returns
 * {@code ""} — callers should treat that as "unknown tag, skip" to match
 * the defensive semantics at the call site in getDomElementStates.
 */
static String extractTagFromXpath(String xpath) {
    if (xpath == null || xpath.isEmpty()) return "";
    int lastSlash = xpath.lastIndexOf('/');
    String tail = lastSlash >= 0 ? xpath.substring(lastSlash + 1) : xpath;
    int bracket = tail.indexOf('[');
    String tag = bracket >= 0 ? tail.substring(0, bracket) : tail;
    // Strip leading wildcard prefixes like "*[...]" or namespace prefixes.
    int colon = tag.indexOf(':');
    return colon >= 0 ? tag.substring(colon + 1) : tag;
}
```

Replace the two `web_element.getTagName()` sites:

```java
// Line 471 (inside getDomElementStates's for-xpath loop):
// Before:
if( !is_displayed
        || !hasWidthAndHeight(element_size)
        || doesElementHaveNegativePosition(element_location)
        || isStructureTag( web_element.getTagName())
        || BrowserUtils.isHidden(element_location, element_size)){
    continue;
}
// After:
if( !is_displayed
        || !hasWidthAndHeight(element_size)
        || doesElementHaveNegativePosition(element_location)
        || isStructureTag( extractTagFromXpath(xpath))
        || BrowserUtils.isHidden(element_location, element_size)){
    continue;
}

// Line 547 (inside isImageElement(WebElement) helper):
// Rewrite the helper to take an xpath instead of a WebElement OR migrate
// its caller to the xpath form. Let the concrete code diff decide during
// execution — both options preserve local-mode semantics.
```

Local mode: `web_element.getTagName()` previously returned the tag as reported by Selenium. For xpaths produced by Look-see's own xpath generator (`BrowserService.extractAllUniqueElementXpaths` and `generateXpath`), the xpath's final segment is always `tag[...]` or `tag`, so `extractTagFromXpath` returns the same value Selenium would. The helper handles wildcards (`*[...]`) and namespaced tags (`svg:rect`) defensively. Add a unit test (Step 4) to lock the parity.

**Commit:** `refactor(core): derive tag from xpath instead of WebElement.getTagName() in getDomElementStates`

## Step 3 — Extend `RemoteWebElement.getTagName()`

**File:** `LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/RemoteWebElement.java`

Currently throws `UnsupportedOperationException(PHASE_3C + " (getTagName)")`. Change to:

```java
@Override
public String getTagName() {
    // Server-side engines may synthesize a "tag_name" pseudo-attribute on
    // the findElement response; if they do, read it from the cache. If not,
    // fail fast with a clear pointer to either server-side synthesis or the
    // xpath-derived workaround in BrowserService.extractTagFromXpath.
    String cached = attributes.get("tag_name");
    if (cached != null) return cached;
    throw new UnsupportedOperationException(
        "RemoteWebElement.getTagName: server did not include a 'tag_name' "
        + "attribute in the findElement response. Either add tag_name to "
        + "the server-side attributes synthesis (phase 3e candidate) or "
        + "derive from xpath via BrowserService.extractTagFromXpath.");
}
```

All other phase-3c unsupported methods stay as-is.

**Commit:** `feat(core): RemoteWebElement.getTagName reads cached tag_name attribute when present`

## Step 4 — Tests

### 4.1 `BrowserServiceExtractTagFromXpathTest` — new

`LookseeCore/looksee-core/src/test/java/com/looksee/services/BrowserServiceExtractTagFromXpathTest.java`

One test per xpath shape — parity with what Look-see's xpath generator emits:

```java
@Test void simpleTag()                { assertEquals("div",  extract("//div")); }
@Test void indexedTag()               { assertEquals("li",   extract("//body/header/nav/ul/li[3]")); }
@Test void attributePredicate()       { assertEquals("a",    extract("//a[@id='signin']")); }
@Test void namespacedTag()            { assertEquals("rect", extract("//svg:rect[1]")); }
@Test void nestedPath()               { assertEquals("span", extract("/html/body/div[1]/p/span")); }
@Test void malformed_returnsEmpty()   { assertEquals("",     extract("")); }
@Test void null_returnsEmpty()        { assertEquals("",     extract(null)); }
```

### 4.2 `BrowserServiceGetDomElementStatesRemoteModeTest` — new

Drives `getDomElementStates` with a mocked `RemoteBrowser` + mocked `BrowsingClient`. Returns a `ElementState` per xpath. Verifies:
- No `UnsupportedOperationException` (the driver-throw + tagName-throw both gone).
- Tag filtering still works — `isStructureTag` receives the xpath-derived tag.
- `browser.findElement` is called once per xpath, `client.getCurrentUrl` once total.

### 4.3 `RemoteWebElementTest` — extend

Add two cases:
- `getTagName_readsCachedTagNameAttribute` — ElementState with `attributes.tag_name = "div"` → returns `"div"`.
- `getTagName_throwsWhenAbsent` — attributes without `tag_name` → throws with the "phase 3e / xpath workaround" message.

### 4.4 Regression

- Existing `PageStateAdapterRemoteModeTest` + `RemoteBrowserElementOpsTest` pass unchanged.
- Full `mvn clean verify` green across all 11 modules with new tests counted.

**Commit:** `test(core): cover extractTagFromXpath + remote-mode getDomElementStates`

## Step 5 — Version bump 0.7.1 → 0.7.2

Same pattern as phase 3c version bump. `A11yParent` + 13 consumer poms + CHANGELOG under `## [0.7.2]`:

```
### Added
- `RemoteWebElement.getTagName()` now reads from the cached `tag_name` attribute when the server includes it (phase 3e-gated); throws with a clearer pointer otherwise.

### Changed
- `BrowserService.getDomElementStates` is remote-safe: the URL-host lookup at line 454 uses `browser.getCurrentUrl()`, and the tag-based filter at lines 471/547 derives the tag from the iteration xpath rather than `WebElement.getTagName()`.
- No behavior change in local mode.

### Unblocks
- Phase 4a.2 — PageBuilder `buildPageState` → `capturePage` migration. The full single-session flow (getConnection + buildPageState + getDomElementStates + close) now works transparently against a RemoteBrowser.

### Still deferred (phase 3e)
- `BrowserService.enrichElementStates` (lines 1953, 1997, 2003, 2510) — needs a new `Browser.findElements` or equivalent.
- `BrowserService` form extraction (lines 3291, 3292, 3306, 3385, 3401).
- `com.looksee.browsing.table.Table` helper.
- Remaining `RemoteWebElement` unsupported WebElement methods (click, submit, sendKeys, clear, isSelected, isEnabled, getText, findElement(s), getCssValue, getScreenshotAs).
```

**Commit:** `chore: bump LookseeCore to 0.7.2 + CHANGELOG + consumer pins`

## Step 6 — Verification

1. **Clean build:**
   ```bash
   cd LookseeCore && mvn -q clean verify
   ```
   Expect: `BUILD SUCCESS`. Test counts: looksee-core ≥ 170 (was 161 in 0.7.1; +~10 for the two new test classes + extended RemoteWebElementTest), browsing-client unchanged at 31.

2. **Scope-preservation check:**
   ```bash
   git diff --stat main..HEAD -- LookseeCore/looksee-browser/
   # Expect: empty. No new Browser methods needed for this phase.
   git diff --stat main..HEAD -- LookseeCore/looksee-browsing-client/
   # Expect: only the pom version bump.
   ```

3. **Post-3d sweep:**
   ```bash
   grep -n "browser\\.getDriver()\\.getCurrentUrl\\|web_element\\.getTagName" \
     LookseeCore/looksee-core/src/main/java/com/looksee/services/BrowserService.java | \
     grep -v "// line [0-9]" | wc -l
   ```
   Expect: 0 in `getDomElementStates` / `isImageElement`. Other call sites (enrichElementStates / form extraction) still have them by design — see §14 below.

4. **Optional manual remote-mode verification.** Same script as phase 3b §11 — spin up browser-service in Docker, run PageBuilder's audit flow against a remote session, confirm element-state rows land in Neo4j.

## Definition of done

- [ ] `BrowserService.getDomElementStates` has zero `browser.getDriver()` or `WebElement.getTagName()` calls.
- [ ] `extractTagFromXpath` helper exists with unit tests covering the 7 xpath shapes listed in §4.1.
- [ ] `RemoteWebElement.getTagName()` reads `attributes.tag_name` when present; throws with a descriptive message otherwise.
- [ ] `BrowserServiceGetDomElementStatesRemoteModeTest` passes; all existing tests pass unchanged.
- [ ] `A11yParent` at 0.7.2 across every pom; all 13 consumer service poms pinned.
- [ ] PR opened with title **"Phase 3d: unblock getDomElementStates for remote mode"**.

## Push and open PR

```bash
git push -u origin phase-3d/unblock-getdomelementstates
```

PR body links to this doc + phase-3b §14.9 + phase-3c §14.3. Explicit note: default mode stays `local`; this PR ships no behavior change. Flags phase 4a.2 as fully unblocked after this merges.

## 14. Open items flagged for reviewer

1. **Xpath-derived tag assumes Look-see's xpath format.** `extractTagFromXpath` reads the last `/`-separated segment and strips `[index]` / `[@attr=...]`. That matches every xpath I could find in Look-see's xpath generator and the xpaths stored in `ElementState.getXpath()`. If a consumer stores an xpath from a different generator (e.g. XPath 2.0 functions), this could misparse. Low risk today; flag in PR review if anyone surfaces it.

2. **Phase 3e inherit list.** Everything in phase-3b §14.9 that this phase does **not** address:
   - `BrowserService.enrichElementStates` (used by `element-enrichment`): lines 1953, 1997, 2003, 2510 — needs `Browser.findElements` + `Browser.waitForElement` + a remote-safe `CssUtils.loadCssProperties` path.
   - `BrowserService` form extraction (lines 3291, 3292, 3306, 3385, 3401): needs `Browser.findElements` on the document.
   - `com.looksee.browsing.table.Table` (lines 44, 51, 52): `element.findElements` on a passed-in WebElement. Needs `RemoteWebElement.findElements` backed by either (a) a new `/v1/sessions/{id}/element/find-children` endpoint or (b) xpath composition (parent xpath + child xpath in a single `/element/find` call).
   - `RemoteWebElement` unsupported methods: click, submit, sendKeys, clear, isSelected, isEnabled, getText, findElement(s), getCssValue, getScreenshotAs. `getTagName` moves out of the list in this phase.

3. **`tag_name` in the OpenAPI contract.** We're not adding it in this phase — purely a client-side change. If a future phase wants `RemoteWebElement.getTagName` to be authoritative instead of xpath-derived, it requires a browser-service engine update to synthesize `attributes.tag_name` on the `/element/find` response. Flag for the browser-service repo's roadmap; this phase is compatible with either outcome.

4. **When phase 3e triggers.** Phase 4b (element-enrichment cutover) exercises `enrichElementStates` and will surface the first 3e blocker. Either wait until then, or pre-emptively scope a 3e plan doc once 4a.2 lands and proves the 3d approach works. Recommend the former — reactive scoping matches project cadence.

5. **Phase 4a.2 is now fully unblocked.** After this merges, the PageBuilder `buildPageState` → `capturePage` re-migration can ship the single-session flow that PR #36 couldn't. No changes to PageBuilder's plan beyond the revert undo.
