# Phase 3e — Unblock `enrichElementState` for Remote Mode

> **Goal:** `BrowserService.enrichElementState(Browser, WebElement, ElementState, BufferedImage, String)` — the helper called by **both** `enrichElementStates` (element-enrichment) and the 6-arg `getDomElementStates` (PageBuilder + journeyExecutor) — runs to completion against a `RemoteBrowser`. This unblocks **phase 4b** (element-enrichment cutover) and **phase 4c** (journeyExecutor cutover).
>
> **Where to work:** locally in the Look-see monorepo against LookseeCore. Suggest a feature branch: `git checkout -b phase-3e/unblock-enrichelementstate`.
>
> **Sibling references:** mirrors [`phase-3-looksee-shim.md`](./phase-3-looksee-shim.md), [`phase-3b-element-handle-ops.md`](./phase-3b-element-handle-ops.md), [`phase-3c-internal-remote-compat.md`](./phase-3c-internal-remote-compat.md), [`phase-3d-unblock-getdomelementstates.md`](./phase-3d-unblock-getdomelementstates.md), [`phase-4-consumer-cutover.md`](./phase-4-consumer-cutover.md). Scoped from phase-3b §14.9 + phase-3d §14.

## Why this phase exists now

Phase 3d (LookseeCore 0.7.2, merged in PR #46) made the 4-arg `getDomElementStates` and `isImageElement` remote-safe, fully unblocking PageBuilder's page-state path. But the **6-arg `getDomElementStates`** (used by journeyExecutor's `buildPage` at `journeyExecutor/.../AuditController.java:493`) and **`enrichElementStates`** (used by element-enrichment's `AuditController.java:118`) both ultimately call the `enrichElementState` helper — which has three remote-incompatible call sites still in main:

| Site | Call | Fix |
|---|---|---|
| `BrowserService.java:1942` (`enrichElementStates`) | `browser.getDriver().findElement(By.xpath(element.getXpath()))` | One-line swap to `browser.findElement(...)` (already wired in phase 3b). |
| `BrowserService.java:1986` (`enrichElementState`) | `new WebDriverWait(browser.getDriver(), 10).until(ExpectedConditions.elementToBeClickable(web_element))` | New `Browser.waitForElementClickable(WebElement, Duration)` method. Local: existing WebDriverWait. Remote: poll `client.findElement` for the `displayed` field. |
| `BrowserService.java:1992 + 2499` (`enrichElementState` + a sibling helper) | `CssUtils.loadCssProperties(web_element, browser.getDriver())` | New `Browser.getComputedCssProperties(WebElement)` method. Local: wraps existing `CssUtils.loadCssProperties`. Remote: extends `BrowsingClient` with `executeScript` and runs `getComputedStyle()` JS server-side. |

After this phase:
- **Phase 4b** — element-enrichment cutover becomes a config-only flip (still gated on browser-service deployment, same as 4a).
- **Phase 4c** — journeyExecutor cutover becomes config-only too (StepExecutor was made remote-safe in 3b; phase 3e closes the last `enrichElementState` reach-throughs).
- Form extraction (lines 3339, 3340, 3354, 3433, 3449), the `Table` helper, and remaining `RemoteWebElement` unsupported `WebElement` methods stay phase 3f territory.

## Scope

| In scope | Out of scope |
|---|---|
| `BrowserService.java:1942` swap to `browser.findElement(xpath)` | All `BrowserService` form-extraction reach-throughs (lines 3339, 3340, 3354, 3433, 3449) |
| New `Browser.waitForElementClickable(WebElement, Duration)` with local default + RemoteBrowser polling override | Adding a server-side `/wait/clickable` endpoint — keep the polling client-side to avoid a contract change |
| New `Browser.getComputedCssProperties(WebElement)` with local default + RemoteBrowser implementation via `BrowsingClient.executeScript` | A new `/element/css` endpoint — use the existing `/execute` endpoint instead |
| Extend `BrowsingClient` facade with `executeScript(String sessionId, String script, List<Object> args)` (phase-3b §14.3 deferred this; the deferral expires here) | Wrapping `ExecuteApi` for arbitrary consumer use — the new facade method is internal to LookseeCore for now |
| Migrate `BrowserService.enrichElementState`'s three call sites + the sibling at line 2499 | Refactoring `CssUtils` itself; we keep its existing `(WebElement, WebDriver)` signature for local callers and route through the new `Browser` method |
| Tests: extend `BrowsingClientTest` for `executeScript`, new `RemoteBrowserExecuteTest`, extended `RemoteBrowserElementOpsTest` for `waitForElementClickable` + `getComputedCssProperties` | Live integration test against a running browser-service (manual verification only) |
| LookseeCore 0.7.2 → **0.8.0** minor bump (new `Browser` methods are an additive API surface) + 13 consumer pom pins | Any change that breaks local-mode behavior |

If something looks refactor-tempting inside `enrichElementState` beyond the four call-site swaps, resist. Phase 3e is wider than 3c/3d (touches three modules instead of one) but still scoped tight — phase 3f handles whatever 4b/4c surfaces.

## Locked decisions (from planning)

| Area | Decision |
|---|---|
| `waitForElementClickable` remote impl | **Client-side polling** of `client.findElement` for `displayed` + `attributes` (no `disabled`, no `aria-disabled`). Polls every 250ms up to the user-supplied `Duration`. No new server endpoint — keeps the OpenAPI contract stable. Documented as a phase-3f cleanup if a real `/wait/clickable` endpoint shows up in browser-service. |
| `getComputedCssProperties` remote impl | **Use `/v1/sessions/{id}/execute`** with the same JS that `CssUtils.loadCssProperties` runs locally — `getComputedStyle(arguments[0])` over a property list, return as a `{name: value}` object. Wires up the `executeScript` facade method that phase-3b §14.3 deliberately deferred ("on demand"); the demand has arrived. |
| `BrowsingClient.executeScript` signature | `Object executeScript(String sessionId, String script, List<Object> args)` — returns the generated `ExecuteScript200Response.result`'s untyped value. RemoteBrowser-internal callers cast as needed. Element references in `args` are passed as `RemoteWebElement` instances and serialized to `{"element_handle": "..."}` per the OpenAPI contract for the `/execute` endpoint. (Verify the contract supports element-handle serialization during Step 0; if not, add a fallback that inlines the element xpath into the script.) |
| `Browser.getComputedCssProperties` return type | `Map<String, String>` matching `CssUtils.loadCssProperties`'s current return type. Local-mode delegates to existing `CssUtils.loadCssProperties(WebElement, WebDriver)` so there's zero behavior change for local consumers. |
| Version bump | 0.7.2 → **0.8.0** (minor). Two new `Browser` methods + one new `BrowsingClient` facade method = additive API surface change. Same sizing rationale as phase 3 (which bumped 0.5.0 → 0.6.0 for adding `getConnection`/`capturePage`). |
| Phase 3f trigger | Form extraction surface (5 sites), `Table` helper (3 sites), remaining `RemoteWebElement` WebElement methods (10 methods) — all stay deferred until a phase-4 cutover surfaces a blocker. |

## Prerequisites

```bash
java -version     # 17.x
mvn -v            # 3.9+
git status        # clean working tree on main
```

- Phase 3d code merged (LookseeCore 0.7.2 on `main`) — confirmed.
- Re-confirm `enrichElementState`'s reach-throughs by line count via Step 0 grep.
- Re-confirm the OpenAPI spec's `/execute` endpoint accepts an element-handle reference in `args` (or document the inline-xpath fallback).

## Step 0 — Branch + sanity

```bash
cd /path/to/Look-see
git checkout -b phase-3e/unblock-enrichelementstate

cd LookseeCore && mvn -q verify   # 0.7.2 green baseline (175 looksee-core tests, 31 browsing-client tests)

# Expect exactly 4 in-scope hits:
grep -nE "browser\\.getDriver\\(\\)\\.findElement|new WebDriverWait\\(browser|loadCssProperties.*browser" \
  LookseeCore/looksee-core/src/main/java/com/looksee/services/BrowserService.java | head -10
```

Expected output:
- `1942` — `browser.getDriver().findElement(...)` (Step 1 fix)
- `1986` — `new WebDriverWait(browser.getDriver(), 10)` (Step 2 fix)
- `1992`, `2499` — `CssUtils.loadCssProperties(web_element, browser.getDriver())` (Step 3 fixes)

Confirm the OpenAPI spec's `ExecuteRequest` schema accepts element references in `args`:

```bash
grep -A 12 "ExecuteRequest:" LookseeCore/looksee-browsing-client/src/main/resources/openapi.yaml
```

If `args` is `array(items: {})` (any), the server may or may not handle element handles — Step 4's verification confirms this against the spec's documented behavior.

## Step 1 — Migrate `BrowserService.java:1942`

Single-line swap from the phase-3b plan playbook:

```java
// Before:
WebElement web_element = browser.getDriver().findElement(By.xpath(element.getXpath()));

// After:
WebElement web_element = browser.findElement(element.getXpath());
```

Local-mode: `browser.findElement` is the existing method that delegates to `driver.findElement(By.xpath(xpath))`. Byte-identical.
Remote-mode: `RemoteBrowser.findElement` returns a `RemoteWebElement` (phase 3b).

**Commit:** `refactor(core): route enrichElementStates through browser.findElement`

## Step 2 — Add `Browser.waitForElementClickable`

**File:** `LookseeCore/looksee-browser/src/main/java/com/looksee/browser/Browser.java`

```java
import java.time.Duration;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Waits up to {@code timeout} for {@code element} to become clickable. Local
 * mode uses Selenium's {@link WebDriverWait}; {@link com.looksee.services.browser.RemoteBrowser}
 * overrides this to poll {@code client.findElement} for the element's
 * displayed state, since the OpenAPI contract has no dedicated wait endpoint.
 */
public void waitForElementClickable(WebElement element, Duration timeout) {
    assert element != null;
    assert timeout != null;
    new WebDriverWait(driver, timeout).until(ExpectedConditions.elementToBeClickable(element));
}
```

**File:** `LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/RemoteBrowser.java`

Override:

```java
@Override
public void waitForElementClickable(WebElement element, Duration timeout) {
    String handle = requireRemote(element, "waitForElementClickable").getElementHandle();
    long deadline = System.nanoTime() + timeout.toNanos();
    long pollMillis = 250;
    while (System.nanoTime() < deadline) {
        // The element handle is stable across calls; we re-issue findElement
        // by xpath only if the handle disappears server-side. For a clickable
        // check, a re-fetch via attributes/displayed is cheaper.
        // … look up the element's xpath from cache (RemoteWebElement carries
        // it via its source ElementState), re-fetch fresh attributes, and
        // check displayed + not [disabled].
        // Implementation detail: see phase-3e doc §Step 2 for exact polling.
        if (isElementClickable(handle)) return;
        try { Thread.sleep(pollMillis); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
    }
    throw new org.openqa.selenium.TimeoutException(
        "RemoteBrowser.waitForElementClickable: element " + handle + " did not become clickable within " + timeout);
}
```

(Implementation detail: the polling re-issues `client.findElement(sessionId, xpath)` to refresh the cached `displayed` flag. `RemoteWebElement` needs a package-private getter for the original xpath if we want to avoid round-tripping by handle alone — alternative: add a `client.getElementState(sessionId, elementHandle)` facade method that hits a future `/v1/sessions/{id}/element/{handle}/state` endpoint. **For 3e, ship the xpath-refetch approach** — works today, doesn't require a server change. Document the endpoint as a phase-3f cleanup.)

**Migrate** `BrowserService.java:1986`:

```java
// Before:
WebDriverWait wait = new WebDriverWait(browser.getDriver(), 10);
wait.until(ExpectedConditions.elementToBeClickable(web_element));

// After:
browser.waitForElementClickable(web_element, java.time.Duration.ofSeconds(10));
```

**Commit:** `feat(core): add Browser.waitForElementClickable with polling RemoteBrowser override`

## Step 3 — Add `Browser.getComputedCssProperties` + `BrowsingClient.executeScript`

### 3.1 Extend `BrowsingClient` facade

**File:** `LookseeCore/looksee-browsing-client/src/main/java/com/looksee/browsing/client/BrowsingClient.java`

Add a new `OpsApi`/`MiscApi` field (whichever the generated code uses for `/execute`) and a wrapper. Phase-3b §14.3 deliberately deferred this; the deferral expires here.

```java
public Object executeScript(String sessionId, String script, java.util.List<Object> args) {
    return recordCall("executeScript", sessionId, () -> {
        ExecuteRequest req = new ExecuteRequest().script(script).args(args);
        ExecuteScript200Response resp = miscApi.executeScript(sessionId, req);
        return resp.getResult();
    });
}
```

### 3.2 Add the `Browser` method

**File:** `LookseeCore/looksee-browser/src/main/java/com/looksee/browser/Browser.java`

```java
/**
 * Returns the computed CSS properties for {@code element} as a name → value map.
 * Local mode delegates to {@link com.looksee.browser.utils.CssUtils#loadCssProperties};
 * {@link com.looksee.services.browser.RemoteBrowser} overrides this to run
 * {@code window.getComputedStyle(...)} server-side via the
 * {@code POST /v1/sessions/{id}/execute} endpoint.
 */
public java.util.Map<String, String> getComputedCssProperties(WebElement element) {
    assert element != null;
    return com.looksee.browser.utils.CssUtils.loadCssProperties(element, driver);
}
```

### 3.3 Override on `RemoteBrowser`

**File:** `LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/RemoteBrowser.java`

```java
@Override
public java.util.Map<String, String> getComputedCssProperties(WebElement element) {
    String handle = requireRemote(element, "getComputedCssProperties").getElementHandle();
    // Mirror CssUtils.loadCssProperties's JS — getComputedStyle over the
    // property list it cares about, returned as { name: value }. Names list
    // pinned to whatever CssUtils currently uses; keep in sync if that
    // changes.
    String script = "var el = arguments[0]; var s = window.getComputedStyle(el); "
                  + "var out = {}; for (var i = 0; i < s.length; i++) { "
                  + "  var n = s.item(i); out[n] = s.getPropertyValue(n); } return out;";
    Object result = client.executeScript(sessionId, script,
        java.util.List.of(java.util.Map.of("element_handle", handle)));
    @SuppressWarnings("unchecked")
    java.util.Map<String, String> map = (java.util.Map<String, String>) result;
    return map == null ? java.util.Collections.emptyMap() : map;
}
```

### 3.4 Migrate the two call sites

```java
// BrowserService.java:1992 + 2499 — Before:
Map<String, String> rendered_css_props = CssUtils.loadCssProperties(web_element, browser.getDriver());

// After (both sites):
Map<String, String> rendered_css_props = browser.getComputedCssProperties(web_element);
```

**Commit:** `feat(core): add Browser.getComputedCssProperties via BrowsingClient.executeScript`

## Step 4 — Tests

### 4.1 `BrowsingClientTest` — extend

Add forward-case test for the new `executeScript` facade method. Mock `MiscApi.executeScript` (or whichever generated class wraps `/execute`); assert sessionId + script + args forwarded correctly; assert timer registers under `operation=executeScript`.

### 4.2 `RemoteBrowserElementOpsTest` — extend

- `waitForElementClickable_returnsWhenDisplayed` — mock `client.findElement` to return `displayed=true` on first poll; assert no timeout.
- `waitForElementClickable_pollsUntilDisplayed` — first poll returns `displayed=false`, second returns true; assert success and ≥ 1 sleep cycle elapsed.
- `waitForElementClickable_throwsOnTimeout` — all polls return `displayed=false`; assert `TimeoutException`.
- `getComputedCssProperties_invokesExecuteScript` — mock `client.executeScript` to return a sample CSS map; assert the script string contains `getComputedStyle` and the returned map equals the mock.

### 4.3 `BrowserServiceEnrichElementStateLocalModeTest` (regression)

Drive `enrichElementState` against a mocked local `Browser` to confirm local-mode behavior is byte-identical: same scrollTo*, same Wait, same CssUtils.loadCssProperties invocation. Locks in the no-regression contract.

### 4.4 Full mvn clean verify

Expect ≥ 180 looksee-core tests (was 175 in 0.7.2), ≥ 33 browsing-client tests (was 31).

**Commit:** `test(core): cover waitForElementClickable + getComputedCssProperties + executeScript`

## Step 5 — Version bump 0.7.2 → 0.8.0

Same pattern as prior phases. `A11yParent` 0.7.2 → 0.8.0; `<looksee.version>` property; `LOOKSEE_CORE_VERSION`. CHANGELOG entry under `## [0.8.0]`:

```
### Added
- `Browser.waitForElementClickable(WebElement, Duration)` with a Selenium-WebDriverWait local default and a polling RemoteBrowser override.
- `Browser.getComputedCssProperties(WebElement)` with a CssUtils local default and a RemoteBrowser override that runs `getComputedStyle()` server-side via `BrowsingClient.executeScript`.
- `BrowsingClient.executeScript(sessionId, script, args)` — wraps the previously-unused `/v1/sessions/{id}/execute` endpoint. Phase-3b §14.3 deferred this; phase 3e wires it up.

### Changed
- `BrowserService.enrichElementState` is remote-safe — three reach-throughs migrated to the new Browser methods. `enrichElementStates` (used by element-enrichment) and the 6-arg `getDomElementStates` (used by journeyExecutor) both now run end-to-end against a `RemoteBrowser`.

### Unblocks
- Phase 4b — element-enrichment cutover (config-only flip).
- Phase 4c — journeyExecutor cutover (config-only flip).

### Still deferred (phase 3f)
- BrowserService form extraction (lines 3339, 3340, 3354, 3433, 3449)
- com.looksee.browsing.table.Table helper
- Remaining RemoteWebElement unsupported WebElement methods (click, submit, sendKeys, clear, isSelected, isEnabled, getText, findElement(s), getCssValue, getScreenshotAs)
```

13 consumer pom pins. Same `mvn versions:set` + sed pattern as 3c/3d.

**Commit:** `chore: bump LookseeCore to 0.8.0 + CHANGELOG + consumer pins`

## Step 6 — Verification

1. `mvn clean verify` from LookseeCore root — `BUILD SUCCESS` across all 11 modules. Test counts: looksee-core ≥ 180, browsing-client ≥ 33.

2. **Scope-preservation check:**
   ```bash
   git diff --stat main..HEAD -- LookseeCore/looksee-browser/
   # Expect: only Browser.java (+2 methods) and pom version bump.
   ```

3. **Post-3e sweep on enrichElementState path:**
   ```bash
   sed -n '1914,2050p' LookseeCore/looksee-core/src/main/java/com/looksee/services/BrowserService.java | \
     grep -nE "browser\\.getDriver|new WebDriverWait\\(browser|loadCssProperties.*browser"
   ```
   Expect: empty.

4. **Optional manual verification.** Same script as phase-3b §11.

## Definition of done

- [ ] `Browser.waitForElementClickable` exists with local + RemoteBrowser implementations.
- [ ] `Browser.getComputedCssProperties` exists with local + RemoteBrowser implementations.
- [ ] `BrowsingClient.executeScript` wires the `/execute` endpoint.
- [ ] `enrichElementState` (and the sibling helper at line 2499) have zero `browser.getDriver()` calls.
- [ ] All four `enrichElementState` reach-throughs (lines 1942, 1986, 1992, 2499) migrated.
- [ ] `mvn clean verify` green; test counts up by ≥ 5 in looksee-core, ≥ 2 in browsing-client.
- [ ] LookseeCore 0.8.0 across all poms + 13 consumer pom pins.
- [ ] PR opened with title **"Phase 3e: unblock enrichElementState for remote mode"**.

## Push and open PR

`git push -u origin phase-3e/unblock-enrichelementstate`. PR with `Closes #<issue>`. Body links to this doc + phase-3d. Flags 4b and 4c as fully unblocked after merge.

## 14. Open items flagged for reviewer

1. **Element-handle serialization in `/execute` `args`.** Step 0 verifies whether the OpenAPI contract documents how to pass an element handle to the script. If it does (e.g. `{"element_handle": "..."}` becomes a real `arguments[0]` element on the server), we use it. If not, the implementation falls back to **inlining the element via xpath into the script body** (e.g. `var el = document.evaluate('${xpath}', …)`). Either works for `getComputedStyle`; document whichever ships.

2. **Polling cadence for `waitForElementClickable`.** Hard-coded at 250ms. Reasonable default; configurable polling cadence (a follow-up overload `waitForElementClickable(WebElement, Duration timeout, Duration pollInterval)`) is a cheap future add if 4b/4c surface tighter timing requirements.

3. **`CssUtils.loadCssProperties` not refactored.** Kept its `(WebElement, WebDriver)` signature for direct local callers; `Browser.getComputedCssProperties` is a thin local wrapper. If `CssUtils` ever ends up with no direct callers other than the wrapper, fold it into `Browser.getComputedCssProperties` as a phase-5 cleanup.

4. **Property list: local vs. remote drift.** `CssUtils.loadCssProperties` reads a specific property list (whatever it iterates over locally). The Step 3.3 remote JS returns *all* computed styles via `s.length` enumeration — a superset. Acceptable for the consumers we have today (they pull specific keys out of the returned map), but verify in Step 4.4 that the existing element-enrichment audits still receive the same key set they expect.

5. **Phase 3f inherit list.** Form extraction (5 sites), `Table` helper (3 sites), remaining `RemoteWebElement` unsupported WebElement methods (10 methods). Triggered when a phase-4 cutover surfaces them — same trigger logic as 3d/3e.

6. **`/execute` endpoint security posture.** This is the first time we're sending arbitrary JS to browser-service from LookseeCore. The script is hard-coded inside `RemoteBrowser`, not user-controlled, so the surface is internal. But once the facade method exists, future LookseeCore code could pass user-controlled strings through it — which would be a script-injection risk. Add a doc note on `BrowsingClient.executeScript` warning that `script` must be a literal, not an interpolation. Phase-3f optional follow-up: enforce via a lint rule or a `@SafeScript` marker.

7. **0.8.0 version sizing.** The original phase-4 plan reserved 0.8.0 for "the Browser facade API change" — consistent with this phase's Browser additions. If the user prefers patch-only versioning until phase 4 actually ships, this can be 0.7.3 instead; flagging the choice for review.
