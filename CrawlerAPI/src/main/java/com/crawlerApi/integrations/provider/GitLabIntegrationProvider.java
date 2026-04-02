package com.crawlerApi.integrations.provider;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * GitLab integration (CI/CD). Stub implementation.
 */
@Component
public class GitLabIntegrationProvider extends AbstractIntegrationProvider {

    private static final List<String> CONFIG_KEYS = Arrays.asList("baseUrl", "token", "projectIds");

    public GitLabIntegrationProvider() {
        super("gitlab", "GitLab", "CI/CD", CONFIG_KEYS);
    }
}
