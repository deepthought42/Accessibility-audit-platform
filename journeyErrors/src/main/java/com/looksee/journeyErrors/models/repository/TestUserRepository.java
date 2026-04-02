package com.looksee.journeyErrors.models.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import com.looksee.journeyErrors.models.TestUser;

/**
 * 
 */
public interface TestUserRepository extends Neo4jRepository<TestUser, Long> {

	@Query("MATCH (d:Domain)-[:HAS_TEST_USER]->(t:TestUser) WHERE id(d)=$domain_id RETURN t")
	public Set<TestUser> getTestUsers(@Param("domain_id") long domain_id);

	@Query("MATCH (d:Domain)-[]->(user:TestUser) WHERE id(d)=$domain_id RETURN user")
	public List<TestUser> findTestUsers(@Param("domain_id") long domain_id);
	
	@Query("MATCH (s:Step) WITH s MATCH (user:TestUser) WHERE id(s)=$step_id AND id(user)=$user_id MERGE (s)-[:USES]->(user) RETURN user")
	public TestUser addTestUser(@Param("step_id") long id, @Param("user_id") long user_id);

	@Query("MATCH (s:LoginStep)-[:USES]->(user:TestUser) WHERE id(s)=$step_id RETURN user")
	public TestUser getTestUser(@Param("step_id") long id);
}
