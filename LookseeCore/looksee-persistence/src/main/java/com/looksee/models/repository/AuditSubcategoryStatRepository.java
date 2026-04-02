package com.looksee.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.looksee.models.audit.stats.AuditSubcategoryStat;

/**
 * Repository for {@link AuditSubcategoryStat}
 */
public interface AuditSubcategoryStatRepository extends Neo4jRepository<AuditSubcategoryStat, Long> {
	
}
