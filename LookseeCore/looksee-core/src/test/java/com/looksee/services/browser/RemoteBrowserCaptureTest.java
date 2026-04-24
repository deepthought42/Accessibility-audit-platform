package com.looksee.services.browser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import com.looksee.browsing.client.BrowsingClient;
import com.looksee.browsing.generated.model.ScreenshotStrategy;
import com.looksee.browsing.generated.model.Session;
import com.looksee.services.BrowserService;
import com.looksee.config.LookseeBrowsingProperties;
import com.looksee.gcp.GoogleCloudStorage;
import com.looksee.models.PageState;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Verifies the phase-3b remote {@code capturePage} explicit lifecycle:
 * createSession → navigate → getSource → screenshot(VIEWPORT) →
 * screenshot(FULL_PAGE_SHUTTERBUG) → deleteSession, and that the resulting
 * PageState carries distinct viewport and full-page screenshot URLs.
 */
class RemoteBrowserCaptureTest {

    @Test
    void remoteCapturePage_issuesExplicitLifecycleInOrder() throws Exception {
        BrowsingClient client = mock(BrowsingClient.class);
        GoogleCloudStorage gcs = mock(GoogleCloudStorage.class);
        when(client.createSession(any(), any())).thenReturn(new Session().sessionId("s-7"));
        when(client.getSource("s-7")).thenReturn(
            "<!doctype html><html><head><title>hi</title></head><body>hi</body></html>");
        byte[] viewport = pngBytes(10, 10);
        byte[] full = pngBytes(10, 40);
        when(client.screenshot("s-7", ScreenshotStrategy.VIEWPORT)).thenReturn(viewport);
        when(client.screenshot("s-7", ScreenshotStrategy.FULL_PAGE_SHUTTERBUG)).thenReturn(full);
        when(gcs.saveImage(any(), any(), any(), any()))
            .thenAnswer(inv -> "gs://bucket/" + inv.getArgument(2));

        LookseeBrowsingProperties props = new LookseeBrowsingProperties();
        props.setMode(LookseeBrowsingProperties.Mode.REMOTE);

        PageStateAdapter adapter = new PageStateAdapter(gcs);
        BrowserService svc = new BrowserService();
        ReflectionTestUtils.setField(svc, "browsingProps", props);
        ReflectionTestUtils.setField(svc, "browsingClient", client);
        ReflectionTestUtils.setField(svc, "pageStateAdapter", adapter);

        PageState page = svc.capturePage(new URL("https://example.com/page"), BrowserType.CHROME, 42L);

        InOrder order = inOrder(client);
        order.verify(client).createSession(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
        order.verify(client).navigate("s-7", "https://example.com/page");
        order.verify(client).getSource("s-7");
        order.verify(client).screenshot("s-7", ScreenshotStrategy.VIEWPORT);
        order.verify(client).screenshot("s-7", ScreenshotStrategy.FULL_PAGE_SHUTTERBUG);
        order.verify(client).deleteSession("s-7");
        order.verifyNoMoreInteractions();

        // Viewport and full-page must land on DISTINCT URLs (different checksum).
        assertNotEquals(page.getViewportScreenshotUrl(), page.getFullPageScreenshotUrl(),
            "viewport and full-page screenshots must be stored separately");
    }

    @Test
    void remoteCapturePage_swallowsDeleteSessionFailure() throws Exception {
        BrowsingClient client = mock(BrowsingClient.class);
        GoogleCloudStorage gcs = mock(GoogleCloudStorage.class);
        when(client.createSession(any(), any())).thenReturn(new Session().sessionId("s-8"));
        when(client.getSource(any())).thenReturn(
            "<!doctype html><html><head><title>t</title></head><body/></html>");
        when(client.screenshot(any(), any())).thenReturn(pngBytes(1, 1));
        when(gcs.saveImage(any(), any(), any(), any())).thenReturn("gs://bucket/x");
        doThrow(new com.looksee.browsing.client.BrowsingClientException("500", null))
            .when(client).deleteSession("s-8");

        LookseeBrowsingProperties props = new LookseeBrowsingProperties();
        props.setMode(LookseeBrowsingProperties.Mode.REMOTE);
        BrowserService svc = new BrowserService();
        ReflectionTestUtils.setField(svc, "browsingProps", props);
        ReflectionTestUtils.setField(svc, "browsingClient", client);
        ReflectionTestUtils.setField(svc, "pageStateAdapter", new PageStateAdapter(gcs));

        // Cleanup failure must NOT mask the successful return.
        PageState page = svc.capturePage(new URL("https://example.com/"), BrowserType.CHROME, 1L);
        assertNotNull(page);
    }

    private static byte[] pngBytes(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
