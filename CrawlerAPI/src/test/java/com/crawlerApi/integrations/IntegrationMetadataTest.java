package com.crawlerApi.integrations;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class IntegrationMetadataTest {

    @Test
    void testConstructorAndGetters() {
        List<String> schema = Arrays.asList("token", "url");
        IntegrationMetadata metadata = new IntegrationMetadata("jira", "Jira", "Product Management", schema);

        assertEquals("jira", metadata.getId());
        assertEquals("Jira", metadata.getName());
        assertEquals("Product Management", metadata.getCategory());
        assertEquals(schema, metadata.getConfigSchema());
    }

    @Test
    void testEmptyConfigSchema() {
        IntegrationMetadata metadata = new IntegrationMetadata("test", "Test", "Testing", Collections.emptyList());
        assertNotNull(metadata.getConfigSchema());
        assertTrue(metadata.getConfigSchema().isEmpty());
    }

    @Test
    void testNullValues() {
        IntegrationMetadata metadata = new IntegrationMetadata(null, null, null, null);
        assertNull(metadata.getId());
        assertNull(metadata.getName());
        assertNull(metadata.getCategory());
        assertNull(metadata.getConfigSchema());
    }
}
