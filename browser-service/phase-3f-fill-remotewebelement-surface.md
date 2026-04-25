# Phase 3f — Fill the `RemoteWebElement` WebElement Surface

> **Goal:** Nine of the eleven `RemoteWebElement` methods that currently throw with the `phase 3c` marker get real, server-routed implementations. After this phase, any code path that interacts with a `RemoteWebElement` via standard Selenium `WebElement` calls — `click`, `submit`, `sendKeys`, `clear`, `getText`, `isSelected`, `isEnabled`, `getCssValue`, `getScreenshotAs(BYTES)` — works in remote mode without needing to route through dedicated `Browser` methods.
>
> **Where to work:** locally in the Look-see monorepo against LookseeCore. Suggest a feature branch: `git checkout -b phase-3f/fill-remotewebelement-surface`.
>
> **Sibling references:** mirrors [`phase-3-looksee-shim.md`](./phase-3-looksee-shim.md), [`phase-3b-element-handle-ops.md`](./phase-3b-element-handle-ops.md), [`phase-3c-internal-remote-compat.md`](./phase-3c-internal-remote-compat.md), [`phase-3d-unblock-getdomelementstates.md`](./phase-3d-unblock-getdomelementstates.md), [`phase-3e-unblock-enrichelementstate.md`](./phase-3e-unblock-enrichelementstate.md), [`phase-4-consumer-cutover.md`](./phase-4-consumer-cutover.md). Closes the `RemoteWebElement` portion of the phase-3b §14 inherit list that's now trivially implementable.

## Why this phase exists now

Phases 3a–3e closed every `BrowserService`/`PageStateAdapter` reach-through that any active consumer (PageBuilder, element-enrichment, journeyExecutor) actually exercises. Those consumers are now flip-ready. `RemoteWebElement`, however, still throws on **11 standard WebElement methods** — the bulk of the Selenium WebElement API surface that any *future* consumer might naturally call.

After phase 3e shipped two things — `BrowsingClient.executeScript` (the `/v1/sessions/{id}/execute` endpoint) and the existing `client.captureElementScreenshot` facade — **nine of those eleven methods become trivially implementable** without any browser-service contract change:

| Method | Implementation |
|---|---|
| `click()` | `client.performElementAction(sessionId, handle, ElementAction.CLICK, null)` |
| `submit()` | `executeScript("arguments[0].form ? arguments[0].form.submit() : arguments[0].submit()", [{handle}])` |
| `sendKeys(CharSequence...)` | `client.performElementAction(sessionId, handle, ElementAction.SEND_KEYS, joined)` |
| `clear()` | `executeScript("arguments[0].value = ''; arguments[0].dispatchEvent(new Event('input'))", [{handle}])` |
| `isSelected()` | `executeScript("return !!(arguments[0].selected \|\| arguments[0].checked)", [{handle}])` |
| `isEnabled()` | `executeScript("return !arguments[0].disabled", [{handle}])` |
| `getText()` | `executeScript("return arguments[0].textContent", [{handle}])` |
| `getCssValue(String)` | `executeScript("return window.getComputedStyle(arguments[0]).getPropertyValue(arguments[1])", [{handle}, name])` |
| `getScreenshotAs(OutputType<X>)` | `client.captureElementScreenshot(sessionId, handle)` for `OutputType.BYTES`; throw for other output types |

The two methods that **stay deferred** to a future phase 3g are `findElements(By)` and `findElement(By)` (nested finds on a `RemoteWebElement`). Both need either a new server endpoint (e.g. `POST /v1/sessions/{id}/element/{handle}/find-children`) or xpath-composition logic in the client (parent xpath + child xpath as a single `/element/find` call). Neither is small; both are demand-driven.

The `BrowserService` form extraction (lines 3339, 3340, 3354, 3433, 3449) and the `com.looksee.browsing.table.Table` helper (3 sites) also remain deferred — same trigger logic.

## Scope

