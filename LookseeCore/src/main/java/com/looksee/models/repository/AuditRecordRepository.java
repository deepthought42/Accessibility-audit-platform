package com.looksee.models.repository;

import com.looksee.models.Label;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link Audit} objects.
 *
 * <p><b>Thread Safety:</b>
 * This repository is thread-safe as it is managed by Spring's dependency injection container.
 * All operations are atomic at the database level.
 */
@Repository
@Retry(name = "neoforj")
public interface AuditRecordRepository extends Neo4jRepository<AuditRecord, Long> {
	/**
	 * Finds an audit record by its unique key.
	 *
	 * @param key the unique identifier of the audit record
	 * @return the audit record if found, null otherwise
	 */
	public AuditRecord findByKey(@Param("key") String key);
	
	/**
	 * Retrieves a specific audit associated with an audit record by their keys.
	 *
	 * @param audit_record_key the key of the audit record
	 * @param audit_key the key of the audit
	 * @return Optional containing the audit if found and associated
	 */
	@Query("MATCH (:AuditRecord{key:$audit_record_key})-[:HAS]->(a:Audit{key:$audit_key}) RETURN a")
	public Optional<Audit> getAuditForAuditRecord(@Param("audit_record_key") String audit_record_key, @Param("audit_key") String audit_key);

	/**
	 * Retrieves a specific audit associated with an audit record by their IDs.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @param audit_id the ID of the audit
	 * @return Optional containing the audit if found and associated
	 */
	@Query("MATCH (ar:PageAuditRecord)-[:HAS]->(a:Audit) WHERE id(ar)=$audit_record_id AND id(a)=$audit_id RETURN a")
	public Optional<Audit> getAuditForAuditRecord(@Param("audit_record_id") long audit_record_id, @Param("audit_id") long audit_id);

	/**
	 * Adds an audit to an audit record by their keys.
	 *
	 * @param audit_record_key the key of the audit record
	 * @param audit_key the key of the audit to add
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key}) WITH ar MATCH (a:Audit{key:$audit_key}) MERGE (ar)-[h:HAS]->(a) RETURN ar")
	public void addAudit(@Param("audit_record_key") String audit_record_key, @Param("audit_key") String audit_key);
	
	/**
	 * Adds an audit to an audit record by their IDs.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @param audit_id the ID of the audit to add
	 */
	@Query("MATCH (ar:PageAuditRecord) WITH ar MATCH (a:Audit) WHERE id(ar)=$audit_record_id AND id(a)=$audit_id MERGE (ar)-[h:HAS]->(a) RETURN ar")
	public void addAudit(@Param("audit_record_id") long audit_record_id, @Param("audit_id") long audit_id);
	
	/**
	 * Adds a page audit record to a domain audit record by key.
	 *
	 * @param domain_audit_record_id the ID of the domain audit record
	 * @param page_audit_key the key of the page audit record to add
	 */
	@Query("MATCH (dar:DomainAuditRecord) WITH dar MATCH (par:PageAuditRecord{key:$page_audit_key}) WHERE id(dar)=$domain_audit_record_id MERGE (dar)-[h:HAS]->(par) RETURN dar")
	public void addPageAuditRecord(@Param("domain_audit_record_id") long domain_audit_record_id, @Param("page_audit_key") String page_audit_key);

	/**
	 * Adds a page audit record to a domain audit record by IDs.
	 *
	 * @param domain_audit_record_id the ID of the domain audit record
	 * @param page_audit_record_id the ID of the page audit record to add
	 */
	@Query("MATCH (dar:DomainAuditRecord) WITH dar MATCH (par:PageAuditRecord) WHERE id(dar)=$domain_audit_record_id AND id(par)=$page_audit_id MERGE (dar)-[h:HAS]->(par) RETURN dar")
	public void addPageAuditRecord(@Param("domain_audit_record_id") long domain_audit_record_id, @Param("page_audit_id") long page_audit_record_id);
	
