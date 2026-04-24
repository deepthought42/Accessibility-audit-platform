# Phase 3b — Remote Element-Handle Ops for `RemoteBrowser`

> **Goal:** Every currently-throwing override in `LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/RemoteBrowser.java` gets a real remote implementation that routes through `brandonkindred/browser-service`, plus `StepExecutor` stops reaching through `browser.getDriver()` so journey execution works in remote mode. Shipped as LookseeCore 0.7.0.
>
> **Where to work:** locally in the Look-see monorepo. Suggest a feature branch: `git checkout -b phase-3b/element-handle-ops`.
>
> **Sibling reference:** this doc mirrors [`phase-3-looksee-shim.md`](./phase-3-looksee-shim.md) in structure (step-by-step, per-step commits, definition-of-done). Phase 3 shipped page-level remote ops and left an explicit `UnsupportedOperationException("phase 3b")` marker in every method this doc is about to wire up.

## Why this phase exists now

Phase 3 (merged as LookseeCore 0.6.0) shipped a working remote shim for page-level operations. But it deliberately left every element-handle method stubbed:

```java
// RemoteBrowser.java today
@Override
public WebElement findElement(String xpath) {
    throw new UnsupportedOperationException(PHASE_3B + " (findElement)");
}
```

That decision was right for 0.6.0 — phase 3 was already a large change — but it means **every consumer that uses element-handle ops is still forced onto `looksee.browsing.mode=local`**. A consumer census (run during phase-3b planning) found:

| Consumer | Element-handle usage | Remote-mode ready? |
|---|---|---|
| PageBuilder | Page-level only | Yes, today |
| AuditManager / audit-service / contentAudit / IA audit / visualDesignAudit / journeyErrors / journey-map-cleanup / CrawlerAPI / front-end-broadcaster | No direct `Browser` calls | Yes, today |
| **element-enrichment** | `browser.removeDriftChat()` in prod | Blocked on 3b |
| **journeyExecutor** | `stepExecutor.execute(...)` + `browser.getDriver().getCurrentUrl()` | Blocked on 3b |
| **StepExecutor** (LookseeCore internal; journeyExecutor uses it) | `findElement`, `scrollToElementCentered`, raw `getDriver()`-based click + LoginStep field entry + exception path URL read | Blocked on 3b |

Phase 3b unblocks everything in the bottom three rows. Phase 4 (per-consumer cutover) becomes feasible for every consumer after it merges.

## Scope

| In scope | Out of scope |
|---|---|
| Extend `BrowsingClient` facade with `ElementsApi`, `TouchApi`, `DomApi`, `MouseApi`, `AlertsApi`, `OpsApi` wrappers | Expanding `ExecuteApi` coverage — we have dedicated endpoints for every op StepExecutor needs, so `/execute` stays unused in 3b |
| New `RemoteWebElement implements WebElement` (in `looksee-core`) with cached `rect` / `attributes` / `displayed` | Replacing `Selenium WebElement` in consumer signatures with a Look-see–owned type (Option B — breaks every caller; out of scope) |
| New `RemoteAlert implements org.openqa.selenium.Alert` for the `isAlertPresent` surface | Full Selenium `Alert` parity — we ship `accept`/`dismiss`/`getText`; `sendKeys` stays phase-3c if needed |
| Wire all 18 currently-throwing `RemoteBrowser` overrides | Any change to local-mode `Browser.java` methods beyond three explicit additions (§6) |
| Three new `Browser` methods: `performClick(WebElement)`, `performAction(WebElement, Action, String)`, `getCurrentUrl()` — local bodies preserve 0.6.0 semantics byte-for-byte | Replacing `ActionFactory` wholesale; we keep it for local mode and route around it in remote |
| Surgical `StepExecutor` refactor: 3 edits replacing raw-driver access | Any larger `StepExecutor` restructure or test overhaul |
| Remote-mode `BrowserService.capturePage` rewrite: explicit session lifecycle producing separate viewport + full-page screenshots | Adding new screenshot strategies; we consume `VIEWPORT` + `FULL_PAGE_SHUTTERBUG` as defined in 0.6.0 |
| Version bump 0.6.0 → 0.7.0 + CHANGELOG | Downstream consumer-side changes beyond the one-line fix in `journeyExecutor/.../AuditController.java:562` |
| Unit tests for every new facade method, `RemoteWebElement`, `RemoteAlert`, every wired override, and the `capturePage` explicit lifecycle | Live integration tests against a running browser-service (documented manual verification only, same policy as phase 3) |

If something looks refactor-tempting inside `Browser.java` or `StepExecutor.java` beyond the three-new-methods / four-call-site plan below — resist. This phase is already the larger of the two remaining.

## Locked decisions (from planning)

