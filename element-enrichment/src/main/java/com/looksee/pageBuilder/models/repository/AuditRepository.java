package com.looksee.pageBuilder.models.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.pageBuilder.models.Audit;
import com.looksee.pageBuilder.models.ElementState;
import com.looksee.pageBuilder.models.UXIssueMessage;


/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link Audit} objects
 */
@Repository
public interface AuditRepository extends Neo4jRepository<Audit, Long> {
	public Audit findByKey(@Param("key") String key);

	@Query("MATCH (audit:Audit)-[:HAS]-(issue:UXIssueMessage) WHERE id(audit)=$audit_id OPTIONAL MATCH y=(issue)-->(element) RETURN issue, element")
	public Set<UXIssueMessage> findIssueMessages(@Param("audit_id") long audit_id);

	@Query("MATCH (audit:Audit{key:$key}) WITH audit MATCH (msg:UXIssueMessage{key:$msg_key}) MERGE audit_issue=(audit)-[:HAS]->(msg) RETURN msg")
	public UXIssueMessage addIssueMessage(@Param("key") String key, 
									  @Param("msg_key") String issue_msg_key);

	@Query("MATCH (audit:Audit) WITH audit MATCH (msg:UXIssueMessage) WHERE id(audit)=$audit_id AND id(msg) IN $issue_ids MERGE audit_issue=(audit)-[:HAS]->(msg) RETURN audit LIMIT 1")
	public void addAllIssues(@Param("audit_id") long audit_id, @Param("issue_ids") List<Long> issue_ids);

	@Query("MATCH (audit:Audit{name:$audit_name})-[]->(msg:UXIssueMessage) MATCH (msg)-[]->(element:ElementState) WHERE msg.score >= $score RETURN element ORDER BY element.created_at DESC LIMIT 50")
	public List<ElementState> getIssuesByNameAndScore(@Param("audit_name") String audit_name,
													  @Param("score") int score);

	@Query("MATCH (audit:Audit)-[]->(ux_issue:UXIssueMessage) WHERE id(audit)=$audit_id AND NOT ux_issue.points=ux_issue.max_points RETURN COUNT(ux_issue)")
	public int getMessageCount(@Param("audit_id") long id);
}
