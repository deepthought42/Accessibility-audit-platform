package com.crawlerApi.integrations.provider;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Jenkins integration (CI/CD). Stub implementation.
 */
@Component
public class JenkinsIntegrationProvider extends AbstractIntegrationProvider {

    private static final List<String> CONFIG_KEYS = Arrays.asList("baseUrl", "token", "jobNames");

    public JenkinsIntegrationProvider() {
        super("jenkins", "Jenkins", "CI/CD", CONFIG_KEYS);
    }
}