| Area | Decision |
|---|---|
| `WebElement` mapping | **Option A:** `RemoteWebElement implements WebElement`, cache-backed (rect + attributes + displayed). See §14.1 on staleness. |
| Unsupported `WebElement` methods | Enumerated in §14.2 below — every method that throws is a phase-3c candidate, same discipline phase 3 used for element-handle ops. |
| `/execute` endpoint | Unused in 3b. We route through dedicated endpoints. Noted in §14.3 so a future `/execute` extension has a clean start. |
| `RemoteAlert` | Cache text at `isAlertPresent` time; `accept`/`dismiss` forward to `/alert/respond`. |
| Remote `capturePage` | Explicit 5-step lifecycle instead of `/capture`. See §8. |
| `BrowsingClient.capture` | Retired from the facade (still available on the generated `CaptureApi` if a later caller wants a cheaper single-shot). |
| `Actions` API | Consumers that build `new Actions(browser.getDriver())` are broken in remote mode. Grep in Step 0 confirms StepExecutor is the only caller in-tree. `Browser.performAction` covers it; broader `Actions` parity is phase-3c if anyone else shows up. |
| Retries / backoff | Deferred again. Element ops are chattier than page ops; revisit if 3b cutover surfaces transient failures. |

## Prerequisites

```bash
java -version     # 17.x (LookseeCore stays on Java 17; do not bump)
mvn -v            # 3.9+
git status        # clean working tree
```

Phase 3 must be merged (`LookseeCore 0.6.0` on `main`). Re-sync `browser-service/openapi.yaml` into `LookseeCore/looksee-browsing-client/src/main/resources/openapi.yaml` if it drifted since phase 3; the element/scroll/dom/mouse/alert endpoints listed in §4 must all be present in the spec.

## Step 0 — Branch + sanity

```bash
cd /path/to/Look-see
git checkout -b phase-3b/element-handle-ops
cd LookseeCore
mvn -q -pl looksee-core -am verify
```

Expect: `BUILD SUCCESS`, 119 tests run, 0 failures (the 0.6.0 baseline).

Then run the grep-sanity Step 0 asks for — any hit outside the expected sites means the phase-3b surface area has expanded since planning:

```bash
# Any consumer reaching through to build its own Actions chain?
grep -rn "new Actions(" --include="*.java" | grep -v LookseeCore/looksee-browser

# Any consumer calling findElements (plural)?
grep -rn "\.findElements(" --include="*.java" | grep -v LookseeCore/looksee-browser

# Any raw getDriver() use outside StepExecutor / element-enrichment test?
grep -rn "\.getDriver()\." --include="*.java" | grep -v LookseeCore/looksee-browser | grep -v "test"
```

Expected hits:
- `new Actions(` — only `StepExecutor.java` (indirectly via `ActionFactory`). If anything else shows up, extend the `Browser.performAction` enum or add to §14.
- `.findElements(` — nothing in consumer code today.
- `.getDriver()` — `StepExecutor.java` (4 call sites documented below) and `journeyExecutor/.../AuditController.java:562` (1 site). Anything else is a new finding.

If a new hit appears, resolve it by either (a) adding it to the §6 `Browser` extension surface, or (b) documenting the consumer as a phase-3c follow-up. Do not start implementing until this grep sweep is clean.

## Step 1 — Extend `BrowsingClient` facade

Add wrappers over six generated API classes. The facade still returns generated DTOs directly (same pragmatic choice as phase 3 — see `phase-3-looksee-shim.md` §14).

**File:** `LookseeCore/looksee-browsing-client/src/main/java/com/looksee/browsing/client/BrowsingClient.java`

New fields:

```java
private final ElementsApi elementsApi;
private final TouchApi touchApi;
private final DomApi domApi;
private final MouseApi mouseApi;
private final AlertsApi alertsApi;
// ScrollingApi already in place from phase 3; just new methods forwarded below.
```

Constructor: new `*Api(apiClient)` assignments alongside the five existing ones. Package-private test constructor grows to 10 `*Api` parameters — keep the ordering predictable (session / navigation / screenshots / scrolling / capture / elements / touch / dom / mouse / alerts) and document at the top of the class.

New facade methods:

```java
// Element lookup + action
public ElementState findElement(String sessionId, String xpath);
public void performElementAction(String sessionId, String elementHandle, ElementAction action, String input);
public void performElementTouch(String sessionId, String elementHandle, TouchAction action, String input);
public byte[] captureElementScreenshot(String sessionId, String elementHandle);

// Scroll — the four new enum modes
public ScrollOffset scrollToTop(String sessionId);
public ScrollOffset scrollToBottom(String sessionId);
public ScrollOffset scrollToElement(String sessionId, String elementHandle, String xpathHint);
public ScrollOffset scrollToElementCentered(String sessionId, String elementHandle);
public ScrollOffset scrollDownPercent(String sessionId, double percent);
public ScrollOffset scrollDownFull(String sessionId);

// DOM removal
public void removeDomElement(String sessionId, DomRemovePreset preset, String valueOrNull);

// Mouse
public void moveMouseOutOfFrame(String sessionId);
public void moveMouseToNonInteractive(String sessionId, int x, int y);

// Alert
public AlertState getAlert(String sessionId);
public void respondToAlert(String sessionId, AlertChoice choice, String inputOrNull);
```

