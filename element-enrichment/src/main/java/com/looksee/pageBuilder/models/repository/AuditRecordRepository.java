package com.looksee.pageBuilder.models.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.pageBuilder.models.Audit;
import com.looksee.pageBuilder.models.AuditRecord;
import com.looksee.pageBuilder.models.DesignSystem;
import com.looksee.pageBuilder.models.DomainAuditRecord;
import com.looksee.pageBuilder.models.Label;
import com.looksee.pageBuilder.models.PageAuditRecord;
import com.looksee.pageBuilder.models.PageState;
import com.looksee.pageBuilder.models.UXIssueMessage;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with {@link Audit} objects
 */
@Repository
@Retry(name = "neoforj")
public interface AuditRecordRepository extends Neo4jRepository<AuditRecord, Long> {
	public AuditRecord findByKey(@Param("key") String key);
	
	@Query("MATCH (:AuditRecord{key:$audit_record_key})-[:HAS]->(a:Audit{key:$audit_key}) RETURN a")
	public Optional<Audit> getAuditForAuditRecord(@Param("audit_record_key") String audit_record_key, @Param("audit_key") String audit_key);

	@Query("MATCH (ar:PageAuditRecord)-[:HAS]->(a:Audit) WHERE id(ar)=$audit_record_id AND id(a)=$audit_id RETURN a")
	public Optional<Audit> getAuditForAuditRecord(@Param("audit_record_id") long audit_record_id, @Param("audit_id") long audit_id);

	@Query("MATCH (ar:AuditRecord{key:$audit_record_key}) WITH ar MATCH (a:Audit{key:$audit_key}) MERGE (ar)-[h:HAS]->(a) RETURN ar")
	public void addAudit(@Param("audit_record_key") String audit_record_key, @Param("audit_key") String audit_key);
	
	@Query("MATCH (ar:PageAuditRecord) WITH ar MATCH (a:Audit) WHERE id(ar)=$audit_record_id AND id(a)=$audit_id MERGE (ar)-[h:HAS]->(a) RETURN ar")
	public void addAudit(@Param("audit_record_id") long audit_record_id, @Param("audit_id") long audit_id);
	
	@Query("MATCH (dar:DomainAuditRecord) WITH dar MATCH (par:PageAuditRecord{key:$page_audit_key}) WHERE id(dar)=$domain_audit_record_id MERGE (dar)-[h:HAS]->(par) RETURN dar")
	public void addPageAuditRecord(@Param("domain_audit_record_id") long domain_audit_record_id, @Param("page_audit_key") String page_audit_key);

	@Query("MATCH (dar:DomainAuditRecord) WITH dar MATCH (par:PageAuditRecord) WHERE id(dar)=$domain_audit_record_id AND id(par)=$page_audit_id MERGE (dar)-[h:HAS]->(par) RETURN dar")
	public void addPageAuditRecord(@Param("domain_audit_record_id") long domain_audit_record_id, @Param("page_audit_id") long page_audit_record_id);
	
	@Query("MATCH (ar:AuditRecord)-[]->(audit:Audit) WHERE id(ar)=$audit_record_id RETURN audit")
	public Set<Audit> getAllAudits(@Param("audit_record_id") long audit_record_id);
	
	@Query("MATCH (d:Domain)-[]-(ar:DomainAuditRecord) WHERE id(d)=$domain_id RETURN ar ORDER BY ar.created_at DESC LIMIT 1")
	public Optional<DomainAuditRecord> findMostRecentDomainAuditRecord(@Param("domain_id") long domain_id);

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
	
	@Query("MATCH (domain_audit:DomainAuditRecord)-[:HAS]->(audit:PageAuditRecord) WHERE id(domain_audit)=$domain_audit_id RETURN audit")
	public Set<PageAuditRecord> getAllPageAudits(@Param("domain_audit_id") long domain_audit_id);

