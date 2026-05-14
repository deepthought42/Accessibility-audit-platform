package com.looksee.services;

import javax.annotation.PostConstruct;

import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

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
 * <p>This class is deliberately not annotated {@code @Service}. Spring's
 * {@code @ConditionalOnMissingBean} is reliable only when evaluated
 * against an ordered processing model — component scanning is not
 * ordered relative to user-supplied overrides, so a {@code @Service}
 * with that condition can race a test or profile bean. Registration is
 * performed instead by {@link com.looksee.config.PoisonMessagingAutoConfiguration},
 * which is loaded after user configuration and after the audit-service
 * specific {@code @Bean} in {@code AuditManager.PubSubConfig}. The
 * property-and-port-type gating lives on those registration sites.
 */
public class OutboxPoisonMessagePublisher implements PoisonMessagePublisher {

    private final OutboxPublishingGateway outboxGateway;
    private final OutboxEventRepository outboxEventRepository;
    private final String poisonTopic;
    private final ApplicationContext applicationContext;

    public OutboxPoisonMessagePublisher(
        OutboxPublishingGateway outboxGateway,
        OutboxEventRepository outboxEventRepository,
        String poisonTopic,
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
