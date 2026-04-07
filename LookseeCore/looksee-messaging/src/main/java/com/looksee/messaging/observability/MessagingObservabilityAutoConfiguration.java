package com.looksee.messaging.observability;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Spring auto-configuration that wires the shared {@link PubSubMetrics}
 * component into any service that includes the {@code A11yMessaging} module.
 *
 * <p>If the host application provides its own {@link MeterRegistry} (for
 * example via {@code micrometer-registry-stackdriver}) we use it directly.
 * Otherwise we fall back to a {@link SimpleMeterRegistry} so unit tests and
 * services without metrics export still get a working bean.</p>
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
}
