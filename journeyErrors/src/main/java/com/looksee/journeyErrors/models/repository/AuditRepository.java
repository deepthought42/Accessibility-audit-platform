package com.looksee.journeyErrors.models.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.journeyErrors.models.Audit;
import com.looksee.journeyErrors.models.ElementState;
import com.looksee.journeyErrors.models.UXIssueMessage;

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

	@Query("MATCH (audit:Audit) WITH audit MATCH (msg:UXIssueMessage) WHERE id(audit)=$audit_id AND id(msg) IN $issue_ids MERGE audit_issue=(audit)-[:HAS]->(msg) RETURN msg")
	public void addAllIssues(@Param("audit_id") long audit_id, @Param("issue_ids") List<Long> issue_ids);

	@Query("MATCH (audit:Audit{name:$audit_name})-[]->(msg:UXIssueMessage) MATCH (msg)-[]->(element:ElementState) WHERE msg.score >= $score RETURN element ORDER BY element.created_at DESC LIMIT 50")
	public List<ElementState> getIssuesByNameAndScore(@Param("audit_name") String audit_name,
													  @Param("score") int score);

	@Query("MATCH (audit:Audit)-[]->(ux_issue:UXIssueMessage) WHERE id(audit)=$audit_id AND NOT ux_issue.points=ux_issue.max_points RETURN COUNT(ux_issue)")
	public int getMessageCount(@Param("audit_id") long id);
	
	@Query("MATCH (dar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{is_accessibility:true}) WHERE id(dar)=$domain_audit_id RETURN audit")
	public Set<Audit> getAllAccessibilityAuditsForDomainRecord(@Param("domain_audit_id") long domain_audit_id);
	
	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState{key:$page_key}) MATCH (page_audit)-[]->(audit:Audit) RETURN audit")
	public Set<Audit> getMostRecentAuditsForPage(@Param("page_key") String key);

	@Query("MATCH (ar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{category:'Content'}) WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllContentAuditsForDomainRecord(@Param("id") long id);

	@Query("MATCH (ar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{category:'Information Architecture'})  WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllInformationArchitectureAuditsForDomainRecord(@Param("id") long id);

	@Query("MATCH (ar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{category:'Aesthetics'}) WHERE id(ar) = $id RETURN audit")
	public Set<Audit> getAllAestheticsAuditsForDomainRecord(@Param("id") long id);

	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit{category:'Content'}) WHERE id(ar)=$audit_record_id RETURN audit")
	public Set<Audit> getAllContentAudits(@Param("audit_record_id") long audit_record_id);

	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit{category:'Information Architecture'})  WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllInformationArchitectureAudits(@Param("id") long id);

	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit{category:'Aesthetics'}) WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllAestheticsAudits(@Param("id") long id);

	@Query("MATCH (par:PageAuditRecord)-[]->(audit:Audit{is_accessibility:true}) WHERE id(par)=$page_audit_id RETURN audit")
	public Set<Audit> getAllAccessibilityAudits(@Param("page_audit_id") long page_audit_id);

	@Query("MATCH (page_audit:PageAuditRecord)-[]->(audit:Audit) OPTIONAL MATCH auditsAndMessages=(audit)-->(:UXIssueMessage) WHERE id(page_audit)=$page_audit_id RETURN auditsAndMessages")
	public Set<Audit> getAllAuditsForPageAuditRecord(@Param("page_audit_id") long page_audit_id);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Color Management'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllColorManagementAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Visuals'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllVisualAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Color Palette'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageColorPaletteAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Text Background Contrast'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageTextColorContrastAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Non Text Background Contrast'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageNonTextColorContrastAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Typography'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllTypographyAudits(@Param("audit_record_key") String key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Typefaces'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageTypefaceAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Information Architecture'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllInformationArchitectureAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Links'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageLinkAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Titles'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageTitleAndHeaderAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Alt Text'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageAltTextAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Margin'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageMarginAudits(@Param("audit_record_key") String audit_record_key);
	
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Padding'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPagePaddingAudits(@Param("audit_record_key") String audit_record_key);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Paragraphing'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageParagraphingAudits(@Param("audit_record_key") String audit_record_key);
	
	@Query("MATCH (:AuditRecord{key:$audit_record_key})-[:HAS]->(a:Audit{key:$audit_key}) RETURN a")
	public Optional<Audit> getAuditForAuditRecord(@Param("audit_record_key") String audit_record_key, @Param("audit_key") String audit_key);

	@Query("MATCH (ar:PageAuditRecord)-[:HAS]->(a:Audit) WHERE id(ar)=$audit_record_id AND id(a)=$audit_id RETURN a")
	public Optional<Audit> getAuditForAuditRecord(@Param("audit_record_id") long audit_record_id, @Param("audit_id") long audit_id);

	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit) WHERE id(ar)=$audit_record_id RETURN audit")
	public Set<Audit> getAllAudits(@Param("audit_record_id") long audit_record_id);
	
	@Query("MATCH (ps:PageState{key:$page_state_key})<-[]-(a:Audit) RETURN a")
	public List<Audit> getAudits(@Param("page_state_key") String page_state_key);

	@Query("MATCH (p:PageState{key:$page_state_key})-[*]->(a:Audit{subcategory:$subcategory}) RETURN a")
	public Audit findAuditBySubCategory(@Param("subcategory") String subcategory, @Param("page_state_key") String page_state_key);

	
}
