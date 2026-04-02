package com.crawlerApi.models.repository;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import com.crawlerApi.models.IntegrationConfig;

/**
 * Spring Data Neo4j repository for per-account integration configuration.
 */
@Repository
public interface IntegrationConfigRepository extends Neo4jRepository<IntegrationConfig, Long> {

    Optional<IntegrationConfig> findByAccountIdAndIntegrationType(Long accountId, String integrationType);

    void deleteByAccountIdAndIntegrationType(Long accountId, String integrationType);

    boolean existsByAccountIdAndIntegrationType(Long accountId, String integrationType);
}
