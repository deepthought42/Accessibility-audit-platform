package com.looksee.models.repository;

import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.audits.performance.PageSpeedAudit;
import com.looksee.audits.performance.PerformanceInsight;

/**
 * Repository for PerformanceInsight
 */
@Repository
public interface PerformanceInsightRepository  extends Neo4jRepository<PerformanceInsight, Long> {
	/**
	 * Finds a performance insight by key
	 * @param key the key of the performance insight
	 * @return the performance insight
	 */
	public PerformanceInsight findByKey(@Param("key") String key);

	/**
	 * Adds an audit to a performance insight
	 * @param user_id the user id
	 * @param domain_url the domain url
	 * @param performance_insight_key the performance insight key
	 * @param audit_key the audit key
	 * @return the audit
	 */
	@Query("MATCH(:Account{user_id:$user_id})-[]-(d:Domain{url:$domain_url}) MATCH (d)-[:HAS]->(p:Page) MATCH (p)-[:HAS]->(i:PerformanceInsight{key:$performance_insight_key}),(audit:Audit{key:$audit_key}) MERGE (i)-[:HAS]->(audit) RETURN audit LIMIT 1")
	public PageSpeedAudit addAudit(@Param("user_id") String user_id, @Param("domain_url") String domain_url, @Param("performance_insight_key") String performance_insight_key, @Param("audit_key") String audit_key);

	/**
	 * Finds an audit by key
	 * @param user_id the user id
	 * @param domain_url the domain url
	 * @param performance_insight_key the performance insight key
	 * @param audit_key the audit key
	 * @return the audit
	 */
	@Query("MATCH(:Account{user_id:$user_id})-[]-(d:Domain{url:$domain_url}) MATCH (d)-[:HAS]->(p:Page) MATCH (p)-[]->(i:PerformanceInsight{key:$performance_insight_key}) MATCH (i)-[:HAS]->(audit:Audit{key:$audit_key}) RETURN audit LIMIT 1")
	public PageSpeedAudit findAuditByKey(@Param("user_id") String user_id, @Param("domain_url") String domain_url, @Param("performance_insight_key") String performance_insight_key, @Param("audit_key") String audit_key);

	/**
	 * Gets all audits for a performance insight
	 * @param page_key the page key
	 * @param insight_key the performance insight key
	 * @return the audits
	 */
	@Query("MATCH (p:Page{key:$page_key})-[:HAS]->(insight:PerformanceInsight{key:$insight_key}) MATCH (insight)-[:HAS]->(audit) OPTIONAL MATCH z=(audit)-->(x) OPTIONAL MATCH y=(x)-->(detail) RETURN z,y")
	public List<PageSpeedAudit> getAllAudits(@Param("page_key") String page_key, @Param("insight_key") String insight_key);

}
