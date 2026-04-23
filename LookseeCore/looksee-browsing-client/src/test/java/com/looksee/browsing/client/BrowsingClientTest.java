package com.looksee.browsing.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import com.looksee.browsing.generated.ApiException;
import com.looksee.browsing.generated.api.CaptureApi;
import com.looksee.browsing.generated.api.NavigationApi;
import com.looksee.browsing.generated.api.ScreenshotsApi;
import com.looksee.browsing.generated.api.ScrollingApi;
import com.looksee.browsing.generated.api.SessionsApi;
import com.looksee.browsing.generated.model.CaptureRequest;
import com.looksee.browsing.generated.model.CaptureResponse;
import com.looksee.browsing.generated.model.CreateSessionRequest;
import com.looksee.browsing.generated.model.NavigateRequest;
import com.looksee.browsing.generated.model.PageStatus;
import com.looksee.browsing.generated.model.ScreenshotRequest;
import com.looksee.browsing.generated.model.ScreenshotStrategy;
import com.looksee.browsing.generated.model.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BrowsingClientTest {

    private SessionsApi sessionsApi;
    private NavigationApi navigationApi;
    private ScreenshotsApi screenshotsApi;
    private ScrollingApi scrollingApi;
    private CaptureApi captureApi;
    private BrowsingClient client;

    @BeforeEach
    void setUp() {
        sessionsApi = mock(SessionsApi.class);
        navigationApi = mock(NavigationApi.class);
        screenshotsApi = mock(ScreenshotsApi.class);
        scrollingApi = mock(ScrollingApi.class);
        captureApi = mock(CaptureApi.class);
        client = new BrowsingClient(sessionsApi, navigationApi, screenshotsApi, scrollingApi, captureApi);
    }

    @Test
    void createSession_forwardsEnumsToGeneratedRequest() throws Exception {
        when(sessionsApi.createSession(any())).thenReturn(new Session().sessionId("abc"));

        Session result = client.createSession(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);

        ArgumentCaptor<CreateSessionRequest> cap = ArgumentCaptor.forClass(CreateSessionRequest.class);
        verify(sessionsApi).createSession(cap.capture());
        assertEquals("chrome", cap.getValue().getBrowser().getValue());
        assertEquals("discovery", cap.getValue().getEnvironment().getValue());
        assertEquals("abc", result.getSessionId());
    }

    @Test
    void navigate_buildsUriAndForwards() throws Exception {
        client.navigate("s1", "https://example.com");
        ArgumentCaptor<NavigateRequest> cap = ArgumentCaptor.forClass(NavigateRequest.class);
        verify(navigationApi).navigate(eq("s1"), cap.capture());
        assertEquals("https://example.com", cap.getValue().getUrl().toString());
    }

    @Test
    void getStatus_forwards() throws Exception {
        when(navigationApi.getPageStatus("s1")).thenReturn(new PageStatus().currentUrl("x").is503(false));
        PageStatus ps = client.getStatus("s1");
        assertEquals("x", ps.getCurrentUrl());
        assertFalse(ps.getIs503());
    }

    @Test
    void deleteSession_forwards() throws Exception {
        client.deleteSession("s1");
        verify(sessionsApi).deleteSession("s1");
    }

    @Test
    void screenshot_requestsStrategy() throws Exception {
        java.io.File tmp = java.io.File.createTempFile("shot", ".bin");
        java.nio.file.Files.write(tmp.toPath(), new byte[] {1, 2, 3});
        tmp.deleteOnExit();
        when(screenshotsApi.captureScreenshot(eq("s1"), any())).thenReturn(tmp);

        byte[] bytes = client.screenshot("s1", ScreenshotStrategy.VIEWPORT);

        ArgumentCaptor<ScreenshotRequest> cap = ArgumentCaptor.forClass(ScreenshotRequest.class);
        verify(screenshotsApi).captureScreenshot(eq("s1"), cap.capture());
        assertEquals(ScreenshotStrategy.VIEWPORT, cap.getValue().getStrategy());
        assertArrayEquals(new byte[] {1, 2, 3}, bytes);
    }

    @Test
    void capture_forwards() throws Exception {
        CaptureRequest req = new CaptureRequest();
        CaptureResponse resp = new CaptureResponse().captureId("cap1");
        when(captureApi.capture(req)).thenReturn(resp);
        assertSame(resp, client.capture(req));
    }

    @Test
    void apiException_isWrapped() throws Exception {
        when(navigationApi.getPageStatus("bad")).thenThrow(new ApiException(500, "boom"));
        BrowsingClientException ex = assertThrows(BrowsingClientException.class,
            () -> client.getStatus("bad"));
        assertTrue(ex.getMessage().contains("bad"));
        assertTrue(ex.getCause() instanceof ApiException);
    }

    @Test
    void config_rejectsBlankUrl() {
        assertThrows(IllegalArgumentException.class,
            () -> new BrowsingClientConfig("", java.time.Duration.ofSeconds(5), java.time.Duration.ofSeconds(5)));
    }

    @Test
    void config_stripsTrailingSlash() {
        BrowsingClientConfig c = new BrowsingClientConfig(
            "http://x/", java.time.Duration.ofSeconds(5), java.time.Duration.ofSeconds(5));
        assertEquals("http://x", c.getServiceUrl());
    }
}
