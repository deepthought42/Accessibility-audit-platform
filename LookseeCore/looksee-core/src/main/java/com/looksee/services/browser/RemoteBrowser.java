package com.looksee.services.browser;

import com.looksee.browser.Browser;
import com.looksee.browsing.client.BrowsingClient;
import com.looksee.browsing.client.BrowsingClientException;
import com.looksee.browsing.generated.model.PageStatus;
import com.looksee.browsing.generated.model.ScreenshotStrategy;
import com.looksee.browsing.generated.model.ScrollOffset;
import com.looksee.browsing.generated.model.Viewport;
import com.looksee.browsing.generated.model.ViewportState;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openqa.selenium.Alert;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

/**
 * {@link Browser} implementation that forwards page-level operations to
 * brandonkindred/browser-service via {@link BrowsingClient}.
 *
 * <p>This lives in {@code looksee-core} (not {@code looksee-browser}) so the
 * engine module stays pure-local. In remote-only deployments
 * {@code looksee-browser}'s Selenium transitive deps still load but no local
 * {@link WebDriver} is ever constructed — this class passes a null driver to
 * super and overrides every method that would otherwise dereference it.
 *
 * <p><b>Scope:</b> page-level ops only (navigate, screenshot, source, close,
 * status, viewport). Element-handle ops (findElement, extractAttributes,
 * removeDriftChat, scroll helpers, …) throw {@link UnsupportedOperationException}
 * — those are phase 3b. Consumers that exercise those paths must stay on
 * {@code looksee.browsing.mode=local} until 3b ships.
 */
public class RemoteBrowser extends Browser {

    private static final Logger log = LoggerFactory.getLogger(RemoteBrowser.class);
    private static final String PHASE_3B = "RemoteBrowser: element-handle ops are deferred to phase 3b";

    private final BrowsingClient client;
    private final String sessionId;
    private final String browserName;

    public RemoteBrowser(BrowsingClient client, String sessionId, String browserName) {
        super(); // Browser is @NoArgsConstructor; driver field stays null
        this.client = client;
        this.sessionId = sessionId;
        this.browserName = browserName;
    }

    public String getSessionId() {
        return sessionId;
    }

    // --- Page-level forwards ----------------------------------------------

    @Override
    public void navigateTo(String url) {
        client.navigate(sessionId, url);
    }

    @Override
    public void close() {
        // Mirror Browser.close() semantics: callers invoke this from finally
        // blocks, so a transient cleanup failure must not mask the original
        // exception or break retry/control flow. Log and move on.
        try {
            client.deleteSession(sessionId);
        } catch (BrowsingClientException e) {
            log.warn("RemoteBrowser.close: deleteSession({}) failed; swallowing", sessionId, e);
        }
    }

    @Override
    public String getSource() {
        return client.getSource(sessionId);
    }

    @Override
    public boolean is503Error() {
        PageStatus s = client.getStatus(sessionId);
        return Boolean.TRUE.equals(s.getIs503());
    }

    @Override
    public BufferedImage getViewportScreenshot() throws IOException {
        return readPng(client.screenshot(sessionId, ScreenshotStrategy.VIEWPORT));
    }

    @Override
    public BufferedImage getFullPageScreenshot() throws IOException {
        // No dedicated "plain full_page" strategy in the contract; use
        // shutterbug as the canonical full-page capture.
        return readPng(client.screenshot(sessionId, ScreenshotStrategy.FULL_PAGE_SHUTTERBUG));
    }

    @Override
    public BufferedImage getFullPageScreenshotAshot() throws IOException {
        return readPng(client.screenshot(sessionId, ScreenshotStrategy.FULL_PAGE_ASHOT));
    }

    @Override
    public BufferedImage getFullPageScreenshotShutterbug() throws IOException {
        return readPng(client.screenshot(sessionId, ScreenshotStrategy.FULL_PAGE_SHUTTERBUG));
    }

    @Override
    public Point getViewportScrollOffset() {
        ScrollOffset off = client.getViewport(sessionId).getScrollOffset();
        return new Point(off.getX(), off.getY());
    }

