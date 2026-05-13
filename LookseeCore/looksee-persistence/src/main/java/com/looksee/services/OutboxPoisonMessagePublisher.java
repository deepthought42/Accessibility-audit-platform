package com.looksee.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.looksee.messaging.poison.PoisonMessagePublisher;
import com.looksee.models.message.PoisonMessageEnvelope;

/**
 * Outbox-backed adapter implementing {@link PoisonMessagePublisher}.
 * Delegates to {@link OutboxPublishingGateway#enqueueOutOfBand} so the
 * poison row commits in its own transaction — independent of whatever
 * caller transaction is rolling back when the original message turned
 * out to be unprocessable.
 *
 * <p>Exceptions from the gateway are intentionally not caught here:
 * a failure to stage the poison row must surface to
 * {@code PubSubAuditController}, which will return HTTP 500 and let
 * Pub/Sub redeliver the original message so a later attempt can poison-
 * publish it. Duplicate poison rows on retry are acceptable; silent loss
 * is not.
 */
@Service
public class OutboxPoisonMessagePublisher implements PoisonMessagePublisher {

    private final OutboxPublishingGateway outboxGateway;
    private final String poisonTopic;

    @Autowired
    public OutboxPoisonMessagePublisher(
        OutboxPublishingGateway outboxGateway,
        @Value("${pubsub.poison:looksee.poison}") String poisonTopic
    ) {
        this.outboxGateway = outboxGateway;
        this.poisonTopic = poisonTopic;
    }

    @Override
    public void publishPoison(PoisonMessageEnvelope envelope, String correlationId) {
        outboxGateway.enqueueOutOfBand(poisonTopic, envelope, correlationId);
    }
}
