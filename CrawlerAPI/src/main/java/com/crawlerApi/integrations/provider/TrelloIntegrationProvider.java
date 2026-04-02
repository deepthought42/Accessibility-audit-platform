package com.crawlerApi.integrations.provider;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Trello integration (Product Management). Stub implementation.
 */
@Component
public class TrelloIntegrationProvider extends AbstractIntegrationProvider {

    private static final List<String> CONFIG_KEYS = Arrays.asList("apiKey", "token", "boardIds");

    public TrelloIntegrationProvider() {
        super("trello", "Trello", "Product Management", CONFIG_KEYS);
    }
}
