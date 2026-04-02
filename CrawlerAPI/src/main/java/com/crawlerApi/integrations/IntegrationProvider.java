package com.crawlerApi.integrations;

import java.util.Map;

/**
 * Strategy interface for an external integration (Jira, Slack, etc.).
 * Each integration type implements this and is resolved via IntegrationProviderFactory.
 */
public interface IntegrationProvider {

    /**
     * Unique type identifier (e.g. "jira", "slack"). Used in API paths and storage.
     */
    String getType();

    /**
     * Display metadata: name, category, config schema keys.
     */
    IntegrationMetadata getMetadata();

    /**
     * Validate that the given config map contains required keys and valid values.
     *
     * @param config user-provided config (e.g. from PUT body)
     * @return true if valid
     */
    boolean validateConfig(Map<String, Object> config);

    /**
     * Test connection using the account's stored config. Optional; default false = not implemented.
     *
     * @param accountId account id
     * @return true if connection succeeded
     */
    default boolean testConnection(Long accountId) {
        return false;
    }

    /**
     * Test connection using the given config (loaded by facade). Default delegates to {@link #testConnection(Long)}.
     *
     * @param accountId account id
     * @param config    stored config for this account and integration type
     * @return true if connection succeeded
     */
    default boolean testConnection(Long accountId, java.util.Map<String, Object> config) {
        return testConnection(accountId);
    }
}
