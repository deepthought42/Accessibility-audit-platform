package com.crawlerApi.integrations.provider;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Slack integration (Messaging). Stub implementation.
 */
@Component
public class SlackIntegrationProvider extends AbstractIntegrationProvider {

    private static final List<String> CONFIG_KEYS = Arrays.asList("webhookUrl", "channel");

    public SlackIntegrationProvider() {
        super("slack", "Slack", "Messaging", CONFIG_KEYS);
    }
}
