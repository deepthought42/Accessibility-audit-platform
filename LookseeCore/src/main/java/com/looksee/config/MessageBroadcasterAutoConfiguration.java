package com.looksee.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.looksee.services.MessageBroadcaster;
import com.pusher.rest.Pusher;

/**
 * Lightweight auto-configuration specifically for MessageBroadcaster.
 * 
 * This configuration ensures MessageBroadcaster is always available without
 * depending on Neo4j or other heavy dependencies that might not be configured
 * in consuming applications.
 * 
 * It imports PusherConfiguration to ensure a Pusher bean is available (either real or fallback),
 * then creates MessageBroadcaster using that Pusher bean.
 */
@Configuration
@Import(PusherConfiguration.class)
public class MessageBroadcasterAutoConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(MessageBroadcasterAutoConfiguration.class);
    
    /**
     * Constructor for the MessageBroadcasterAutoConfiguration class.
     * Logs a message when the MessageBroadcasterAutoConfiguration is loaded.
     */
    public MessageBroadcasterAutoConfiguration() {
        log.info("🎯 MessageBroadcasterAutoConfiguration loaded - ensuring MessageBroadcaster is always available");
        log.info("📦 Importing PusherConfiguration to provide Pusher client (real or fallback)");
    }
    
    /**
     * Creates MessageBroadcaster bean - always available regardless of other dependencies.
     * 
     * This method depends on a Pusher bean being available, which is guaranteed by the
     * imported PusherConfiguration (either real pusherClient or fallbackPusherClient).
     * 
     * @param pusher the Pusher client (real or fallback) provided by PusherConfiguration
     * @param environment Spring Environment for property access and fallback detection
     * @return MessageBroadcaster instance ready for use
     */
    @Bean
    public MessageBroadcaster messageBroadcaster(Pusher pusher, org.springframework.core.env.Environment environment) {
        log.info("✅ Creating MessageBroadcaster bean - guaranteed to be available");
        log.info("🔗 Using Pusher bean provided by PusherConfiguration");
        return new MessageBroadcaster(pusher, environment);
    }
} 