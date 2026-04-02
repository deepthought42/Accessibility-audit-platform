package com.crawlerApi.integrations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.crawlerApi.models.IntegrationConfig;
import com.crawlerApi.models.repository.IntegrationConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class IntegrationFacadeTest {

    @Mock
    private IntegrationProviderFactory factory;

    @Mock
    private IntegrationConfigRepository repository;

    @Mock
    private IntegrationConfigEncryption encryption;

    private IntegrationFacade facade;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        facade = new IntegrationFacade(factory, repository, objectMapper, encryption);
    }

    @Test
    void testListAvailableIntegrations() {
        List<IntegrationMetadata> expected = Arrays.asList(
            new IntegrationMetadata("jira", "Jira", "PM", Collections.emptyList())
        );
        when(factory.getAllMetadata()).thenReturn(expected);

        List<IntegrationMetadata> result = facade.listAvailableIntegrations();
        assertEquals(expected, result);
    }

    @Test
    void testGetProvider() {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(factory.getProvider("jira")).thenReturn(Optional.of(mockProvider));

        Optional<IntegrationProvider> result = facade.getProvider("jira");
        assertTrue(result.isPresent());
    }

    @Test
    void testGetMetadata() {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        IntegrationMetadata metadata = new IntegrationMetadata("jira", "Jira", "PM", Collections.emptyList());
        when(mockProvider.getMetadata()).thenReturn(metadata);
        when(factory.getProvider("jira")).thenReturn(Optional.of(mockProvider));

        Optional<IntegrationMetadata> result = facade.getMetadata("jira");
        assertTrue(result.isPresent());
        assertEquals("jira", result.get().getId());
    }

    @Test
    void testGetMetadataUnknownProvider() {
        when(factory.getProvider("unknown")).thenReturn(Optional.empty());

        Optional<IntegrationMetadata> result = facade.getMetadata("unknown");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetConfig() throws Exception {
        IntegrationConfig entity = new IntegrationConfig(1L, "jira", "{\"token\":\"abc\"}", true);
        when(repository.findByAccountIdAndIntegrationType(1L, "jira")).thenReturn(Optional.of(entity));
        when(encryption.decrypt("{\"token\":\"abc\"}")).thenReturn("{\"token\":\"abc\"}");

        Optional<Map<String, Object>> result = facade.getConfig(1L, "jira");
        assertTrue(result.isPresent());
        assertEquals("abc", result.get().get("token"));
    }

    @Test
    void testGetConfigNotFound() {
        when(repository.findByAccountIdAndIntegrationType(1L, "jira")).thenReturn(Optional.empty());

        Optional<Map<String, Object>> result = facade.getConfig(1L, "jira");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetConfigMasked() throws Exception {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        IntegrationMetadata metadata = new IntegrationMetadata("jira", "Jira", "PM", Arrays.asList("token", "baseUrl"));
        when(mockProvider.getMetadata()).thenReturn(metadata);
        when(factory.getProvider("jira")).thenReturn(Optional.of(mockProvider));

        IntegrationConfig entity = new IntegrationConfig(1L, "jira", "{\"token\":\"secretvalue\",\"baseUrl\":\"https://jira.example.com\"}", true);
        when(repository.findByAccountIdAndIntegrationType(1L, "jira")).thenReturn(Optional.of(entity));
        when(encryption.decrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));

        Optional<Map<String, Object>> result = facade.getConfigMasked(1L, "jira");
        assertTrue(result.isPresent());
        assertEquals("***ue", result.get().get("token"));
        assertEquals("***om", result.get().get("baseUrl"));
    }

    @Test
    void testGetConfigMaskedUnknownProvider() {
        when(factory.getProvider("unknown")).thenReturn(Optional.empty());

        Optional<Map<String, Object>> result = facade.getConfigMasked(1L, "unknown");
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetConfigMaskedShortValues() throws Exception {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        IntegrationMetadata metadata = new IntegrationMetadata("jira", "Jira", "PM", Arrays.asList("token"));
        when(mockProvider.getMetadata()).thenReturn(metadata);
        when(factory.getProvider("jira")).thenReturn(Optional.of(mockProvider));

        IntegrationConfig entity = new IntegrationConfig(1L, "jira", "{\"token\":\"ab\"}", true);
        when(repository.findByAccountIdAndIntegrationType(1L, "jira")).thenReturn(Optional.of(entity));
        when(encryption.decrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));

        Optional<Map<String, Object>> result = facade.getConfigMasked(1L, "jira");
        assertTrue(result.isPresent());
        assertEquals("***", result.get().get("token"));
    }

    @Test
    void testPutConfigSuccess() throws Exception {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(mockProvider.validateConfig(any())).thenReturn(true);
        when(factory.getProvider("jira")).thenReturn(Optional.of(mockProvider));
        when(repository.findByAccountIdAndIntegrationType(1L, "jira")).thenReturn(Optional.empty());
        when(encryption.encrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> config = new HashMap<>();
        config.put("token", "abc");

        boolean result = facade.putConfig(1L, "jira", config);
        assertTrue(result);
        verify(repository).save(any(IntegrationConfig.class));
    }

    @Test
    void testPutConfigUpdateExisting() throws Exception {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(mockProvider.validateConfig(any())).thenReturn(true);
        when(factory.getProvider("jira")).thenReturn(Optional.of(mockProvider));

        IntegrationConfig existing = new IntegrationConfig(1L, "jira", "{\"token\":\"old\"}", true);
        when(repository.findByAccountIdAndIntegrationType(1L, "jira")).thenReturn(Optional.of(existing));
        when(encryption.encrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> config = new HashMap<>();
        config.put("token", "new");

        boolean result = facade.putConfig(1L, "jira", config);
        assertTrue(result);
        verify(repository).save(any(IntegrationConfig.class));
    }

    @Test
    void testPutConfigUnknownProvider() {
        when(factory.getProvider("unknown")).thenReturn(Optional.empty());

        boolean result = facade.putConfig(1L, "unknown", new HashMap<>());
        assertFalse(result);
    }

    @Test
    void testPutConfigValidationFails() {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(mockProvider.validateConfig(any())).thenReturn(false);
        when(factory.getProvider("jira")).thenReturn(Optional.of(mockProvider));

        boolean result = facade.putConfig(1L, "jira", new HashMap<>());
        assertFalse(result);
    }

    @Test
    void testDeleteConfigSuccess() {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(factory.getProvider("jira")).thenReturn(Optional.of(mockProvider));

        boolean result = facade.deleteConfig(1L, "jira");
        assertTrue(result);
        verify(repository).deleteByAccountIdAndIntegrationType(1L, "jira");
    }

    @Test
    void testDeleteConfigUnknownProvider() {
        when(factory.getProvider("unknown")).thenReturn(Optional.empty());

        boolean result = facade.deleteConfig(1L, "unknown");
        assertFalse(result);
    }

    @Test
    void testTestConnectionSuccess() throws Exception {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(mockProvider.testConnection(eq(1L), any())).thenReturn(true);
        when(factory.getProvider("jira")).thenReturn(Optional.of(mockProvider));

        IntegrationConfig entity = new IntegrationConfig(1L, "jira", "{\"token\":\"abc\"}", true);
        when(repository.findByAccountIdAndIntegrationType(1L, "jira")).thenReturn(Optional.of(entity));
        when(encryption.decrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = facade.testConnection(1L, "jira");
        assertTrue(result);
    }

    @Test
    void testTestConnectionNoProvider() {
        when(factory.getProvider("unknown")).thenReturn(Optional.empty());

        boolean result = facade.testConnection(1L, "unknown");
        assertFalse(result);
    }

    @Test
    void testTestConnectionNoConfig() {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(factory.getProvider("jira")).thenReturn(Optional.of(mockProvider));
        when(repository.findByAccountIdAndIntegrationType(1L, "jira")).thenReturn(Optional.empty());

        boolean result = facade.testConnection(1L, "jira");
        assertFalse(result);
    }

    @Test
    void testGetConfigWithEmptyConfigString() {
        IntegrationConfig entity = new IntegrationConfig(1L, "jira", "", true);
        when(repository.findByAccountIdAndIntegrationType(1L, "jira")).thenReturn(Optional.of(entity));

        Optional<Map<String, Object>> result = facade.getConfig(1L, "jira");
        assertTrue(result.isPresent());
        assertTrue(result.get().isEmpty());
    }

    @Test
    void testGetConfigWithNullConfigString() {
        IntegrationConfig entity = new IntegrationConfig();
        entity.setAccountId(1L);
        entity.setIntegrationType("jira");
        entity.setConfig(null);
        when(repository.findByAccountIdAndIntegrationType(1L, "jira")).thenReturn(Optional.of(entity));

        Optional<Map<String, Object>> result = facade.getConfig(1L, "jira");
        assertTrue(result.isPresent());
        assertTrue(result.get().isEmpty());
    }

    @Test
    void testGetConfigWithInvalidJson() {
        IntegrationConfig entity = new IntegrationConfig(1L, "jira", "invalid-json", true);
        when(repository.findByAccountIdAndIntegrationType(1L, "jira")).thenReturn(Optional.of(entity));
        when(encryption.decrypt("invalid-json")).thenReturn("invalid-json");

        Optional<Map<String, Object>> result = facade.getConfig(1L, "jira");
        assertTrue(result.isPresent());
        assertTrue(result.get().isEmpty());
    }

    @Test
    void testNullObjectMapperFallback() {
        // Test that null objectMapper gets replaced
        IntegrationFacade facadeWithNull = new IntegrationFacade(factory, repository, null, encryption);
        assertNotNull(facadeWithNull);
    }

    @Test
    void testPutConfigNormalizesType() throws Exception {
        IntegrationProvider mockProvider = mock(IntegrationProvider.class);
        when(mockProvider.validateConfig(any())).thenReturn(true);
        when(factory.getProvider("jira")).thenReturn(Optional.of(mockProvider));
        when(repository.findByAccountIdAndIntegrationType(1L, "jira")).thenReturn(Optional.empty());
        when(encryption.encrypt(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> config = new HashMap<>();
        config.put("token", "abc");

        boolean result = facade.putConfig(1L, "JIRA", config);
        assertTrue(result);
    }
}
