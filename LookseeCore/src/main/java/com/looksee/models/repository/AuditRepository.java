package com.looksee.models.repository;

import com.looksee.models.ElementState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.messages.UXIssueMessage;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link Audit} objects
 */
@Repository
public interface AuditRepository extends Neo4jRepository<Audit, Long> {
	/**
	 * Find an audit by key
	 *
	 * @param key the key of the audit
	 * @return the audit
	 */
	public Audit findByKey(@Param("key") String key);

	/**
	 * Get the issue messages for an audit
	 *
	 * @param audit_id the id of the audit
	 * @return the issue messages
	 */
	@Query("MATCH (audit:Audit)-[:HAS]-(issue:UXIssueMessage) WHERE id(audit)=$audit_id OPTIONAL MATCH y=(issue)-->(element) RETURN issue, element")
	public Set<UXIssueMessage> findIssueMessages(@Param("audit_id") long audit_id);

	/**
	 * Add an issue message to the audit
	 *
	 * @param key the key of the audit
	 * @param issue_msg_key the key of the issue message
	 * @return the issue message
	 */
	@Query("MATCH (audit:Audit{key:$key}) WITH audit MATCH (msg:UXIssueMessage{key:$msg_key}) MERGE audit_issue=(audit)-[:HAS]->(msg) RETURN msg")
	public UXIssueMessage addIssueMessage(@Param("key") String key,
											@Param("msg_key") String issue_msg_key);

	/**
	 * Add all issues to the audit
	 *
	 * @param audit_id the id of the audit
	 * @param issue_ids the ids of the issues
	 */
	@Query("MATCH (audit:Audit) WITH audit MATCH (msg:UXIssueMessage) WHERE id(audit)=$audit_id AND id(msg) IN $issue_ids MERGE audit_issue=(audit)-[:HAS]->(msg) RETURN audit LIMIT 1")
	public void addAllIssues(@Param("audit_id") long audit_id, @Param("issue_ids") List<Long> issue_ids);

	/**
	 * Get the issues by name and score
	 *
	 * @param audit_name the name of the audit
	 * @param score the score of the issues
	 * @return the issues that have a score greater than or equal to the given score
	 */
	@Query("MATCH (audit:Audit{name:$audit_name})-[]->(msg:UXIssueMessage) MATCH (msg)-[]->(element:ElementState) WHERE msg.score >= $score RETURN element ORDER BY element.created_at DESC LIMIT 50")
	public List<ElementState> getIssuesByNameAndScore(@Param("audit_name") String audit_name,
														@Param("score") int score);

	/**
	 * Get the count of UXIssueMessages that have a score greater than or equal to the given score
	 *
	 * @param id the id of the audit
	 * @return the count of UXIssueMessages that have a score greater than or equal to the given score
	 */
	@Query("MATCH (audit:Audit)-[]->(ux_issue:UXIssueMessage) WHERE id(audit)=$audit_id AND NOT ux_issue.points=ux_issue.max_points RETURN COUNT(ux_issue)")
	public int getMessageCount(@Param("audit_id") long id);
	
	/**
	 * Retrieves all accessibility audits for a domain audit record.
	 *
	 * @param domain_audit_id the ID of the domain audit record
	 * @return the accessibility audits for the domain audit record
	 */
	@Query("MATCH (dar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{is_accessibility:true}) WHERE id(dar)=$domain_audit_id RETURN audit")
	public Set<Audit> getAllAccessibilityAuditsForDomainRecord(@Param("domain_audit_id") long domain_audit_id);
	
