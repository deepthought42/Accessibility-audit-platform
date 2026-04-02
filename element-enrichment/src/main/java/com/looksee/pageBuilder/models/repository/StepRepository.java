package com.looksee.pageBuilder.models.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.pageBuilder.models.PageState;
import com.looksee.pageBuilder.models.ElementState;
import com.looksee.pageBuilder.models.journeys.Step;


@Repository
public interface StepRepository extends Neo4jRepository<Step, Long>{

	@Query("MATCH (step:Step{key:$key}) RETURN step LIMIT 1")
	public Step findByKey(@Param("key") String step_key);

	@Query("MATCH (:ElementInteractionStep{key:$step_key})-[:HAS]->(e:ElementState) RETURN e")
	public ElementState getElementState(@Param("step_key") String step_key);

	@Query("MATCH (s:Step) WITH s MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:STARTS_WITH]->(p) RETURN p")
	public PageState addStartPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	@Query("MATCH (s:Step) WITH s MATCH (p:ElementState) WHERE id(s)=$step_id AND id(p)=$element_state_id MERGE (s)-[:HAS]->(p) RETURN p")
	public ElementState addElementState(@Param("step_id") long id, @Param("element_state_id") long element_state_id);

	@Query("MATCH (map:DomainMap)-[*2]->(step:Step) where id(map)=$domain_map_id MATCH (step)-[:STARTS_WITH]->(p:PageState) WHERE id(p)=$page_state_id RETURN step")
	public List<Step> getStepsWithStartPage(@Param("domain_map_id") long domain_map_id, @Param("page_state_id") long id);

	@Query("MATCH (d:DomainAuditRecord) with d WHERE id(d)=$domain_audit_id MATCH(d)-[]->(p:PageState{key:$page_state_key}) WITH p MATCH (step:Step)-[:STARTS_WITH]->(p:PageState) RETURN step")
	public List<Step> getStepsWithStartPage(@Param("domain_audit_id") long domain_audit_id, @Param("page_state_key") String key);
	
	@Query("MATCH (step:Step) WITH step WHERE id(step)=$step_id MATCH (page:PageState) WHERE id(page)=$page_id MERGE (step)-[:ENDS_WITH]->(page) RETURN step")
	public Step addEndPage(@Param("step_id") long step_id, @Param("page_id") long page_id);

	@Query("MATCH (step:Step) WHERE id(step)=$step_id SET step.key=$step_key RETURN step")
	public Step updateKey(@Param("step_id") long step_id, @Param("step_key") String key);

	@Query("MATCH (step:Step) WITH step WHERE id(step)=$step_id MATCH (page:PageState) WHERE id(page)=$page_id MERGE (step)-[:STARTS_WITH]->(page) RETURN step")
	public Object setStartPage(@Param("step_id") Long step_id, @Param("page_id") Long page_id);

	@Query("MATCH (step:Step) WITH step WHERE id(step)=$step_id MATCH (element:ElementState) WHERE id(element)=$element_id MERGE (step)-[:HAS]->(element) RETURN step")
	public Object setElementState(@Param("step_id") Long step_id, @Param("element_id") Long element_id);

	@Query("MATCH (map:DomainMap)-[*2]-(step:Step{candidate_key:$key}) WHERE id(map)=$domain_map_id RETURN step LIMIT 1")
	public Step findByCandidateKey(@Param("key") String candidate_key, @Param("domain_map_id") long domain_map_id);
}
