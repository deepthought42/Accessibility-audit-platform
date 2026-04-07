package com.looksee.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.concurrent.ListenableFuture;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.looksee.messaging.observability.TraceContextPropagation;

/**
 * Thin wrapper around {@link PubSubTemplate} that automatically injects the
 * active OpenTelemetry context as Pub/Sub message attributes.
 *
 * <p>This is the <b>producer-side</b> half of the Wave 2.2 distributed
 * tracing contract: the consumer-side {@link
 * com.looksee.messaging.web.PubSubAuditController} extracts trace context
 * from {@code body.getMessage().getAttributes()} on every inbound message,
 * and this class injects the same attributes on every outbound message. A
 * service can opt in simply by autowiring {@code TracingPubSubPublisher}
 * instead of {@link PubSubTemplate}.</p>
 *
 * <p><b>Why not bake this into {@code PubSubTemplate} directly?</b> The
 * existing {@link com.looksee.services.OutboxEventPublisher} in
 * {@code looksee-persistence} has a test suite that mocks {@code
 * PubSubTemplate.publish(String, String)}. Swapping that call site to the
 * {@code Message<T>} overload so it can carry headers breaks all of those
 * mocks. Providing a separate wrapper lets direct in-request publishers
 * opt into tracing without forcing every existing caller to be rewritten
 * in the same commit.</p>
 *
 * <p><b>Thread-safety:</b> stateless beyond the injected {@link
 * PubSubTemplate}, which is itself thread-safe per Spring Cloud GCP's
 * contract. Safe to share as a singleton bean.</p>
 */
public class TracingPubSubPublisher {

    private static final Logger log = LoggerFactory.getLogger(TracingPubSubPublisher.class);

    private final PubSubTemplate pubSubTemplate;

    public TracingPubSubPublisher(PubSubTemplate pubSubTemplate) {
        this.pubSubTemplate = pubSubTemplate;
    }

    /**
     * Publish a String payload to the given topic, attaching the current
     * OpenTelemetry context as W3C {@code traceparent}/{@code tracestate}
     * message attributes so downstream consumers can join the same trace.
     *
     * <p>When no span is active on the calling thread the attribute map is
     * empty and the message is published without trace metadata. This is
     * intentional: the caller may still want the message to flow, and the
     * consumer-side extraction falls back to {@code Context.current()} so
     * no NPE results.</p>
     *
     * @param topic   the Pub/Sub topic name (short form, not the full path)
     * @param payload the JSON-serialized message body
     * @return a {@link ListenableFuture} that completes when the emulator
     *         or real Pub/Sub service has acknowledged the publish. Spring
     *         Cloud GCP 3.x's {@code PubSubTemplate.publish(Message)}
     *         returns Spring's {@code ListenableFuture}, not Google's
     *         {@code ApiFuture}; Wave 4 of the architecture review's move
     *         to Spring Cloud GCP 5 will promote this to {@code
     *         CompletableFuture}.
     */
    public ListenableFuture<String> publish(String topic, String payload) {
        Map<String, String> attributes = new HashMap<>();
        TraceContextPropagation.inject(attributes);
        if (log.isDebugEnabled()) {
            log.debug("publishing to topic={} with {} trace attribute(s)",
                topic, attributes.size());
        }
        return pubSubTemplate.publish(
            topic,
            MessageBuilder.withPayload(payload)
                .copyHeaders(attributes)
                .build()
        );
    }

    /**
     * Convenience variant that blocks on the publish future and rethrows
     * any failure as an unchecked exception. Use this only in contexts
     * where blocking is acceptable (scheduled jobs, tests).
     */
    public void publishAndWait(String topic, String payload) {
        try {
            publish(topic, payload).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted publishing to " + topic, e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("failed to publish to " + topic, e.getCause());
        }
    }

    /** Exposed for tests and callers that need to fall back to untraced publishing. */
    public PubSubTemplate getDelegate() {
        return pubSubTemplate;
    }
}
