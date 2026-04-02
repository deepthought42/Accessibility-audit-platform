package com.looksee.models.repository;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.TestUser;
import com.looksee.models.journeys.LoginStep;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with
 * {@link LoginStep} objects
 */
@Repository
public interface LoginStepRepository extends Neo4jRepository<LoginStep, Long> {

	/**
	 * Finds a login step by its key
	 *
	 * @param step_key the key of the login step
	 * @return the login step
	 */
	@Query("MATCH (step:LoginStep{key:$step_key}) RETURN step LIMIT 1")
	public LoginStep findByKey(@Param("step_key") String step_key);

	/**
	 * Adds a username element to a login step
	 *
	 * @param id the ID of the login step
	 * @param element_id the ID of the element
	 * @return the element state
	 */
	@Query("MATCH (s:Step) WITH s MATCH (e:ElementState) WHERE id(s)=$step_id AND id(e)=$element_id MERGE (s)-[:USERNAME_INPUT]->(e) RETURN e")
	public ElementState addUsernameElement(@Param("step_id") long id, @Param("element_id") long element_id);
	
	/**
	 * Finds the username element for a login step
	 *
	 * @param id the ID of the login step
	 * @return the element state
	 */
	@Query("MATCH (s:LoginStep)-[:USERNAME_INPUT]->(e:ElementState) WHERE id(s)=$step_id RETURN e")
	public ElementState getUsernameElement(@Param("step_id") long id);
	
	/**
	 * Adds a password element to a login step
	 *
	 * @param id the ID of the login step
	 * @param element_id the ID of the element
	 * @return the element state
	 */
	@Query("MATCH (s:Step) WITH s MATCH (e:ElementState) WHERE id(s)=$step_id AND id(e)=$element_id MERGE (s)-[:PASSWORD_INPUT]->(e) RETURN e")
	public ElementState addPasswordElement(@Param("step_id") long id, @Param("element_id") long element_id);
	
	/**
	 * Finds the password element for a login step
	 *
	 * @param id the ID of the login step
	 * @return the element state
	 */
	@Query("MATCH (s:LoginStep)-[:PASSWORD_INPUT]->(e:ElementState) WHERE id(s)=$step_id RETURN e")
	public ElementState getPasswordElement(@Param("step_id") long id);
	
	/**
	 * Adds a submit element to a login step
	 *
	 * @param id the ID of the login step
	 * @param element_id the ID of the element
	 * @return the element state
	 */
	@Query("MATCH (s:Step) WITH s MATCH (e:ElementState) WHERE id(s)=$step_id AND id(e)=$element_id MERGE (s)-[:SUBMIT]->(e) RETURN e")
	public ElementState addSubmitElement(@Param("step_id") long id, @Param("element_id") long element_id);

	/**
	 * Finds the submit element for a login step
	 *
	 * @param id the ID of the login step
	 * @return the element state
	 */
	@Query("MATCH (s:LoginStep)-[:SUBMIT]->(e:ElementState) WHERE id(s)=$step_id RETURN e")
	public ElementState getSubmitElement(@Param("step_id") long id);
	
	/**
	 * Adds a start page to a login step
	 *
	 * @param id the ID of the login step
	 * @param page_state_id the ID of the page state
	 * @return the page state
	 */
	@Query("MATCH (s:Step) WITH s MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:STARTS_WITH]->(p) RETURN p")
	public PageState addStartPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	/**
	 * Finds the start page for a login step
	 *
	 * @param id the ID of the login step
	 * @return the page state
	 */
	@Query("MATCH (s:LoginStep)-[:STARTS_WITH]->(page:PageState) WHERE id(s)=$step_id RETURN page")
	public PageState getStartPage(@Param("step_id") long id);
	
	/**
	 * Adds an end page to a login step
	 *
	 * @param id the ID of the login step
	 * @param page_state_id the ID of the page state
	 * @return the page state
	 */
	@Query("MATCH (s:Step) WITH s MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:ENDS_WITH]->(p) RETURN p")
	public PageState addEndPage(@Param("step_id") long id, @Param("page_state_id") long page_state_id);
	
	/**
	 * Finds the end page for a login step
	 *
	 * @param id the ID of the login step
	 * @return the page state
	 */
	@Query("MATCH (s:Step)-[:ENDS_WITH]->(page:PageState) WHERE id(s)=$step_id RETURN page")
	public PageState getEndPage(@Param("step_id") long id);
	
	/**
	 * Adds a test user to a login step
	 *
	 * @param id the ID of the login step
	 * @param user_id the ID of the test user
	 * @return the test user
	 */
	@Query("MATCH (s:Step) WITH s MATCH (user:TestUser) WHERE id(s)=$step_id AND id(user)=$user_id MERGE (s)-[:USES]->(user) RETURN user")
	public TestUser addTestUser(@Param("step_id") long id, @Param("user_id") long user_id);

	/**
	 * Finds the test user for a login step
	 *
	 * @param id the ID of the login step
	 * @return the test user
	 */
	@Query("MATCH (s:LoginStep)-[:USES]->(user:TestUser) WHERE id(s)=$step_id RETURN user")
	public TestUser getTestUser(@Param("step_id") long id);
}
