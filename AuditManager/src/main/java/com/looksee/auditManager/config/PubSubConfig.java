package com.looksee.auditManager.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.looksee.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.services.AuditRecordService;
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
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Postcondition:</b> Each factory method returns a non-null,
 *       fully-constructed instance ready for dependency injection.</li>
 * </ul>
 */
@Configuration
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
}
