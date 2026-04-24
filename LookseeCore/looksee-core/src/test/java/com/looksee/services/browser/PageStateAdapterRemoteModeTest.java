package com.looksee.services.browser;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.looksee.browser.enums.BrowserType;
import com.looksee.browsing.client.BrowsingClient;
import com.looksee.browsing.generated.model.PageStatus;
import com.looksee.browsing.generated.model.ScreenshotStrategy;
import com.looksee.browsing.generated.model.ScrollOffset;
import com.looksee.browsing.generated.model.Viewport;
import com.looksee.browsing.generated.model.ViewportState;
import com.looksee.gcp.GoogleCloudStorage;
import com.looksee.models.PageState;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Guards phase-3c's §Step 2: both Browser-taking overloads of
 * {@link PageStateAdapter} must run to completion against a
 * {@link RemoteBrowser} without reaching through {@code browser.getDriver()}
 * (which throws {@code UnsupportedOperationException} on RemoteBrowser).
 */
class PageStateAdapterRemoteModeTest {

    private BrowsingClient client;
    private GoogleCloudStorage gcs;
    private PageStateAdapter adapter;
    private RemoteBrowser remote;

    @BeforeEach
    void setUp() throws Exception {
        client = mock(BrowsingClient.class);
        gcs = mock(GoogleCloudStorage.class);
        adapter = new PageStateAdapter(gcs);
        remote = new RemoteBrowser(client, "s-rca", "chrome");

        // Defaults for every RemoteBrowser method the adapter calls.
        when(client.getStatus("s-rca")).thenReturn(
            new PageStatus().currentUrl("https://example.com/page").is503(false));
        when(client.getSource("s-rca")).thenReturn(
            "<!doctype html><html><head><title>Remote Title</title></head><body>hi</body></html>");
        when(client.getViewport("s-rca")).thenReturn(new ViewportState()
            .viewport(new Viewport().width(1920).height(1080))
            .scrollOffset(new ScrollOffset().x(0).y(0)));
        byte[] png = pngBytes(5, 5);
        when(client.screenshot(eq("s-rca"), any())).thenReturn(png);
        when(gcs.saveImage(any(), any(), any(), any()))
            .thenAnswer(inv -> "gs://bucket/" + inv.getArgument(2));
    }

    @Test
    void browserTakingOverload_runsToCompletionAgainstRemoteBrowser() throws Exception {
        // toPageState(Browser, long, String) — covers lines 86, 94, 106, 126.
        PageState page = adapter.toPageState(remote, 42L, "https://example.com/page");

        assertNotNull(page);
        assertEquals("Remote Title", page.getTitle());
        assertEquals(1920, page.getViewportWidth());
        assertEquals(1080, page.getViewportHeight());
        // getDriver would have thrown; absence of exception proves the
        // migration worked.
        verify(client, never()).getSession(any()); // didn't accidentally route to SessionState
    }

    @Test
    void urlBrowserTakingOverload_runsToCompletionAgainstRemoteBrowser() throws Exception {
        // toPageState(URL, Browser, boolean, int, long) — covers lines 177, 194, 216.
        PageState page = adapter.toPageState(
            new URL("https://example.com/page"), remote, true, 200, 7L);

        assertNotNull(page);
        assertEquals("Remote Title", page.getTitle());
        assertEquals(1920, page.getViewportWidth());
        assertEquals(1080, page.getViewportHeight());
    }

    @Test
    void getTitle_readsFromRemoteSource() {
        assertEquals("Remote Title", remote.getTitle());
    }

    private static byte[] pngBytes(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
