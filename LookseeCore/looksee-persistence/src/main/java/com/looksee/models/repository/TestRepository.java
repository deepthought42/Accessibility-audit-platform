package com.looksee.models.repository;

import com.looksee.models.LookseeObject;
import com.looksee.models.Test;
import java.util.List;
import java.util.Set;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for {@link Test}
 */
public interface TestRepository extends Neo4jRepository<Test, Long> {
	/**
	 * Finds a test by name
	 * @param name the name of the test
	 * @return the test if found
	 */
	public Test findByName(@Param("name") String name);

	/**
	 * Finds a test by key
	 * @param key the key of the test
	 * @param url the url of the page
	 * @param account_id the id of the account
	 * @return the test if found
	 */
	@Query("MATCH (acct:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test{key:$key}) MATCH a=(t)-[r:HAS_PATH_OBJECT]->(p) WHERE id(acct)=$account_id return a")
	public Test findByKey(@Param("key") String key, @Param("url") String url, @Param("account_id") long account_id);

	/**
	 * Gets the path objects for a test
	 * @param key the key of the test
	 * @param url the url of the page
	 * @param account_id the id of the account
	 * @return the path objects
	 */
	@Query("MATCH (a:Account{user_id:$user_id})-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test{key:$key}) MATCH (t)-[r:HAS_PATH_OBJECT]->(p) OPTIONAL MATCH b=(p)-[:HAS]->() WHERE id(a)=$account_id RETURN p,b")
	public List<LookseeObject> getPathObjects(@Param("key") String key, @Param("url") String url, @Param("account_id") long account_id);

	/**
	 * Finds tests with a specific element state
	 * @param page_state_key the key of the page state
	 * @param element_state_key the key of the element state
	 * @return the tests with the specific element state
	 */
	@Query("MATCH (t:Test)-[r:HAS_PATH_OBJECT]->(e:ElementState{key:$element_state_key}) MATCH (t)-[]->(p:PageState{key:$page_state_key}) RETURN t")
	public List<Test> findTestWithElementState(@Param("page_state_key") String page_state_key, @Param("element_state_key") String element_state_key);

	/**
	 * Finds tests with a specific page state
	 * @param page_state_key the key of the page state
	 * @param url the url of the page
	 * @param account_id the id of the account
	 * @return the tests with the specific page state
	 */
	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH (t)-[r:HAS_PATH_OBJECT]->(p:PageState{key:$page_state_key}) WHERE id(account)=$account_id RETURN t")
	public List<Test> findTestWithPageState(@Param("page_state_key") String page_state_key, @Param("url") String url, @Param("account_id") long account_id);

	/**
	 * Adds a test record to a test
	 * @param key the key of the test
	 * @param test_record_key the key of the test record
	 */
	@Query("MATCH (a:Account{user_id: $user_id})-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test),(tr:TestRecord{key:$test_record_key}) MERGE (t)-[r:HAS_TEST_RECORD]->(tr) RETURN r")
	public void addTestRecord(@Param("key") String key, @Param("test_record_key") String test_record_key);

	/**
	 * Finds all tests containing a specific path object key
	 * @param path_object_key the key of the path object
	 * @param url the url of the page
	 * @param user_id the id of the user
	 * @return the tests containing the path object key
	 */
	@Query("MATCH (a:Account{user_id: $user_id})-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test) WHERE {path_obj_key} in t.path_keys RETURN t")
	public Set<Test> findAllTestRecordsContainingKey(@Param("path_obj_key") String path_object_key, @Param("url") String url, @Param("user_id") String user_id);

	/**
	 * Adds a group to a test
	 * @param test_key the key of the test
	 * @param group_key the key of the group
	 * @param url the url of the page
	 * @param user_id the id of the user
	 */
	@Query("MATCH (a:Account{user_id: $user_id})-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test{key:$test_key}),(g:Group{key:$group_key}) MERGE (t)-[r:HAS_GROUP]->(g) RETURN r")
	public void addGroup(@Param("test_key") String test_key, @Param("group_key") String group_key, @Param("url") String url, @Param("user_id") String user_id);
}
