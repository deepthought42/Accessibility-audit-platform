package com.looksee.models.repository;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.journeys.Step;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with
 * {@link Step} objects
 */
@Repository
public interface StepRepository extends Neo4jRepository<Step, Long>{

	/**
	 * Finds a step by its key
	 *
	 * @param step_key the key of the step
	 * @return the step
	 */
	@Query("MATCH (step:Step{key:$key}) RETURN step LIMIT 1")
	public Step findByKey(@Param("key") String step_key);

	/**
	 * Finds an element state for a step
	 *
	 * @param step_key the key of the step
	 * @return the element state
	 */
	@Query("MATCH (:ElementInteractionStep{key:$step_key})-[:HAS]->(e:ElementState) RETURN e")
	public ElementState getElementState(@Param("step_key") String step_key);

	/**
	 * Adds a start page to a step
	 *
	 * @param step_id the ID of the step
	 * @param page_state_id the ID of the page state
	 * @return the page state
	 */
	@Query("MATCH (s:Step) WITH s MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:STARTS_WITH]->(p) RETURN p")
	public PageState addStartPage(@Param("step_id") long step_id, @Param("page_state_id") long page_state_id);
	
	/**
	 * Adds an element state to a step
	 *
	 * @param step_id the ID of the step
	 * @param element_state_id the ID of the element state
	 * @return the element state
	 */
	@Query("MATCH (s:Step) WITH s MATCH (p:ElementState) WHERE id(s)=$step_id AND id(p)=$element_state_id MERGE (s)-[:HAS]->(p) RETURN p")
	public ElementState addElementState(@Param("step_id") long step_id, @Param("element_state_id") long element_state_id);

	/**
	 * Finds steps with a start page
	 *
	 * @param domain_map_id the ID of the domain map
	 * @param page_state_id the ID of the page state
	 * @return the steps
	 */
	@Query("MATCH (map:DomainMap)-[*2]->(step:Step) where id(map)=$domain_map_id MATCH (step)-[:STARTS_WITH]->(p:PageState) WHERE id(p)=$page_state_id RETURN step")
	public List<Step> getStepsWithStartPage(@Param("domain_map_id") long domain_map_id, @Param("page_state_id") long page_state_id);

	/**
	 * Finds steps with a start page
	 *
	 * @param domain_audit_id the ID of the domain audit
	 * @param page_state_key the key of the page state
	 * @return the steps
	 */
	@Query("MATCH (d:DomainAuditRecord) with d WHERE id(d)=$domain_audit_id MATCH(d)-[]->(p:PageState{key:$page_state_key}) WITH p MATCH (step:Step)-[:STARTS_WITH]->(p:PageState) RETURN step")
	public List<Step> getStepsWithStartPage(@Param("domain_audit_id") long domain_audit_id, @Param("page_state_key") String page_state_key);
	
	/**
	 * Adds an end page to a step
	 *
	 * @param step_id the ID of the step
	 * @param page_id the ID of the page state
	 * @return the step
	 */
	@Query("MATCH (step:Step) WITH step WHERE id(step)=$step_id MATCH (page:PageState) WHERE id(page)=$page_id MERGE (step)-[:ENDS_WITH]->(page) RETURN step")
	public Step addEndPage(@Param("step_id") long step_id, @Param("page_id") long page_id);

	/**
	 * Updates the key of a step
	 *
	 * @param step_id the ID of the step
	 * @param step_key the key of the step
	 * @return the step
	 */
	@Query("MATCH (step:Step) WHERE id(step)=$step_id SET step.key=$step_key RETURN step")
	public Step updateKey(@Param("step_id") long step_id, @Param("step_key") String step_key);

	/**
	 * Sets the start page for a step
	 *
	 * @param step_id the ID of the step
	 * @param page_id the ID of the page state
	 * @return the step
	 */
	@Query("MATCH (step:Step) WITH step WHERE id(step)=$step_id MATCH (page:PageState) WHERE id(page)=$page_id MERGE (step)-[:STARTS_WITH]->(page) RETURN step")
	public Object setStartPage(@Param("step_id") Long step_id, @Param("page_id") Long page_id);

	/**
	 * Sets the element state for a step
	 *
	 * @param step_id the ID of the step
	 * @param element_id the ID of the element state
	 * @return the step
	 */
	@Query("MATCH (step:Step) WITH step WHERE id(step)=$step_id MATCH (element:ElementState) WHERE id(element)=$element_id MERGE (step)-[:HAS]->(element) RETURN step")
	public Object setElementState(@Param("step_id") Long step_id, @Param("element_id") Long element_id);

	/**
	 * Finds a step by its candidate key
	 *
	 * @param candidate_key the candidate key of the step
	 * @param domain_map_id the ID of the domain map
	 * @return the step
	 */
	@Query("MATCH (map:DomainMap)-[*2]-(step:Step{candidate_key:$key}) WHERE id(map)=$domain_map_id RETURN step LIMIT 1")
	public Step findByCandidateKey(@Param("key") String candidate_key, @Param("domain_map_id") long domain_map_id);

	/**
	 * Finds steps with a start page
	 *
	 * @param page_state_id the ID of the page state
	 * @return the steps
	 */
	@Query("MATCH (p:PageState) WITH p WHERE id(p)=$page_state_id MATCH (step:Step)-[:STARTS_WITH]->(p:PageState) RETURN step")
	public List<Step> getStepsWithStartPage(@Param("page_state_id") long page_state_id);

	/**
	 * Updates the {@link JourneyStatus} for a step with the given ID
	 * 
	 * @param step_id the ID of the step
	 * @param status the {@link JourneyStatus} to set
	 */
	@Query("MATCH (step:Step) WHERE id(step)=$step_id SET step.status=$status RETURN step")
	public void updateStatus(@Param("step_id") long step_id, @Param("status") JourneyStatus status);

	/**
	 * Adds a step to a journey
	 *
	 * @param journey_id the ID of the journey
	 * @param step_id the ID of the step
	 * @return the step
	 */
	@Query("MATCH (j:Journey) WITH j MATCH (s:Step) WHERE id(s)=$step_id AND id(j)=$journey_id MERGE (j)-[:HAS]->(s) RETURN s")
	public Step addStep(@Param("journey_id") long journey_id, @Param("step_id") long step_id);
}
