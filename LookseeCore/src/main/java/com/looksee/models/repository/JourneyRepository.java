package com.looksee.models.repository;

import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.journeys.Journey;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with
 * {@link Journey} objects
 */
public interface JourneyRepository extends Neo4jRepository<Journey, Long>  {
	
	/**
	 * Finds a journey by its key
	 *
	 * @param key the key of the journey
	 * @return the journey
	 */
	public Journey findByKey(@Param("key") String key);

	/**
	 * Adds a step to a journey
	 *
	 * @param journey_id the ID of the journey
	 * @param step_id the ID of the step
	 * @return the journey
	 */
	@Query("MATCH (j:Journey) WITH j MATCH (s:Step) WHERE id(s)=$step_id AND id(j)=$journey_id MERGE (j)-[:HAS]->(s) RETURN j")
	public Journey addStep(@Param("journey_id") long journey_id, @Param("step_id") long step_id);

	/**
	 * Finds a journey by its candidate key
	 *
	 * @param candidateKey the candidate key of the journey
	 * @return the journey
	 */
	@Query("MATCH (j:Journey{candidateKey:$candidateKey}) RETURN j")
	public Journey findByCandidateKey(@Param("candidateKey") String candidateKey);

	/**
	 * Finds a journey by its candidate key within a domain map
	 *
	 * @param domain_map_id the ID of the domain map
	 * @param candidate_key the candidate key of the journey
	 * @return the journey
	 */
	@Query("MATCH (map:DomainMap)-[*2]-(j:Journey{candidateKey:$candidate_key}) WHERE id(map)=$domain_map_id RETURN j LIMIT 1")
	public Journey findByCandidateKey(@Param("domain_map_id") long domain_map_id, @Param("candidate_key") String candidate_key);

	/**
	 * Finds a journey by its key or candidate key within a domain map
	 *
	 * @param domain_map_id the ID of the domain map
	 * @param key the key of the journey
	 * @param candidateKey the candidate key of the journey
	 * @return the journey
	 */
	@Query("MATCH (map:DomainMap)-[*2]-(j:Journey) WHERE id(map)=$domain_map_id AND (j.key=$key OR j.candidateKey=$candidateKey) RETURN j LIMIT 1")
	public Journey findByKeyOrCandidateKey(@Param("domain_map_id") long domain_map_id, @Param("key") String key, @Param("candidateKey") String candidateKey);

	/**
	 * Updates the fields of a journey
	 *
	 * @param journey_id the ID of the journey
	 * @param status the status of the journey
	 * @param key the key of the journey
	 * @param ordered_ids the ordered IDs of the journey
	 * @return the journey
	 */
	@Query("MATCH (j:Journey) WHERE id(j)=$journey_id SET j.status=$status, j.key=$key, j.orderedIds=$ordered_ids RETURN j")
	public Journey updateFields(@Param("journey_id") long journey_id, @Param("status") JourneyStatus status, @Param("key") String key, @Param("ordered_ids") List<Long> ordered_ids);

	/**
	 * Finds all journeys for a domain audit record
	 *
	 * @param audit_id the ID of the domain audit record
	 * @param status the status of the journey
	 * @return the number of journeys
	 */
	@Query("MATCH (audit:DomainAuditRecord) WHERE id(audit)=$audit_id MATCH (audit)-[*2]->(j:Journey) WHERE j.status=$status RETURN COUNT(j)")
	public int findAllJourneysForDomainAudit(@Param("audit_id") long audit_id, @Param("status") JourneyStatus status);

	/**
	 * Finds a journey by its key within a domain map
	 *
	 * @param domain_map_id the ID of the domain map
	 * @param key the key of the journey
	 * @return the journey
	 */
	@Query("MATCH (map:DomainMap) WHERE id(map)=$map_id MATCH (map)-[:CONTAINS]->(j:Journey{key:$key}) RETURN j LIMIT 1")
	public Journey findByKey(@Param("map_id") long domain_map_id, @Param("key") String key);

	/**
	 * Updates the fields of a journey
	 *
	 * @param journey_id the ID of the journey
	 * @param status the status of the journey
	 * @param key the key of the journey
	 * @param ordered_ids the ordered IDs of the journey
	 * @return the journey
	 */
	@Query("MATCH (j:Journey) WHERE id(j)=$journey_id SET j.status=$status, j.key=$key, j.orderedIds=$ordered_ids RETURN j")
	public Journey updateFields(@Param("journey_id") long journey_id, 
								@Param("status") String status, 
								@Param("key") String key,
								@Param("ordered_ids") List<Long> ordered_ids);

	/**
	 * Updates the status of a journey
	 *
	 * @param journey_id the ID of the journey
	 * @param status the status of the journey
	 * @return the journey
	 */
	@Query("MATCH (journey:Journey) WHERE id(journey)=$journey_id SET journey.status=$status RETURN journey")
	public Journey updateStatus(@Param("journey_id") long journey_id, @Param("status") String status);

	/**
	 * Finds all journeys for a domain audit record
	 *
	 * @param audit_id the ID of the domain audit record
	 * @param status the status of the journey
	 * @return the number of journeys
	 */
	@Query("MATCH (audit:DomainAuditRecord) WHERE id(audit)=$audit_id MATCH (audit)-[*2]->(j:Journey) WHERE j.status=$status RETURN COUNT(j)")
	public int findAllJourneysForDomainAudit(@Param("audit_id") long audit_id, @Param("status") String status);

	/**
	 * Finds all journeys for a domain audit record that are not in a given status
	 *
	 * @param audit_id the ID of the domain audit record
	 * @param status the status of the journey
	 * @return the number of journeys
	 */
	@Query("MATCH (audit:DomainAuditRecord) WHERE id(audit)=$audit_id MATCH (audit)-[*2]->(j:Journey) WHERE NOT j.status=$status RETURN COUNT(j)")
	public int findAllNonStatusJourneysForDomainAudit(@Param("audit_id") long audit_id, @Param("status") String status);
}
