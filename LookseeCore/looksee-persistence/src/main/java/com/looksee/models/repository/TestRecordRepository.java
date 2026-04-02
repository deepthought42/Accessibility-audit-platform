package com.looksee.models.repository;

import com.looksee.models.TestRecord;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TestRecord}s
 */
@Repository
public interface TestRecordRepository extends Neo4jRepository<TestRecord, Long> {
	
	/**
	 * Finds a test record by key
	 *
	 * @param key the key of the test record
	 * @return the test record
	 */
	@Query("MATCH b=(tr:TestRecord{key:$key})-[:HAS_RESULT]->(p) MATCH a=(p)-[]->() RETURN a,b")
	public TestRecord findByKey(@Param("key") String key);
	
	/**
	 * Updates the status of a test record
	 *
	 * @param key the key of the test record
	 * @param status the status of the test record
	 * @return the test record
	 */
	@Query("MATCH (tr:TestRecord{key:$key}) SET tr.status={status} RETURN tr")
	public TestRecord updateStatus(@Param("key") String key, @Param("status") String status);
	
	/**
	 * Finds all test records for a test
	 *
	 * @param key the key of the test
	 * @param url the URL of the domain
	 * @param user_id the ID of the user
	 * @return the test records
	 */
	@Query("MATCH (a:Account{user_id: $user_id})-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test{key:$key}) MATCH (t)-[r:HAS_TEST_RECORD]->(tr:TestRecord) MATCH ps=(tr)-[:HAS_PAGE_STATE]->(p) RETURN ps ORDER BY tr.ran_at DESC")
	public List<TestRecord> findAllTestRecords(@Param("key") String key, @Param("url") String url, @Param("user_id") String user_id);

	/**
	 * Finds the most recent test record for a test
	 *
	 * @param key the key of the test
	 * @param url the URL of the domain
	 * @param user_id the ID of the user
	 * @return the test record
	 */
	@Query("MATCH (a:Account{user_id: $user_id})-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test{key:$key}) MATCH (t)-[r:HAS_TEST_RECORD]->(tr:TestRecord) RETURN tr ORDER BY tr.ran_at DESC LIMIT 1")
	public TestRecord getMostRecentRecord(@Param("key") String key, @Param("url") String url, @Param("user_id") String user_id);
}
