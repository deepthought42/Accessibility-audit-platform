package com.looksee.auditManager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.looksee.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.services.AuditRecordService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.OutboxEventPublisher;
import com.looksee.services.OutboxPoisonMessagePublisher;
import com.looksee.services.OutboxPublishingGateway;
import com.looksee.services.PageStateService;

/**
 * Manual bean definitions for LookseeCore components used by this service.
 *
 * <p>These beans are defined here because
 * {@link com.looksee.LookseeCoreAutoConfiguration} is excluded from
 * auto-configuration (see {@link com.looksee.auditManager.Application}) to
 * avoid a circular-import issue. Each bean is annotated with
 * {@link ConditionalOnMissingBean} so that test or profile-specific overrides
 * take precedence.
 *
 * <p>{@link EnableScheduling} is on this config so the {@link OutboxEventPublisher}
 * scheduled poll runs and the audit-manager service can drain its own
 * outbox writes without depending on another deployment.</p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Postcondition:</b> Each factory method returns a non-null,
 *       fully-constructed instance ready for dependency injection.</li>
 * </ul>
 */
@Configuration
@EnableScheduling
public class PubSubConfig {

    /**
     * Provides the Pub/Sub publisher used to emit page-audit messages.
     *
     * @return a new {@link PubSubPageAuditPublisherImpl} instance; never {@code null}
     */
    @Bean(name = "audit_record_topic")
    @ConditionalOnMissingBean(name = "audit_record_topic")
    public PubSubPageAuditPublisherImpl auditRecordTopic() {
        return new PubSubPageAuditPublisherImpl();
    }

    /**
     * Provides the service for persisting and querying audit records in Neo4j.
     *
     * @return a new {@link AuditRecordService} instance; never {@code null}
     */
    @Bean(name = "audit_record_service")
    @ConditionalOnMissingBean(name = "audit_record_service")
    public AuditRecordService auditRecordService() {
        return new AuditRecordService();
    }

    /**
     * Provides the service for querying page state and landability.
     *
     * @return a new {@link PageStateService} instance; never {@code null}
     */
    @Bean(name = "page_state_service")
    @ConditionalOnMissingBean(name = "page_state_service")
    public PageStateService pageStateService() {
        return new PageStateService();
    }

    /**
     * Provides the atomic idempotency guard used by the inherited
     * {@link com.looksee.messaging.web.PubSubAuditController}. Without this
     * bean the base class's {@code @Autowired IdempotencyGuard} cannot be
     * satisfied, since {@link IdempotencyService} lives in
     * {@code com.looksee.services} and is not picked up by audit-manager's
     * narrow component scan.
     */
    @Bean
    @ConditionalOnMissingBean
    public IdempotencyService idempotencyService() {
        return new IdempotencyService();
    }

    /**
     * Provides the transactional outbox staging gateway. Required by
     * {@link com.looksee.auditManager.AuditController#handle(com.looksee.models.message.PageBuiltMessage)}
     * so PageAudit publishes commit or roll back with the audit-record
     * domain writes. Counterpart bean to {@link #outboxEventPublisher()}
     * which drains the staged rows out to Pub/Sub.
     */
    @Bean
    @ConditionalOnMissingBean
    public OutboxPublishingGateway outboxPublishingGateway() {
        return new OutboxPublishingGateway();
    }

    /**
     * Provides the outbox-backed adapter that lets the inherited
     * {@link com.looksee.messaging.web.PubSubAuditController} stage
     * unprocessable messages to the shared {@code looksee.poison} topic.
     * The {@code looksee-messaging} module exposes only the
     * {@code PoisonMessagePublisher} port, so the concrete adapter has
     * to be wired here for audit-manager's narrow component scan to
     * pick it up.
     */
    @Bean
    @ConditionalOnMissingBean
    public OutboxPoisonMessagePublisher outboxPoisonMessagePublisher(
        OutboxPublishingGateway outboxGateway,
        @Value("${pubsub.poison:looksee.poison}") String poisonTopic
    ) {
        return new OutboxPoisonMessagePublisher(outboxGateway, poisonTopic);
    }

    /**
     * Provides the background poller that drains the {@code OutboxEvent}
     * table to Pub/Sub. Wired here so audit-manager processes events it
     * has staged without relying on another service to do the draining;
     * @{@link EnableScheduling} on this config activates the publisher's
     * {@code @Scheduled} methods.
     */
    @Bean
    @ConditionalOnMissingBean
    public OutboxEventPublisher outboxEventPublisher() {
        return new OutboxEventPublisher();
    }
}

