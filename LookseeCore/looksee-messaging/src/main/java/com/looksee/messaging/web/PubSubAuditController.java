package com.looksee.messaging.web;

import java.util.Base64;

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
            pubSubMetrics.recordInvalid(serviceName(), topicName());
            log.warn("invalid pubsub payload received in {}", serviceName());
            return new ResponseEntity<>("Invalid pubsub payload", HttpStatus.BAD_REQUEST);
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
}
