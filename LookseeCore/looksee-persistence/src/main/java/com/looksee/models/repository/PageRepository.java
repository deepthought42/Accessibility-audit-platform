package com.looksee.models.repository;

import com.looksee.audits.performance.PerformanceInsight;
import com.looksee.models.Element;
import com.looksee.models.Page;
import com.looksee.models.PageState;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for pages
 */
@Repository
public interface PageRepository extends Neo4jRepository<Page, Long> {
	/**
	 * Finds a page by key and user id
	 * @param user_id the user id
	 * @param key the key of the page
	 * @return the page if found
	 */
	@Deprecated
	@Query("MATCH(:Account{user_id:{user_id}})-[*]->(p:Page{key:{page_key}}) RETURN p LIMIT 1")
	public Page findByKeyAndUser(@Param("user_id") String user_id, @Param("page_key") String key);
	
	/**
	 * Finds a page by key
	 * @param key the key of the page
	 * @return the page if found
	 */
	@Query("MATCH (p:Page{key:{page_key}}) RETURN p LIMIT 1")
	public Page findByKey(@Param("page_key") String key);
	
	/**
	 * Gets the page states for a page
	 * @param url the url of the page
	 * @return the page states
	 */
	@Query("MATCH (p:Page{url:{url}})-[h:HAS]->(e:PageState) RETURN e")
	public Set<PageState> getPageStates(@Param("url") String url);

	/**
	 * Adds a performance insight to a page
	 * @param user_id the user id
	 * @param url the url of the page
	 * @param page_key the key of the page
	 * @param insight_key the key of the performance insight
	 * @return the performance insight
	 */
	@Query("MATCH(:Account{user_id:{user_id}})-[]-(d:Domain{url:{url}}) MATCH (d)-[]->(p:Page{key:{page_key}}),(insight:PerformanceInsight{key:{insight_key}}) CREATE (p)-[h:HAS]->(insight) RETURN insight")
	public PerformanceInsight addPerformanceInsight(@Param("user_id") String user_id, @Param("url") String url, @Param("page_key") String page_key, @Param("insight_key") String insight_key);

	/**
	 * Gets a performance insight for a page
	 * @param user_id the user id
	 * @param url the url of the page
	 * @param page_key the key of the page
	 * @param insight_key the key of the performance insight
	 * @return the performance insight
	 */
	@Query("MATCH(:Account{user_id:{user_id}})-[]-(d:Domain{url:{url}}) MATCH (d)-[]->(p:Page{key:{page_key}}) MATCH (p)-[]->(insight:PerformanceInsight{key:{insight_key}}) RETURN insight LIMIT 1")
	public PerformanceInsight getPerformanceInsight(
			@Param("user_id") String user_id, 
			@Param("url") String url, 
			@Param("page_key") String page_key, 
			@Param("insight_key") String insight_key);

	/**
	 * Gets all performance insights for a page
	 * @param page_key the key of the page
	 * @return the performance insights
	 */
	@Query("MATCH (p:Page{key:{page_key}})-[]->(insight:PerformanceInsight) RETURN insight")
	public List<PerformanceInsight> getAllPerformanceInsights(@Param("page_key") String page_key);
	
	/**
	 * Gets the latest performance insight for a page
	 * @param page_key the key of the page
	 * @return the latest performance insight
	 */
	@Query("MATCH (p:Page{key:{page_key}})-[]->(insight:PerformanceInsight) RETURN insight ORDER BY insight.executed_at DESC LIMIT 1")
	public PerformanceInsight getLatestPerformanceInsight(@Param("page_key") String page_key);

	/**
	 * Finds the most recent page state for a page
	 * @param page_key the key of the page
	 * @return the most recent page state
	 */
	@Query("MATCH (p:Page{key:{page_key}})-[:HAS]->(page_state:PageState) RETURN page_state ORDER BY page_state.created_at DESC LIMIT 1")
	public PageState findMostRecentPageState(@Param("page_key") String page_key);

	/**
	 * Finds a page state for a page
	 * @param page_key the key of the page
	 * @param page_state_id the id of the page state
	 * @return the page state if found
	 */
	@Query("MATCH (p:Page{key:{page_key}})-[]->(page_state:PageState) WHERE id(page_state)=$page_state_id RETURN page_state LIMIT 1")
	public Optional<PageState> findPageStateForPage(@Param("page_key") String page_key, @Param("page_state_id") long page_state_id);

	/**
	 * Adds a page state to a page
	 * @param page_key the key of the page
	 * @param page_state_id the id of the page state
	 */
	@Query("MATCH (p:Page{key:{page_key}}) WITH p MATCH (ps:PageState) WHERE id(ps)=$page_state_id CREATE (p)-[h:HAS]->(ps) RETURN ps")
	public void addPageState(@Param("page_key") String page_key, @Param("page_state_id") long page_state_id);

	/**
	 * Finds a page by url
	 * @param url the url of the page
	 * @return the page if found
	 */
	@Query("MATCH (p:Page{url:{url}}) RETURN p LIMIT 1")
	public Page findByUrl(@Param("url") String url);

	/**
	 * Gets the elements for a page
	 * @param key the key of the page
	 * @return the elements
	 */
	@Query("MATCH (p:Page{key:{key}})-[]->(e:Element) RETURN e")
	public List<Element> getElements(@Param("key") String key);
}
