package com.looksee.services.browser;

import static org.junit.jupiter.api.Assertions.*;

import com.looksee.browsing.generated.model.ElementState;
import com.looksee.browsing.generated.model.Rect;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;

class RemoteWebElementTest {

    private static ElementState state(String handle, boolean displayed, Map<String, String> attrs, Rect rect) {
        ElementState s = new ElementState()
            .elementHandle(handle)
            .found(true)
            .displayed(displayed)
            .attributes(attrs);
        if (rect != null) {
            s.setRect(rect);
        }
        return s;
    }

    @Test
    void isDisplayed_servedFromCacheWithoutNetwork() {
        RemoteWebElement el = new RemoteWebElement("s1",
            state("h1", true, Map.of(), null));
        assertTrue(el.isDisplayed());
    }

    @Test
    void getLocationAndSize_derivedFromRect() {
        RemoteWebElement el = new RemoteWebElement("s1",
            state("h1", true, Map.of(),
                new Rect().x(10).y(20).width(100).height(50)));
        assertEquals(10, el.getLocation().getX());
        assertEquals(20, el.getLocation().getY());
        assertEquals(100, el.getSize().getWidth());
        assertEquals(50, el.getSize().getHeight());
    }

    @Test
    void getRect_composesLocationAndSize() {
        RemoteWebElement el = new RemoteWebElement("s1",
            state("h1", true, Map.of(),
                new Rect().x(1).y(2).width(3).height(4)));
        assertEquals(1, el.getRect().getX());
        assertEquals(2, el.getRect().getY());
        assertEquals(3, el.getRect().getWidth());
        assertEquals(4, el.getRect().getHeight());
    }

    @Test
    void missingRect_returnsZeroLocation() {
        RemoteWebElement el = new RemoteWebElement("s1", state("h1", true, Map.of(), null));
        assertEquals(0, el.getLocation().getX());
        assertEquals(0, el.getSize().getWidth());
    }

    @Test
    void getAttribute_readsCachedMap() {
        RemoteWebElement el = new RemoteWebElement("s1",
            state("h1", true, Map.of("id", "submit", "class", "btn primary"), null));
        assertEquals("submit", el.getAttribute("id"));
        assertEquals("btn primary", el.getAttribute("class"));
        assertNull(el.getAttribute("missing"));
    }

    @Test
    void nullAttributes_treatedAsEmpty() {
        RemoteWebElement el = new RemoteWebElement("s1",
            new ElementState().elementHandle("h1").found(true));
        assertNull(el.getAttribute("any"));
    }

    @Test
    void equality_sessionIdPlusElementHandle() {
        RemoteWebElement a = new RemoteWebElement("s1", state("h1", false, Map.of(), null));
        RemoteWebElement b = new RemoteWebElement("s1", state("h1", true, Map.of("x", "y"), null));
        RemoteWebElement c = new RemoteWebElement("s1", state("h2", false, Map.of(), null));
        RemoteWebElement d = new RemoteWebElement("s2", state("h1", false, Map.of(), null));

        assertEquals(a, b, "same sessionId + elementHandle → equal regardless of cached state");
        assertNotEquals(a, c, "different handle → not equal");
        assertNotEquals(a, d, "different sessionId → not equal");
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void unsupportedWebElementMethods_allThrowWithPhase3cMarker() {
        // getTagName moved out of the always-unsupported set in phase 3d —
        // it's now graceful: returns a cached tag_name attribute when present
        // and throws with a phase-3d-pointing message otherwise. Covered in
        // its own test cases below.
        RemoteWebElement el = new RemoteWebElement("s1", state("h1", true, Map.of(), null));

        Runnable[] checks = new Runnable[] {
            () -> el.click(),
            () -> el.submit(),
            () -> el.sendKeys("x"),
            () -> el.clear(),
            () -> el.isSelected(),
            () -> el.isEnabled(),
            () -> el.getText(),
            () -> el.findElements(By.xpath("//*")),
            () -> el.findElement(By.xpath("//*")),
            () -> el.getCssValue("color"),
            () -> el.getScreenshotAs(OutputType.BYTES),
        };
        for (Runnable r : checks) {
            UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, r::run);
            assertTrue(ex.getMessage().contains("phase 3c"),
                "message should point at phase 3c: " + ex.getMessage());
        }
    }

    @Test
    void getTagName_readsCachedTagNameAttribute() {
        // Phase 3d: when the server includes a synthetic tag_name in the
        // findElement response's attributes map, RemoteWebElement.getTagName
        // returns it without a round-trip.
        RemoteWebElement el = new RemoteWebElement("s1",
            state("h1", true, Map.of("tag_name", "div"), null));
        assertEquals("div", el.getTagName());
    }

    @Test
    void getTagName_throwsWhenAttributeAbsent() {
        // Browser-service today does NOT synthesize tag_name. Confirms the
        // throw still fires with an actionable message pointing at the
        // phase-3d xpath workaround.
        RemoteWebElement el = new RemoteWebElement("s1",
            state("h1", true, Map.of("id", "submit"), null));
        UnsupportedOperationException ex = assertThrows(
            UnsupportedOperationException.class, el::getTagName);
        assertTrue(ex.getMessage().contains("tag_name"),
            "message should mention the missing tag_name attribute: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("extractTagFromXpath"),
            "message should point at the workaround: " + ex.getMessage());
    }

    @Test
    void nullSessionId_throws() {
        assertThrows(NullPointerException.class,
            () -> new RemoteWebElement(null, state("h1", true, Map.of(), null)));
    }

    @Test
    void nullElementHandle_throws() {
        assertThrows(NullPointerException.class,
            () -> new RemoteWebElement("s1", new ElementState().found(true)));
    }
}
