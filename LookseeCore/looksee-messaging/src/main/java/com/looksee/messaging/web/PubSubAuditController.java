package com.looksee.messaging.web;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.mapper.Body;
import com.looksee.messaging.idempotency.IdempotencyGuard;
import com.looksee.messaging.observability.PubSubMetrics;
import com.looksee.messaging.observability.TraceContextPropagation;
import com.looksee.messaging.poison.PoisonMessagePublisher;
import com.looksee.models.message.PoisonMessageEnvelope;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

/**
 * Reusable Pub/Sub push-subscription controller for every Looksee audit
 * service. Implements the boilerplate that was previously duplicated in 13
 * per-service {@code AuditController.java} classes:
 *
 * <ul>
 *   <li>Pub/Sub envelope null/blank validation.</li>
 *   <li>Idempotency check via the shared {@link IdempotencyService}.</li>
 *   <li>Base64 decode of the message data.</li>
 *   <li>Distributed-tracing context extraction (Wave 2.2) and span creation.</li>
 *   <li>Standard metrics emission via {@link PubSubMetrics} (Wave 2.1).</li>
 *   <li>Uniform error handling with HTTP-level acknowledgement so that
 *       Pub/Sub does not enter an infinite retry loop on poison messages.</li>
 * </ul>
 *
 * <p>Subclasses provide the strongly-typed payload class and a {@link
 * #handle(Object)} method that contains the per-service business logic.
 * Both the message routing and the per-service-name tagging are intentionally
 * abstract so each service can keep its existing service-name strings stable
 * for downstream dashboards.</p>
 *
 * <p>Migration order from the architecture review plan:
 * audit-service → journey-map-cleanup → journeyErrors → journeyExecutor →
 * journeyExpander → element-enrichment → informationArchitectureAudit →
 * visualDesignAudit → contentAudit → look-see-front-end-broadcaster →
 * AuditManager → PageBuilder → CrawlerAPI.</p>
 *
 * @param <T> the payload type this controller deserializes
 */
public abstract class PubSubAuditController<T> {

    private static final Logger log = LoggerFactory.getLogger(PubSubAuditController.class);

    @Autowired
    protected IdempotencyGuard idempotencyService;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PubSubMetrics pubSubMetrics;

    /**
     * Optional publisher for non-retryable messages. When wired (services
     * with {@code looksee-persistence} on the classpath and an
     * {@code OutboxPoisonMessagePublisher} bean defined), the three
     * poison branches below stage the original message to
     * {@code looksee.poison} via the outbox so operators have a single
     * subscription capturing every message the platform couldn't
     * understand. When absent, the publish path is skipped and the
     * existing 200 + metric behavior is unchanged.
     */
    @Autowired(required = false)
    protected PoisonMessagePublisher poisonPublisher;

    /** Stable service name used as a metric tag and as the idempotency namespace. */
    protected abstract String serviceName();

    /** Topic name for metric tagging; defaults to {@code unknown} if not overridden. */
    protected String topicName() {
        return "unknown";
    }

    /** Concrete payload type. Used for Jackson deserialization. */
    protected abstract Class<T> payloadType();

    /**
     * Per-service business logic. Implementations should be idempotent on
     * retry: the framework will only call this method once for a given
     * Pub/Sub messageId, but at-least-once delivery is the underlying
     * contract.
     */
    protected abstract void handle(T payload) throws Exception;

