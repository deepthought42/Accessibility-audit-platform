package com.looksee.models.repository;

import com.looksee.models.TestUser;
import java.util.List;
import java.util.Set;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;


/**
 * Repository for {@link TestUser}s
 */
public interface TestUserRepository extends Neo4jRepository<TestUser, Long> {

	/**
	 * Finds all test users for a domain
	 *
	 * @param domain_id the ID of the domain
	 * @return the test users
	 */
	@Query("MATCH (d:Domain)-[:HAS_TEST_USER]->(t:TestUser) WHERE id(d)=$domain_id RETURN t")
	public Set<TestUser> getTestUsers(@Param("domain_id") long domain_id);

	/**
	 * Finds all test users for a domain
	 *
	 * @param domain_id the ID of the domain
	 * @return the test users
	 */
	@Query("MATCH (d:Domain)-[]->(user:TestUser) WHERE id(d)=$domain_id RETURN user")
	public List<TestUser> findTestUsers(@Param("domain_id") long domain_id);
	
	/**
	 * Adds a test user to a step
	 *
	 * @param step_id the ID of the step
	 * @param user_id the ID of the test user
	 * @return the test user
	 */
	@Query("MATCH (s:Step) WITH s MATCH (user:TestUser) WHERE id(s)=$step_id AND id(user)=$user_id MERGE (s)-[:USES]->(user) RETURN user")
	public TestUser addTestUser(@Param("step_id") long step_id, @Param("user_id") long user_id);

	/**
	 * Finds a test user for a step
	 *
	 * @param step_id the ID of the step
	 * @return the test user
	 */
	@Query("MATCH (s:LoginStep)-[:USES]->(user:TestUser) WHERE id(s)=$step_id RETURN user")
	public TestUser getTestUser(@Param("step_id") long step_id);
}
