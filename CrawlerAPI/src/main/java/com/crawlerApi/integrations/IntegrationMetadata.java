package com.crawlerApi.integrations;

import java.util.List;

/**
 * Metadata for an integration type: id, display name, category, and config schema.
 */
public class IntegrationMetadata {

    private final String id;
    private final String name;
    private final String category;
    private final List<String> configSchema;

    public IntegrationMetadata(String id, String name, String category, List<String> configSchema) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.configSchema = configSchema;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getConfigSchema() {
        return configSchema;
    }
}
