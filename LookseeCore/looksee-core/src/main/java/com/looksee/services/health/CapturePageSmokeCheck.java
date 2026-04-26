package com.looksee.services.health;

import com.looksee.config.LookseeBrowsingProperties;
import com.looksee.services.BrowserService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * Periodically calls {@link BrowserService#capturePage} against the configured
 * {@code looksee.browsing.smoke-check.target-url} and reports success/failure
 * via Micrometer + structured logs. Doubles as a watchdog during phase-4
 * staging burn-in and prod cutover (see
 * {@code browser-service/phase-4-consumer-cutover.md} §4a.4 and
 * {@code browser-service/phase-4a4-smoke-check-bean.md}).
 *
 * <p>Disabled by default; opt in per-consumer with
 * {@code looksee.browsing.smoke-check.enabled=true}. When disabled the bean
 * isn't created — zero runtime overhead.
 *
 * <p>Metric contract: counter {@code browser_service_smoke_checks} with tag
 * {@code outcome=success|failure}. The {@code consumer} tag is the consumer's
 * {@code MeterFilter.commonTags} responsibility (same pattern as the phase-4a.1
 * facade instrumentation).
 *
 * <p>Implements {@link SchedulingConfigurer} rather than using
 * {@code @Scheduled(fixedRateString=...)} so the interval can be read from a
 * {@link java.time.Duration} property without needing a SpEL bean reference.
 * The {@code @EnableConfigurationProperties}-bound bean name (e.g.
 * {@code looksee.browsing-com.looksee.config.LookseeBrowsingProperties}) isn't
 * the camelCase form SpEL would expect, so a {@code @bean} reference in
 * {@code fixedRateString} would fail to resolve at startup. This pattern
 * sidesteps that — the interval is read directly via constructor-injected
 * {@code LookseeBrowsingProperties}.
 *
 * <p>Note: scheduling itself depends on the consumer's app having
 * {@code @EnableScheduling} (or auto-config equivalent). If absent,
 * {@link #configureTasks} simply isn't invoked — bean exists, never fires,
 * visible as a flat-zero smoke-check counter on the dashboard.
 */
@Configuration
@ConditionalOnProperty(name = "looksee.browsing.smoke-check.enabled", havingValue = "true")
public class CapturePageSmokeCheck implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CapturePageSmokeCheck.class);
    private static final String METRIC_NAME = "browser_service_smoke_checks";

    private final BrowserService browserService;
    private final LookseeBrowsingProperties props;
    private final MeterRegistry meterRegistry; // may be null

    public CapturePageSmokeCheck(BrowserService browserService,
                                 LookseeBrowsingProperties props,
                                 ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.browserService = browserService;
        this.props = props;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // Reads the Duration property at registration time. PeriodicTrigger
        // accepts millis-as-long; convert via Duration.toMillis().
        long intervalMs = props.getSmokeCheck().getInterval().toMillis();
        taskRegistrar.addTriggerTask(this::probe, new PeriodicTrigger(intervalMs));
    }

    /**
     * Visible-for-tests: package-private. Outcome defaults to "failure" and
     * flips to "success" only after a successful capturePage return — matches
     * the phase-4a.1 facade-instrumentation safety pattern so unexpected
     * exceptions still record a failure metric.
     */
    void probe() {
        String outcome = "failure";
        try {
            URL url = new URL(props.getSmokeCheck().getTargetUrl());
            browserService.capturePage(url, props.getSmokeCheck().getBrowser(), -1L);
            outcome = "success";
        } catch (Throwable t) {
            // Catch broadest — a probe failure must never crash the scheduler
            // thread. BrowsingClientException, IOException,
            // IllegalArgumentException, OutOfMemoryError all need to be
            // contained.
            log.warn("CapturePageSmokeCheck probe failed: {} ({})",
                t.getClass().getSimpleName(), t.getMessage());
        } finally {
            recordOutcome(outcome);
        }
    }

    private void recordOutcome(String outcome) {
        if (meterRegistry == null) return;
        Counter.builder(METRIC_NAME)
            .tag("outcome", outcome)
            .register(meterRegistry)
            .increment();
    }
}
