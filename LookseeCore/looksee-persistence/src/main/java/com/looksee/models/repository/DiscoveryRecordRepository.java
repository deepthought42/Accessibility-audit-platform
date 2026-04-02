package com.looksee.models.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.models.Account;
import com.looksee.models.DiscoveryRecord;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link DiscoveryRecord} objects
 */
@Repository
@Retry(name = "neoforj")
public interface DiscoveryRecordRepository extends Neo4jRepository<DiscoveryRecord, Long> {
	/**
	 * Finds a discovery record by key
	 *
	 * @param key the key of the discovery record
	 * @return the discovery record
	 */
	public DiscoveryRecord findByKey(@Param("key") String key);

	/**
	 * Retrieves all accounts for a given discovery record key
	 *
	 * @param key the key of the discovery record
	 * @return a list of all accounts for the given discovery record key
	 */
	@Query("MATCH (a:Account)-[:HAS_DISCOVERY_RECORD]->(:DiscoveryRecord{key:$key}) RETURN a")
	public List<Account> getAllAccounts(@Param("key") String key);
}
