package com.looksee.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

import com.looksee.audits.performance.PageSpeedAudit;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link PageSpeedAudit} objects
 */
public interface PageSpeedAuditRepository extends Neo4jRepository<PageSpeedAudit, Long> {
	/**
	 * Finds a {@link PageSpeedAudit} by its key
	 * 
	 * @param key the key of the {@link PageSpeedAudit}
	 * @return the {@link PageSpeedAudit}
	 */
	public PageSpeedAudit findByKey(@Param("key") String key);
}
