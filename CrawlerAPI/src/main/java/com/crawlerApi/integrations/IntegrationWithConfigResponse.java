package com.crawlerApi.integrations;

import java.util.Map;

/**
 * Response DTO: integration metadata plus optional (masked) config for current account.
 */
public class IntegrationWithConfigResponse {

    private final IntegrationMetadata metadata;
    private final Map<String, Object> config;

    public IntegrationWithConfigResponse(IntegrationMetadata metadata, Map<String, Object> config) {
        this.metadata = metadata;
        this.config = config;
    }

    public IntegrationMetadata getMetadata() {
        return metadata;
    }

    public Map<String, Object> getConfig() {
        return config;
    }
}