| In scope | Out of scope |
|---|---|
| 9 new `RemoteWebElement` method implementations replacing phase-3c throws | `findElements(By)` and `findElement(By)` — phase 3g |
| Tests in `RemoteWebElementTest` covering each new method | `BrowserService` form extraction (5 sites) — phase 3g |
| Tests for the new "no longer throws" surface in `RemoteBrowserElementOpsTest` | `com.looksee.browsing.table.Table` helper (3 sites) — phase 3g |
| `getScreenshotAs(OutputType<X>)` implemented for `OutputType.BYTES`; explicit throw with a clear pointer for other output types | A general `OutputType<X>` polymorphism — Selenium's API is awkward and the BYTES path is the only one any current consumer uses |
| LookseeCore 0.8.0 → **0.8.1** patch + 13 consumer pom pins | Any change to local-mode behavior |

If a consumer surfaces a `findElements`-shaped need during phase 4 cutover, that's the trigger for phase 3g — not anything in this phase's scope.

## Locked decisions (from planning)

| Area | Decision |
|---|---|
| `click` and `sendKeys` | Route via the existing `client.performElementAction` (the `/element/action` endpoint phase 3b wired). Don't invent a JS path when there's a dedicated server endpoint. |
| `submit`, `clear`, `getText`, `isSelected`, `isEnabled`, `getCssValue` | Route via `client.executeScript` (the `/execute` endpoint phase 3e wired). All are pure DOM property reads/writes that fit one short literal script each. |
| `getScreenshotAs(OutputType<X>)` | Implement only `OutputType.BYTES` — call `client.captureElementScreenshot` and return raw bytes. For any other output type (`BASE64`, `FILE`), throw with a message saying "only BYTES is supported in remote mode; convert client-side if needed". Real consumers (Look-see audits) only ever use BYTES. |
| `sendKeys(CharSequence...)` joining | Concatenate all `CharSequence` args with no separator — matches Selenium's `WebElement.sendKeys(CharSequence...)` semantics ("the strings are concatenated"). |
| `clear()` event dispatch | Setting `value = ''` alone doesn't fire an `input` event, which some web frameworks (React, Vue) listen for to update bound state. Dispatch a synthetic `Event('input')` after the assignment for parity with Selenium's `clear()` behavior. |
| Cross-session guard on each new method | Reuse the same `requireRemote(WebElement, methodName)` pattern from `RemoteBrowser`, but inlined into `RemoteWebElement` (it's `this`, so just check `this.sessionId` against… wait, RemoteWebElement's constructor takes a sessionId. The check that matters is "is this RemoteWebElement bound to a still-valid session?" which we can't easily verify client-side. Skip the guard inside `RemoteWebElement` — let the server return 404 for an expired session and let `BrowsingClientException` bubble up. |
| Version bump | 0.8.0 → **0.8.1** (patch). No new public `Browser` API; only `RemoteWebElement` overrides become real. Same sizing as 3c/3d. |

## Prerequisites

```bash
java -version     # 17.x
mvn -v            # 3.9+
git status        # clean working tree on main
```

- Phase 3e merged (LookseeCore 0.8.0 on `main`) — confirmed.
- `BrowsingClient.executeScript` and `client.captureElementScreenshot` both work and are tested (phase 3e + phase 3b).
- `BrowsingClient.performElementAction` works and is tested (phase 3b).

## Step 0 — Branch + sanity

```bash
cd /path/to/Look-see
git checkout -b phase-3f/fill-remotewebelement-surface

cd LookseeCore && mvn -q verify   # 0.8.0 green baseline (186 looksee-core tests, 32 browsing-client tests)
cd ..   # back to repo root for the grep below; the path is repo-rooted

# Confirm exactly 11 PHASE_3C throw sites remain (we'll convert 9, leave 2
# for phase 3g). Use the throw-line pattern — a bare `PHASE_3C` grep would
# also count the `private static final String PHASE_3C = …` declaration.
grep -c "throw new UnsupportedOperationException(PHASE_3C" \
  LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/RemoteWebElement.java
```

Expected: `11`. After this phase: `2` (the two `findElement(s)` overloads).

## Step 1 — Wire `click`, `submit`, `sendKeys`, `clear`

Replace the four phase-3c throws in `RemoteWebElement.java` with real implementations. The `click` and `sendKeys` paths route via the existing `BrowsingClient.performElementAction`; `submit` and `clear` route via the new `executeScript`.

Inline diffs (paste-ready):

```java
// click — uses /element/action endpoint
@Override public void click() {
    client.performElementAction(sessionId, elementHandle,
        com.looksee.browsing.generated.model.ElementAction.CLICK, null);
}

// submit — falls back to JS form.submit() since /element/action has no SUBMIT enum
@Override public void submit() {
    client.executeScript(sessionId,
        "var el = arguments[0]; "
        + "if (el.form) { el.form.submit(); } "
        + "else if (typeof el.submit === 'function') { el.submit(); } "
        + "else { throw new Error('not submittable: ' + el.tagName); }",
        java.util.List.of(java.util.Map.of("element_handle", elementHandle)));
}

// sendKeys — concat then route via /element/action SEND_KEYS
@Override public void sendKeys(CharSequence... keys) {
    StringBuilder sb = new StringBuilder();
    if (keys != null) for (CharSequence k : keys) if (k != null) sb.append(k);
    client.performElementAction(sessionId, elementHandle,
        com.looksee.browsing.generated.model.ElementAction.SEND_KEYS, sb.toString());
}

// clear — set value to "" + dispatch synthetic input event for framework parity
@Override public void clear() {
    client.executeScript(sessionId,
        "var el = arguments[0]; el.value = ''; "
        + "el.dispatchEvent(new Event('input', { bubbles: true }));",
        java.util.List.of(java.util.Map.of("element_handle", elementHandle)));
}
```

`RemoteWebElement` needs a `BrowsingClient` reference to do this routing. Currently the constructor only takes `(sessionId, sourceXpath, state)`. Two options:

- **(A)** Add `BrowsingClient` to the constructor — every `RemoteWebElement` carries its client. Simple but couples element to client at construction time.
- **(B)** Add a `RemoteBrowser` reference instead — element forwards through its parent browser's facade. More indirection but matches WebDriver mental model.

**Decision: (A).** Add `client` as a field; pass it from `RemoteBrowser.findElement` at construction. Keep the existing 2-arg and 3-arg constructors as test-only (no client = methods can't act, throw with a clear "constructed without BrowsingClient" pointer).

**Commit:** `feat(core): wire RemoteWebElement click/submit/sendKeys/clear via BrowsingClient`

## Step 2 — Wire `getText`, `isSelected`, `isEnabled`, `getCssValue`

Four DOM property reads via `executeScript`:

```java
@Override public String getText() {
    Object r = client.executeScript(sessionId,
        "return arguments[0].textContent;",
        java.util.List.of(java.util.Map.of("element_handle", elementHandle)));
    return r == null ? "" : r.toString();
}

@Override public boolean isSelected() {
    Object r = client.executeScript(sessionId,
        "return !!(arguments[0].selected || arguments[0].checked);",
        java.util.List.of(java.util.Map.of("element_handle", elementHandle)));
    return Boolean.TRUE.equals(r);
}

@Override public boolean isEnabled() {
    Object r = client.executeScript(sessionId,
        "return !arguments[0].disabled;",
        java.util.List.of(java.util.Map.of("element_handle", elementHandle)));
    return !Boolean.FALSE.equals(r);  // default-true if server returns null
}

@Override public String getCssValue(String propertyName) {
    Object r = client.executeScript(sessionId,
        "return window.getComputedStyle(arguments[0]).getPropertyValue(arguments[1]);",
        java.util.List.of(java.util.Map.of("element_handle", elementHandle), propertyName));
    return r == null ? "" : r.toString();
}
```

**Commit:** `feat(core): wire RemoteWebElement getText/isSelected/isEnabled/getCssValue via executeScript`

## Step 3 — Wire `getScreenshotAs`

Selenium's `OutputType<X>` is awkward. Real consumers use `OutputType.BYTES` exclusively (the Look-see audit suite checked: zero callers pass anything else). Implement only that path:

```java
@Override
@SuppressWarnings("unchecked")
public <X> X getScreenshotAs(OutputType<X> outputType) {
    if (outputType == OutputType.BYTES) {
        return (X) client.captureElementScreenshot(sessionId, elementHandle);
    }
    throw new UnsupportedOperationException(
        "RemoteWebElement.getScreenshotAs: only OutputType.BYTES is supported "
        + "in remote mode (got " + outputType + "). Convert client-side if "
        + "BASE64 or FILE is needed.");
}
```

**Commit:** `feat(core): wire RemoteWebElement getScreenshotAs(BYTES)`

## Step 4 — Tests

Extend `RemoteWebElementTest`:

- `click_routesPerformElementAction` — mock BrowsingClient; assert `performElementAction(sessionId, handle, CLICK, null)`.
- `sendKeys_concatsArgsAndForwards` — multi-arg sendKeys; assert concatenated string forwarded.
- `submit_executesFormSubmitJs` — captures the executeScript call's script string; assert it contains `el.form.submit()`.
- `clear_setsValueAndDispatchesInputEvent` — same; assert script contains both `value = ''` and `dispatchEvent(new Event('input'))`.
- `getText_returnsTextContent` — mock executeScript to return a string; assert returned.
- `getText_returnsEmptyOnNull` — null result → empty string.
- `isSelected_trueWhenServerReturnsTrue` + `isSelected_falseWhenServerReturnsFalse`.
- `isEnabled_trueWhenNotDisabled` + `isEnabled_falseWhenDisabled`.
- `getCssValue_passesPropertyNameAsArg` — captures the script's args; assert property name in args.
- `getScreenshotAs_BYTES_returnsBytes` — mock captureElementScreenshot; assert bytes returned.
- `getScreenshotAs_otherOutputTypes_throw` — pass OutputType.BASE64; assert UnsupportedOperationException.
- Update `unsupportedWebElementMethods_allThrowWithPhase3cMarker` — remove the 9 newly-implemented methods from the loop; only `findElement(By)` and `findElements(By)` remain.

Also add **two cases to `RemoteBrowserElementOpsTest`** that confirm a RemoteWebElement obtained from `remote.findElement(xpath)` carries a working BrowsingClient reference (end-to-end smoke that Step 1's constructor wiring works).

**Commit:** `test(core): cover RemoteWebElement WebElement-method implementations`

## Step 5 — Version bump 0.8.0 → 0.8.1

Same pattern as prior patches. CHANGELOG entry under `## [0.8.1]`:

```
### Added
- `RemoteWebElement` now implements 9 of the 11 phase-3c-deferred WebElement methods: `click`, `submit`, `sendKeys`, `clear`, `getText`, `isSelected`, `isEnabled`, `getCssValue`, `getScreenshotAs(OutputType.BYTES)`. Routes through the existing `BrowsingClient.performElementAction` / `executeScript` / `captureElementScreenshot` facades — no new browser-service endpoint required.

### Changed
- `RemoteWebElement` constructor now accepts a `BrowsingClient` so each element can route its WebElement-API calls without needing a parent `RemoteBrowser` reference. Existing 2-arg and 3-arg constructors remain (without client = WebElement-API methods throw with a clear pointer).
- No behavior change in local mode.

### Still deferred (phase 3g — trigger on demand)
- `RemoteWebElement.findElement(By)` and `findElements(By)` — needs either xpath-composition logic or a new `/v1/sessions/{id}/element/{handle}/find-children` endpoint.
- `BrowserService` form extraction (lines 3339, 3340, 3354, 3433, 3449)
- `com.looksee.browsing.table.Table` helper

### Unblocks
- Any future consumer that uses standard Selenium `WebElement` API on RemoteWebElement instances. Current consumer set (PageBuilder, element-enrichment, journeyExecutor) doesn't directly use these methods today, so this phase is preemptive completion of the phase-3 surface.
```

13 consumer pom pins. Same `mvn versions:set` + sed pattern as prior patches.

**Commit:** `chore: bump LookseeCore to 0.8.1 + CHANGELOG + consumer pins`

## Step 6 — Verification

1. `mvn clean verify` from LookseeCore root — `BUILD SUCCESS` across all 11 modules. Test counts: looksee-core ≥ 196 (was 186; +~10 for the new RemoteWebElement tests), browsing-client unchanged at 32.

2. Post-3f sweep (run from repo root, throw-lines only — same pattern as Step 0):
   ```bash
   grep -c "throw new UnsupportedOperationException(PHASE_3C" \
     LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/RemoteWebElement.java
   ```
   Expect: `2` (only the two `findElement(By)` / `findElements(By)` overloads still throw).

3. **Scope-preservation check:**
   ```bash
   git diff --stat main..HEAD -- LookseeCore/looksee-browser/
   # Expect: empty. No engine-module changes needed for 3f.
   ```

## Definition of done

- [ ] 9 `RemoteWebElement` WebElement methods have real implementations; 2 (`findElement(s)`) still throw.
- [ ] `RemoteWebElement` carries a `BrowsingClient` reference via a new constructor.
- [ ] `RemoteBrowser.findElement` passes the client through to the constructed element.
- [ ] `mvn clean verify` green; test counts up.
- [ ] LookseeCore 0.8.1 across all poms + 13 consumer pom pins.
- [ ] PR opened with title **"Phase 3f: fill the RemoteWebElement WebElement surface"**.

## Push and open PR

`git push -u origin phase-3f/fill-remotewebelement-surface`. PR with `Closes #<issue>`. Body links to this doc + phase-3e. Flags that **the LookseeCore-side prep for phase 4 is now complete** — every code path the active consumer set exercises works in remote mode, plus most of the broader WebElement API surface.

## 14. Open items flagged for reviewer

1. **Constructor coupling.** Adding `BrowsingClient` to `RemoteWebElement` couples the element to its facade at construction. Acceptable because elements are always created by `RemoteBrowser.findElement`, which has the client in scope. Tests that need client-less elements use the legacy 2-arg/3-arg constructors and accept that WebElement-API methods will throw.

2. **`getScreenshotAs(OutputType<X>)` polymorphism.** Only `OutputType.BYTES` works remotely. `OutputType.BASE64` and `OutputType.FILE` throw with a clear pointer. If a consumer needs them, they can convert client-side (BASE64 from BYTES is one Base64.encoder call; FILE is a `Files.write`). Adding server-side support is a phase-3g candidate if a real demand surfaces.

3. **`isEnabled` default-true semantics.** The script `return !arguments[0].disabled` returns `true` for elements without a `disabled` attribute (matches Selenium's behavior on non-form elements). The Java side defensively returns `true` when the server returns `null` rather than `false`, matching Selenium's "if uncertain, assume enabled" convention. Document in javadoc.

4. **`clear` event dispatch.** Setting `value = ''` alone doesn't fire `input`, which React/Vue/etc. need to update bound state. Selenium's local `clear()` does fire the event because of how WebDriver routes through the browser. The remote impl dispatches a synthetic `Event('input', { bubbles: true })` for parity. If a consumer reports framework-specific bind issues, we may also need to dispatch `change`.

5. **No `Browser` API change in this phase.** Phase 3f is purely about making `RemoteWebElement` more useful. The `Browser`-routed paths (`Browser.performClick`, `Browser.performAction`, `Browser.getComputedCssProperties`, etc.) all stay as-is. Consumers can continue to use either path; both work.

6. **Phase 3g remaining inherit list.** When the next consumer-side need surfaces (most likely during 4b or 4c live-flip):
   - `RemoteWebElement.findElement(By)` / `findElements(By)` — requires either client-side xpath composition or a new server endpoint.
   - `BrowserService` form extraction (5 sites) — requires `Browser.findElements` or a server form-list endpoint.
   - `com.looksee.browsing.table.Table` helper (3 sites) — same constraint.

7. **After phase 3f, the LookseeCore prep for phase 4 is functionally complete.** The remaining work to unblock production cutover is **browser-service deployment** (external repo). Phase-4 doc §Prerequisites enumerates what browser-service must ship before 4a.5 (staging flip) can begin. This phase is the natural pause-point before that handoff.
