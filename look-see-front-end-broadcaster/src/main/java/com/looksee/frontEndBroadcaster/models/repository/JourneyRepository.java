package com.looksee.frontEndBroadcaster.models.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.frontEndBroadcaster.models.journeys.Journey;

@Repository
public interface JourneyRepository extends Neo4jRepository<Journey, Long>  {
	
	public Journey findByKey(@Param("key") String key);

	@Query("MATCH (map:DomainMap) WITH map WHERE id(map)=$map_id MATCH (map)-[:CONTAINS]->(j:Journey{key:$key}) RETURN j LIMIT 1")
	public Journey findByKey(@Param("map_id") long domain_map_id, @Param("key") String key);

	@Query("MATCH (j:Journey) WITH j WHERE id(j)=$journey_id MATCH (s:Step) WHERE id(s)=$step_id MERGE (j)-[:HAS]->(s) RETURN j")
	public Journey addStep(@Param("journey_id") long journey_id, @Param("step_id") long id);

	@Query("MATCH (j:Journey) WHERE id(j)=$journey_id SET j.status=$status, j.key=$key, j.orderedIds=$ordered_ids RETURN j")
	public Journey updateFields(@Param("journey_id") long journey_id, 
								@Param("status") String status, 
								@Param("key") String key,
								@Param("ordered_ids") List<Long> ordered_ids);

	@Query("MATCH (map:DomainMap) WITH map WHERE id(map)=$map_id MATCH (map)-[:CONTAINS]->(j:Journey{candidateKey:$candidateKey}) RETURN j LIMIT 1")
	public Journey findByCandidateKey(@Param("map_id") long domain_map_id, @Param("candidateKey") String candidate_key);

	@Query("MATCH (journey:Journey) WHERE id(journey)=$journey_id SET journey.status=$status RETURN journey")
	public Journey updateStatus(@Param("journey_id") long journey_id, @Param("status") String status);
}
