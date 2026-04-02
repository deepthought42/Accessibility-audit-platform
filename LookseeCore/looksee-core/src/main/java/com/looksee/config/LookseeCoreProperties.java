package com.looksee.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for LookseeCore library.
 * 
 * This class allows consuming applications to configure various aspects
 * of the LookseeCore library through application.properties or application.yml.
 * 
 * Example usage in application.yml:
 * looksee:
 *   core:
 *     enabled: true
 *     neo4j:
 *       connection-timeout: 30000
 *       max-connection-pool-size: 50

 */
@ConfigurationProperties(prefix = "looksee.core")
public class LookseeCoreProperties {
    
    /**
     * Whether to enable LookseeCore auto-configuration.
     * Default is true.
     */
    private boolean enabled = true;
    
    /**
     * Neo4j connection properties.
     */
    private Neo4j neo4j = new Neo4j();
    

    
    /**
     * Checks if LookseeCore is enabled
     * @return true if LookseeCore is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Sets the enabled state
     * @param enabled the enabled state
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Gets the Neo4j configuration
     * @return the Neo4j configuration
     */
    public Neo4j getNeo4j() {
        return neo4j;
    }
    
    /**
     * Sets the Neo4j configuration
     * @param neo4j the Neo4j configuration
     */
    public void setNeo4j(Neo4j neo4j) {
        this.neo4j = neo4j;
    }
    

    
    /**
     * Neo4j-specific configuration properties.
     */
    public static class Neo4j {
        
        /**
         * Connection timeout in milliseconds.
         * Default is 30000 (30 seconds).
         */
        private int connectionTimeout = 30000;
        
        /**
         * Maximum connection pool size.
         * Default is 50.
         */
        private int maxConnectionPoolSize = 50;
        
        /**
         * Whether to enable connection pooling.
         * Default is true.
         */
        private boolean connectionPoolingEnabled = true;
        
        /**
         * Gets the connection timeout
         * @return the connection timeout
         */
        public int getConnectionTimeout() {
            return connectionTimeout;
        }
        
        /**
         * Sets the connection timeout
         * @param connectionTimeout the connection timeout
         */
        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }
        
        /**
         * Gets the maximum connection pool size
         * @return the maximum connection pool size
         */
        public int getMaxConnectionPoolSize() {
            return maxConnectionPoolSize;
        }
        
        /**
         * Sets the maximum connection pool size
         * @param maxConnectionPoolSize the maximum connection pool size
         */
        public void setMaxConnectionPoolSize(int maxConnectionPoolSize) {
            this.maxConnectionPoolSize = maxConnectionPoolSize;
        }
        
        /**
         * Checks if connection pooling is enabled
         * @return true if connection pooling is enabled, false otherwise
         */
        public boolean isConnectionPoolingEnabled() {
            return connectionPoolingEnabled;
        }
        
        /**
         * Sets the connection pooling enabled
         * @param connectionPoolingEnabled the connection pooling enabled
         */
        public void setConnectionPoolingEnabled(boolean connectionPoolingEnabled) {
            this.connectionPoolingEnabled = connectionPoolingEnabled;
        }
    }
    

} 