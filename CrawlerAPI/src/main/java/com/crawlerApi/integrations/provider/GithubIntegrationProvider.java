package com.crawlerApi.integrations.provider;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Github integration (CI/CD). Stub implementation.
 */
@Component
public class GithubIntegrationProvider extends AbstractIntegrationProvider {

    private static final List<String> CONFIG_KEYS = Arrays.asList("token", "org", "repo");

    public GithubIntegrationProvider() {
        super("github", "Github", "CI/CD", CONFIG_KEYS);
    }
}
