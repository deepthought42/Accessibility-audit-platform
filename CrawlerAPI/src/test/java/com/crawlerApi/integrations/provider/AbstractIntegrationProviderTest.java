package com.crawlerApi.integrations.provider;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.crawlerApi.integrations.IntegrationMetadata;

class AbstractIntegrationProviderTest {

    // Concrete subclass for testing
    static class TestProvider extends AbstractIntegrationProvider {
        TestProvider(String type, String name, String category, List<String> keys) {
            super(type, name, category, keys);
        }
    }

    @Test
    void testGetType() {
        TestProvider provider = new TestProvider("jira", "Jira", "PM", Collections.emptyList());
        assertEquals("jira", provider.getType());
    }

    @Test
    void testGetMetadata() {
        List<String> keys = Arrays.asList("token", "url");
        TestProvider provider = new TestProvider("jira", "Jira", "PM", keys);
        IntegrationMetadata metadata = provider.getMetadata();
        assertEquals("jira", metadata.getId());
        assertEquals("Jira", metadata.getName());
        assertEquals("PM", metadata.getCategory());
        assertEquals(keys, metadata.getConfigSchema());
    }

    @Test
    void testValidateConfigNullConfig() {
        TestProvider provider = new TestProvider("jira", "Jira", "PM", Arrays.asList("token"));
        assertFalse(provider.validateConfig(null));
    }

    @Test
    void testValidateConfigMissingKey() {
        TestProvider provider = new TestProvider("jira", "Jira", "PM", Arrays.asList("token", "url"));
        Map<String, Object> config = new HashMap<>();
        config.put("token", "abc");
        assertFalse(provider.validateConfig(config));
    }

    @Test
    void testValidateConfigNullValue() {
        TestProvider provider = new TestProvider("jira", "Jira", "PM", Arrays.asList("token"));
        Map<String, Object> config = new HashMap<>();
        config.put("token", null);
        assertFalse(provider.validateConfig(config));
    }

    @Test
    void testValidateConfigBlankStringValue() {
        TestProvider provider = new TestProvider("jira", "Jira", "PM", Arrays.asList("token"));
        Map<String, Object> config = new HashMap<>();
        config.put("token", "   ");
        assertFalse(provider.validateConfig(config));
    }

    @Test
    void testValidateConfigEmptyStringValue() {
        TestProvider provider = new TestProvider("jira", "Jira", "PM", Arrays.asList("token"));
        Map<String, Object> config = new HashMap<>();
        config.put("token", "");
        assertFalse(provider.validateConfig(config));
    }

    @Test
    void testValidateConfigValidConfig() {
        TestProvider provider = new TestProvider("jira", "Jira", "PM", Arrays.asList("token", "url"));
        Map<String, Object> config = new HashMap<>();
        config.put("token", "abc123");
        config.put("url", "https://jira.example.com");
        assertTrue(provider.validateConfig(config));
    }

    @Test
    void testValidateConfigNoRequiredKeys() {
        TestProvider provider = new TestProvider("test", "Test", "Test", Collections.emptyList());
        Map<String, Object> config = new HashMap<>();
        assertTrue(provider.validateConfig(config));
    }

    @Test
    void testValidateConfigNonStringValue() {
        TestProvider provider = new TestProvider("jira", "Jira", "PM", Arrays.asList("count"));
        Map<String, Object> config = new HashMap<>();
        config.put("count", 42);
        assertTrue(provider.validateConfig(config));
    }

    @Test
    void testValidateConfigWithExtraKeys() {
        TestProvider provider = new TestProvider("jira", "Jira", "PM", Arrays.asList("token"));
        Map<String, Object> config = new HashMap<>();
        config.put("token", "abc");
        config.put("extra", "value");
        assertTrue(provider.validateConfig(config));
    }

    @Test
    void testGetRequiredConfigKeys() {
        List<String> keys = Arrays.asList("a", "b");
        TestProvider provider = new TestProvider("t", "T", "C", keys);
        assertEquals(keys, provider.getRequiredConfigKeys());
    }

    @Test
    void testDefaultTestConnectionReturnsFalse() {
        TestProvider provider = new TestProvider("t", "T", "C", Collections.emptyList());
        assertFalse(provider.testConnection(1L));
    }

    @Test
    void testDefaultTestConnectionWithConfigReturnsFalse() {
        TestProvider provider = new TestProvider("t", "T", "C", Collections.emptyList());
        assertFalse(provider.testConnection(1L, new HashMap<>()));
    }
}
