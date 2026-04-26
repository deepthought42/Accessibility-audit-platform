package com.looksee.services.browser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.looksee.browsing.client.BrowsingClient;
import com.looksee.browsing.generated.model.ElementAction;
import com.looksee.browsing.generated.model.ElementState;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.OutputType;

/**
 * Phase 3f: every previously-deferred WebElement method on RemoteWebElement
 * (except the two findElement(s)(By) overloads) now routes through the
 * BrowsingClient facade. These tests verify the routing — script content,
 * enum translation, output-type handling, etc.
 */
class RemoteWebElementWiredMethodsTest {

    private BrowsingClient client;
    private RemoteWebElement el;

    @BeforeEach
    void setUp() {
        client = mock(BrowsingClient.class);
        ElementState s = new ElementState()
            .elementHandle("h1").found(true).displayed(true).attributes(Map.of());
        el = new RemoteWebElement("s1", "//button", s, client);
    }

    // --- click + sendKeys → /element/action -------------------------------

    @Test
    void click_routesPerformElementActionClick() {
        el.click();
        verify(client).performElementAction("s1", "h1", ElementAction.CLICK, null);
    }

    @Test
    void sendKeys_concatsArgsAndForwards() {
        el.sendKeys("hello", " ", "world");
        verify(client).performElementAction("s1", "h1", ElementAction.SEND_KEYS, "hello world");
    }

    @Test
    void sendKeys_nullArrayThrowsIllegalArgument() {
        // Selenium contract: null keys array throws. Don't silently no-op
        // (PR #54 review).
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> el.sendKeys((CharSequence[]) null));
        assertTrue(ex.getMessage().contains("not null"),
            "error should mention nulls being rejected: " + ex.getMessage());
        verify(client, never()).performElementAction(any(), any(), any(), any());
    }

    @Test
    void sendKeys_nullElementThrowsIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> el.sendKeys("ok", null, "more"));
        assertTrue(ex.getMessage().contains("not null"),
            "error should mention nulls being rejected: " + ex.getMessage());
        verify(client, never()).performElementAction(any(), any(), any(), any());
    }

    // --- submit + clear → /execute ----------------------------------------

    @Test
    void submit_prefersRequestSubmitOverSubmit() {
        // PR #54 review: prefer form.requestSubmit() over form.submit() so
        // submit handlers fire and constraint validation runs. Falls back
        // to .submit() only if requestSubmit isn't available.
        el.submit();
        ArgumentCaptor<String> scriptCap = ArgumentCaptor.forClass(String.class);
        verify(client).executeScript(eq("s1"), scriptCap.capture(), any());
        String script = scriptCap.getValue();
        assertTrue(script.contains("requestSubmit"),
            "submit script must prefer requestSubmit() to fire submit events: " + script);
    }

    @Test
    void clear_setsValueAndDispatchesInputEvent() {
        el.clear();
        ArgumentCaptor<String> scriptCap = ArgumentCaptor.forClass(String.class);
        verify(client).executeScript(eq("s1"), scriptCap.capture(), any());
        String script = scriptCap.getValue();
        assertTrue(script.contains("el.value = ''"),
            "clear must set value to empty: " + script);
        assertTrue(script.contains("dispatchEvent(new Event('input'"),
            "clear must dispatch input event for framework parity: " + script);
    }

    // --- DOM property reads → /execute ------------------------------------

    @Test
    void getText_returnsTextContent() {
        when(client.executeScript(eq("s1"), any(), any())).thenReturn("Hello, world");
        assertEquals("Hello, world", el.getText());
    }

    @Test
    void getText_returnsEmptyStringOnNullResult() {
        when(client.executeScript(eq("s1"), any(), any())).thenReturn(null);
        assertEquals("", el.getText());
    }

    @Test
    void isSelected_trueWhenServerReturnsTrue() {
        when(client.executeScript(eq("s1"), any(), any())).thenReturn(Boolean.TRUE);
        assertTrue(el.isSelected());
    }

    @Test
    void isSelected_falseWhenServerReturnsFalseOrNull() {
        when(client.executeScript(eq("s1"), any(), any())).thenReturn(Boolean.FALSE);
        assertFalse(el.isSelected());
        when(client.executeScript(eq("s1"), any(), any())).thenReturn(null);
        assertFalse(el.isSelected());
    }

    @Test
    void isEnabled_trueWhenServerReturnsTrue() {
        when(client.executeScript(eq("s1"), any(), any())).thenReturn(Boolean.TRUE);
        assertTrue(el.isEnabled());
    }

    @Test
    void isEnabled_defaultsTrueOnNullResult() {
        // Selenium convention: assume enabled when uncertain.
        when(client.executeScript(eq("s1"), any(), any())).thenReturn(null);
        assertTrue(el.isEnabled());
    }

    @Test
    void isEnabled_falseOnlyWhenServerReturnsExplicitFalse() {
        when(client.executeScript(eq("s1"), any(), any())).thenReturn(Boolean.FALSE);
        assertFalse(el.isEnabled());
    }

    @Test
    void getCssValue_passesPropertyNameAsArg() {
        when(client.executeScript(eq("s1"), any(), any())).thenReturn("rgb(0, 0, 0)");
        ArgumentCaptor<List<Object>> argsCap = ArgumentCaptor.forClass(List.class);
        el.getCssValue("color");
        verify(client).executeScript(eq("s1"), any(), argsCap.capture());
        // args = [{element_handle: h1}, "color"]
        assertEquals(2, argsCap.getValue().size());
        assertEquals("color", argsCap.getValue().get(1));
    }

    @Test
    void getCssValue_returnsEmptyStringOnNull() {
        when(client.executeScript(eq("s1"), any(), any())).thenReturn(null);
        assertEquals("", el.getCssValue("display"));
    }

    // --- getScreenshotAs --------------------------------------------------

    @Test
    void getScreenshotAs_BYTES_returnsBytes() {
        byte[] bytes = new byte[] {1, 2, 3};
        when(client.captureElementScreenshot("s1", "h1")).thenReturn(bytes);
        assertArrayEquals(bytes, el.getScreenshotAs(OutputType.BYTES));
    }

    @Test
    void getScreenshotAs_BASE64_throwsWithPointer() {
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
            () -> el.getScreenshotAs(OutputType.BASE64));
        assertTrue(ex.getMessage().contains("BYTES"),
            "error should mention BYTES is the supported alternative: " + ex.getMessage());
        verify(client, never()).captureElementScreenshot(any(), any());
    }

    @Test
    void getScreenshotAs_FILE_throwsWithPointer() {
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class,
            () -> el.getScreenshotAs(OutputType.FILE));
        assertTrue(ex.getMessage().contains("BYTES"),
            "error should mention BYTES is the supported alternative: " + ex.getMessage());
    }
}
