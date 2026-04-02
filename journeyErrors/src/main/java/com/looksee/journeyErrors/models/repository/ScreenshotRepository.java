package com.looksee.journeyErrors.models.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import com.looksee.journeyErrors.models.Screenshot;


/**
 * 
 */
public interface ScreenshotRepository extends Neo4jRepository<Screenshot, Long> {
	@Query("MATCH (p:Screenshot{key:$key}) RETURN p LIMIT 1")
	public Screenshot findByKey(@Param("key") String key);

	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$page_key}) MATCH (p)-[h:HAS]->(s:Screenshot) RETURN s")
	public List<Screenshot> getScreenshots(@Param("user_id") String user_id, @Param("page_key") String page_key);

}
