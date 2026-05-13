package com.looksee.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.looksee.models.OutboxEvent;
import com.looksee.models.OutboxSerializationException;
import com.looksee.models.config.JacksonConfig;
import com.looksee.models.repository.OutboxEventRepository;

/**
 * Single entry point for routing every cross-service Pub/Sub publish through
 * the transactional outbox.
 *
 * <p>Callers do not interact with {@code OutboxEventRepository} directly. They
 * call {@link #enqueue} from inside the same {@code @Transactional} method
 * that wrote their domain changes — the outbox row commits or rolls back with
 * the rest of the transaction, eliminating the "DB committed but publish
 * failed" failure mode.</p>
 *
 * <p>For error publishes that must survive a rolling-back outer transaction
 * (e.g. a {@code catch} block whose surrounding handler is about to throw),
 * use {@link #enqueueOutOfBand} — it runs in {@code REQUIRES_NEW} so the
 * outbox row commits independently of the caller's fate.</p>
 *
 * <p>The asynchronous {@link OutboxEventPublisher} polls these rows on a
 * scheduled background thread and delivers them to Pub/Sub with exponential
 * backoff and W3C trace-context propagation.</p>
 */
@Service
public class OutboxPublishingGateway {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublishingGateway.class);

    @Autowired(required = false)
    private OutboxEventRepository outboxEventRepository;

    /**
     * Stage a Pub/Sub publish inside the caller's existing transaction.
     *
     * <p>Fails fast with {@code IllegalTransactionStateException} when called
     * outside an {@code @Transactional} method — this is intentional, since
     * the entire point of the outbox is to share atomicity with the domain
     * write that produced the event.</p>
     *
     * @param topic         the Pub/Sub topic name (typically resolved via
     *                      {@code somePublisherImpl.getTopic()})
     * @param payload       any object Jackson can serialize to JSON; the
     *                      payload-side {@code MessageBuilder} of the
     *                      original publisher impl is intentionally bypassed
     * @param correlationId the W3C {@code traceparent} header value of the
     *                      inbound message that triggered this publish, or
     *                      {@code null} when no inbound trace exists
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(String topic, Object payload, String correlationId) {
        save(topic, payload, correlationId);
    }

    /**
     * Stage a Pub/Sub publish in a fresh transaction independent of the
     * caller's tx state. Use only for error publishes from {@code catch}
     * blocks where the outer transaction is rolling back but the error
     * signal must survive.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueOutOfBand(String topic, Object payload, String correlationId) {
        save(topic, payload, correlationId);
    }

    private void save(String topic, Object payload, String correlationId) {
        if (outboxEventRepository == null) {
            log.warn("outbox repository not wired; dropping publish to topic={}", topic);
            return;
        }
        String json = serialize(payload);
        OutboxEvent event = new OutboxEvent(topic, json, correlationId);
        outboxEventRepository.save(event);
        if (log.isDebugEnabled()) {
            log.debug("outbox event staged: topic={} eventId={} correlationId={}",
                topic, event.getEventId(), correlationId);
        }
    }

    private String serialize(Object payload) {
        if (payload instanceof String) {
            return (String) payload;
        }
        try {
            return JacksonConfig.mapper().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new OutboxSerializationException(payload, e);
        }
    }
}
