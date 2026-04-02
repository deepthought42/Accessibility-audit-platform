package com.looksee.pageBuilder.models.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.pageBuilder.models.ElementState;
import com.looksee.pageBuilder.models.PageState;
import com.looksee.pageBuilder.models.journeys.SimpleStep;


@Repository
public interface SimpleStepRepository extends Neo4jRepository<SimpleStep, Long> {

	@Query("MATCH (step:SimpleStep{key:$step_key}) RETURN step LIMIT 1")
	public SimpleStep findByKey(@Param("step_key") String step_key);

	@Query("MATCH (:SimpleStep{key:$step_key})-[:HAS]->(e:ElementState) RETURN e")
	public ElementState getElementState(@Param("step_key") String step_key);

	@Query("MATCH (s:SimpleStep) WITH s MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:STARTS_WITH]->(p) RETURN p")
	public PageState addStartPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	@Query("MATCH (s:SimpleStep) WITH s MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:ENDS_WITH]->(p) RETURN p")
	public PageState addEndPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	@Query("MATCH (s:SimpleStep) WITH s MATCH(p:ElementState) WHERE id(s)=$step_id AND id(p)=$element_state_id MERGE (s)-[:HAS]->(p) RETURN p")
	public ElementState addElementState(@Param("step_id") long id, @Param("element_state_id") long element_state_id);

	@Query("MATCH (:SimpleStep{key:$step_key})-[:STARTS_WITH]->(p:PageState) RETURN p")
	public PageState getEndPage(@Param("step_key") String key);
	
	@Query("MATCH (:SimpleStep{key:$step_key})-[:ENDS_WITH]->(p:PageState) RETURN p")
	public PageState getStartPage(@Param("step_key") String key);
}
