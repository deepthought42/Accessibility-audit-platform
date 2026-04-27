package com.looksee.services.health;

import com.looksee.browser.enums.BrowserType;
import com.looksee.config.LookseeBrowsingProperties;
import com.looksee.services.BrowserService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

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
 * <p><b>Scheduling isolation.</b> Uses a private {@link ScheduledExecutorService}
 * (single-thread, daemon) rather than Spring's {@code @Scheduled} /
 * {@code @EnableScheduling} infrastructure. This is deliberate: enabling Spring
 * scheduling at the application level would also activate any other
 * {@code @Scheduled} methods on the consumer's classpath — for example
 * {@code OutboxEventPublisher} and {@code IdempotencyService} in
 * {@code looksee-persistence} — which are currently dormant in consumers that
 * don't declare {@code @EnableScheduling}. Turning on the smoke-check must
 * never have the side effect of waking unrelated background jobs.
 *
 * <p>Uses {@code scheduleWithFixedDelay} (not fixed-rate) so back-to-back
 * executions can never pile up if the browser-service is slow — there's
 * always a full {@code interval} of quiet between completed probes. This
 * matters during incidents: a watchdog should not amplify load on a
 * struggling dependency.
 *
 * <p>The target URL and interval are validated and parsed once during
 * {@link PostConstruct}, so misconfiguration surfaces as a startup failure
 * rather than an infinite loop of warn-level probe failures.
 */
@Configuration
@ConditionalOnProperty(name = "looksee.browsing.smoke-check.enabled", havingValue = "true")
public class CapturePageSmokeCheck {

    private static final Logger log = LoggerFactory.getLogger(CapturePageSmokeCheck.class);
    private static final String METRIC_NAME = "browser_service_smoke_checks";

    private final BrowserService browserService;
    private final LookseeBrowsingProperties props;
    private final MeterRegistry meterRegistry; // may be null

    private ScheduledExecutorService scheduler;
    private URL targetUrl;
    private BrowserType browser;

    public CapturePageSmokeCheck(BrowserService browserService,
                                 LookseeBrowsingProperties props,
                                 ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.browserService = browserService;
        this.props = props;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
    }

    @PostConstruct
    void start() {
        long intervalMs = props.getSmokeCheck().getInterval().toMillis();
        if (intervalMs <= 0) {
            throw new IllegalStateException(
                "looksee.browsing.smoke-check.interval must be a positive duration; got "
                    + props.getSmokeCheck().getInterval());
        }
        try {
            this.targetUrl = new URL(props.getSmokeCheck().getTargetUrl());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(
                "looksee.browsing.smoke-check.target-url is not a valid URL: "
                    + props.getSmokeCheck().getTargetUrl(), e);
        }
        this.browser = props.getSmokeCheck().getBrowser();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "looksee-smoke-check");
            t.setDaemon(true);
            return t;
        });
        // Fixed-delay (not fixed-rate): if a probe runs long, the next one
        // still waits a full `intervalMs` after completion — never pile up.
        scheduler.scheduleWithFixedDelay(this::probe, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        log.info("CapturePageSmokeCheck started: interval={}ms target={}", intervalMs, targetUrl);
    }

    @PreDestroy
    void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /**
     * Visible-for-tests: package-private. Outcome defaults to "failure" and
     * flips to "success" only after a successful capturePage return — matches
     * the phase-4a.1 facade-instrumentation safety pattern so unexpected
     * exceptions still record a failure metric.
     *
     * <p>The whole body is wrapped in a Throwable-safe outer catch so an
     * exception escaping {@code probe()} can never suppress subsequent
     * scheduled executions (a quirk of {@link ScheduledExecutorService}).
     * That includes failures from metric recording — a custom
     * {@code MeterRegistry}/{@code MeterFilter} that throws must not silently
     * disable the watchdog.
     */
    void probe() {
        try {
            String outcome = "failure";
            try {
                browserService.capturePage(targetUrl, browser, -1L);
                outcome = "success";
            } catch (Throwable t) {
                // Probe failure must never crash the scheduler thread.
                log.warn("CapturePageSmokeCheck probe failed: {} ({})",
                    t.getClass().getSimpleName(), t.getMessage());
            } finally {
                recordOutcome(outcome);
            }
        } catch (Throwable t) {
            // Defensive: catches anything from recordOutcome (custom registry
            // behavior, OOM, etc.). A swallowed exception here is preferable
            // to having ScheduledExecutorService cancel the periodic task.
            log.warn("CapturePageSmokeCheck instrumentation failed: {} ({})",
                t.getClass().getSimpleName(), t.getMessage());
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
