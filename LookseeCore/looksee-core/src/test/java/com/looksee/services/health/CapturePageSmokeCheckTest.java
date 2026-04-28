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
    private ObjectProvider<BrowserService> browserServiceProvider;
    @SuppressWarnings("unchecked")
    private ObjectProvider<MeterRegistry> registryProvider = mock(ObjectProvider.class);
    private SimpleMeterRegistry registry;
    private LookseeBrowsingProperties props;
    private CapturePageSmokeCheck check;

    @SuppressWarnings("unchecked")
    private static ObjectProvider<BrowserService> providerOf(BrowserService bs) {
        ObjectProvider<BrowserService> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(bs);
        return p;
    }

    @BeforeEach
    void setUp() {
        browserService = mock(BrowserService.class);
        browserServiceProvider = providerOf(browserService);
        registry = new SimpleMeterRegistry();
        when(registryProvider.getIfAvailable()).thenReturn(registry);
        props = new LookseeBrowsingProperties();
        props.setMode(LookseeBrowsingProperties.Mode.REMOTE);
        props.getSmokeCheck().setTargetUrl("https://example.com");
        props.getSmokeCheck().setBrowser(BrowserType.CHROME);
        props.getSmokeCheck().setInterval(Duration.ofHours(1));
        check = new CapturePageSmokeCheck(browserServiceProvider, props, registryProvider);
        // Use prepare() rather than start() to validate config + assign
        // fields without starting the scheduler. Initial delay 0 means
        // start() would race with the manual probe() calls below.
        check.prepare();
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
        MeterRegistry throwingRegistry = mock(MeterRegistry.class);
        when(throwingRegistry.config()).thenThrow(new RuntimeException("registry exploded"));
        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> throwingProvider = mock(ObjectProvider.class);
        when(throwingProvider.getIfAvailable()).thenReturn(throwingRegistry);

        CapturePageSmokeCheck instrumented =
            new CapturePageSmokeCheck(providerOf(browserService), props, throwingProvider);
        instrumented.prepare();
        try {
            when(browserService.capturePage(any(URL.class), any(), anyLong())).thenReturn(null);
            assertDoesNotThrow(instrumented::probe);
        } finally {
            instrumented.stop();
        }
    }

    @Test
    void absentRegistry_isNoOpForMetrics() throws Exception {
        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        CapturePageSmokeCheck unmetered =
            new CapturePageSmokeCheck(providerOf(browserService), props, emptyProvider);
        unmetered.prepare();
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
        check.stop();
        check = new CapturePageSmokeCheck(providerOf(browserService), props, registryProvider);
        check.prepare();

        when(browserService.capturePage(any(URL.class), any(), anyLong())).thenReturn(null);
        check.probe();

        ArgumentCaptor<URL> urlCap = ArgumentCaptor.forClass(URL.class);
        ArgumentCaptor<BrowserType> browserCap = ArgumentCaptor.forClass(BrowserType.class);
        verify(browserService).capturePage(urlCap.capture(), browserCap.capture(), eq(-1L));
        assertEquals("https://probe.internal/health", urlCap.getValue().toString());
        assertEquals(BrowserType.FIREFOX, browserCap.getValue());
    }

    @Test
    void start_requiresRemoteMode() {
        LookseeBrowsingProperties local = new LookseeBrowsingProperties();
        local.setMode(LookseeBrowsingProperties.Mode.LOCAL);
        local.getSmokeCheck().setTargetUrl("https://example.com");
        local.getSmokeCheck().setBrowser(BrowserType.CHROME);
        CapturePageSmokeCheck c = new CapturePageSmokeCheck(
            providerOf(browserService), local, registryProvider);

        IllegalStateException ex = assertThrows(IllegalStateException.class, c::start);
        assertTrue(ex.getMessage().contains("looksee.browsing.mode=remote"),
            "error message should reference the mode requirement: " + ex.getMessage());
    }

    @Test
    void start_skipsWhenBrowserServiceMissing() {
        // Consumer enabled the smoke-check but BrowserService isn't on the
        // classpath / context (no GCS). Should log and return — not throw.
        @SuppressWarnings("unchecked")
        ObjectProvider<BrowserService> missing = mock(ObjectProvider.class);
        when(missing.getIfAvailable()).thenReturn(null);
        CapturePageSmokeCheck c = new CapturePageSmokeCheck(missing, props, registryProvider);
        assertDoesNotThrow(c::start);
        // And stop() must be safe even though start() short-circuited.
        assertDoesNotThrow(c::stop);
    }

    @Test
    void start_failsFastOnMalformedUrl() {
        LookseeBrowsingProperties bad = new LookseeBrowsingProperties();
        bad.setMode(LookseeBrowsingProperties.Mode.REMOTE);
        bad.getSmokeCheck().setTargetUrl("not a url");
        bad.getSmokeCheck().setInterval(Duration.ofSeconds(60));
        CapturePageSmokeCheck c = new CapturePageSmokeCheck(
            providerOf(browserService), bad, registryProvider);

        IllegalStateException ex = assertThrows(IllegalStateException.class, c::start);
        assertTrue(ex.getMessage().contains("target-url"),
            "error message should reference the offending property: " + ex.getMessage());
    }

    @Test
    void start_failsFastOnNullInterval() {
        LookseeBrowsingProperties bad = new LookseeBrowsingProperties();
        bad.setMode(LookseeBrowsingProperties.Mode.REMOTE);
        bad.getSmokeCheck().setTargetUrl("https://example.com");
        bad.getSmokeCheck().setInterval(null);
        CapturePageSmokeCheck c = new CapturePageSmokeCheck(
            providerOf(browserService), bad, registryProvider);

        IllegalStateException ex = assertThrows(IllegalStateException.class, c::start);
        assertTrue(ex.getMessage().contains("interval"),
            "error message should reference the offending property: " + ex.getMessage());
    }

    @Test
    void start_failsFastOnNonPositiveInterval() {
        LookseeBrowsingProperties bad = new LookseeBrowsingProperties();
        bad.setMode(LookseeBrowsingProperties.Mode.REMOTE);
        bad.getSmokeCheck().setTargetUrl("https://example.com");
        bad.getSmokeCheck().setInterval(Duration.ZERO);
        CapturePageSmokeCheck c = new CapturePageSmokeCheck(
            providerOf(browserService), bad, registryProvider);

        IllegalStateException ex = assertThrows(IllegalStateException.class, c::start);
        assertTrue(ex.getMessage().contains("interval"),
            "error message should reference the offending property: " + ex.getMessage());
    }

    @Test
    void start_failsFastOnNullBrowser() {
        LookseeBrowsingProperties bad = new LookseeBrowsingProperties();
        bad.setMode(LookseeBrowsingProperties.Mode.REMOTE);
        bad.getSmokeCheck().setTargetUrl("https://example.com");
        bad.getSmokeCheck().setBrowser(null);
        CapturePageSmokeCheck c = new CapturePageSmokeCheck(
            providerOf(browserService), bad, registryProvider);

        IllegalStateException ex = assertThrows(IllegalStateException.class, c::start);
        assertTrue(ex.getMessage().contains("browser"),
            "error message should reference the offending property: " + ex.getMessage());
    }

    @Test
    void start_firesFirstProbeImmediately() throws Exception {
        CountDownLatch fired = new CountDownLatch(1);
        BrowserService latchingService = mock(BrowserService.class);
        when(latchingService.capturePage(any(URL.class), any(), anyLong()))
            .thenAnswer(inv -> { fired.countDown(); return null; });

        LookseeBrowsingProperties p = new LookseeBrowsingProperties();
        p.setMode(LookseeBrowsingProperties.Mode.REMOTE);
        p.getSmokeCheck().setTargetUrl("https://example.com");
        p.getSmokeCheck().setBrowser(BrowserType.CHROME);
        // Long interval — relying on initial-delay-0 to fire the first probe.
        p.getSmokeCheck().setInterval(Duration.ofHours(1));

        CapturePageSmokeCheck lifecycleCheck =
            new CapturePageSmokeCheck(providerOf(latchingService), p, registryProvider);
        lifecycleCheck.start();
        try {
            assertTrue(fired.await(5, TimeUnit.SECONDS),
                "first probe should fire immediately on startup, not after one interval");
        } finally {
            lifecycleCheck.stop();
        }
    }

    @Test
    void stop_isSafeWhenNeverStarted() {
        CapturePageSmokeCheck unstarted = new CapturePageSmokeCheck(
            providerOf(browserService), props, registryProvider);
        assertDoesNotThrow(unstarted::stop);
    }
}