Translate the three overlapping enum types (`ElementAction`, `TouchAction`, `AlertChoice`) between Look-see's existing `com.looksee.browser.enums.*` and the generated variants at this seam, same pattern as `BrowserType`/`BrowserEnvironment` in phase 3.

Retire `BrowsingClient.capture` and `BrowsingClient.getCaptureScreenshotBytes` — no longer called from `BrowserService.capturePage` after Step 8. Leave a deprecation comment pointing callers at the generated `CaptureApi` if they want the single-shot.

**Commit:** `feat(browsing-client): extend facade with elements/touch/dom/mouse/alerts/ops`

## Step 2 — `RemoteWebElement`

New file:

`LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/RemoteWebElement.java`

```java
package com.looksee.services.browser;

import com.looksee.browsing.generated.model.ElementState;
import com.looksee.browsing.generated.model.Rect;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.openqa.selenium.*;

/**
 * WebElement bound to a remote browser-service session. Holds the opaque
 * element_handle returned by findElement plus the rect/attributes/displayed
 * fields the server returned at find time. Cached reads are served locally;
 * every mutating op routes through RemoteBrowser + BrowsingClient.
 *
 * <p>Equality is sessionId + elementHandle — matches Selenium's contract of
 * "two references to the same DOM node are equal" without requiring a live
 * driver.
 */
public final class RemoteWebElement implements WebElement {

    private static final String PHASE_3C =
        "RemoteWebElement: this WebElement op is deferred to phase 3c "
        + "(route through Browser.performClick / performAction / extractAttributes / "
        + "getElementScreenshot / scrollToElement instead)";

    private final String sessionId;
    private final String elementHandle;
    private final Rect rect;                     // may be null if server omitted
    private final Map<String, String> attributes; // never null
    private final boolean displayed;

    public RemoteWebElement(String sessionId, ElementState state) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(state, "state");
        this.sessionId = sessionId;
        this.elementHandle = Objects.requireNonNull(state.getElementHandle(), "element_handle");
        this.rect = state.getRect();
        this.attributes = state.getAttributes() == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(state.getAttributes());
        this.displayed = Boolean.TRUE.equals(state.getDisplayed());
    }

    public String getSessionId()    { return sessionId; }
    public String getElementHandle() { return elementHandle; }
    Map<String, String> cachedAttributes() { return attributes; } // package-private; used by RemoteBrowser.extractAttributes

    // --- Cache-backed WebElement methods ---------------------------------

    @Override public boolean isDisplayed()          { return displayed; }
    @Override public Point  getLocation()           { return rect == null ? new Point(0, 0) : new Point(rect.getX(), rect.getY()); }
    @Override public Dimension getSize()            { return rect == null ? new Dimension(0, 0) : new Dimension(rect.getWidth(), rect.getHeight()); }
    @Override public Rectangle getRect()            { return new Rectangle(getLocation(), getSize()); }
    @Override public String getAttribute(String n)  { return attributes.get(n); }

    // --- Unsupported (phase 3c) ------------------------------------------

    @Override public void click()                        { throw new UnsupportedOperationException(PHASE_3C + " (click)"); }
    @Override public void submit()                       { throw new UnsupportedOperationException(PHASE_3C + " (submit)"); }
    @Override public void sendKeys(CharSequence... k)    { throw new UnsupportedOperationException(PHASE_3C + " (sendKeys)"); }
    @Override public void clear()                        { throw new UnsupportedOperationException(PHASE_3C + " (clear)"); }
    @Override public String getTagName()                 { throw new UnsupportedOperationException(PHASE_3C + " (getTagName)"); }
    @Override public boolean isSelected()                { throw new UnsupportedOperationException(PHASE_3C + " (isSelected)"); }
    @Override public boolean isEnabled()                 { throw new UnsupportedOperationException(PHASE_3C + " (isEnabled)"); }
    @Override public String getText()                    { throw new UnsupportedOperationException(PHASE_3C + " (getText)"); }
    @Override public java.util.List<WebElement> findElements(By by) { throw new UnsupportedOperationException(PHASE_3C + " (findElements)"); }
    @Override public WebElement findElement(By by)       { throw new UnsupportedOperationException(PHASE_3C + " (findElement-nested)"); }
    @Override public String getCssValue(String p)        { throw new UnsupportedOperationException(PHASE_3C + " (getCssValue)"); }
    @Override public <X> X getScreenshotAs(OutputType<X> t) { throw new UnsupportedOperationException(PHASE_3C + " (getScreenshotAs)"); }

    // equals/hashCode — sessionId + elementHandle
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteWebElement)) return false;
        RemoteWebElement that = (RemoteWebElement) o;
        return sessionId.equals(that.sessionId) && elementHandle.equals(that.elementHandle);
    }
    @Override public int hashCode() { return Objects.hash(sessionId, elementHandle); }
    @Override public String toString() { return "RemoteWebElement{session=" + sessionId + ", handle=" + elementHandle + "}"; }
}
```