	@Query("MATCH (page_audit:PageAuditRecord)-[]->(audit:Audit) OPTIONAL MATCH auditsAndMessages=(audit)-->(:UXIssueMessage) WHERE id(page_audit)=$page_audit_id RETURN auditsAndMessages")
	public Set<Audit> getAllAuditsForPageAuditRecord(@Param("page_audit_id") long page_audit_id);

	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState{url:$url}) RETURN page_audit ORDER BY page_audit.created_at DESC LIMIT 1")
	public Optional<PageAuditRecord> getMostRecentPageAuditRecord(@Param("url") String url);

	@Query("MATCH (page_audit:PageAuditRecord{key:$page_audit_key})-[]->(page_state:PageState) RETURN page_state LIMIT 1")
	@Deprecated
	public PageState getPageStateForAuditRecord(@Param("page_audit_key") String page_audit_key);

	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState) WHERE id(page_audit)=$page_audit_id RETURN page_state LIMIT 1")
	public PageState getPageStateForAuditRecord(@Param("page_audit_id") long page_audit_id);
	
	@Query("MATCH (domain_audit:DomainAuditRecord)-[]->(page_state:PageState) WHERE id(domain_audit)=$domain_audit_id RETURN page_state")
	public Set<PageState> getPageStatesForDomainAuditRecord(@Param("domain_audit_id") long domain_audit_id);
	
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

	@Query("MATCH (dar:DomainAuditRecord)-[]->(par:PageAuditRecord) MATCH (par)-[]->(audit:Audit{is_accessibility:true}) WHERE id(dar)=$domain_audit_id RETURN audit")
	public Set<Audit> getAllAccessibilityAuditsForDomainRecord(@Param("domain_audit_id") long domain_audit_id);

	@Query("MATCH (par:PageAuditRecord)-[]->(audit:Audit{is_accessibility:true}) WHERE id(par)=$page_audit_id RETURN audit")
	public Set<Audit> getAllAccessibilityAudits(@Param("page_audit_id") long page_audit_id);

	@Query("MATCH (audit_record:PageAuditRecord)-[]-(audit:Audit)  MATCH (audit)-[:HAS]-(issue:UXIssueMessage) WHERE id(audit_record)=$audit_record_id RETURN issue")
	public Set<UXIssueMessage> getIssues(@Param("audit_record_id") long audit_record_id);

	@Query("MATCH (ar:AuditRecord) WITH ar MATCH (page:PageState) WHERE id(ar)=$audit_record_id AND id(page)=$page_state_id MERGE (ar)-[h:FOR]->(page) RETURN ar")
	public void addPageToAuditRecord(@Param("audit_record_id") long audit_record_id, @Param("page_state_id") long page_state_id);

	@Query("MATCH (audit_record:PageAuditRecord)-[]-(audit:Audit) MATCH (audit)-[:HAS]-(issue:UXIssueMessage{priority:$severity}) WHERE id(audit_record)=$audit_record_id RETURN count(issue) as count")
	public long getIssueCountBySeverity(@Param("audit_record_id") long id, @Param("severity") String severity);

	@Query("MATCH (audit_record:DomainAuditRecord)-[]->(page_audit:PageAuditRecord) WHERE id(audit_record)=$audit_record_id RETURN count(page_audit) as count")
	public int getPageAuditRecordCount(@Param("audit_record_id") long domain_audit_id);

	@Query("MATCH (doman_audit:DomainAuditRecord)-[:HAS]->(page_audit:PageAuditRecord) WHERE id(page_audit)=$audit_record_id RETURN doman_audit LIMIT 1")
	public Optional<DomainAuditRecord> getDomainForPageAuditRecord(@Param("audit_record_id") long audit_record_id);

	@Query("MATCH (audit_record:AuditRecord) WITH audit_record WHERE id(audit_record)=$audit_record_id MATCH (audit_record)-[*]->(element:ImageElementState) MATCH (element)-[]->(label:Label) RETURN label")
	public Set<Label> getLabelsForImageElements(@Param("audit_record_id") long id);

	@Query("MATCH (audit_record:AuditRecord) WITH audit_record MATCH (audit_record)-[:DETECTED]->(design_system:DesignSystem) WHERE id(audit_record)=$audit_record_id RETURN design_system")
	public Optional<DesignSystem> getDesignSystem(@Param("audit_record_id") long audit_record_id);

	@Query("MATCH (ar:DomainAuditRecord) WITH ar MATCH (journey:Journey) WHERE id(ar)=$audit_record_id AND id(journey)=$journey_id MERGE (ar)-[:HAS_PATH]->(journey) RETURN ar")
	public AuditRecord addJourney(@Param("audit_record_id") long audit_record_id, @Param("journey_id")  long journey_id);

	@Query("MATCH (audit_record:DomainAuditRecord) WITH audit_record WHERE id(audit_record)=$audit_record_id MATCH (audit_record)-[:HAS]->(page:PageState) WHERE page.url=$page_url RETURN page")
	public PageState findPageWithUrl(@Param("audit_record_id") long audit_record_id, @Param("page_url") String page_url);

	@Query("MATCH (ar:DomainAuditRecord) WITH ar MATCH (map:DomainMap) WHERE id(ar)=$audit_record_id AND id(map)=$map_id MERGE (ar)-[:CONTAINS]->(map) RETURN ar")
	public void addDomainMap(@Param("audit_record_id") long domainId, @Param("map_id") long domainMapId);
}
