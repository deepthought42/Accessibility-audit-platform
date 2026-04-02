package com.looksee.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

/**
 * Configuration properties for Pusher real-time messaging.
 * 
 * This allows applications to configure Pusher credentials using either:
 * - pusher.* properties (e.g., pusher.appId, pusher.key, etc.)
 * - Environment variables (e.g., PUSHER_APP_ID, PUSHER_KEY, etc.)
 */
@ConfigurationProperties(prefix = "pusher")
@ConstructorBinding
public class PusherProperties {

    /**
     * Pusher application ID.
     */
    private final String appId;

    /**
     * Pusher key.
     */
    private final String key;

    /**
     * Pusher secret.
     */
    private final String secret;

    /**
     * Pusher cluster.
     */
    private final String cluster;

    /**
     * Whether to enable encryption.
     * Default is true.
     */
    private final boolean encrypted;

    /**
     * Constructor for PusherProperties
     * 
     * @param appId the Pusher application ID
     * @param key the Pusher key
     * @param secret the Pusher secret
     * @param cluster the Pusher cluster
     * @param encrypted whether to enable encryption (defaults to true)
     */
    public PusherProperties(String appId, String key, String secret, String cluster, Boolean encrypted) {
        this.appId = appId;
        this.key = key;
        this.secret = secret;
        this.cluster = cluster;
        this.encrypted = encrypted != null ? encrypted : true;
    }

    /**
     * Gets the Pusher application ID
     * @return the Pusher application ID
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Gets the Pusher key
     * @return the Pusher key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the Pusher secret
     * @return the Pusher secret
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Gets the Pusher cluster
     * @return the Pusher cluster
     */
    public String getCluster() {
        return cluster;
    }

    /**
     * Checks if encryption is enabled
     * @return true if encryption is enabled, false otherwise
     */
    public boolean isEncrypted() {
        return encrypted;
    }
} 