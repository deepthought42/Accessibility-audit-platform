package com.looksee.services.browser;

import com.looksee.browsing.generated.model.ElementState;
import com.looksee.browsing.generated.model.Rect;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/**
 * {@link WebElement} bound to a remote browser-service session. Holds the opaque
 * {@code element_handle} returned by {@code POST /v1/sessions/{id}/element/find}
 * plus the {@code rect}, {@code attributes}, and {@code displayed} fields the
 * server returned at find time. Cached reads are served locally; every
 * mutating op routes through {@link RemoteBrowser} + {@code BrowsingClient}.
 *
 * <p>Equality is {@code sessionId + elementHandle} — matches Selenium's
 * contract of "two references to the same DOM node are equal" without
 * requiring a live driver.
 *
 * <p><b>Cache staleness.</b> {@link #isDisplayed()} and {@link #getAttribute(String)}
 * serve the findElement-response snapshot. If the DOM mutates between find and
 * read, remote and local disagree. Phase-3b ships cache-only; a follow-up can
 * add a {@code refresh()} if staleness surfaces in practice. See
 * {@code browser-service/phase-3b-element-handle-ops.md} §14.1.
 *
 * <p><b>Unsupported WebElement methods.</b> Every method that throws
 * {@link UnsupportedOperationException} below is a phase-3c candidate. The
 * current consumer census routes through {@link RemoteBrowser} /
 * {@code Browser.performClick} / {@code performAction} instead of calling
 * these directly, so they're only reachable via code outside the census —
 * which would be a new finding to reconcile in phase 3c.
 */
public final class RemoteWebElement implements WebElement {

    private static final String PHASE_3C =
        "RemoteWebElement: this WebElement op is deferred to phase 3c "
        + "(route through Browser.performClick / performAction / extractAttributes / "
        + "getElementScreenshot / scrollToElement instead)";

    private final String sessionId;
    private final String elementHandle;
    private final Rect rect;                      // may be null if server omitted
    private final Map<String, String> attributes; // never null, immutable
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

    public String getSessionId()     { return sessionId; }
    public String getElementHandle() { return elementHandle; }

    /** Package-private: used by {@link RemoteBrowser#extractAttributes(WebElement)}. */
    Map<String, String> cachedAttributes() { return attributes; }

    // --- Cache-backed WebElement methods ---------------------------------

    @Override public boolean isDisplayed() { return displayed; }

    @Override public Point getLocation() {
        return rect == null ? new Point(0, 0) : new Point(rect.getX(), rect.getY());
    }

    @Override public Dimension getSize() {
        return rect == null ? new Dimension(0, 0) : new Dimension(rect.getWidth(), rect.getHeight());
    }

    @Override public Rectangle getRect() {
        Point p = getLocation();
        Dimension d = getSize();
        return new Rectangle(p, d);
    }

    @Override public String getAttribute(String name) {
        return attributes.get(name);
    }

    // --- Unsupported (phase 3c) ------------------------------------------

    @Override public void click()                           { throw new UnsupportedOperationException(PHASE_3C + " (click)"); }
    @Override public void submit()                          { throw new UnsupportedOperationException(PHASE_3C + " (submit)"); }
    @Override public void sendKeys(CharSequence... keys)    { throw new UnsupportedOperationException(PHASE_3C + " (sendKeys)"); }
    @Override public void clear()                           { throw new UnsupportedOperationException(PHASE_3C + " (clear)"); }
    @Override public String getTagName()                    { throw new UnsupportedOperationException(PHASE_3C + " (getTagName)"); }
    @Override public boolean isSelected()                   { throw new UnsupportedOperationException(PHASE_3C + " (isSelected)"); }
    @Override public boolean isEnabled()                    { throw new UnsupportedOperationException(PHASE_3C + " (isEnabled)"); }
    @Override public String getText()                       { throw new UnsupportedOperationException(PHASE_3C + " (getText)"); }
    @Override public java.util.List<WebElement> findElements(By by) { throw new UnsupportedOperationException(PHASE_3C + " (findElements)"); }
    @Override public WebElement findElement(By by)          { throw new UnsupportedOperationException(PHASE_3C + " (findElement-nested)"); }
    @Override public String getCssValue(String propertyName){ throw new UnsupportedOperationException(PHASE_3C + " (getCssValue)"); }
    @Override public <X> X getScreenshotAs(OutputType<X> t) { throw new UnsupportedOperationException(PHASE_3C + " (getScreenshotAs)"); }

    // --- Identity --------------------------------------------------------

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteWebElement)) return false;
        RemoteWebElement that = (RemoteWebElement) o;
        return sessionId.equals(that.sessionId) && elementHandle.equals(that.elementHandle);
    }

    @Override public int hashCode() { return Objects.hash(sessionId, elementHandle); }

    @Override public String toString() {
        return "RemoteWebElement{session=" + sessionId + ", handle=" + elementHandle + "}";
    }
}
