package com.looksee.models.repository;


import com.looksee.models.journeys.DomainMap;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with 
 * {@link DomainMap} objects
 */
@Repository
public interface DomainMapRepository extends Neo4jRepository<DomainMap, Long>{

	/**
	 * Finds a domain map by its key
	 *
	 * @param domain_map_key the key of the domain map
	 * @return the domain map
	 */
	@Query("MATCH (map:DomainMap{key:$key}) RETURN map LIMIT 1")
	public DomainMap findByKey(@Param("key") String domain_map_key);

	/**
	 * Finds a domain map by its domain ID
	 *
	 * @param domain_id the ID of the domain
	 * @return the domain map
	 */
	@Query("MATCH (d:Domain) with d WHERE id(d)=$domain_id MATCH (d)-[:CONTAINS]->(map:DomainMap) RETURN map")
	public DomainMap findByDomainId(@Param("domain_id") long domain_id);

	/**
	 * Adds a journey to a domain map
	 *
	 * @param journey_id the ID of the journey
	 * @param domain_map_id the ID of the domain map
	 * @return the domain map
	 */
	@Query("MATCH (dm:DomainMap) WITH dm MATCH (journey:Journey) WHERE id(dm)=$domain_map_id AND id(journey)=$journey_id MERGE (dm)-[:CONTAINS]->(journey) RETURN dm LIMIT 1")
	public DomainMap addJourneyToDomainMap(@Param("journey_id") long journey_id, @Param("domain_map_id") long domain_map_id);

	/**
	 * Finds a domain map by its domain audit ID
	 *
	 * @param audit_record_id the ID of the domain audit record
	 * @return the domain map
	 */
	@Query("MATCH (ar:DomainAuditRecord) with ar WHERE id(ar)=$audit_record_id MATCH (ar)-[:CONTAINS]->(map:DomainMap) RETURN map")
	public DomainMap findByDomainAuditId(@Param("audit_record_id") long audit_record_id);

	/**
	 * Adds a page to a domain map
	 *
	 * @param domain_map_id the ID of the domain map
	 * @param page_state_id the ID of the page
	 * @return the domain map
	 *
	 * precondition: domain_map_id > 0
	 * precondition: page_state_id > 0
	 */
	@Query("MATCH (dm:DomainMap) WHERE id(dm)=$domain_map_id MATCH (page:PageState) WHERE id(page)=$page_state_id MERGE (dm)-[:CONTAINS]->(page) RETURN dm LIMIT 1")
	public DomainMap addPageToDomainMap(@Param("domain_map_id") long domain_map_id, @Param("page_state_id") long page_state_id);
}
