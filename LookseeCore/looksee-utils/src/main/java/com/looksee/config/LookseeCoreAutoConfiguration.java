package com.looksee.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.looksee.gcp.GoogleCloudStorageProperties;

/**
 * Main auto-configuration class for LookseeCore library.
 * 
 * This configuration serves as the entry point for LookseeCore auto-configuration
 * and imports focused configuration classes to avoid circular dependencies.
 * 
 * This auto-configuration will be automatically loaded by Spring Boot when this
 * library is included as a dependency in other Spring Boot applications.
 */
@Configuration
@EnableConfigurationProperties({LookseeCoreProperties.class, GoogleCloudStorageProperties.class, PusherProperties.class, SeleniumProperties.class})
@ConditionalOnProperty(prefix = "looksee.core", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
    LookseeCoreComponentConfiguration.class,
    LookseeCoreRepositoryConfiguration.class,
    PusherConfiguration.class,
    SeleniumConfiguration.class
})
public class LookseeCoreAutoConfiguration {
    
    /**
     * This class serves as the main entry point for LookseeCore auto-configuration.
     * 
     * The configuration is split into focused components:
     * 1. LookseeCoreComponentConfiguration - handles component scanning
     * 2. LookseeCoreRepositoryConfiguration - handles Neo4j repository configuration
     * 3. PusherConfiguration - handles Pusher client configuration
     * 4. SeleniumConfiguration - handles Selenium WebDriver configuration (optional)
     * 
     * This approach prevents circular dependencies and provides better separation of concerns.
     * 
     * The configuration can be disabled by setting looksee.core.enabled=false
     * in the consuming application's configuration.
     */
} 