    @PostMapping("/")
    public ResponseEntity<String> receiveMessage(@RequestBody Body body) {
        if (body == null || body.getMessage() == null
                || body.getMessage().getData() == null
                || body.getMessage().getData().isBlank()) {
            // Empty envelope is poison: Pub/Sub will never deliver content
            // we can act on. Acknowledge with 200 so the message drains
            // instead of looping until retention.
            pubSubMetrics.recordInvalid(serviceName(), topicName());
            log.warn("invalid pubsub payload received in {}, acknowledging", serviceName());
            // poison-publish failures intentionally escalate to 500 — see #102
            try {
                emitPoison(body == null ? null : body.getMessage(), "EmptyEnvelope",
                    "envelope was null or missing data");
            } catch (RuntimeException poisonFailure) {
                pubSubMetrics.recordError(serviceName(), topicName(), poisonFailure);
                log.error("failed to publish empty-envelope poison in {}", serviceName(), poisonFailure);
                return new ResponseEntity<>(
                    "poison publish failed: " + poisonFailure.getClass().getSimpleName(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return ResponseEntity.ok("Invalid pubsub payload, acknowledged");
        }

        String messageId = body.getMessage().getMessageId();
        if (!idempotencyService.claim(messageId, serviceName())) {
            pubSubMetrics.recordDuplicate(serviceName(), topicName());
            return ResponseEntity.ok("Duplicate message, already processed");
        }

        Context parent = TraceContextPropagation.extract(body.getMessage().getAttributes());
        Tracer tracer = GlobalOpenTelemetry.getTracer("com.looksee.messaging");
        long start = System.nanoTime();

        try (Scope parentScope = parent.makeCurrent()) {
            Span span = tracer.spanBuilder("pubsub.handle." + serviceName())
                .setAttribute("looksee.service", serviceName())
                .setAttribute("looksee.topic", topicName())
                .setAttribute("messaging.system", "google_pubsub")
                .setAttribute("messaging.message.id", messageId)
                .startSpan();
            try (Scope spanScope = span.makeCurrent()) {
                byte[] decoded = Base64.getDecoder().decode(body.getMessage().getData());
                T payload = objectMapper.readValue(decoded, payloadType());
                handle(payload);
                pubSubMetrics.recordSuccess(serviceName(), topicName());
                return ResponseEntity.ok("ok");
            } catch (IllegalArgumentException e) {
                // Bad base64 — acknowledge so Pub/Sub does not retry forever.
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, "invalid_base64");
                pubSubMetrics.recordError(serviceName(), topicName(), e);
                log.warn("invalid base64 payload in {}, acknowledging", serviceName(), e);
                ResponseEntity<String> poisonFailure = tryEmitPoison(
                    body.getMessage(), e, messageId, span);
                if (poisonFailure != null) {
                    return poisonFailure;
                }
                return ResponseEntity.ok("Invalid payload, acknowledged");
            } catch (JsonProcessingException e) {
                // Malformed or schema-incompatible JSON — re-delivery cannot
                // succeed, so acknowledge as poison rather than loop forever.
                // Especially important for dead-letter consumers (journeyErrors)
                // where stale-schema payloads are routine.
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, "invalid_json");
                pubSubMetrics.recordError(serviceName(), topicName(), e);
                log.warn("invalid json payload in {}, acknowledging", serviceName(), e);
                ResponseEntity<String> poisonFailure = tryEmitPoison(
                    body.getMessage(), e, messageId, span);
                if (poisonFailure != null) {
                    return poisonFailure;
                }
                return ResponseEntity.ok("Invalid payload, acknowledged");
            } catch (Exception e) {
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, e.getClass().getSimpleName());
                pubSubMetrics.recordError(serviceName(), topicName(), e);
                log.error("processing error in {} for messageId={}", serviceName(), messageId, e);
                // Release the eager claim so Pub/Sub's redelivery is allowed to
                // re-run handle(); otherwise the next attempt would short-circuit
                // as a duplicate and the message would be silently dropped.
                idempotencyService.release(messageId, serviceName());
                return new ResponseEntity<>(
                    "processing error: " + e.getClass().getSimpleName(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
            } finally {
                pubSubMetrics.recordDuration(serviceName(), topicName(), System.nanoTime() - start);
                span.end();
            }
        }
    }

    /**
     * Build a {@link PoisonMessageEnvelope} and stage it via the optional
     * {@link PoisonMessagePublisher}. No-op when no publisher bean is
     * wired — services without {@code looksee-persistence} on their
     * classpath fall back to the legacy log-and-ack behavior. Any
     * exception thrown by the publisher is propagated; callers decide
     * how to surface a poison-publish failure to Pub/Sub.
     */
    private void emitPoison(Body.Message msg, String errorClass, String errorMessage) {
        if (poisonPublisher == null) {
            return;
        }
        String originalMessageId = msg == null ? null : msg.getMessageId();
        Map<String, String> attributes = msg == null ? Collections.emptyMap() : msg.getAttributes();
        String base64Data = msg == null ? null : msg.getData();
        // Prefer the active handler span (the bad-Base64 and bad-JSON
        // branches reach this method from inside the pubsub.handle.* span)
        // so the poison row joins the same trace as the inbound delivery.
        // Fall back to the inbound attributes for the empty-envelope path,
        // which runs before the span is started.
        String correlationId = TraceContextPropagation.currentTraceparent();
        if (correlationId == null) {
            correlationId = TraceContextPropagation.currentOrMintTraceparent(attributes);
        }
        PoisonMessageEnvelope envelope = new PoisonMessageEnvelope(
            serviceName(),
            topicName(),
            originalMessageId,
            attributes,
            base64Data,
            errorClass,
            errorMessage
        );
        poisonPublisher.publishPoison(envelope, correlationId);
    }

    /**
     * Wrap {@link #emitPoison} so a publish failure escalates to HTTP
     * 500 with metric + claim release, matching the contract recorded
     * on issue #102: duplicate poison publishes on retry are acceptable;
     * silent loss of a poison message is not. Returns {@code null} on
     * success (caller proceeds with its own 200 response).
     */
    private ResponseEntity<String> tryEmitPoison(
        Body.Message msg, Exception cause, String claimedMessageId, Span span
    ) {
        try {
            emitPoison(msg, cause.getClass().getSimpleName(), cause.getMessage());
            return null;
        } catch (RuntimeException poisonFailure) {
            span.recordException(poisonFailure);
            span.setStatus(StatusCode.ERROR, "poison_publish_failed");
            pubSubMetrics.recordError(serviceName(), topicName(), poisonFailure);
            log.error("failed to publish poison in {} for messageId={}",
                serviceName(), claimedMessageId, poisonFailure);
            idempotencyService.release(claimedMessageId, serviceName());
            return new ResponseEntity<>(
                "poison publish failed: " + poisonFailure.getClass().getSimpleName(),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
