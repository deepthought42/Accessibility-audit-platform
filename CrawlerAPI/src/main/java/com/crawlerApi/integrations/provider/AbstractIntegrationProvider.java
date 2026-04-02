package com.crawlerApi.integrations.provider;

import java.util.List;
import java.util.Map;

import com.crawlerApi.integrations.IntegrationMetadata;
import com.crawlerApi.integrations.IntegrationProvider;

/**
 * Base for stub integration providers: fixed metadata and validation that required keys exist.
 */
public abstract class AbstractIntegrationProvider implements IntegrationProvider {

    private final String type;
    private final String name;
    private final String category;
    private final List<String> requiredConfigKeys;

    protected AbstractIntegrationProvider(String type, String name, String category, List<String> requiredConfigKeys) {
        this.type = type;
        this.name = name;
        this.category = category;
        this.requiredConfigKeys = requiredConfigKeys;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public IntegrationMetadata getMetadata() {
        return new IntegrationMetadata(type, name, category, requiredConfigKeys);
    }

    @Override
    public boolean validateConfig(Map<String, Object> config) {
        if (config == null) {
            return false;
        }
        for (String key : requiredConfigKeys) {
            if (!config.containsKey(key) || config.get(key) == null) {
                return false;
            }
            Object v = config.get(key);
            if (v instanceof String && ((String) v).isBlank()) {
                return false;
            }
        }
        return true;
    }

    protected List<String> getRequiredConfigKeys() {
        return requiredConfigKeys;
    }
}
