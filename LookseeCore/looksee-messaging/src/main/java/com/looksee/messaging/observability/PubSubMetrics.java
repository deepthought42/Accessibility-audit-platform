package com.looksee.messaging.observability;

import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Counter;
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
}
