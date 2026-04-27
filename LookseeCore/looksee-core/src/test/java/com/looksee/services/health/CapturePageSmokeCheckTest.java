package com.looksee.services.health;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.looksee.browser.enums.BrowserType;
import com.looksee.config.LookseeBrowsingProperties;
import com.looksee.services.BrowserService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Phase 4a.4: covers the CapturePageSmokeCheck probe path + Throwable-safety
 * + null-MeterRegistry fallback + scheduler lifecycle + startup validation.
 * The conditional bean creation itself (the @ConditionalOnProperty gate) is
 * exercised by Spring at app startup; we verify that contract by constructing
 * the bean directly here — no full @SpringBootTest needed.
 */
class CapturePageSmokeCheckTest {

    private BrowserService browserService;
    @SuppressWarnings("unchecked")
    private ObjectProvider<MeterRegistry> registryProvider = mock(ObjectProvider.class);
    private SimpleMeterRegistry registry;
    private LookseeBrowsingProperties props;
    private CapturePageSmokeCheck check;

    @BeforeEach
    void setUp() {
        browserService = mock(BrowserService.class);
        registry = new SimpleMeterRegistry();
        when(registryProvider.getIfAvailable()).thenReturn(registry);
        props = new LookseeBrowsingProperties();
        props.getSmokeCheck().setTargetUrl("https://example.com");
        props.getSmokeCheck().setBrowser(BrowserType.CHROME);
        // Long interval so the background task never fires during a unit test
        // — we drive probe() manually to assert behavior deterministically.
        props.getSmokeCheck().setInterval(Duration.ofHours(1));
        check = new CapturePageSmokeCheck(browserService, props, registryProvider);
        check.start();
    }

    @AfterEach
    void tearDown() {
        if (check != null) check.stop();
    }

    @Test
    void successfulProbe_recordsSuccessCounter() throws Exception {
        when(browserService.capturePage(any(URL.class), eq(BrowserType.CHROME), eq(-1L)))
            .thenReturn(null /* not asserting on PageState content */);

        check.probe();

        Counter c = registry.find("browser_service_smoke_checks")
            .tags(Tags.of("outcome", "success"))
            .counter();
        assertNotNull(c, "success counter should be registered");
        assertEquals(1.0, c.count());
    }

    @Test
    void browsingClientFailure_recordsFailureCounter() throws Exception {
        when(browserService.capturePage(any(URL.class), any(), anyLong()))
            .thenThrow(new com.looksee.browsing.client.BrowsingClientException("boom", null));

        check.probe();

        Counter c = registry.find("browser_service_smoke_checks")
            .tags(Tags.of("outcome", "failure"))
            .counter();
        assertNotNull(c, "failure counter should be registered");
        assertEquals(1.0, c.count());
    }

    @Test
    void probe_doesNotPropagateAnyThrowable() throws Exception {
        // Throwable safety: a probe failure must never crash the scheduler.
        // OutOfMemoryError stands in for any unexpected runtime error.
        when(browserService.capturePage(any(URL.class), any(), anyLong()))
            .thenThrow(new OutOfMemoryError("sim"));

        assertDoesNotThrow(() -> check.probe());

        Counter c = registry.find("browser_service_smoke_checks")
            .tags(Tags.of("outcome", "failure"))
            .counter();
        assertNotNull(c);
        assertEquals(1.0, c.count());
    }

    @Test
    void probe_swallowsMetricRegistryFailures() throws Exception {
        // A throwing MeterRegistry must not propagate out of probe(), or the
        // ScheduledExecutorService will silently cancel the periodic task.
        MeterRegistry throwingRegistry = mock(MeterRegistry.class);
        when(throwingRegistry.config()).thenThrow(new RuntimeException("registry exploded"));
        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> throwingProvider = mock(ObjectProvider.class);
        when(throwingProvider.getIfAvailable()).thenReturn(throwingRegistry);

        CapturePageSmokeCheck instrumented =
            new CapturePageSmokeCheck(browserService, props, throwingProvider);
        instrumented.start();
        try {
            when(browserService.capturePage(any(URL.class), any(), anyLong())).thenReturn(null);
            assertDoesNotThrow(instrumented::probe);
        } finally {
            instrumented.stop();
        }
    }

