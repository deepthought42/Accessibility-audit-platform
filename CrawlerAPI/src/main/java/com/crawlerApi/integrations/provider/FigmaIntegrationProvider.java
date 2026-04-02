package com.crawlerApi.integrations.provider;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Figma integration (Design tools). Stub implementation.
 */
@Component
public class FigmaIntegrationProvider extends AbstractIntegrationProvider {

    private static final List<String> CONFIG_KEYS = Arrays.asList("token", "fileIds");

    public FigmaIntegrationProvider() {
        super("figma", "Figma", "Design tools", CONFIG_KEYS);
    }
}