**Commit:** `feat(core): add RemoteWebElement with cached attributes/rect/displayed`

## Step 3 — Wire `findElement` / `isDisplayed` / `extractAttributes` / `getElementScreenshot`

Remove the phase-3b throws and replace with forwards. These four share the same "cache-backed" logic, so they commit together.

```java
// In RemoteBrowser.java — replace the throwing phase-3b overrides with:

@Override
public WebElement findElement(String xpath) throws WebDriverException {
    ElementState state = client.findElement(sessionId, xpath);
    if (!Boolean.TRUE.equals(state.getFound())) {
        throw new NoSuchElementException("RemoteBrowser: element not found for xpath=" + xpath);
    }
    return new RemoteWebElement(sessionId, state);
}

@Override
public WebElement findWebElementByXpath(String xpath) {
    return findElement(xpath); // Browser.java is identical under the hood
}

@Override
public boolean isDisplayed(String xpath) {
    ElementState state = client.findElement(sessionId, xpath);
    return Boolean.TRUE.equals(state.getFound()) && Boolean.TRUE.equals(state.getDisplayed());
}

@Override
public Map<String, String> extractAttributes(WebElement element) {
    if (!(element instanceof RemoteWebElement)) {
        throw new IllegalStateException(
            "RemoteBrowser.extractAttributes: element was not obtained from this RemoteBrowser session");
    }
    return ((RemoteWebElement) element).cachedAttributes();
}

@Override
public BufferedImage getElementScreenshot(WebElement element) throws IOException {
    if (!(element instanceof RemoteWebElement)) {
        throw new IllegalStateException(
            "RemoteBrowser.getElementScreenshot: element was not obtained from this RemoteBrowser session");
    }
    byte[] bytes = client.captureElementScreenshot(
        sessionId, ((RemoteWebElement) element).getElementHandle());
    return ImageIO.read(new ByteArrayInputStream(bytes));
}
```

Note: `extractAttributes` intentionally does **not** re-hit `/element/find`. The findElement response already carried attributes; serving them from the cache avoids N+1 roundtrips for typical audit flows that enumerate elements. See §14.1 for staleness caveats.

**Commit:** `feat(core): wire RemoteBrowser findElement/isDisplayed/extractAttributes/getElementScreenshot`

## Step 4 — Wire scroll ops via `ScrollMode`

Six Browser scroll methods map 1:1 to six `ScrollMode` enum values, as confirmed in the planning grep:

| Browser.java method | ScrollMode enum value | Extra fields |
|---|---|---|
| `scrollToTopOfPage()` | `TO_TOP` | — |
| `scrollToBottomOfPage()` | `TO_BOTTOM` | — |
| `scrollToElement(String xpath, WebElement)` | `TO_ELEMENT` | `element_handle`, `xpath` (hint) |
| `scrollToElement(WebElement)` | `TO_ELEMENT_CENTERED` | `element_handle` |
| `scrollToElementCentered(WebElement)` | `TO_ELEMENT_CENTERED` | `element_handle` |
| `scrollDownPercent(double)` | `DOWN_PERCENT` | `percent` |
| `scrollDownFull()` | `DOWN_FULL` | — |

One representative override inline; the rest follow the table exactly:

```java
@Override
public void scrollToElementCentered(WebElement element) {
    if (!(element instanceof RemoteWebElement)) {
        throw new IllegalStateException(
            "RemoteBrowser.scrollToElementCentered: element was not obtained from this session");
    }
    client.scrollToElementCentered(sessionId, ((RemoteWebElement) element).getElementHandle());
}
```

Each override still ends by *not* caching a scroll offset — phase-3 `getViewportScrollOffset()` already queries live state.

**Commit:** `feat(core): wire RemoteBrowser scroll ops via ScrollMode`

## Step 5 — Wire DOM-remove + mouse + alert ops

```java
@Override
public void removeElement(String class_name) {
    client.removeDomElement(sessionId, DomRemovePreset.BY_CLASS, class_name);
}

@Override
public void removeDriftChat() {
    client.removeDomElement(sessionId, DomRemovePreset.DRIFT_CHAT, null);
}

@Override
public void removeGDPRmodals() {
    client.removeDomElement(sessionId, DomRemovePreset.GDPR_MODAL, null);
}

@Override
public void removeGDPR() {
    client.removeDomElement(sessionId, DomRemovePreset.GDPR, null);
}

@Override
public void moveMouseOutOfFrame() {
    try { client.moveMouseOutOfFrame(sessionId); }
    catch (BrowsingClientException e) { /* swallow — matches local try/catch */ }
}

@Override
public void moveMouseToNonInteractive(Point point) {
    try { client.moveMouseToNonInteractive(sessionId, point.getX(), point.getY()); }
    catch (BrowsingClientException e) { /* swallow — matches local try/catch */ }
}

@Override
public Alert isAlertPresent() {
    AlertState state = client.getAlert(sessionId);
    if (!Boolean.TRUE.equals(state.getPresent())) return null;
    return new RemoteAlert(client, sessionId, state.getText());
}
```

