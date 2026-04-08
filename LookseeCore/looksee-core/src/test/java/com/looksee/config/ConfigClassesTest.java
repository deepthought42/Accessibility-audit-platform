package com.looksee.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.looksee.browser.config.SeleniumProperties;

/**
 * Unit tests for configuration property classes.
 */
class ConfigClassesTest {

    // ===== LookseeCoreProperties =====
    @Test
    void lookseeCorePropertiesDefaults() {
        LookseeCoreProperties props = new LookseeCoreProperties();
        assertTrue(props.isEnabled());
        assertNotNull(props.getNeo4j());
    }

    @Test
    void lookseeCorePropertiesSetEnabled() {
        LookseeCoreProperties props = new LookseeCoreProperties();
        props.setEnabled(false);
        assertFalse(props.isEnabled());
    }

    @Test
    void lookseeCorePropertiesNeo4jDefaults() {
        LookseeCoreProperties.Neo4j neo4j = new LookseeCoreProperties.Neo4j();
        assertEquals(30000, neo4j.getConnectionTimeout());
        assertEquals(50, neo4j.getMaxConnectionPoolSize());
        assertTrue(neo4j.isConnectionPoolingEnabled());
    }

    @Test
    void lookseeCorePropertiesNeo4jSetters() {
        LookseeCoreProperties.Neo4j neo4j = new LookseeCoreProperties.Neo4j();
        neo4j.setConnectionTimeout(5000);
        neo4j.setMaxConnectionPoolSize(10);
        neo4j.setConnectionPoolingEnabled(false);
        assertEquals(5000, neo4j.getConnectionTimeout());
        assertEquals(10, neo4j.getMaxConnectionPoolSize());
        assertFalse(neo4j.isConnectionPoolingEnabled());
    }

    // ===== PusherProperties (immutable, constructor-bound) =====
    @Test
    void pusherPropertiesAllArgs() {
        PusherProperties props = new PusherProperties("app123", "key123", "secret123", "us1", true);
        assertEquals("app123", props.getAppId());
        assertEquals("key123", props.getKey());
        assertEquals("secret123", props.getSecret());
        assertEquals("us1", props.getCluster());
        assertTrue(props.isEncrypted());
    }

    @Test
    void pusherPropertiesNullEncryptedDefaultsToTrue() {
        PusherProperties props = new PusherProperties("app", "key", "secret", "cluster", null);
        assertTrue(props.isEncrypted());
    }

    @Test
    void pusherPropertiesEncryptedFalse() {
        PusherProperties props = new PusherProperties("app", "key", "secret", "cluster", false);
        assertFalse(props.isEncrypted());
    }

    // ===== SeleniumProperties (plain POJO, setter-bound) =====
    @Test
    void seleniumPropertiesSetters() {
        SeleniumProperties props = new SeleniumProperties();
        props.setUrls("http://hub:4444/wd/hub");
        props.setConnectionTimeout(30000);
        props.setMaxRetries(3);
        props.setImplicitWaitEnabled(true);
        props.setImplicitWaitTimeout(10000);

        assertEquals("http://hub:4444/wd/hub", props.getUrls());
        assertEquals(30000, props.getConnectionTimeout());
        assertEquals(3, props.getMaxRetries());
        assertTrue(props.isImplicitWaitEnabled());
        assertEquals(10000, props.getImplicitWaitTimeout());
    }

    @Test
    void seleniumPropertiesDefaults() {
        SeleniumProperties props = new SeleniumProperties();
        props.setUrls("http://hub:4444");

        assertEquals("http://hub:4444", props.getUrls());
        assertEquals(30000, props.getConnectionTimeout());
        assertEquals(3, props.getMaxRetries());
        assertTrue(props.isImplicitWaitEnabled());
        assertEquals(10000, props.getImplicitWaitTimeout());
    }

    @Test
    void seleniumPropertiesGetUrlsArray() {
        SeleniumProperties props = new SeleniumProperties();
        props.setUrls("http://a,http://b");
        String[] urls = props.getUrlsArray();
        assertEquals(2, urls.length);
        assertEquals("http://a", urls[0]);
        assertEquals("http://b", urls[1]);
    }

    @Test
    void seleniumPropertiesGetUrlsArrayNull() {
        SeleniumProperties props = new SeleniumProperties();
        String[] urls = props.getUrlsArray();
        assertEquals(0, urls.length);
    }
}
