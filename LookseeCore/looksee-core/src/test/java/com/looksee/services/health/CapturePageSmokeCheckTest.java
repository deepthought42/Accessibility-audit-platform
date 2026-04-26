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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * Phase 4a.4: covers the CapturePageSmokeCheck probe path + Throwable-safety
 * + null-MeterRegistry fallback + scheduler registration. The conditional
 * bean creation itself (the @ConditionalOnProperty gate) is exercised by
 * Spring at app startup; we verify that contract by constructing the bean
 * directly here — no full @SpringBootTest needed.
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
        check = new CapturePageSmokeCheck(browserService, props, registryProvider);
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
    void absentRegistry_isNoOpForMetrics() throws Exception {
        // Consumer hasn't wired a MeterRegistry — bean still works, just
        // doesn't emit metrics.
        ObjectProvider<MeterRegistry> emptyProvider = mock(ObjectProvider.class);
        when(emptyProvider.getIfAvailable()).thenReturn(null);
        CapturePageSmokeCheck unmetered = new CapturePageSmokeCheck(browserService, props, emptyProvider);

        when(browserService.capturePage(any(URL.class), any(), anyLong())).thenReturn(null);
        assertDoesNotThrow(() -> unmetered.probe());
        // No registry → nothing to assert beyond "no NPE".
    }

    @Test
    void probe_usesConfiguredBrowserAndUrl() throws Exception {
        props.getSmokeCheck().setTargetUrl("https://probe.internal/health");
        props.getSmokeCheck().setBrowser(BrowserType.FIREFOX);

        when(browserService.capturePage(any(URL.class), any(), anyLong())).thenReturn(null);
        check.probe();

        ArgumentCaptor<URL> urlCap = ArgumentCaptor.forClass(URL.class);
        ArgumentCaptor<BrowserType> browserCap = ArgumentCaptor.forClass(BrowserType.class);
        verify(browserService).capturePage(urlCap.capture(), browserCap.capture(), eq(-1L));
        assertEquals("https://probe.internal/health", urlCap.getValue().toString());
        assertEquals(BrowserType.FIREFOX, browserCap.getValue());
    }

    @Test
    void configureTasks_registersPeriodicTriggerWithConfiguredInterval() {
        props.getSmokeCheck().setInterval(java.time.Duration.ofSeconds(30));

        ScheduledTaskRegistrar registrar = mock(ScheduledTaskRegistrar.class);
        check.configureTasks(registrar);

        ArgumentCaptor<PeriodicTrigger> triggerCap = ArgumentCaptor.forClass(PeriodicTrigger.class);
        verify(registrar).addTriggerTask(any(Runnable.class), triggerCap.capture());
        // Spring 5's PeriodicTrigger exposes the period via getPeriod() (long millis).
        assertEquals(30_000L, triggerCap.getValue().getPeriod(),
            "trigger should reflect 30s interval");
    }
}
