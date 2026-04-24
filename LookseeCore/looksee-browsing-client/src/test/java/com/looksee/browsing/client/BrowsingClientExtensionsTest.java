package com.looksee.browsing.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.looksee.browsing.generated.api.AlertsApi;
import com.looksee.browsing.generated.api.CaptureApi;
import com.looksee.browsing.generated.api.DomApi;
import com.looksee.browsing.generated.api.ElementsApi;
import com.looksee.browsing.generated.api.MouseApi;
import com.looksee.browsing.generated.api.NavigationApi;
import com.looksee.browsing.generated.api.ScreenshotsApi;
import com.looksee.browsing.generated.api.ScrollingApi;
import com.looksee.browsing.generated.api.SessionsApi;
import com.looksee.browsing.generated.api.TouchApi;
import com.looksee.browsing.generated.model.AlertChoice;
import com.looksee.browsing.generated.model.AlertRespondRequest;
import com.looksee.browsing.generated.model.AlertState;
import com.looksee.browsing.generated.model.DomRemovePreset;
import com.looksee.browsing.generated.model.DomRemoveRequest;
import com.looksee.browsing.generated.model.ElementAction;
import com.looksee.browsing.generated.model.ElementActionRequest;
import com.looksee.browsing.generated.model.ElementScreenshotRequest;
import com.looksee.browsing.generated.model.ElementState;
import com.looksee.browsing.generated.model.ElementTouchRequest;
import com.looksee.browsing.generated.model.FindElementRequest;
import com.looksee.browsing.generated.model.MouseMoveMode;
import com.looksee.browsing.generated.model.MouseMoveRequest;
import com.looksee.browsing.generated.model.ScrollMode;
import com.looksee.browsing.generated.model.ScrollOffset;
import com.looksee.browsing.generated.model.ScrollRequest;
import com.looksee.browsing.generated.model.TouchAction;
import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * One forward-case test per new phase-3b facade method. Verifies each
 * method passes the right sessionId / element_handle / enum / input to
 * the underlying generated *Api and returns what the API returns.
 */
class BrowsingClientExtensionsTest {

    private SessionsApi sessionsApi;
    private NavigationApi navigationApi;
    private ScreenshotsApi screenshotsApi;
    private ScrollingApi scrollingApi;
    private CaptureApi captureApi;
    private ElementsApi elementsApi;
    private TouchApi touchApi;
    private DomApi domApi;
    private MouseApi mouseApi;
    private AlertsApi alertsApi;
    private BrowsingClient client;

    @BeforeEach
    void setUp() {
        sessionsApi = mock(SessionsApi.class);
        navigationApi = mock(NavigationApi.class);
        screenshotsApi = mock(ScreenshotsApi.class);
        scrollingApi = mock(ScrollingApi.class);
        captureApi = mock(CaptureApi.class);
        elementsApi = mock(ElementsApi.class);
        touchApi = mock(TouchApi.class);
        domApi = mock(DomApi.class);
        mouseApi = mock(MouseApi.class);
        alertsApi = mock(AlertsApi.class);
        client = new BrowsingClient(
            sessionsApi, navigationApi, screenshotsApi, scrollingApi, captureApi,
            elementsApi, touchApi, domApi, mouseApi, alertsApi,
            null /* no meter registry */);
    }

    @Test
    void findElement_forwardsXpath() throws Exception {
        when(elementsApi.findElement(eq("s1"), any())).thenReturn(new ElementState().found(true).elementHandle("h"));
        ElementState result = client.findElement("s1", "//button");
        ArgumentCaptor<FindElementRequest> cap = ArgumentCaptor.forClass(FindElementRequest.class);
        verify(elementsApi).findElement(eq("s1"), cap.capture());
        assertEquals("//button", cap.getValue().getXpath());
        assertEquals("h", result.getElementHandle());
    }

    @Test
    void performElementAction_forwardsHandleActionInput() throws Exception {
        client.performElementAction("s1", "h", ElementAction.CLICK, null);
        ArgumentCaptor<ElementActionRequest> cap = ArgumentCaptor.forClass(ElementActionRequest.class);
        verify(elementsApi).performElementAction(eq("s1"), cap.capture());
        assertEquals("h", cap.getValue().getElementHandle());
        assertEquals(ElementAction.CLICK, cap.getValue().getAction());
    }

    @Test
    void performElementTouch_forwards() throws Exception {
        client.performElementTouch("s1", "h", TouchAction.TAP, null);
        ArgumentCaptor<ElementTouchRequest> cap = ArgumentCaptor.forClass(ElementTouchRequest.class);
        verify(touchApi).performElementTouch(eq("s1"), cap.capture());
        assertEquals("h", cap.getValue().getElementHandle());
        assertEquals(TouchAction.TAP, cap.getValue().getAction());
    }

    @Test
    void captureElementScreenshot_readsBytes() throws Exception {
        File tmp = File.createTempFile("shot", ".bin");
        Files.write(tmp.toPath(), new byte[] {9, 8, 7});
        tmp.deleteOnExit();
        when(screenshotsApi.captureElementScreenshot(eq("s1"), any())).thenReturn(tmp);

        byte[] out = client.captureElementScreenshot("s1", "h");

        ArgumentCaptor<ElementScreenshotRequest> cap = ArgumentCaptor.forClass(ElementScreenshotRequest.class);
        verify(screenshotsApi).captureElementScreenshot(eq("s1"), cap.capture());
        assertEquals("h", cap.getValue().getElementHandle());
        assertArrayEquals(new byte[] {9, 8, 7}, out);
    }

