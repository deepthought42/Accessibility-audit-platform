package com.crawlerApi.integrations;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class IntegrationWithConfigResponseTest {

    @Test
    void testConstructorAndGetters() {
        IntegrationMetadata metadata = new IntegrationMetadata("slack", "Slack", "Messaging", Arrays.asList("webhookUrl"));
        Map<String, Object> config = new HashMap<>();
        config.put("webhookUrl", "https://hooks.slack.com/xxx");

        IntegrationWithConfigResponse response = new IntegrationWithConfigResponse(metadata, config);

        assertEquals(metadata, response.getMetadata());
        assertEquals(config, response.getConfig());
    }

    @Test
    void testNullConfig() {
        IntegrationMetadata metadata = new IntegrationMetadata("slack", "Slack", "Messaging", Arrays.asList("webhookUrl"));
        IntegrationWithConfigResponse response = new IntegrationWithConfigResponse(metadata, null);

        assertEquals(metadata, response.getMetadata());
        assertNull(response.getConfig());
    }

    @Test
    void testNullMetadata() {
        Map<String, Object> config = new HashMap<>();
        IntegrationWithConfigResponse response = new IntegrationWithConfigResponse(null, config);
        assertNull(response.getMetadata());
        assertNotNull(response.getConfig());
    }
}
