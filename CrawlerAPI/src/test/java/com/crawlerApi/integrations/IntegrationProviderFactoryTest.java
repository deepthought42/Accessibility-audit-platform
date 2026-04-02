package com.crawlerApi.integrations;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IntegrationProviderFactoryTest {

    private IntegrationProviderFactory factory;

    private IntegrationProvider createStubProvider(String type, String name, String category) {
        return new IntegrationProvider() {
            @Override
            public String getType() { return type; }

            @Override
            public IntegrationMetadata getMetadata() {
                return new IntegrationMetadata(type, name, category, Collections.emptyList());
            }

            @Override
            public boolean validateConfig(Map<String, Object> config) { return true; }
        };
    }

    @BeforeEach
    void setUp() {
        List<IntegrationProvider> providers = Arrays.asList(
            createStubProvider("jira", "Jira", "Product Management"),
            createStubProvider("slack", "Slack", "Messaging")
        );
        factory = new IntegrationProviderFactory(providers);
        factory.init();
    }

    @Test
    void testGetProviderByType() {
        Optional<IntegrationProvider> provider = factory.getProvider("jira");
        assertTrue(provider.isPresent());
        assertEquals("jira", provider.get().getType());
    }

    @Test
    void testGetProviderByTypeCaseInsensitive() {
        Optional<IntegrationProvider> provider = factory.getProvider("JIRA");
        assertTrue(provider.isPresent());
        assertEquals("jira", provider.get().getType());
    }

    @Test
    void testGetProviderUnknownType() {
        Optional<IntegrationProvider> provider = factory.getProvider("unknown");
        assertTrue(provider.isEmpty());
    }

    @Test
    void testGetProviderNullType() {
        Optional<IntegrationProvider> provider = factory.getProvider(null);
        assertTrue(provider.isEmpty());
    }

    @Test
    void testGetAllMetadata() {
        List<IntegrationMetadata> metadata = factory.getAllMetadata();
        assertEquals(2, metadata.size());
    }

    @Test
    void testEmptyProviderList() {
        IntegrationProviderFactory emptyFactory = new IntegrationProviderFactory(Collections.emptyList());
        emptyFactory.init();
        assertTrue(emptyFactory.getProvider("anything").isEmpty());
        assertTrue(emptyFactory.getAllMetadata().isEmpty());
    }

    @Test
    void testNullProviderList() {
        IntegrationProviderFactory nullFactory = new IntegrationProviderFactory(null);
        nullFactory.init();
        assertTrue(nullFactory.getProvider("anything").isEmpty());
        assertTrue(nullFactory.getAllMetadata().isEmpty());
    }
}