	/**
	 * Retrieves the most recent audits for a page.
	 *
	 * @param key the key of the page
	 * @return the most recent audits for the page
	 */
	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState{key:$page_key}) MATCH (page_audit)-[]->(audit:Audit) RETURN audit")
	public Set<Audit> getMostRecentAuditsForPage(@Param("page_key") String key);

	/**
	 * Retrieves all content audits for a domain audit record.
	 *
	 * @param id the ID of the domain audit record
	 * @return set of all content audits associated with the domain audit record
	 */
	@Query("MATCH (ar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{category:'Content'}) WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllContentAuditsForDomainRecord(@Param("id") long id);

	/**
	 * Retrieves all information architecture audits for a domain audit record.
	 *
	 * @param id the ID of the domain audit record
	 * @return set of all information architecture audits associated with the domain audit record
	 */
	@Query("MATCH (ar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{category:'Information Architecture'})  WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllInformationArchitectureAuditsForDomainRecord(@Param("id") long id);

	/**
	 * Retrieves all aesthetics audits for a domain audit record.
	 *
	 * @param id the ID of the domain audit record
	 * @return set of all aesthetics audits associated with the domain audit record
	 */
	@Query("MATCH (ar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{category:'Aesthetics'}) WHERE id(ar) = $id RETURN audit")
	public Set<Audit> getAllAestheticsAuditsForDomainRecord(@Param("id") long id);

	/**
	 * Retrieves all content audits for an audit record.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @return set of all content audits associated with the audit record
	 */
	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit{category:'Content'}) WHERE id(ar)=$audit_record_id RETURN audit")
	public Set<Audit> getAllContentAudits(@Param("audit_record_id") long audit_record_id);

	/**
	 * Retrieves all information architecture audits for an audit record.
	 *
	 * @param id the ID of the audit record
	 * @return set of all information architecture audits associated with the audit record
	 */
	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit{category:'Information Architecture'})  WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllInformationArchitectureAudits(@Param("id") long id);

	/**
	 * Retrieves all aesthetics audits for an audit record.
	 *
	 * @param id the ID of the audit record
	 * @return set of all aesthetics audits associated with the audit record
	 */
	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit{category:'Aesthetics'}) WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllAestheticsAudits(@Param("id") long id);

	/**
	 * Retrieves all accessibility audits for a page audit record.
	 *
	 * @param page_audit_id the ID of the page audit record
	 * @return set of all accessibility audits associated with the page audit record
	 */
	@Query("MATCH (par:PageAuditRecord)-[]->(audit:Audit{is_accessibility:true}) WHERE id(par)=$page_audit_id RETURN audit")
	public Set<Audit> getAllAccessibilityAudits(@Param("page_audit_id") long page_audit_id);

	/**
	 * Retrieves all audits and their associated messages for a page audit record.
	 *
	 * @param page_audit_id the ID of the page audit record
	 * @return set of all audits and their associated messages for the page audit record
	 */
	@Query("MATCH (page_audit:PageAuditRecord)-[]->(audit:Audit) OPTIONAL MATCH auditsAndMessages=(audit)-->(:UXIssueMessage) WHERE id(page_audit)=$page_audit_id RETURN auditsAndMessages")
	public Set<Audit> getAllAuditsForPageAuditRecord(@Param("page_audit_id") long page_audit_id);

	/**
	 * Retrieves all color management audits for a domain-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all color management audits at domain level
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Color Management'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllColorManagementAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all visual audits for a domain-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all visual audits at domain level
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Visuals'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllVisualAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page color palette audits.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page color palette audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Color Palette'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageColorPaletteAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page text color contrast audits.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page text color contrast audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Text Background Contrast'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageTextColorContrastAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page non-text color contrast audits.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page non-text color contrast audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Non Text Background Contrast'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageNonTextColorContrastAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all typography audits for a domain-level audit record.
	 *
	 * @param key the key of the audit record
	 * @return set of all typography audits at domain level
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Typography'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllTypographyAudits(@Param("audit_record_key") String key);

	/**
	 * Retrieves all page typeface audits.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page typeface audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Typefaces'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageTypefaceAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all information architecture audits for a domain-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all information architecture audits at domain level
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Information Architecture'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllInformationArchitectureAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page link audits.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page link audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Links'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageLinkAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page title and header audits.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page title and header audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Titles'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageTitleAndHeaderAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page alt text audits.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page alt text audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Alt Text'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageAltTextAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page margin audits.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page margin audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Margin'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageMarginAudits(@Param("audit_record_key") String audit_record_key);
	
	/**
	 * Retrieves all page padding audits.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page padding audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Padding'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPagePaddingAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page paragraphing audits.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page paragraphing audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Paragraphing'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageParagraphingAudits(@Param("audit_record_key") String audit_record_key);
	
	/**
	 * Retrieves an audit by its key for a specific audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @param audit_key the key of the audit
	 * @return Optional containing the audit if found
	 */
	@Query("MATCH (:AuditRecord{key:$audit_record_key})-[:HAS]->(a:Audit{key:$audit_key}) RETURN a")
	public Optional<Audit> getAuditForAuditRecord(@Param("audit_record_key") String audit_record_key, @Param("audit_key") String audit_key);

	/**
	 * Retrieves an audit by its ID for a specific audit record.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @param audit_id the ID of the audit
	 * @return Optional containing the audit if found
	 */
	@Query("MATCH (ar:PageAuditRecord)-[:HAS]->(a:Audit) WHERE id(ar)=$audit_record_id AND id(a)=$audit_id RETURN a")
	public Optional<Audit> getAuditForAuditRecord(@Param("audit_record_id") long audit_record_id, @Param("audit_id") long audit_id);

	/**
	 * Retrieves an audit by its ID for a specific audit record.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @param audit_key the key of the audit
	 * @return Optional containing the audit if found
	 */
	@Query("MATCH (ar:PageAuditRecord)-[:HAS]->(a:Audit{key:$audit_key}) WHERE id(ar)=$audit_record_id RETURN a")
	public Optional<Audit> getAuditForAuditRecord(@Param("audit_record_id") long audit_record_id, @Param("audit_key") String audit_key);


	/**
	 * Retrieves all audits for a page audit record.
	 *
	 * @param audit_record_id the ID of the page audit record
	 * @return set of all audits associated with the page audit record
	 */
	@Query("MATCH (ar:PageAuditRecord)-[:HAS]->(audit:Audit) WHERE id(ar)=$audit_record_id RETURN audit")
	public Set<Audit> getAllAudits(@Param("audit_record_id") long audit_record_id);
	
	/**
	 * Retrieves all audits for a domain audit record through its page audit records.
	 *
	 * @param audit_record_id the ID of the domain audit record
	 * @return set of all audits associated with the domain audit record
	 */
	@Query("MATCH (ar:DomainAuditRecord)-[:HAS*2]->(audit:Audit) WHERE id(ar)=$audit_record_id RETURN audit")
	public Set<Audit> getAllAuditsForDomainRecord(@Param("audit_record_id") long audit_record_id);
	
	/**
	 * Retrieves all audits for a domain audit record through all relationships.
	 *
	 * @param domain_audit_record_id the ID of the domain audit record
	 * @return set of all audits associated with the domain audit record
	 */
	@Query("MATCH (ar:AuditRecord)-[*2]->(audit:Audit) WHERE id(ar)=$audit_record_id RETURN audit")
	public Set<Audit> getAllAuditsForDomainAudit(@Param("audit_record_id") long domain_audit_record_id);

	/**
	 * Retrieves all audits for a page state.
	 *
	 * @param page_state_key the key of the page state
	 * @return list of all audits associated with the page state
	 */
	@Query("MATCH (ps:PageState{key:$page_state_key})<-[]-(a:Audit) RETURN a")
	public List<Audit> getAudits(@Param("page_state_key") String page_state_key);

	/**
	 * Finds an audit by subcategory for a specific page state.
	 *
	 * @param subcategory the subcategory of the audit
	 * @param page_state_key the key of the page state
	 * @return the audit if found, null otherwise
	 */
	@Query("MATCH (p:PageState{key:$page_state_key})-[*]->(a:Audit{subcategory:$subcategory}) RETURN a")
	public Audit findAuditBySubCategory(@Param("subcategory") String subcategory, @Param("page_state_key") String page_state_key);
}
