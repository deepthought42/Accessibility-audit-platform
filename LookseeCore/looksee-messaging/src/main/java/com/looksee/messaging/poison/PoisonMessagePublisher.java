package com.looksee.messaging.poison;

import com.looksee.models.message.PoisonMessageEnvelope;

/**
 * Port for publishing a non-retryable Pub/Sub message to the shared
 * {@code looksee.poison} topic. Implementations live in
 * {@code looksee-persistence} and route through the transactional
 * outbox; the base controller in {@code looksee-messaging} depends only
 * on this interface so the module graph stays acyclic.
 *
 * <p>Implementations MUST propagate any underlying publish exception.
 * {@code PubSubAuditController} relies on the throw to abandon its
 * idempotency claim and return HTTP 500, letting Pub/Sub redeliver the
 * original message. Duplicate poison publishes on retry are acceptable;
 * silent loss of a poison message is not.
 */
public interface PoisonMessagePublisher {

    /**
     * Publish the envelope to the poison topic.
     *
     * @param envelope      structured wrapper around the original message
     * @param correlationId W3C {@code traceparent} stamped onto the outbox
     *                      row so the poison publish joins the original
     *                      distributed trace; may be {@code null} when no
     *                      inbound trace was present
     */
    void publishPoison(PoisonMessageEnvelope envelope, String correlationId);
}
