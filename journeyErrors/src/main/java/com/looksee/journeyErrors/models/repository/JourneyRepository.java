package com.looksee.journeyErrors.models.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.journeyErrors.models.enums.JourneyStatus;
import com.looksee.journeyErrors.models.journeys.Journey;

import io.github.resilience4j.retry.annotation.Retry;

@Repository
@Retry(name = "neoforj")
public interface JourneyRepository extends Neo4jRepository<Journey, Long>  {
	
	public Journey findByKey(@Param("key") String key);

	@Query("MATCH (j:Journey) WITH j WHERE id(j)=$journey_id MATCH (s:Step) WHERE id(s)=$step_id MERGE (j)-[:HAS]->(s) RETURN j")
	public Journey addStep(@Param("journey_id") long journey_id, @Param("step_id") long id);

	@Query("MATCH (j:Journey{candidateKey:$candidateKey}) RETURN j LIMIT 1")
	public Journey findByCandidateKey(@Param("candidateKey") String candidateKey);
	
	@Query("MATCH (audit:DomainAuditRecord) WHERE id(audit)=$audit_id MATCH (audit)-[*2]->(j:Journey) WHERE j.status=$status RETURN COUNT(j)")
	public int findAllJourneysForDomainAudit(@Param("audit_id") long audit_id, @Param("status") JourneyStatus status);

	@Query("MATCH (j:Journey) WHERE id(j)=$journey_id SET j.status=$status, j.key=$key, j.orderedIds=$ordered_ids RETURN j")
	public Journey updateFields(@Param("journey_id") long journey_id, 
								@Param("status") JourneyStatus status, 
								@Param("key") String key,
								@Param("ordered_ids") List<Long> ordered_ids);

	@Query("MATCH (map:DomainMap) WITH map WHERE id(map)=$map_id MATCH (map)-[:CONTAINS]->(j:Journey{candidateKey:$candidateKey}) RETURN j")
	public Journey findByCandidateKey(@Param("map_id") long domain_map_id, @Param("candidateKey") String candidate_key);

	@Query("MATCH (journey:Journey) WHERE id(journey)=$journey_id SET journey.status=$status RETURN journey")
	public Journey updateStatus(@Param("journey_id") long journey_id, @Param("status") String status);
}