    @Test
    void scrollToTop_sendsToTopMode() throws Exception {
        when(scrollingApi.scroll(eq("s1"), any())).thenReturn(new ScrollOffset().x(0).y(0));
        client.scrollToTop("s1");
        ArgumentCaptor<ScrollRequest> cap = ArgumentCaptor.forClass(ScrollRequest.class);
        verify(scrollingApi).scroll(eq("s1"), cap.capture());
        assertEquals(ScrollMode.TO_TOP, cap.getValue().getMode());
    }

    @Test
    void scrollToBottom_sendsToBottomMode() throws Exception {
        when(scrollingApi.scroll(eq("s1"), any())).thenReturn(new ScrollOffset().x(0).y(0));
        client.scrollToBottom("s1");
        ArgumentCaptor<ScrollRequest> cap = ArgumentCaptor.forClass(ScrollRequest.class);
        verify(scrollingApi).scroll(eq("s1"), cap.capture());
        assertEquals(ScrollMode.TO_BOTTOM, cap.getValue().getMode());
    }

    @Test
    void scrollToElement_passesHandleAndXpathHint() throws Exception {
        when(scrollingApi.scroll(eq("s1"), any())).thenReturn(new ScrollOffset().x(0).y(0));
        client.scrollToElement("s1", "h", "//nav");
        ArgumentCaptor<ScrollRequest> cap = ArgumentCaptor.forClass(ScrollRequest.class);
        verify(scrollingApi).scroll(eq("s1"), cap.capture());
        assertEquals(ScrollMode.TO_ELEMENT, cap.getValue().getMode());
        assertEquals("h", cap.getValue().getElementHandle());
        assertEquals("//nav", cap.getValue().getXpath());
    }

    @Test
    void scrollToElementCentered_passesHandle() throws Exception {
        when(scrollingApi.scroll(eq("s1"), any())).thenReturn(new ScrollOffset().x(0).y(0));
        client.scrollToElementCentered("s1", "h");
        ArgumentCaptor<ScrollRequest> cap = ArgumentCaptor.forClass(ScrollRequest.class);
        verify(scrollingApi).scroll(eq("s1"), cap.capture());
        assertEquals(ScrollMode.TO_ELEMENT_CENTERED, cap.getValue().getMode());
        assertEquals("h", cap.getValue().getElementHandle());
    }

    @Test
    void scrollDownPercent_passesPercentAsBigDecimal() throws Exception {
        when(scrollingApi.scroll(eq("s1"), any())).thenReturn(new ScrollOffset().x(0).y(0));
        client.scrollDownPercent("s1", 0.5);
        ArgumentCaptor<ScrollRequest> cap = ArgumentCaptor.forClass(ScrollRequest.class);
        verify(scrollingApi).scroll(eq("s1"), cap.capture());
        assertEquals(ScrollMode.DOWN_PERCENT, cap.getValue().getMode());
        assertEquals(0, cap.getValue().getPercent().compareTo(java.math.BigDecimal.valueOf(0.5)));
    }

    @Test
    void scrollDownFull_sendsDownFullMode() throws Exception {
        when(scrollingApi.scroll(eq("s1"), any())).thenReturn(new ScrollOffset().x(0).y(0));
        client.scrollDownFull("s1");
        ArgumentCaptor<ScrollRequest> cap = ArgumentCaptor.forClass(ScrollRequest.class);
        verify(scrollingApi).scroll(eq("s1"), cap.capture());
        assertEquals(ScrollMode.DOWN_FULL, cap.getValue().getMode());
    }

    @Test
    void removeDomElement_byClass_passesValue() throws Exception {
        client.removeDomElement("s1", DomRemovePreset.BY_CLASS, "banner");
        ArgumentCaptor<DomRemoveRequest> cap = ArgumentCaptor.forClass(DomRemoveRequest.class);
        verify(domApi).removeDomElement(eq("s1"), cap.capture());
        assertEquals(DomRemovePreset.BY_CLASS, cap.getValue().getPreset());
        assertEquals("banner", cap.getValue().getValue());
    }

    @Test
    void moveMouseOutOfFrame_sendsOutOfFrameMode() throws Exception {
        client.moveMouseOutOfFrame("s1");
        ArgumentCaptor<MouseMoveRequest> cap = ArgumentCaptor.forClass(MouseMoveRequest.class);
        verify(mouseApi).moveMouse(eq("s1"), cap.capture());
        assertEquals(MouseMoveMode.OUT_OF_FRAME, cap.getValue().getMode());
    }

    @Test
    void moveMouseToNonInteractive_sendsCoordsAndMode() throws Exception {
        client.moveMouseToNonInteractive("s1", 42, 99);
        ArgumentCaptor<MouseMoveRequest> cap = ArgumentCaptor.forClass(MouseMoveRequest.class);
        verify(mouseApi).moveMouse(eq("s1"), cap.capture());
        assertEquals(MouseMoveMode.TO_NON_INTERACTIVE, cap.getValue().getMode());
        assertEquals(42, cap.getValue().getX());
        assertEquals(99, cap.getValue().getY());
    }

    @Test
    void getAlert_forwards() throws Exception {
        when(alertsApi.getAlert("s1")).thenReturn(new AlertState().present(true).text("Are you sure?"));
        AlertState state = client.getAlert("s1");
        assertTrue(state.getPresent());
        assertEquals("Are you sure?", state.getText());
    }

    @Test
    void respondToAlert_passesChoiceAndInput() throws Exception {
        client.respondToAlert("s1", AlertChoice.ACCEPT, null);
        ArgumentCaptor<AlertRespondRequest> cap = ArgumentCaptor.forClass(AlertRespondRequest.class);
        verify(alertsApi).respondToAlert(eq("s1"), cap.capture());
        assertEquals(AlertChoice.ACCEPT, cap.getValue().getChoice());
    }
}
