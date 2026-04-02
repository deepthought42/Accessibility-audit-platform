package com.crawlerApi.models;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class IntegrationConfigTest {

    @Test
    void testDefaultConstructor() {
        IntegrationConfig config = new IntegrationConfig();
        assertNull(config.getId());
        assertNull(config.getAccountId());
        assertNull(config.getIntegrationType());
        assertNull(config.getConfig());
        assertTrue(config.isEnabled());
        assertNull(config.getCreatedAt());
        assertNull(config.getUpdatedAt());
    }

    @Test
    void testParameterizedConstructor() {
        IntegrationConfig config = new IntegrationConfig(1L, "jira", "{\"token\":\"abc\"}", true);
        assertEquals(1L, config.getAccountId());
        assertEquals("jira", config.getIntegrationType());
        assertEquals("{\"token\":\"abc\"}", config.getConfig());
        assertTrue(config.isEnabled());
        assertNotNull(config.getCreatedAt());
        assertNotNull(config.getUpdatedAt());
    }

    @Test
    void testParameterizedConstructorDisabled() {
        IntegrationConfig config = new IntegrationConfig(2L, "slack", "{}", false);
        assertEquals(2L, config.getAccountId());
        assertEquals("slack", config.getIntegrationType());
        assertFalse(config.isEnabled());
    }

    @Test
    void testSettersAndGetters() {
        IntegrationConfig config = new IntegrationConfig();
        config.setId(10L);
        config.setAccountId(20L);
        config.setIntegrationType("github");
        config.setConfig("{\"token\":\"xyz\"}");
        config.setEnabled(false);
        LocalDateTime now = LocalDateTime.now();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);

        assertEquals(10L, config.getId());
        assertEquals(20L, config.getAccountId());
        assertEquals("github", config.getIntegrationType());
        assertEquals("{\"token\":\"xyz\"}", config.getConfig());
        assertFalse(config.isEnabled());
        assertEquals(now, config.getCreatedAt());
        assertEquals(now, config.getUpdatedAt());
    }

    @Test
    void testCreatedAtIsSetOnConstruction() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        IntegrationConfig config = new IntegrationConfig(1L, "jira", "{}", true);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertTrue(config.getCreatedAt().isAfter(before));
        assertTrue(config.getCreatedAt().isBefore(after));
    }
}