	/**
	 * Retrieves all audits associated with an audit record.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @return set of all associated audits
	 */
	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit) WHERE id(ar)=$audit_record_id RETURN audit")
	public Set<Audit> getAllAudits(@Param("audit_record_id") long audit_record_id);

	/**
	 * Finds the most recent domain audit record for a domain.
	 *
	 * @param domain_id the ID of the domain
	 * @return Optional containing the most recent domain audit record
	 */
	@Query("MATCH (d:Domain)-[]-(ar:DomainAuditRecord) WHERE id(d)=$domain_id RETURN ar ORDER BY ar.created_at DESC LIMIT 1")
	public Optional<DomainAuditRecord> findMostRecentDomainAuditRecord(@Param("domain_id") long domain_id);

	/**
	 * Retrieves all color management audits for a domain-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all color management audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Color Management'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllColorManagementAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all visual audits for a domain-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all visual audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Visuals'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllVisualAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page color palette audits for a page-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page color palette audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Color Palette'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageColorPaletteAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page text color contrast audits for a page-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page text color contrast audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Text Background Contrast'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageTextColorContrastAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page non-text color contrast audits for a page-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page non-text color contrast audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Non Text Background Contrast'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageNonTextColorContrastAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all typography audits for a domain-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all typography audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Typography'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllTypographyAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page typeface audits for a page-level audit record.
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
	 * @return set of all information architecture audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{category:'Information Architecture'}) WHERE audit.level='domain' RETURN audit")
	public Set<Audit> getAllInformationArchitectureAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page link audits for a page-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page link audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Links'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageLinkAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page title and header audits for a page-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page title and header audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Titles'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageTitleAndHeaderAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page alt text audits for a page-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page alt text audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Alt Text'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageAltTextAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page margin audits for a page-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page margin audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Margin'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageMarginAudits(@Param("audit_record_key") String audit_record_key);
	
	/**
	 * Retrieves all page padding audits for a page-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page padding audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Padding'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPagePaddingAudits(@Param("audit_record_key") String audit_record_key);

	/**
	 * Retrieves all page paragraphing audits for a page-level audit record.
	 *
	 * @param audit_record_key the key of the audit record
	 * @return set of all page paragraphing audits
	 */
	@Query("MATCH (ar:AuditRecord{key:$audit_record_key})-[]->(audit:Audit{subcategory:'Paragraphing'}) WHERE audit.level='page' RETURN audit")
	public Set<Audit> getAllPageParagraphingAudits(@Param("audit_record_key") String audit_record_key);
	
	/**
	 * Retrieves all audits associated with a domain audit record.
	 *
	 * @param domain_audit_id the ID of the domain audit record
	 * @return set of all associated audits
	 */
	@Query("MATCH (dar:DomainAuditRecord)-[:HAS]->(audit:PageAuditRecord) WHERE id(dar)=$domain_audit_id RETURN audit")
	public Set<PageAuditRecord> getAllPageAudits(@Param("domain_audit_id") long domain_audit_id);

	/**
	 * Retrieves all audits for a page audit record.
	 *
	 * @param page_audit_id the ID of the page audit record
	 * @return set of all associated audits
	 */
	@Query("MATCH (page_audit:PageAuditRecord)-[]->(audit:Audit) OPTIONAL MATCH auditsAndMessages=(audit)-->(:UXIssueMessage) WHERE id(page_audit)=$page_audit_id RETURN auditsAndMessages")
	public Set<Audit> getAllAuditsForPageAuditRecord(@Param("page_audit_id") long page_audit_id);

	/**
	 * Retrieves the most recent page audit record for a given URL.
	 *
	 * @param url the URL to find the audit record for
	 * @return Optional containing the most recent page audit record
	 */
	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState{url:$url}) RETURN page_audit ORDER BY page_audit.created_at DESC LIMIT 1")
	public Optional<PageAuditRecord> getMostRecentPageAuditRecord(@Param("url") String url);

	/**
	 * Retrieves the page state associated with a page audit record.
	 *
	 * @param page_audit_key the key of the page audit record
	 * @return the associated page state
	 */
	@Query("MATCH (page_audit:PageAuditRecord{key:$page_audit_key})-[]->(page_state:PageState) RETURN page_state LIMIT 1")
	public PageState getPageStateForAuditRecord(@Param("page_audit_key") String page_audit_key);

	/**
	 * Retrieves the page state associated with a page audit record.
	 *
	 * @param page_audit_id the ID of the page audit record
	 * @return the associated page state
	 */
	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState) WHERE id(page_audit)=$page_audit_id RETURN page_state LIMIT 1")
	public PageState getPageStateForAuditRecord(@Param("page_audit_id") long page_audit_id);
	
	/**
	 * Retrieves all page states associated with a domain audit record.
	 *
	 * @param domain_audit_id the ID of the domain audit record
	 * @return set of all associated page states
	 */
	@Query("MATCH (domain_audit:DomainAuditRecord)-[]->(page_state:PageState) WHERE id(domain_audit)=$domain_audit_id RETURN page_state")
	public Set<PageState> getPageStatesForDomainAuditRecord(@Param("domain_audit_id") long domain_audit_id);
	
	/**
	 * Retrieves the most recent audits for a page.
	 *
	 * @param key the key of the page
	 * @return set of all associated audits
	 */
	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState{key:$page_key}) MATCH (page_audit)-[]->(audit:Audit) RETURN audit")
	public Set<Audit> getMostRecentAuditsForPage(@Param("page_key") String key);

	/**
	 * Retrieves all content audits for a domain audit record.
	 *
	 * @param id the ID of the domain audit record
	 * @return set of all associated content audits
	 */
	@Query("MATCH (ar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{category:'Content'}) WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllContentAuditsForDomainRecord(@Param("id") long id);

	/**
	 * Retrieves all information architecture audits for a domain audit record.
	 *
	 * @param id the ID of the domain audit record
	 * @return set of all associated information architecture audits
	 */
	@Query("MATCH (ar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{category:'Information Architecture'})  WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllInformationArchitectureAuditsForDomainRecord(@Param("id") long id);

	/**
	 * Retrieves all aesthetics audits for a domain audit record.
	 *
	 * @param id the ID of the domain audit record
	 * @return set of all associated aesthetics audits
	 */
	@Query("MATCH (ar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{category:'Aesthetics'}) WHERE id(ar) = $id RETURN audit")
	public Set<Audit> getAllAestheticsAuditsForDomainRecord(@Param("id") long id);

	/**
	 * Retrieves all content audits for an audit record.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @return set of all associated content audits
	 */
	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit{category:'Content'}) WHERE id(ar)=$audit_record_id RETURN audit")
	public Set<Audit> getAllContentAudits(@Param("audit_record_id") long audit_record_id);

	/**
	 * Retrieves all information architecture audits for an audit record.
	 *
	 * @param id the ID of the audit record
	 * @return set of all associated information architecture audits
	 */
	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit{category:'Information Architecture'})  WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllInformationArchitectureAudits(@Param("id") long id);

	/**
	 * Retrieves all aesthetics audits for an audit record.
	 *
	 * @param id the ID of the audit record
	 * @return set of all associated aesthetics audits
	 */
	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit{category:'Aesthetics'}) WHERE id(ar)=$id RETURN audit")
	public Set<Audit> getAllAestheticsAudits(@Param("id") long id);

	/**
	 * Retrieves all accessibility audits for a domain audit record.
	 *
	 * @param domain_audit_id the ID of the domain audit record
	 * @return set of all associated accessibility audits
	 */
	@Query("MATCH (dar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{is_accessibility:true}) WHERE id(dar)=$domain_audit_id RETURN audit")
	public Set<Audit> getAllAccessibilityAuditsForDomainRecord(@Param("domain_audit_id") long domain_audit_id);

	/**
	 * Retrieves all accessibility audits for a page audit record.
	 *
	 * @param page_audit_id the ID of the page audit record
	 * @return set of all accessibility audits
	 */
	@Query("MATCH (par:PageAuditRecord)-[]->(audit:Audit{is_accessibility:true}) WHERE id(par)=$page_audit_id RETURN audit")
	public Set<Audit> getAllAccessibilityAudits(@Param("page_audit_id") long page_audit_id);

	/**
	 * Retrieves all UX issue messages for a page audit record.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @return set of all associated UX issue messages
	 */
	@Query("MATCH (audit_record:PageAuditRecord)-[]-(audit:Audit)  MATCH (audit)-[:HAS]-(issue:UXIssueMessage) WHERE id(audit_record)=$audit_record_id RETURN issue")
	public Set<UXIssueMessage> getIssues(@Param("audit_record_id") long audit_record_id);

	/**
	 * Adds a page to an audit record.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @param page_state_id the ID of the page state to add
	 */
	@Query("MATCH (ar:AuditRecord) WITH ar MATCH (page:PageState) WHERE id(ar)=$audit_record_id AND id(page)=$page_state_id MERGE (ar)-[h:FOR]->(page) RETURN ar")
	public void addPageToAuditRecord(@Param("audit_record_id") long audit_record_id, @Param("page_state_id") long page_state_id);

	/**
	 * Counts issues of a specific severity for a page audit record.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @param severity the severity level to count
	 * @return the count of issues with the specified severity
	 */
	@Query("MATCH (audit_record:PageAuditRecord)-[]-(audit:Audit) MATCH (audit)-[:HAS]-(issue:UXIssueMessage{priority:$severity}) WHERE id(audit_record)=$audit_record_id RETURN count(issue) as count")
	public long getIssueCountBySeverity(@Param("audit_record_id") long audit_record_id, @Param("severity") String severity);

	/**
	 * Counts the number of page audit records in a domain audit record.
	 *
	 * @param domain_audit_id the ID of the domain audit record
	 * @return the count of page audit records
	 */
	@Query("MATCH (audit_record:DomainAuditRecord)-[]->(page_audit:PageAuditRecord) WHERE id(audit_record)=$audit_record_id RETURN count(page_audit) as count")
	public int getPageAuditRecordCount(@Param("audit_record_id") long domain_audit_id);

	/**
	 * Retrieves the domain audit record associated with a page audit record.
	 *
	 * @param audit_record_id the ID of the page audit record
	 * @return Optional containing the associated domain audit record
	 */
	@Query("MATCH (doman_audit:DomainAuditRecord)-[:HAS]->(page_audit:PageAuditRecord) WHERE id(page_audit)=$audit_record_id RETURN doman_audit LIMIT 1")
	public Optional<DomainAuditRecord> getDomainForPageAuditRecord(@Param("audit_record_id") long audit_record_id);

	/**
	 * Retrieves all labels for image elements in an audit record.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @return set of all associated labels
	 */
	@Query("MATCH (audit_record:AuditRecord) WITH audit_record WHERE id(audit_record)=$audit_record_id MATCH (audit_record)-[*]->(element:ImageElementState) MATCH (element)-[]->(label:Label) RETURN label")
	public Set<Label> getLabelsForImageElements(@Param("audit_record_id") long audit_record_id);

	/**
	 * Retrieves the design system detected for an audit record.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @return Optional containing the detected design system
	 */
	@Query("MATCH (audit_record:AuditRecord) WITH audit_record MATCH (audit_record)-[:DETECTED]->(design_system:DesignSystem) WHERE id(audit_record)=$audit_record_id RETURN design_system")
	public Optional<DesignSystem> getDesignSystem(@Param("audit_record_id") long audit_record_id);

	/**
	 * Adds a journey to a domain audit record.
	 *
	 * @param audit_record_id the ID of the domain audit record
	 * @param journey_id the ID of the journey to add
	 * @return the updated audit record
	 */
	@Query("MATCH (ar:DomainAuditRecord) WITH ar MATCH (journey:Journey) WHERE id(ar)=$audit_record_id AND id(journey)=$journey_id MERGE (ar)-[:HAS_PATH]->(journey) RETURN ar")
	public AuditRecord addJourney(@Param("audit_record_id") long audit_record_id, @Param("journey_id") long journey_id);

	/**
	 * Finds a page with a specific URL in a domain audit record.
	 *
	 * @param audit_record_id the ID of the domain audit record
	 * @param page_url the URL to find
	 * @return the page state if found, null otherwise
	 */
	@Query("MATCH (audit_record:DomainAuditRecord) WITH audit_record WHERE id(audit_record)=$audit_record_id MATCH (audit_record)-[:HAS]->(page:PageState) WHERE page.url=$page_url RETURN page")
	public PageState findPageWithUrl(@Param("audit_record_id") long audit_record_id, @Param("page_url") String page_url);

	/**
	 * Adds a domain map to a domain audit record.
	 *
	 * @param domainId the ID of the domain audit record
	 * @param domainMapId the ID of the domain map to add
	 */
	@Query("MATCH (ar:DomainAuditRecord) WITH ar MATCH (map:DomainMap) WHERE id(ar)=$audit_record_id AND id(map)=$map_id MERGE (ar)-[:CONTAINS]->(map) RETURN ar")
	public void addDomainMap(@Param("audit_record_id") long domainId, @Param("map_id") long domainMapId);

	/**
	 * Finds the most recent audit record for a domain by URL.
	 *
	 * @param url the URL of the domain
	 * @return Optional containing the most recent audit record, or empty if not found
	 */
	@Query("MATCH(d:Domain{url: $url}) WITH d MATCH (audit:DomainAuditRecord)-[:HAS]->(d) WITH audit RETURN audit ORDER BY audit.created_at DESC LIMIT 1")
	public Optional<AuditRecord> getMostRecentAuditRecordForDomain(@Param("url") String url);
	
	/**
	 * Finds the most recent audit record for a domain by ID.
	 *
	 * @param id the ID of the domain
	 * @return Optional containing the most recent audit record, or empty if not found
	 */
	@Query("MATCH(d:Domain) WITH d MATCH (audit:DomainAuditRecord)-[:HAS]->(d) WITH audit WHERE id(d)=$id RETURN audit ORDER BY audit.created_at DESC LIMIT 1")
	public Optional<AuditRecord> getMostRecentAuditRecordForDomain(@Param("id") long id);

	/**
	 * Retrieves all audit records for a domain by key.
	 *
	 * @param domain_key the key of the domain
	 * @return set of all associated audit records
	 */
	@Query("MATCH (d:Domain{key:$domain_key})-[]->(audit:AuditRecord) RETURN audit")
	public Set<AuditRecord> getAuditRecords(@Param("domain_key") String domain_key);

	/**
	 * Retrieves an audit record by domain key and audit record key.
	 *
	 * @param domain_key the key of the domain
	 * @param audit_record_key the key of the audit record
	 * @return the audit record if found, null otherwise
	 */
	@Query("MATCH (d:Domain{key:$domain_key})<-[]-(audit:AuditRecord{key:$audit_record_key}) RETURN audit")
	public AuditRecord getAuditRecords(@Param("domain_key") String domain_key, @Param("audit_record_key") String audit_record_key);
	
	/**
	 * Retrieves the audit record history for a domain by ID.
	 *
	 * @param domain_id the ID of the domain
	 * @return list of domain audit records with associated page audit records and audits
	 */
	@Query("MATCH(d:Domain) WITH d WHERE id(d)=$domain_id MATCH (ar:DomainAuditRecord)-[]->(d) MATCH y=(ar)-[:HAS]->(page_audit:PageAuditRecord) MATCH z=(page_audit)-[:HAS]->(audit:Audit) RETURN y,z")
	public List<DomainAuditRecord> getAuditRecordHistory(@Param("domain_id") long domain_id);

	/**
	 * Retrieves the audit record for a page by ID.
	 *
	 * @param id the ID of the page
	 * @return the audit record if found, null otherwise
	 */
	@Query("MATCH (a:PageAuditRecord)-[:FOR]->(ps:PageState) WHERE id(ps)=$id RETURN a ORDER BY a.created_at DESC LIMIT 1")
	public PageAuditRecord getAuditRecord(@Param("id") long id);

	/**
	 * Updates the progress of an audit record.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @param content_progress the new content progress
	 * @param info_architecture_progress the new information architecture progress
	 * @param aesthetic_progress the new aesthetic progress
	 * @param data_extraction_progress the new data extraction progress
	 */
	@Query("MATCH (a:AuditRecord) WHERE id(a)=$audit_record_id SET a.contentProgress=$content_progress, a.infoArchitectureProgress=$ia_progress, a.aestheticProgress=$aesthetic_progress, a.dataExtractionProgress=$data_progress RETURN a")
    public void updateProgress(@Param("audit_record_id") long audit_record_id, 
								@Param("content_progress") double content_progress, 
								@Param("ia_progress") double info_architecture_progress,
								@Param("aesthentic_progress") double aesthetic_progress, 
								@Param("data_progress") double data_extraction_progress);

	/**
	 * Updates the scores of an audit record.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @param content_score the new content score
	 * @param info_architecture_score the new information architecture score (IA)
	 * @param aesthetic_score the new aesthetic score
	 */
	@Query("MATCH (a:AuditRecord) WHERE id(a)=$audit_record_id SET a.contentScore=$content_score, a.infoArchitectureScore=$ia_score, a.aestheticScore=$aesthetic_score, a.dataExtractionProgress=$data_progress RETURN a")
	public void updateScores(@Param("audit_record_id") long audit_record_id,
							@Param("content_score") double content_score,
							@Param("info_architecture_score") double info_architecture_score,
							@Param("aesthetic_score") double aesthetic_score);

	/**
	 * Retrieves the number of journeys with a specific status for a domain audit record.
	 *
	 * @param audit_record_id the ID of the domain audit record
	 * @param status the status of the journey
	 * @return the number of journeys with the specified status
	 */
	@Query("MATCH (domain_audit:DomainAuditRecord)-[:CONTAINS]->(map:DomainMap) WHERE id(domain_audit)=$audit_record_id MATCH(map)-[:CONTAINS]->(journey:Journey{status:$status}) RETURN COUNT(journey)")
	public int getNumberOfJourneysWithStatus(@Param("audit_record_id") long audit_record_id, @Param("status") String status);
	
	/**
	 * Retrieves the number of journeys for a domain audit record.
	 *
	 * @param audit_record_id the ID of the domain audit record
	 * @return the number of journeys for the domain audit record
	 */
	@Query("MATCH (domain_audit:DomainAuditRecord)-[:CONTAINS]->(map:DomainMap) WHERE id(domain_audit)=$audit_record_id MATCH(map)-[:CONTAINS]->(journey:Journey) RETURN COUNT(journey)")
	public int getNumberOfJourneys(@Param("audit_record_id") long audit_record_id);

	/**
	 * Finds the audit record for a page by ID.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @param page_id the ID of the page
	 * @return the audit record if found, null otherwise
	 */
	@Query("MATCH (audit_record:DomainAuditRecord) WHERE id(audit_record)=$audit_record_id MATCH (audit_record)-[:FOR]->(page:PageState) WHERE id(page)=$page_id RETURN audit_record")
	public AuditRecord findPageWithId(@Param("audit_record_id") long audit_record_id, @Param("page_id") long page_id);

	/**
	 * Retrieves all audits for a domain audit record.
	 *
	 * @param audit_record_id the ID of the domain audit record
	 * @return the audits for the domain audit record
	 */
	@Query("MATCH (ar:AuditRecord)-[*2]->(audit:Audit) WHERE id(ar)=$audit_record_id RETURN audit")
	public Set<Audit> getAllAuditsForDomainAudit(@Param("audit_record_id") long audit_record_id);

	/**
	 * Retrieves the audit record for a page by ID.
	 *
	 * @param domainAuditRecordId the ID of the domain audit record
	 * @param pageId the ID of the page
	 * @return the audit record if found, null otherwise
	 */
	@Query("MATCH (audit_record:DomainAuditRecord)-[:HAS]->(page_audit:PageAuditRecord)-[:FOR]->(page:PageState) WHERE id(audit_record)=$domain_audit_id AND id(page)=$page_id  RETURN page_audit LIMIT 1")
	public AuditRecord wasPageAlreadyAudited(@Param("domain_audit_id")  long domainAuditRecordId, @Param("page_id") long pageId);

	/**
	 * Retrieves the audit record for a single page by ID.
	 *
	 * @param pageAuditRecordId the ID of the page audit record
	 * @param pageId the ID of the page
	 * @return the audit record if found, null otherwise
	 */
	@Query("MATCH (page_audit:PageAuditRecord)-[:FOR]->(page:PageState) WHERE id(page_audit)=$page_audit_id AND id(page)=$page_id  RETURN page_audit LIMIT 1")
	public AuditRecord wasSinglePageAlreadyAudited(@Param("page_audit_id")  long pageAuditRecordId, @Param("page_id") long pageId);

	/**
	 * Retrieves the number of journeys without a specific status for a domain audit record.
	 *
	 * @param audit_record_id the ID of the domain audit record
	 * @param status the status of the journey
	 * @return the number of journeys without the specified status
	 */
	@Query("MATCH (domain_audit:DomainAuditRecord)-[:CONTAINS]->(map:DomainMap) WHERE id(domain_audit)=$audit_record_id MATCH(map)-[:CONTAINS]->(journey:Journey) WHERE NOT journey.status=$status AND NOT journey.status='REVIEWING' RETURN COUNT(journey)")
	public int getNumberOfJourneysWithoutStatus(@Param("audit_record_id") long audit_record_id, @Param("status") String status);

	/**
	 * Retrieves the audit records for an account by ID.
	 *
	 * @param account_id the ID of the account
	 * @return the audit records for the account
	 */
	@Query("MATCH (acct:Account)-[:HAS]->(audit_record:AuditRecord) WHERE id(acct)=$account_id RETURN audit_record")
    public List<AuditRecord> findAuditRecordByAccountId(@Param("account_id") long account_id);
}
