package com.looksee.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import com.looksee.messaging.poison.PoisonMessagePublisher;
import com.looksee.models.message.PoisonMessageEnvelope;
import com.looksee.models.repository.OutboxEventRepository;

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
 *
 * <p>{@link OutboxPublishingGateway#save} no-ops when its
 * {@link OutboxEventRepository} autowire is null (logs a warning and
 * returns). That would let {@link #publishPoison} return successfully
 * while no poison row was actually staged — defeating the whole point of
 * the path. This adapter therefore verifies the repository is wired on
 * every call and fails closed when it is not, so the controller sees a
 * 500 and Pub/Sub redelivers instead of silently dropping a poison
 * message.
 *
 * <p>The {@link ConditionalOnMissingBean} guard scopes registration to
 * the port type rather than the concrete class. Several services
 * component-scan {@code com.looksee*}, which would otherwise register
 * this default alongside a test- or profile-supplied
 * {@link PoisonMessagePublisher} and make the controller's autowire
 * ambiguous.
 */
@Service
@ConditionalOnMissingBean(PoisonMessagePublisher.class)
public class OutboxPoisonMessagePublisher implements PoisonMessagePublisher {

    private final OutboxPublishingGateway outboxGateway;
    private final OutboxEventRepository outboxEventRepository;
    private final String poisonTopic;

    @Autowired
    public OutboxPoisonMessagePublisher(
        OutboxPublishingGateway outboxGateway,
        @Autowired(required = false) OutboxEventRepository outboxEventRepository,
        @Value("${pubsub.poison:looksee.poison}") String poisonTopic
    ) {
        this.outboxGateway = outboxGateway;
        this.outboxEventRepository = outboxEventRepository;
        this.poisonTopic = poisonTopic;
    }

    @Override
    public void publishPoison(PoisonMessageEnvelope envelope, String correlationId) {
        if (outboxEventRepository == null) {
            throw new IllegalStateException(
                "OutboxEventRepository is not wired; poison publish to topic="
                + poisonTopic + " would be silently dropped");
        }
        outboxGateway.enqueueOutOfBand(poisonTopic, envelope, correlationId);
    }
}