New file `LookseeCore/looksee-core/src/main/java/com/looksee/services/browser/RemoteAlert.java`:

```java
package com.looksee.services.browser;

import com.looksee.browsing.client.BrowsingClient;
import com.looksee.browsing.generated.model.AlertChoice;
import org.openqa.selenium.Alert;

public final class RemoteAlert implements Alert {
    private final BrowsingClient client;
    private final String sessionId;
    private final String cachedText;

    public RemoteAlert(BrowsingClient client, String sessionId, String cachedText) {
        this.client = client;
        this.sessionId = sessionId;
        this.cachedText = cachedText;
    }

    @Override public String getText() { return cachedText; }
    @Override public void accept()    { client.respondToAlert(sessionId, AlertChoice.ACCEPT, null); }
    @Override public void dismiss()   { client.respondToAlert(sessionId, AlertChoice.DISMISS, null); }
    @Override public void sendKeys(String keys) {
        throw new UnsupportedOperationException(
            "RemoteAlert.sendKeys is deferred to phase 3c (current consumer census has no callers)");
    }
}
```

At this point every `UnsupportedOperationException("phase 3b")` in `RemoteBrowser` is gone — `getDriver()` is the only one that remains, and that's intentional (the whole point of the shim).

**Commit:** `feat(core): wire RemoteBrowser dom-remove + mouse + alert ops`

## Step 6 — New `Browser` methods: `performClick`, `performAction`, `getCurrentUrl`

Three additions to `LookseeCore/looksee-browser/src/main/java/com/looksee/browser/Browser.java`. Local bodies are exactly what `StepExecutor` was doing against `getDriver()` in phase 3; this just gives remote mode a seam to override.

```java
// Browser.java

public void performClick(WebElement element) {
    assert element != null;
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
}

public void performAction(WebElement element, com.looksee.browser.enums.Action action, String input) {
    assert element != null;
    assert action != null;
    // Existing behavior lives in ActionFactory; centralize here so RemoteBrowser can fork.
    new com.looksee.browser.ActionFactory(driver).execAction(element, input == null ? "" : input, action);
}

public String getCurrentUrl() {
    return driver.getCurrentUrl();
}
```

Corresponding overrides in `RemoteBrowser.java`:

```java
@Override
public void performClick(WebElement element) {
    if (!(element instanceof RemoteWebElement)) {
        throw new IllegalStateException("performClick: element not from this RemoteBrowser session");
    }
    client.performElementAction(
        sessionId,
        ((RemoteWebElement) element).getElementHandle(),
        ElementAction.CLICK,
        null);
}

@Override
public void performAction(WebElement element, com.looksee.browser.enums.Action action, String input) {
    if (!(element instanceof RemoteWebElement)) {
        throw new IllegalStateException("performAction: element not from this RemoteBrowser session");
    }
    client.performElementAction(
        sessionId,
        ((RemoteWebElement) element).getElementHandle(),
        toGeneratedAction(action),
        input);
}

@Override
public String getCurrentUrl() {
    // SessionState.current_url is the authoritative post-navigate URL.
    return client.getSession(sessionId).getCurrentUrl();
}
```

`toGeneratedAction(com.looksee.browser.enums.Action)` is a one-line translator — same pattern as the enum translators in phase 3.

**Commit:** `feat(core): add Browser.performClick/performAction/getCurrentUrl with local defaults`

## Step 7 — Surgical `StepExecutor` refactor

Four edits total — three in `StepExecutor.execute`, one in `journeyExecutor/.../AuditController.java:562`.

### 7.1 `StepExecutor.execute` — SimpleStep click

Current (line 45–51):
```java
ElementState element = simple_step.getElementState();
WebElement web_element = browser.findElement(element.getXpath());
browser.scrollToElementCentered(web_element);
//web_element.click();
//ActionFactory action_factory = new ActionFactory(browser.getDriver());
//action_factory.execAction(web_element, "", simple_step.getAction());
((JavascriptExecutor)browser.getDriver()).executeScript("arguments[0].click();", web_element);
```

After:
```java
ElementState element = simple_step.getElementState();
WebElement web_element = browser.findElement(element.getXpath());
browser.scrollToElementCentered(web_element);
browser.performClick(web_element);
```

### 7.2 `StepExecutor.execute` — LoginStep branch

