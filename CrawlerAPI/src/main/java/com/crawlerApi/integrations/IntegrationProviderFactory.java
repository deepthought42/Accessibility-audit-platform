package com.crawlerApi.integrations;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory and registry for integration providers. Resolves provider by type and exposes all metadata.
 */
@Component
public class IntegrationProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(IntegrationProviderFactory.class);

    private final List<IntegrationProvider> providers;
    private Map<String, IntegrationProvider> registry;

    @Autowired
    public IntegrationProviderFactory(List<IntegrationProvider> providers) {
        this.providers = providers != null ? providers : Collections.emptyList();
    }

    @PostConstruct
    public void init() {
        this.registry = providers.stream()
                .collect(Collectors.toMap(IntegrationProvider::getType, p -> p, (a, b) -> a));
        log.info("Registered {} integration providers: {}", registry.size(), registry.keySet());
    }

    public Optional<IntegrationProvider> getProvider(String type) {
        if (type == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(registry.get(type.toLowerCase()));
    }

    public List<IntegrationMetadata> getAllMetadata() {
        return providers.stream()
                .map(IntegrationProvider::getMetadata)
                .collect(Collectors.toList());
    }
}
