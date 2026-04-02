package com.looksee.frontEndBroadcaster.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.frontEndBroadcaster.models.Domain;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * 
 */
@Repository
@Retry(name = "neoforj")
public interface DomainRepository extends Neo4jRepository<Domain, Long> {
	
	@Query("MATCH (a:Account{username:$username})-[:HAS_DOMAIN]->(d:Domain{key:$key}) RETURN d LIMIT 1")
	public Domain findByKey(@Param("key") String key, @Param("username") String username);
	
	@Query("MATCH (d:Domain)-[:HAS]->(audit_record:AuditRecord) WHERE id(audit_record)=$audit_record_id RETURN d LIMIT 1")
	public Domain findByAuditRecord(@Param("audit_record_id") long audit_record_id);
}
