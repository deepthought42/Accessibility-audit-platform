package com.crawlerApi.integrations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.crawlerApi.models.IntegrationConfig;
import com.crawlerApi.models.repository.IntegrationConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Facade for the integration layer: list integrations, get/put/delete per-account config,
 * resolve provider by type, and run test connection.
 */
@Service
public class IntegrationFacade {

    private static final Logger log = LoggerFactory.getLogger(IntegrationFacade.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};

    private final IntegrationProviderFactory factory;
    private final IntegrationConfigRepository repository;
    private final ObjectMapper objectMapper;
    private final IntegrationConfigEncryption encryption;

    @Autowired
    public IntegrationFacade(IntegrationProviderFactory factory,
                            IntegrationConfigRepository repository,
                            ObjectMapper objectMapper,
                            IntegrationConfigEncryption encryption) {
        this.factory = factory;
        this.repository = repository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.encryption = encryption;
    }

    public List<IntegrationMetadata> listAvailableIntegrations() {
        return factory.getAllMetadata();
    }

    public Optional<IntegrationProvider> getProvider(String type) {
        return factory.getProvider(type);
    }

    public Optional<IntegrationMetadata> getMetadata(String type) {
        return factory.getProvider(type).map(IntegrationProvider::getMetadata);
    }

    public Optional<Map<String, Object>> getConfig(Long accountId, String type) {
        return repository.findByAccountIdAndIntegrationType(accountId, normalizeType(type))
                .map(this::configJsonToMap);
    }

    /**
     * Returns config with secret values masked (e.g. token -> "***") for API responses.
     */
    public Optional<Map<String, Object>> getConfigMasked(Long accountId, String type) {
        Optional<IntegrationProvider> provider = factory.getProvider(type);
        if (provider.isEmpty()) {
            return Optional.empty();
        }
        List<String> schema = provider.get().getMetadata().getConfigSchema();
        return getConfig(accountId, type)
                .map(config -> maskConfig(config, schema));
    }

    public boolean putConfig(Long accountId, String type, Map<String, Object> config) {
        String normalized = normalizeType(type);
        Optional<IntegrationProvider> providerOpt = factory.getProvider(normalized);
        if (providerOpt.isEmpty()) {
            return false;
        }
        if (!providerOpt.get().validateConfig(config)) {
            return false;
        }
        try {
            String json = objectMapper.writeValueAsString(config);
            String toStore = encryption.encrypt(json);
            Optional<IntegrationConfig> existing = repository.findByAccountIdAndIntegrationType(accountId, normalized);
            IntegrationConfig entity;
            if (existing.isPresent()) {
                entity = existing.get();
                entity.setConfig(toStore);
                entity.setUpdatedAt(java.time.LocalDateTime.now());
            } else {
                entity = new IntegrationConfig(accountId, normalized, toStore, true);
            }
            repository.save(entity);
            return true;
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize integration config", e);
            return false;
        }
    }

    public boolean deleteConfig(Long accountId, String type) {
        String normalized = normalizeType(type);
        if (factory.getProvider(normalized).isEmpty()) {
            return false;
        }
        repository.deleteByAccountIdAndIntegrationType(accountId, normalized);
        return true;
    }

    /**
     * Test connection for the given account and integration type. Uses stored config.
     *
     * @return true if test succeeded, false otherwise (or if no config/provider)
     */
    public boolean testConnection(Long accountId, String type) {
        String normalized = normalizeType(type);
        Optional<IntegrationProvider> providerOpt = factory.getProvider(normalized);
        if (providerOpt.isEmpty()) {
            return false;
        }
        Optional<Map<String, Object>> configOpt = getConfig(accountId, normalized);
        if (configOpt.isEmpty()) {
            return false;
        }
        return providerOpt.get().testConnection(accountId, configOpt.get());
    }

    private static String normalizeType(String type) {
        return type == null ? null : type.toLowerCase();
    }

    private Map<String, Object> configJsonToMap(IntegrationConfig entity) {
        if (entity.getConfig() == null || entity.getConfig().isEmpty()) {
            return new HashMap<>();
        }
        String json = encryption.decrypt(entity.getConfig());
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse integration config json for account {} type {}", entity.getAccountId(), entity.getIntegrationType());
            return new HashMap<>();
        }
    }

    private Map<String, Object> maskConfig(Map<String, Object> config, List<String> schema) {
        Map<String, Object> masked = new HashMap<>(config);
        for (String key : schema) {
            if (masked.containsKey(key) && masked.get(key) != null) {
                String v = masked.get(key).toString();
                masked.put(key, v.length() > 4 ? "***" + v.substring(v.length() - 2) : "***");
            }
        }
        return masked;
    }
}
