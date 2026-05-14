package com.looksee.services;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.config.TaskManagementConfigUtils;
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
 *
 * <p>The {@link ConditionalOnProperty} guard further requires
 * {@code pubsub.poison} to be explicitly set in the service's
 * configuration. Until issue #108 provisions the {@code looksee.poison}
 * Pub/Sub topic and operators opt the relevant services in, the adapter
 * stays unregistered: the controller's {@code poisonPublisher} autowire
 * remains null and the legacy 200 + metric behavior continues. This
 * prevents two failure modes during rollout: (a) staging outbox rows
 * for a topic that does not yet exist, and (b) staging rows in services
 * that scan {@code com.looksee*} but do not enable scheduling, where the
 * outbox publisher would never drain them.
 */
@Service
@ConditionalOnMissingBean(PoisonMessagePublisher.class)
@ConditionalOnProperty(name = "pubsub.poison")
public class OutboxPoisonMessagePublisher implements PoisonMessagePublisher {

    private final OutboxPublishingGateway outboxGateway;
    private final OutboxEventRepository outboxEventRepository;
    private final String poisonTopic;
    private final ApplicationContext applicationContext;

    @Autowired
    public OutboxPoisonMessagePublisher(
        OutboxPublishingGateway outboxGateway,
        // @Nullable, not @Autowired(required=false): the latter applied to
        // a single parameter inside an @Autowired constructor is not the
        // documented mechanism for per-parameter optionality and may leave
        // the bean unconstructable when the repository bean is absent
        // (sliced tests, Neo4j-disabled profiles), defeating the
        // fail-closed-at-call-time intent below.
        @Nullable OutboxEventRepository outboxEventRepository,
        @Value("${pubsub.poison}") String poisonTopic,
        ApplicationContext applicationContext
    ) {
        this.outboxGateway = outboxGateway;
        this.outboxEventRepository = outboxEventRepository;
        this.poisonTopic = poisonTopic;
        this.applicationContext = applicationContext;
    }

    /**
     * Fail loudly at startup when this bean is wired into a context that
     * has not enabled scheduling. {@link OutboxEventPublisher}'s
     * {@code @Scheduled} drain methods only fire when {@code @EnableScheduling}
     * is present somewhere in the application context; without it the
     * outbox rows we stage from {@link #publishPoison} would accumulate
     * unsent. This is the contract a future service migration could
     * accidentally break by setting {@code pubsub.poison} without also
     * wiring scheduling — refuse to start rather than silently leak
     * poison.
     */
    @PostConstruct
    void verifySchedulingEnabled() {
        if (!applicationContext.containsBean(
                TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
            throw new IllegalStateException(
                "OutboxPoisonMessagePublisher is registered (pubsub.poison=" + poisonTopic
                + ") but @EnableScheduling is not enabled in the application context."
                + " OutboxEventPublisher's @Scheduled drain methods will not fire, so"
                + " poison rows would accumulate in the outbox without ever being"
                + " published. Add @EnableScheduling to your service configuration."
            );
        }
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
