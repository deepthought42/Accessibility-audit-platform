package com.crawlerApi.integrations.provider;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Jira integration (Product Management). Stub implementation.
 */
@Component
public class JiraIntegrationProvider extends AbstractIntegrationProvider {

    private static final List<String> CONFIG_KEYS = Arrays.asList("baseUrl", "token", "projectKeys");

    public JiraIntegrationProvider() {
        super("jira", "Jira", "Product Management", CONFIG_KEYS);
    }
}
