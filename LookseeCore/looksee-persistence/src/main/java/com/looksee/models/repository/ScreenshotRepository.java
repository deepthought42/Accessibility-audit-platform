package com.looksee.models.repository;

import com.looksee.models.Screenshot;
import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for {@link Screenshot}s
 */
public interface ScreenshotRepository extends Neo4jRepository<Screenshot, Long> {
	/**
	 * Finds a screenshot by key
	 *
	 * @param key the key of the screenshot
	 * @return the screenshot
	 */
	@Query("MATCH (p:Screenshot{key:$key}) RETURN p LIMIT 1")
	public Screenshot findByKey(@Param("key") String key);

	/**
	 * Finds all screenshots for a page state
	 *
	 * @param user_id the ID of the user
	 * @param page_key the key of the page state
	 * @return the screenshots
	 */
	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$page_key}) MATCH (p)-[h:HAS]->(s:Screenshot) RETURN s")
	public List<Screenshot> getScreenshots(@Param("user_id") String user_id, @Param("page_key") String page_key);
}
