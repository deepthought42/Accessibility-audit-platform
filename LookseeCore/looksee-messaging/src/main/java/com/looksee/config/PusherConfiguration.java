package com.looksee.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.pusher.rest.Pusher;

/**
 * Configuration class for Pusher client used within the full LookseeCore auto-configuration.
 * 
 * This is the single source of truth for Pusher client creation, providing both real and fallback clients.
 * MessageBroadcaster configuration imports this to ensure a Pusher bean is always available.
 */
@Configuration
@EnableConfigurationProperties({PusherProperties.class})
public class PusherConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(PusherConfiguration.class);
    
    /**
     * Constructor for the PusherConfiguration class.
     * Logs a message when the PusherConfiguration is loaded.
     */
    public PusherConfiguration() {
        log.info("🔧 PusherConfiguration loaded - part of full LookseeCore auto-configuration");
    }
    
    /**
     * Creates a fully functional Pusher client bean using the PusherProperties configuration.
     * 
     * This bean is only created when all required Pusher properties are provided.
     * The properties can be configured via:
     * - application.properties: pusher.appId, pusher.key, pusher.secret, pusher.cluster
     * - Environment variables: PUSHER_APP_ID, PUSHER_KEY, PUSHER_SECRET, PUSHER_CLUSTER
     * 
     * @param pusherProperties the Pusher configuration properties
     * @return configured Pusher client
     */
    @Bean
    @ConditionalOnProperty(prefix = "pusher", name = {"appId", "key", "secret", "cluster"})
    public Pusher pusherClient(PusherProperties pusherProperties) {
        log.info("Configuring Pusher client with app ID: {}, cluster: {}", 
                 pusherProperties.getAppId(), pusherProperties.getCluster());
        
        // Validate that all required properties are present
        if (pusherProperties.getAppId() == null || pusherProperties.getAppId().trim().isEmpty()) {
            throw new IllegalArgumentException("Pusher appId is required but not configured");
        }
        if (pusherProperties.getKey() == null || pusherProperties.getKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Pusher key is required but not configured");
        }
        if (pusherProperties.getSecret() == null || pusherProperties.getSecret().trim().isEmpty()) {
            throw new IllegalArgumentException("Pusher secret is required but not configured");
        }
        if (pusherProperties.getCluster() == null || pusherProperties.getCluster().trim().isEmpty()) {
            throw new IllegalArgumentException("Pusher cluster is required but not configured");
        }
        
        Pusher pusher = new Pusher(pusherProperties.getAppId(), pusherProperties.getKey(), pusherProperties.getSecret());
        pusher.setCluster(pusherProperties.getCluster());
        pusher.setEncrypted(pusherProperties.isEncrypted());
        
        log.info("Pusher client successfully configured and ready for use");
        return pusher;
    }
    
    /**
     * Creates a fallback no-op Pusher client when real Pusher properties are not configured.
     * This ensures MessageBroadcaster can always be instantiated as a required dependency.
     * 
     * The fallback client logs operations but doesn't actually send messages.
     * 
     * @return fallback Pusher client
     */
    @Bean
    @ConditionalOnMissingBean(Pusher.class)
    public Pusher fallbackPusherClient() {
        log.warn("Creating fallback Pusher client - real-time messaging will be disabled");
        log.warn("To enable Pusher functionality, configure: pusher.appId, pusher.key, pusher.secret, pusher.cluster");
        
        // Create a basic Pusher instance that will be used for logging only
        // The actual trigger methods won't be called in the MessageBroadcaster when using fallback
        Pusher fallbackPusher = new Pusher("fallback-app-id", "fallback-key", "fallback-secret");
        fallbackPusher.setCluster("fallback-cluster");
        
        log.info("Fallback Pusher client created - MessageBroadcaster will detect this and log operations only");
        return fallbackPusher;
    }
    
    /**
     * Diagnostic bean to log Pusher configuration status during application startup.
     * This helps identify why Pusher/MessageBroadcaster beans might not be created.
     * 
     * @param environment the environment
     * @return the pusher diagnostic listener
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> pusherDiagnosticListener(Environment environment) {
        return event -> {
            log.info("=== Pusher Configuration Diagnostic ===");
            
            // Check each required property
            String appId = environment.getProperty("pusher.appId");
            String key = environment.getProperty("pusher.key"); 
            String secret = environment.getProperty("pusher.secret");
            String cluster = environment.getProperty("pusher.cluster");
            
            log.info("pusher.appId: {}", appId != null ? (appId.isEmpty() ? "<EMPTY>" : "<SET>") : "<NULL>");
            log.info("pusher.key: {}", key != null ? (key.isEmpty() ? "<EMPTY>" : "<SET>") : "<NULL>");
            log.info("pusher.secret: {}", secret != null ? (secret.isEmpty() ? "<EMPTY>" : "<SET>") : "<NULL>");
            log.info("pusher.cluster: {}", cluster != null ? (cluster.isEmpty() ? "<EMPTY>" : "<SET>") : "<NULL>");
            
            boolean allPropertiesSet = appId != null && !appId.trim().isEmpty() &&
                                     key != null && !key.trim().isEmpty() &&
                                     secret != null && !secret.trim().isEmpty() &&
                                     cluster != null && !cluster.trim().isEmpty();
            
            if (allPropertiesSet) {
                log.info("✅ All required Pusher properties are configured - Real-time messaging ENABLED");
                log.info("✅ MessageBroadcaster will use fully functional Pusher client");
            } else {
                log.warn("⚠️  Missing required Pusher properties - Using fallback client");
                log.warn("⚠️  Real-time messaging is DISABLED (messages will be logged only)");
                log.warn("✅ MessageBroadcaster will still be available as a required dependency");
                log.warn("💡 To enable real-time messaging, set: PUSHER_APP_ID, PUSHER_KEY, PUSHER_SECRET, PUSHER_CLUSTER");
            }
            
            log.info("🎯 MessageBroadcaster bean should be available via MessageBroadcasterAutoConfiguration");
            log.info("=== End Pusher Configuration Diagnostic ===");
        };
    }
} 