package com.looksee.messaging.observability;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

/**
 * Centralized Pub/Sub processing metrics shared by every Looksee service.
 *
 * <p>Wave 2 of the architecture review (see
 * {@code /root/.claude/plans/gleaming-jumping-thunder.md}) calls for unified
 * observability across all 13 audit services. This component is intentionally
 * stateless and depends only on Micrometer's {@link MeterRegistry} so it can
 * be wired in any Spring Boot service via component scanning of the
 * {@code com.looksee.messaging} package.</p>
 *
 * <p>Metric names are stable; changing them is a breaking change for any
 * dashboards or alert policies that consume them.</p>
 */
public class PubSubMetrics {

    /** Counter incremented for every received Pub/Sub message. Tags: service, topic, result. */
    public static final String MESSAGES_RECEIVED = "looksee.pubsub.messages.received";

    /** Timer measuring end-to-end processing duration. Tags: service, topic. */
    public static final String PROCESSING_DURATION = "looksee.pubsub.processing.duration";

    /** Counter incremented for every processing failure. Tags: service, topic, errorClass. */
    public static final String PROCESSING_ERRORS = "looksee.pubsub.processing.errors";

    /** Counter incremented on every outbox publish attempt. Tags: topic, result (success|retrying|exhausted). */
    public static final String OUTBOX_PUBLISHED = "looksee.outbox.published";

    /** Timer measuring time-from-write to successful publish for outbox events. Tags: topic. */
    public static final String OUTBOX_LAG = "looksee.outbox.lag.seconds";

    /** Counter for outbox publish failures, classified by reason. Tags: topic, reason. */
    public static final String OUTBOX_FAILED = "looksee.outbox.failed";

    /** Gauge of pending outbox events due for publish. No tags. */
    public static final String OUTBOX_PENDING = "looksee.outbox.pending";

    private final MeterRegistry registry;

    public PubSubMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Records a successfully processed message. */
    public void recordSuccess(String service, String topic) {
        Counter.builder(MESSAGES_RECEIVED)
            .tags(Tags.of("service", service, "topic", topic, "result", "success"))
            .register(registry)
            .increment();
    }

    /** Records a duplicate message that was acknowledged without processing. */
    public void recordDuplicate(String service, String topic) {
        Counter.builder(MESSAGES_RECEIVED)
            .tags(Tags.of("service", service, "topic", topic, "result", "duplicate"))
            .register(registry)
            .increment();
    }

    /** Records an invalid payload that was rejected before any processing began. */
    public void recordInvalid(String service, String topic) {
        Counter.builder(MESSAGES_RECEIVED)
            .tags(Tags.of("service", service, "topic", topic, "result", "invalid"))
            .register(registry)
            .increment();
    }

    /** Records a processing error. */
    public void recordError(String service, String topic, Throwable error) {
        Counter.builder(PROCESSING_ERRORS)
            .tags(Tags.of(
                "service", service,
                "topic", topic,
                "errorClass", error.getClass().getSimpleName()))
            .register(registry)
            .increment();
    }

    /** Records the processing duration for a single message. */
    public void recordDuration(String service, String topic, long durationNanos) {
        Timer.builder(PROCESSING_DURATION)
            .tags(Tags.of("service", service, "topic", topic))
            .register(registry)
            .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Records a single outbox publish attempt outcome.
     *
     * @param topic  the Pub/Sub topic of the event
     * @param result {@code "success"}, {@code "retrying"}, or {@code "exhausted"}
     */
    public void recordOutboxPublished(String topic, String result) {
        Counter.builder(OUTBOX_PUBLISHED)
            .tags(Tags.of("topic", topic, "result", result))
            .register(registry)
            .increment();
    }

    /**
     * Records the time elapsed between {@code OutboxEvent.createdAt} and a
     * successful publish, used to monitor outbox drain latency.
     */
    public void recordOutboxLag(String topic, long lagNanos) {
        Timer.builder(OUTBOX_LAG)
            .tags(Tags.of("topic", topic))
            .register(registry)
            .record(lagNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Records an outbox publish failure with a classification tag.
     *
     * @param topic  the Pub/Sub topic of the event
     * @param reason one of {@code "pubsub_unavailable"}, {@code "serialization"},
     *               {@code "exhausted"}, or {@code "unknown"}
     */
    public void recordOutboxFailed(String topic, String reason) {
        Counter.builder(OUTBOX_FAILED)
            .tags(Tags.of("topic", topic, "reason", reason))
            .register(registry)
            .increment();
    }

    /**
     * Register a gauge that publishes the current count of due outbox events
     * via the supplied function. Micrometer invokes the supplier on each
     * metrics scrape so the value always reflects the latest backlog.
     *
     * <p>Safe to call multiple times — Micrometer treats subsequent
     * registrations with the same name and tags as no-ops.</p>
     */
    public void registerOutboxPendingGauge(Supplier<Number> supplier) {
        Gauge.builder(OUTBOX_PENDING, supplier)
            .description("Pending outbox events whose nextAttemptAt has elapsed")
            .register(registry);
    }
}