    @Override
    public void waitForPageToLoad() {
        // The service's /navigate endpoint blocks until document.readyState is
        // "complete"; no client-side wait needed. Left as an override so the
        // super-method (which dereferences the null driver) is never called.
    }

    // --- Lombok @Getter overrides (super reads the null driver) ----------

    @Override
    public WebDriver getDriver() {
        throw new UnsupportedOperationException(
            "RemoteBrowser has no local WebDriver — operate via BrowsingClient");
    }

    @Override
    public String getBrowserName() {
        return browserName;
    }

    @Override
    public Dimension getViewportSize() {
        Viewport v = client.getViewport(sessionId).getViewport();
        return new Dimension(v.getWidth(), v.getHeight());
    }

    @Override
    public long getXScrollOffset() {
        return client.getViewport(sessionId).getScrollOffset().getX();
    }

    @Override
    public long getYScrollOffset() {
        return client.getViewport(sessionId).getScrollOffset().getY();
    }

    // --- Element-handle ops: phase 3b -------------------------------------

    @Override
    public WebElement findElement(String xpath) throws WebDriverException {
        throw new UnsupportedOperationException(PHASE_3B + " (findElement)");
    }

    @Override
    public WebElement findWebElementByXpath(String xpath) {
        throw new UnsupportedOperationException(PHASE_3B + " (findWebElementByXpath)");
    }

    @Override
    public boolean isDisplayed(String xpath) {
        throw new UnsupportedOperationException(PHASE_3B + " (isDisplayed)");
    }

    @Override
    public Map<String, String> extractAttributes(WebElement element) {
        throw new UnsupportedOperationException(PHASE_3B + " (extractAttributes)");
    }

    @Override
    public BufferedImage getElementScreenshot(WebElement element) throws Exception {
        throw new UnsupportedOperationException(PHASE_3B + " (getElementScreenshot)");
    }

    @Override
    public void scrollToElement(String xpath, WebElement elem) {
        throw new UnsupportedOperationException(PHASE_3B + " (scrollToElement)");
    }

    @Override
    public void scrollToElement(WebElement element) {
        throw new UnsupportedOperationException(PHASE_3B + " (scrollToElement)");
    }

    @Override
    public void scrollToElementCentered(WebElement element) {
        throw new UnsupportedOperationException(PHASE_3B + " (scrollToElementCentered)");
    }

    @Override
    public void scrollToBottomOfPage() {
        throw new UnsupportedOperationException(PHASE_3B + " (scrollToBottomOfPage)");
    }

    @Override
    public void scrollToTopOfPage() {
        throw new UnsupportedOperationException(PHASE_3B + " (scrollToTopOfPage)");
    }

    @Override
    public void scrollDownPercent(double percent) {
        throw new UnsupportedOperationException(PHASE_3B + " (scrollDownPercent)");
    }

    @Override
    public void scrollDownFull() {
        throw new UnsupportedOperationException(PHASE_3B + " (scrollDownFull)");
    }

    @Override
    public void removeElement(String className) {
        throw new UnsupportedOperationException(PHASE_3B + " (removeElement)");
    }

    @Override
    public void removeDriftChat() {
        throw new UnsupportedOperationException(PHASE_3B + " (removeDriftChat)");
    }

    @Override
    public void removeGDPRmodals() {
        throw new UnsupportedOperationException(PHASE_3B + " (removeGDPRmodals)");
    }

    @Override
    public void removeGDPR() {
        throw new UnsupportedOperationException(PHASE_3B + " (removeGDPR)");
    }

    @Override
    public void moveMouseOutOfFrame() {
        throw new UnsupportedOperationException(PHASE_3B + " (moveMouseOutOfFrame)");
    }

    @Override
    public void moveMouseToNonInteractive(Point point) {
        throw new UnsupportedOperationException(PHASE_3B + " (moveMouseToNonInteractive)");
    }

    @Override
    public Alert isAlertPresent() {
        throw new UnsupportedOperationException(PHASE_3B + " (isAlertPresent)");
    }

    // --- helpers ----------------------------------------------------------

    private static BufferedImage readPng(byte[] bytes) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            return ImageIO.read(in);
        }
    }
}
