package com.looksee.browsing.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import com.looksee.browsing.generated.ApiException;
import com.looksee.browsing.generated.api.CaptureApi;
import com.looksee.browsing.generated.api.NavigationApi;
import com.looksee.browsing.generated.api.ScreenshotsApi;
import com.looksee.browsing.generated.api.ScrollingApi;
import com.looksee.browsing.generated.api.SessionsApi;
import com.looksee.browsing.generated.model.Session;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the metric contract described in
 * {@code browser-service/phase-4-consumer-cutover.md} §Observability prereqs:
 * meter name {@code browser_service_calls}, required tags
 * {@code operation} + {@code outcome}, consumer tag added per-consumer
 * (not here). Also asserts that a null {@link MeterRegistry} is safe — the
 * facade must work on consumers without one wired.
 */
class BrowsingClientInstrumentationTest {

    private SessionsApi sessionsApi;
    private NavigationApi navigationApi;
    private ScreenshotsApi screenshotsApi;
    private ScrollingApi scrollingApi;
    private CaptureApi captureApi;
    private MeterRegistry registry;
    private BrowsingClient client;

    @BeforeEach
    void setUp() {
        sessionsApi = mock(SessionsApi.class);
        navigationApi = mock(NavigationApi.class);
        screenshotsApi = mock(ScreenshotsApi.class);
        scrollingApi = mock(ScrollingApi.class);
        captureApi = mock(CaptureApi.class);
        registry = new SimpleMeterRegistry();
        client = new BrowsingClient(sessionsApi, navigationApi, screenshotsApi, scrollingApi, captureApi, registry);
    }

    @Test
    void successfulCall_recordsSuccessTimer() throws Exception {
        when(sessionsApi.createSession(any())).thenReturn(new Session().sessionId("s1"));

        client.createSession(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);

        Timer t = registry.find("browser_service_calls")
            .tags(Tags.of("operation", "createSession", "outcome", "success"))
            .timer();
        assertNotNull(t, "timer should be registered");
        assertEquals(1L, t.count());
    }

    @Test
    void failedCall_recordsFailureTimer() throws Exception {
        when(navigationApi.getPageStatus("bad")).thenThrow(new ApiException(500, "boom"));

        assertThrows(BrowsingClientException.class, () -> client.getStatus("bad"));

        Timer t = registry.find("browser_service_calls")
            .tags(Tags.of("operation", "getStatus", "outcome", "failure"))
            .timer();
        assertNotNull(t, "failure timer should be registered");
        assertEquals(1L, t.count());
    }

    @Test
    void successAndFailure_recordedAsSeparateTimers() throws Exception {
        when(sessionsApi.getSession("ok")).thenReturn(new com.looksee.browsing.generated.model.SessionState().sessionId("ok"));
        when(sessionsApi.getSession("bad")).thenThrow(new ApiException(404, "not found"));

        client.getSession("ok");
        assertThrows(BrowsingClientException.class, () -> client.getSession("bad"));

        long success = registry.find("browser_service_calls")
            .tags(Tags.of("operation", "getSession", "outcome", "success"))
            .timer().count();
        long failure = registry.find("browser_service_calls")
            .tags(Tags.of("operation", "getSession", "outcome", "failure"))
            .timer().count();
        assertEquals(1L, success);
        assertEquals(1L, failure);
    }

    @Test
    void allFacadeMethods_emitDistinctOperationTags() throws Exception {
        // Smoke test that every public facade method registers a timer with
        // an operation tag that matches its method name (one line per method).
        when(sessionsApi.createSession(any())).thenReturn(new Session().sessionId("s"));
        when(sessionsApi.getSession(any())).thenReturn(new com.looksee.browsing.generated.model.SessionState());
        when(navigationApi.getPageStatus(any())).thenReturn(new com.looksee.browsing.generated.model.PageStatus().currentUrl("x").is503(false));
        when(navigationApi.getSource(any())).thenReturn("<html/>");
        when(scrollingApi.getViewport(any())).thenReturn(new com.looksee.browsing.generated.model.ViewportState()
            .viewport(new com.looksee.browsing.generated.model.Viewport().width(1).height(1))
            .scrollOffset(new com.looksee.browsing.generated.model.ScrollOffset().x(0).y(0)));

        client.createSession(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
        client.getSession("s");
        client.getStatus("s");
        client.getSource("s");
        client.getViewport("s");
        client.deleteSession("s");
        client.navigate("s", "https://example.com");

        for (String op : new String[] {"createSession", "getSession", "getStatus", "getSource", "getViewport", "deleteSession", "navigate"}) {
            Timer t = registry.find("browser_service_calls")
                .tags(Tags.of("operation", op, "outcome", "success")).timer();
            assertNotNull(t, "timer for operation=" + op + " should exist");
            assertEquals(1L, t.count(), "timer count for " + op);
        }
    }

    @Test
    void nullRegistry_isNoOp() throws Exception {
        // Consumers without a MeterRegistry bean must still work.
        BrowsingClient unmeteredClient = new BrowsingClient(
            sessionsApi, navigationApi, screenshotsApi, scrollingApi, captureApi, null);
        when(sessionsApi.createSession(any())).thenReturn(new Session().sessionId("s"));

        assertDoesNotThrow(() -> unmeteredClient.createSession(BrowserType.CHROME, BrowserEnvironment.DISCOVERY));
        // No registry to check — just confirming no NPE.
    }

    @Test
    void consumerTagIsNotSetByFacade() throws Exception {
        // The facade must emit only operation + outcome. The `consumer` tag is
        // the consumer's responsibility (via MeterFilter.commonTags). This guards
        // against the facade ever adding it, which would create duplicate series
        // when a consumer also adds a common tag.
        when(sessionsApi.createSession(any())).thenReturn(new Session().sessionId("s"));
        client.createSession(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);

        Timer t = registry.find("browser_service_calls")
            .tags(Tags.of("operation", "createSession", "outcome", "success"))
            .timer();
        assertNotNull(t);
        assertNull(t.getId().getTag("consumer"),
            "facade must not set the consumer tag (it's consumer-side common tag responsibility)");
    }
}