Current (lines 52–65):
```java
LoginStep login_step = (LoginStep)step;
WebElement username_element = browser.getDriver().findElement(By.xpath(login_step.getUsernameElement().getXpath()));
ActionFactory action_factory = new ActionFactory(browser.getDriver());
action_factory.execAction(username_element, login_step.getTestUser().getUsername(), com.looksee.browser.enums.Action.SEND_KEYS);

WebElement password_element = browser.getDriver().findElement(By.xpath(login_step.getPasswordElement().getXpath()));
action_factory.execAction(password_element, login_step.getTestUser().getPassword(), com.looksee.browser.enums.Action.SEND_KEYS);

WebElement submit_element = browser.getDriver().findElement(By.xpath(login_step.getSubmitElement().getXpath()));
action_factory.execAction(submit_element, "", com.looksee.browser.enums.Action.CLICK);
```

After:
```java
LoginStep login_step = (LoginStep)step;
WebElement username_element = browser.findElement(login_step.getUsernameElement().getXpath());
browser.performAction(username_element, com.looksee.browser.enums.Action.SEND_KEYS, login_step.getTestUser().getUsername());

WebElement password_element = browser.findElement(login_step.getPasswordElement().getXpath());
browser.performAction(password_element, com.looksee.browser.enums.Action.SEND_KEYS, login_step.getTestUser().getPassword());

WebElement submit_element = browser.findElement(login_step.getSubmitElement().getXpath());
browser.performAction(submit_element, com.looksee.browser.enums.Action.CLICK, "");
```

### 7.3 `StepExecutor.execute` — exception handler URL read

Current (line 79):
```java
log.error("failed after url transition to {}", browser.getDriver().getCurrentUrl());
```

After:
```java
log.error("failed after url transition to {}", browser.getCurrentUrl());
```

### 7.4 `journeyExecutor/.../AuditController.java:562`

Current:
```java
String currentUrl = browser.getDriver().getCurrentUrl();
```

After:
```java
String currentUrl = browser.getCurrentUrl();
```

**Commit:** `refactor(core): route StepExecutor + journeyExecutor through Browser methods`

The existing `StepExecutorTest` must pass unchanged — the local bodies of the new `Browser` methods preserve every observable behavior. See test plan §9.

## Step 8 — Remote `capturePage` explicit lifecycle

`BrowserService.capturePage` in 0.6.0 used `POST /capture` + `GET /capture/{id}/screenshot` — only one screenshot, shared between viewport and full-page fields. Replace the remote branch:

```java
// BrowserService.capturePage — remote branch

// Before (0.6.0):
CaptureRequest req = new CaptureRequest()
    .url(java.net.URI.create(url.toString()))
    .browser(com.looksee.browsing.generated.model.BrowserType.fromValue(browser.toString()))
    .extract(java.util.List.of(CaptureRequest.ExtractEnum.SOURCE));
CaptureResponse resp = browsingClient.capture(req);
byte[] screenshotBytes = browsingClient.getCaptureScreenshotBytes(resp.getCaptureId());
return pageStateAdapter.toPageState(
    screenshotBytes, sourceOrEmpty, audit_record_id, url.toString(), browser);

// After (0.7.0):
Session session = browsingClient.createSession(browser, BrowserEnvironment.DISCOVERY);
String sessionId = session.getSessionId();
try {
    browsingClient.navigate(sessionId, url.toString());
    String source = browsingClient.getSource(sessionId);
    byte[] viewportBytes = browsingClient.screenshot(sessionId, ScreenshotStrategy.VIEWPORT);
    byte[] fullPageBytes = browsingClient.screenshot(sessionId, ScreenshotStrategy.FULL_PAGE_SHUTTERBUG);
    return pageStateAdapter.toPageState(
        viewportBytes, fullPageBytes, source, audit_record_id, url.toString(), browser);
} finally {
    try { browsingClient.deleteSession(sessionId); }
    catch (BrowsingClientException e) { log.warn("capturePage: deleteSession({}) failed; swallowing", sessionId, e); }
}
```

That requires a new `PageStateAdapter.toPageState` overload accepting **two** byte arrays:

```java
public PageState toPageState(byte[] viewportScreenshot,
                             byte[] fullPageScreenshot,
                             String source,
                             long audit_record_id,
                             String browser_url,
                             BrowserType browser_type) throws IOException { … }
```

The existing 5-arg overload (added in phase 3) can delegate — pass the same bytes twice — so it stays around for anything that really did only have one screenshot.

Local-mode `capturePage` stays byte-identical to 0.6.0.

**Commit:** `feat(core): remote capturePage uses explicit lifecycle for viewport + full-page`

## Step 9 — Tests

### 9.1 `looksee-browsing-client`

Extend `BrowsingClientTest` with one forward-case-per-new-facade-method. Target ~15 new test cases. Mock each new `*Api`, assert sessionId + enum translation + element_handle passthrough. No HTTP.

### 9.2 `looksee-core`

Four new test classes:

