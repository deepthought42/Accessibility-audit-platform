package com.looksee.messaging.observability;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.looksee.messaging.TracingPubSubPublisher;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Spring auto-configuration that wires the shared observability components
 * into any service that depends on the {@code A11yMessaging} module.
 *
 * <p>Currently registers:</p>
 * <ul>
 *   <li>A fallback {@link MeterRegistry} when the host application does
 *       not already provide one.</li>
 *   <li>The shared {@link PubSubMetrics} counters/timers.</li>
 *   <li>A {@link TracingPubSubPublisher} wrapper around the service's
 *       {@link PubSubTemplate}, so direct in-request publishers pick up
 *       Wave 2.2 producer-side trace propagation for free.</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
public class MessagingObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MeterRegistry lookseeFallbackMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public PubSubMetrics pubSubMetrics(MeterRegistry registry) {
        return new PubSubMetrics(registry);
    }

    /**
     * Only registered when a {@link PubSubTemplate} is present, which is
     * the case in every service that talks to Pub/Sub and is NOT the case
     * in LookseeCore's own unit tests (they mock PubSubTemplate directly).
     * The {@code @ConditionalOnBean} keeps the auto-config harmless in both
     * environments.
     */
    @Bean
    @ConditionalOnBean(PubSubTemplate.class)
    @ConditionalOnMissingBean
    public TracingPubSubPublisher tracingPubSubPublisher(PubSubTemplate pubSubTemplate) {
        return new TracingPubSubPublisher(pubSubTemplate);
    }
}
