package com.looksee.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import com.looksee.messaging.poison.PoisonMessagePublisher;
import com.looksee.models.repository.OutboxEventRepository;
import com.looksee.services.OutboxPoisonMessagePublisher;
import com.looksee.services.OutboxPublishingGateway;

/**
 * Registers {@link OutboxPoisonMessagePublisher} as the default
 * {@link PoisonMessagePublisher} implementation when:
 *
 * <ul>
 *   <li>{@code pubsub.poison} is explicitly set — until issue #108
 *       provisions the {@code looksee.poison} Pub/Sub topic and
 *       operators opt their service in, the adapter stays unregistered
 *       so {@link com.looksee.messaging.web.PubSubAuditController}'s
 *       optional autowire is null and the legacy 200 + metric behavior
 *       continues; and</li>
 *   <li>no other {@link PoisonMessagePublisher} bean has been registered,
 *       so test- or profile-supplied overrides take precedence without
 *       producing an ambiguous autowire.</li>
 * </ul>
 *
 * <p>This registration deliberately lives in an auto-configuration
 * class rather than on {@code @Service} component scanning. Spring's
 * {@code @ConditionalOnMissingBean} is documented as reliable only on
 * auto-configuration, because Spring orders auto-configuration after
 * user {@code @Configuration} and component scanning; with a
 * component-scanned {@code @Service}, the override could be processed
 * after the default and Spring would register both, breaking the
 * controller's autowire.
 */
@Configuration
@ConditionalOnProperty(name = "pubsub.poison")
public class PoisonMessagingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PoisonMessagePublisher.class)
    public OutboxPoisonMessagePublisher outboxPoisonMessagePublisher(
        OutboxPublishingGateway outboxGateway,
        @Nullable OutboxEventRepository outboxEventRepository,
        @Value("${pubsub.poison}") String poisonTopic,
        ApplicationContext applicationContext
    ) {
        return new OutboxPoisonMessagePublisher(
            outboxGateway, outboxEventRepository, poisonTopic, applicationContext);
    }
}