- **`RemoteWebElementTest`** — cache-backed methods return without touching any client (ideally zero-dep test, no BrowsingClient mock needed); every unsupported `WebElement` method throws with an expected phase-3c pointer; `equals`/`hashCode` honor `sessionId + elementHandle` across two instances built from the same `ElementState`.
- **`RemoteBrowserElementOpsTest`** — one test per previously-throwing override: correct facade method called, correct enum translation, correct element_handle forwarded. `extractAttributes` + `isDisplayed` (on a `RemoteWebElement`) both trigger **zero** `BrowsingClient` calls — verify with `verifyNoInteractions(client)`.
- **`RemoteBrowserCaptureTest`** — remote `capturePage` issues exactly `createSession → navigate → getSource → screenshot(VIEWPORT) → screenshot(FULL_PAGE_SHUTTERBUG) → deleteSession`. Use `InOrder` verification. Resulting `PageState` has distinct `viewport_screenshot_url` vs `full_page_screenshot_url`.
- **`StepExecutorRemoteModeTest`** — hand StepExecutor a `RemoteBrowser` with a mocked `BrowsingClient`; run through a SimpleStep and a LoginStep; verify (a) no `IllegalStateException` from stray `(RemoteWebElement)` casts, (b) the expected `performElementAction` calls with the right handles + actions, (c) no leaked `Actions` or raw-driver access (nothing called `client.executeScript` — that endpoint stays unused).

Existing tests that must pass unchanged:
- `BrowserServiceTest`, `BrowserServiceLocalModeTest`, `BrowserServiceModeForkTest`, `PageStateAdapterTest` (if any), `StepExecutorTest` (existing local-mode regression), `RemoteBrowserTest` (phase 3 — every test still valid; the unsupported-method tests either disappear or get re-purposed to verify the new success paths).

**Commit:** `test(core): RemoteWebElement + element-handle overrides + StepExecutor local regression`

And separately:

**Commit:** `test(browsing-client): cover facade extensions`

## Step 10 — Version bump + CHANGELOG

- `A11yParent` 0.6.0 → 0.7.0 across every pom (same `mvn versions:set -DnewVersion=0.7.0 -DgenerateBackupPoms=false -DprocessAllModules=true` pattern as phase 3; also manually bump `<looksee.version>0.6.0</looksee.version>` in the root pom).
- `LOOKSEE_CORE_VERSION` at repo root → `0.7.0`.
- `LookseeCore/CHANGELOG.md` new `## [0.7.0]` entry summarizing:
  - Remote element-handle ops wired up; every phase-3b `UnsupportedOperationException` removed.
  - New `RemoteWebElement` + `RemoteAlert`.
  - Three new `Browser` methods: `performClick`, `performAction`, `getCurrentUrl`.
  - `StepExecutor` no longer reaches through `getDriver()`.
  - Remote `capturePage` now stores distinct viewport and full-page screenshots.
  - Default mode still `local`; zero behavior change for consumers that don't opt in.
  - `journeyExecutor` consumer: one-line fix so remote journey execution works.

Also bump every consumer service pom that pins LookseeCore (same sweep phase 3 needed): `AuditManager`, `audit-service`, `contentAudit`, `CrawlerAPI`, `PageBuilder`, `visualDesignAudit`, `informationArchitectureAudit`, `journeyExecutor`, `journeyExpander`, `journey-map-cleanup`, `journeyErrors`, `element-enrichment`, `look-see-front-end-broadcaster`. Use the same sed pattern phase 3 used for the version-pin CI check.

**Commit:** `chore: bump LookseeCore to 0.7.0 + CHANGELOG`

## Step 11 — Verification

1. **Clean build from LookseeCore root:**
   ```bash
   cd LookseeCore && mvn -q clean verify
   ```
   Expect: `BUILD SUCCESS`. Test count: ~170 (was 119 in 0.6.0). Any silent drop means a test fell off — investigate before merging.

2. **Local-mode regression:** spin up any consumer (PageBuilder is easiest) against the fresh 0.7.0 with no `looksee.browsing.*` config. Run its existing happy-path integration. Behavior must be indistinguishable from 0.6.0.

3. **Remote-mode sanity (optional, needs a running browser-service):** same script as phase 3 §10.3, plus:
   - `findElement` on a visible button returns a `WebElement` whose `isDisplayed()` is true without a second network call.
   - `performClick` on that element navigates as expected.
   - `capturePage` produces a `PageState` where `full_page_screenshot_url ≠ viewport_screenshot_url` and both files open as valid PNGs.
   - A login journey through StepExecutor against a test site completes end-to-end with `mode=remote`.

4. **Scope-preservation check:**
   ```bash
   git diff --stat main..HEAD -- LookseeCore/looksee-browser/
   # Expect: only Browser.java (three new methods) + pom.xml parent-version bump.
   git diff --stat main..HEAD -- journeyExecutor/
   # Expect: only AuditController.java one-line change + pom.xml bump.
   ```

## Definition of done

