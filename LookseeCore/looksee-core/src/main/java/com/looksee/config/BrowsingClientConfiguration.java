package com.looksee.config;

import com.looksee.browsing.client.BrowsingClient;
import com.looksee.browsing.client.BrowsingClientConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link LookseeBrowsingProperties} and, when
 * {@code looksee.browsing.mode=remote}, a {@link BrowsingClient} bean.
 *
 * <p>In local mode (the default) no {@code BrowsingClient} is instantiated —
 * consumers that don't opt in carry zero runtime overhead from this phase.
 */
@Configuration
@EnableConfigurationProperties(LookseeBrowsingProperties.class)
public class BrowsingClientConfiguration {

    @Bean
    @ConditionalOnProperty(name = "looksee.browsing.mode", havingValue = "remote")
    public BrowsingClient browsingClient(LookseeBrowsingProperties props) {
        return new BrowsingClient(new BrowsingClientConfig(
            props.getServiceUrl(),
            props.getConnectTimeout(),
            props.getReadTimeout()));
    }
}
