package com.looksee.models.repository;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.journeys.SimpleStep;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with
 * {@link SimpleStep} objects
 */
@Repository
public interface SimpleStepRepository extends Neo4jRepository<SimpleStep, Long> {

	/**
	 * Finds a simple step by its key
	 *
	 * @param step_key the key of the simple step
	 * @return the simple step
	 */
	@Query("MATCH (step:SimpleStep{key:$step_key}) RETURN step LIMIT 1")
	public SimpleStep findByKey(@Param("step_key") String step_key);

	/**
	 * Finds an element state for a simple step
	 *
	 * @param step_key the key of the simple step
	 * @return the element state
	 */
	@Query("MATCH (:SimpleStep{key:$step_key})-[:HAS]->(e:ElementState) RETURN e")
	public ElementState getElementState(@Param("step_key") String step_key);

	/**
	 * Adds a start page to a simple step
	 *
	 * @param step_id the ID of the simple step
	 * @param page_state_id the ID of the page state
	 * @return the page state
	 */
	@Query("MATCH (s:SimpleStep) WITH s MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:STARTS_WITH]->(p) RETURN p")
	public PageState addStartPage(@Param("step_id") long step_id, @Param("page_state_id") long page_state_id);
	
	/**
	 * Adds an end page to a simple step
	 *
	 * @param step_id the ID of the simple step
	 * @param page_state_id the ID of the page state
	 * @return the page state
	 */
	@Query("MATCH (s:SimpleStep) WITH s MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:ENDS_WITH]->(p) RETURN p")
	public PageState addEndPage(@Param("step_id") long step_id, @Param("page_state_id") long page_state_id);
	
	/**
	 * Adds an element state to a simple step
	 *
	 * @param step_id the ID of the simple step
	 * @param element_state_id the ID of the element state
	 * @return the element state
	 */
	@Query("MATCH (s:SimpleStep) WITH s MATCH(p:ElementState) WHERE id(s)=$step_id AND id(p)=$element_state_id MERGE (s)-[:HAS]->(p) RETURN p")
	public ElementState addElementState(@Param("step_id") long step_id, @Param("element_state_id") long element_state_id);

	/**
	 * Finds the end page for a simple step
	 *
	 * @param step_key the key of the simple step
	 * @return the page state
	 */
	@Query("MATCH (:SimpleStep{key:$step_key})-[:STARTS_WITH]->(p:PageState) RETURN p")
	public PageState getEndPage(@Param("step_key") String step_key);
	
	/**
	 * Finds the start page for a simple step
	 *
	 * @param step_key the key of the simple step
	 * @return the page state
	 */
	@Query("MATCH (:SimpleStep{key:$step_key})-[:ENDS_WITH]->(p:PageState) RETURN p")
	public PageState getStartPage(@Param("step_key") String step_key);
}