- [ ] `BrowsingClient` exposes wrappers over `ElementsApi`, `TouchApi`, `DomApi`, `MouseApi`, `AlertsApi`. `capture`/`getCaptureScreenshotBytes` retired.
- [ ] `RemoteWebElement` + `RemoteAlert` exist in `looksee-core`; cache-backed reads served without network; unsupported methods throw with phase-3c markers.
- [ ] Every `UnsupportedOperationException("phase 3b")` in `RemoteBrowser` is gone; only `getDriver()` throws (intentionally).
- [ ] `Browser.performClick`, `Browser.performAction`, `Browser.getCurrentUrl` exist with local default bodies; `RemoteBrowser` overrides all three.
- [ ] `StepExecutor` has no raw `browser.getDriver()` calls; `journeyExecutor/.../AuditController.java:562` likewise.
- [ ] Remote `capturePage` uses explicit lifecycle; resulting `PageState` has distinct viewport vs. full-page screenshots.
- [ ] All existing 0.6.0 tests pass unchanged. New tests added per §9.
- [ ] `A11yParent` at 0.7.0 across every pom; all consumer services pinned to 0.7.0 (version-consistency CI check passes).
- [ ] PR opened against `main` with title **"Phase 3b: remote element-handle ops + StepExecutor refactor"**.

## Push and open PR

```bash
git push -u origin phase-3b/element-handle-ops
```

PR body (same template as phase 3): summary of commit clusters, `mvn verify` snippet, explicit note that default mode is still local, link to this doc and to `phase-3-looksee-shim.md`.

## 14. Open items flagged for reviewer

1. **`RemoteWebElement` cache staleness.** `isDisplayed` and `extractAttributes` serve the findElement response without re-hitting the server. If the DOM mutates after findElement, the remote read disagrees with local `Browser`'s live behavior. Local re-queries aren't *quite* live either — the `driver` is still a snapshot at the method's start-of-call — but the window is tighter. Two options:
   - **(a)** Ship cache-only for 0.7.0; document the behavior difference in the CHANGELOG and RemoteWebElement javadoc. Revisit if any consumer surfaces a mismatch. **Recommended.**
   - **(b)** Add `RemoteWebElement.refresh()` that re-hits `/element/find` and returns a new cached copy. Leave the default reads cached.
2. **`RemoteWebElement` unsupported method surface — phase-3c inherit list.** Every `UnsupportedOperationException` in `RemoteWebElement.java` is a phase-3c candidate. The list, verbatim: `click`, `submit`, `sendKeys(CharSequence...)`, `clear`, `getTagName`, `isSelected`, `isEnabled`, `getText`, `findElements(By)`, `findElement(By)`, `getCssValue`, `getScreenshotAs`. Plus `RemoteAlert.sendKeys(String)`. Phase 3c should keep this list as its in-scope task list.
3. **`/execute` endpoint stays unused in 3b.** Every StepExecutor path routes through dedicated endpoints. If a future consumer really needs arbitrary JS exec (e.g. for pulling a CSP nonce), add a `BrowsingClient.executeScript` facade wrapper and a `Browser.executeScript(String, Object...)` method in a separate phase rather than retrofitting.
4. **`Actions` API usage.** The Step 0 grep confirmed only `StepExecutor` builds an `Actions` chain (indirectly via `ActionFactory`). If another consumer ever does so (e.g. for drag-and-drop), extend `Browser.performAction` with additional enum values rather than re-exposing `getDriver()`. This is a guardrail for future reviewers, not an action item now.
5. **OpenAPI spec drift.** Same caveat as phase-3 §14.1. The spec copy in `LookseeCore/looksee-browsing-client/src/main/resources/openapi.yaml` must be re-synced from `brandonkindred/browser-service:openapi.yaml` whenever an element/scroll/dom/mouse/alert endpoint is added or renamed. Promote to a published Maven artifact once browser-service stabilizes release cadence.
6. **Retries / backoff on the facade.** Phase 3 deferred retries; 3b's higher chattiness (every scroll-to-element is a round trip) raises the blast radius of a transient failure. Not a blocker for merging 0.7.0, but the first consumer that sees flakes in production should get this as the immediate next change, centralized in `BrowsingClient` rather than per-caller.
7. **`StepExecutor` remote verification.** Live-service end-to-end verification for a journey with LoginStep requires a running browser-service + a real login target. Document the manual steps in the PR description; don't make them a CI gate for 3b merge (too fragile).
8. **`Browser.performAction` default implementation reaches into `ActionFactory`.** `ActionFactory` is in `com.looksee.browser` and currently only instantiable via `new ActionFactory(driver)`. The local-mode body does exactly what StepExecutor used to inline — no behavior change — but it does centralize a small amount of logic. If this becomes the "one place ActionFactory is called from" and StepExecutor grows new actions, consider folding `ActionFactory` into `Browser.performAction` as a later cleanup. Out of scope now.