    @Test
    void absentRegistry_isNoOpForMetrics() throws Exception {
        // Consumer hasn't wired a MeterRegistry — bean still works, just
        // doesn't emit metrics.
        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        CapturePageSmokeCheck unmetered =
            new CapturePageSmokeCheck(browserService, props, emptyProvider);
        unmetered.start();
        try {
            when(browserService.capturePage(any(URL.class), any(), anyLong())).thenReturn(null);
            assertDoesNotThrow(unmetered::probe);
        } finally {
            unmetered.stop();
        }
    }

    @Test
    void probe_usesConfiguredBrowserAndUrl() throws Exception {
        props.getSmokeCheck().setTargetUrl("https://probe.internal/health");
        props.getSmokeCheck().setBrowser(BrowserType.FIREFOX);
        // Re-construct so start() picks up the updated values (fields are
        // captured at @PostConstruct time — fail-fast on misconfiguration).
        check.stop();
        check = new CapturePageSmokeCheck(browserService, props, registryProvider);
        check.start();

        when(browserService.capturePage(any(URL.class), any(), anyLong())).thenReturn(null);
        check.probe();

        ArgumentCaptor<URL> urlCap = ArgumentCaptor.forClass(URL.class);
        ArgumentCaptor<BrowserType> browserCap = ArgumentCaptor.forClass(BrowserType.class);
        verify(browserService).capturePage(urlCap.capture(), browserCap.capture(), eq(-1L));
        assertEquals("https://probe.internal/health", urlCap.getValue().toString());
        assertEquals(BrowserType.FIREFOX, browserCap.getValue());
    }

    @Test
    void start_failsFastOnMalformedUrl() {
        LookseeBrowsingProperties bad = new LookseeBrowsingProperties();
        bad.getSmokeCheck().setTargetUrl("not a url");
        bad.getSmokeCheck().setInterval(Duration.ofSeconds(60));
        CapturePageSmokeCheck c = new CapturePageSmokeCheck(browserService, bad, registryProvider);

        IllegalStateException ex = assertThrows(IllegalStateException.class, c::start);
        assertTrue(ex.getMessage().contains("target-url"),
            "error message should reference the offending property: " + ex.getMessage());
    }

    @Test
    void start_failsFastOnNonPositiveInterval() {
        LookseeBrowsingProperties bad = new LookseeBrowsingProperties();
        bad.getSmokeCheck().setTargetUrl("https://example.com");
        bad.getSmokeCheck().setInterval(Duration.ZERO);
        CapturePageSmokeCheck c = new CapturePageSmokeCheck(browserService, bad, registryProvider);

        IllegalStateException ex = assertThrows(IllegalStateException.class, c::start);
        assertTrue(ex.getMessage().contains("interval"),
            "error message should reference the offending property: " + ex.getMessage());
    }

    @Test
    void start_actuallySchedulesProbe() throws Exception {
        // Latch-based: deterministic regardless of CI clock noise.
        CountDownLatch fired = new CountDownLatch(1);
        BrowserService latchingService = mock(BrowserService.class);
        when(latchingService.capturePage(any(URL.class), any(), anyLong()))
            .thenAnswer(inv -> { fired.countDown(); return null; });

        LookseeBrowsingProperties tight = new LookseeBrowsingProperties();
        tight.getSmokeCheck().setTargetUrl("https://example.com");
        tight.getSmokeCheck().setBrowser(BrowserType.CHROME);
        tight.getSmokeCheck().setInterval(Duration.ofMillis(20));

        CapturePageSmokeCheck lifecycleCheck =
            new CapturePageSmokeCheck(latchingService, tight, registryProvider);
        lifecycleCheck.start();
        try {
            assertTrue(fired.await(5, TimeUnit.SECONDS),
                "scheduled probe should have fired at least once within 5s");
        } finally {
            lifecycleCheck.stop();
        }
    }

    @Test
    void stop_isSafeWhenNeverStarted() {
        CapturePageSmokeCheck unstarted =
            new CapturePageSmokeCheck(browserService, props, registryProvider);
        assertDoesNotThrow(unstarted::stop);
    }
}
