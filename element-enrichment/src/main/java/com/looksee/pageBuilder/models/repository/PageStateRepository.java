package com.looksee.pageBuilder.models.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.pageBuilder.models.Audit;
import com.looksee.pageBuilder.models.PageState;
import com.looksee.pageBuilder.models.Screenshot;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * 
 */
@Repository
@Retry(name = "neoforj")
public interface PageStateRepository extends Neo4jRepository<PageState, Long> {
	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$key}) RETURN p LIMIT 1")
	public PageState findByKeyAndUsername(@Param("user_id") String user_id, @Param("key") String key);

	@Query("MATCH (p:PageState{key:$key}) RETURN p LIMIT 1")
	public PageState findByKey(@Param("key") String key);

	@Query("MATCH (p:PageState{url:$url})-[h:HAS]->() WHERE $screenshot_checksum IN p.screenshot_checksums RETURN a")
	public List<PageState> findByScreenshotChecksumAndPageUrl(@Param("url") String url, @Param("screenshot_checksum") String checksum );
	
	@Query("MATCH (p:PageState{full_page_checksum:$screenshot_checksum}) MATCH a=(p)-[h:HAS_CHILD]->() RETURN a")
	public List<PageState> findByFullPageScreenshotChecksum(@Param("screenshot_checksum") String checksum );
	
	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$page_key}) MATCH (p)-[h:HAS]->(s:Screenshot) RETURN s")
	public List<Screenshot> getScreenshots(@Param("user_id") String user_id, @Param("page_key") String page_key);

	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$page_key}) WHERE $screenshot_checksum IN p.animated_image_checksums RETURN p LIMIT 1")
	public PageState findByAnimationImageChecksum(@Param("user_id") String user_id, @Param("screenshot_checksum") String screenshot_checksum);

	@Query("MATCH (a:Account)-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState) MATCH (p)-[:HAS]->(f:Form{key:$form_key}) WHERE id(account)=$account_id RETURN p")
	public List<PageState> findPageStatesWithForm(@Param("account_id") long account_id, @Param("url") String url, @Param("form_key") String form_key);

	@Query("MATCH (d:Domain{url:$url})-[:HAS]->(ps:PageState{src_checksum:$src_checksum}) MATCH a=(ps)-[h:HAS]->() RETURN a")
	public List<PageState> findBySourceChecksumForDomain(@Param("url") String url, @Param("src_checksum") String src_checksum);
	
	@Query("MATCH (ps:PageState{key:$page_state_key})<-[]-(a:Audit) RETURN a")
	public List<Audit> getAudits(@Param("page_state_key") String page_state_key);

	@Query("MATCH (p:PageState{key:$page_state_key})-[*]->(a:Audit{subcategory:$subcategory}) RETURN a")
	public Audit findAuditBySubCategory(@Param("subcategory") String subcategory, @Param("page_state_key") String page_state_key);
	
	@Query("ps:PageState{key:$page_state_key}) return p LIMIT 1")
	public PageState getParentPage(@Param("page_state_key") String page_state_key);

	@Query("MATCH (p:PageState{url:$url}) RETURN p ORDER BY p.created_at DESC LIMIT 1")
	public PageState findByUrl(@Param("url") String url);

	@Query("MATCH (p:PageState) WITH p MATCH (element:ElementState) WHERE id(p)=$page_id AND id(element)=$element_id MERGE (p)-[:HAS]->(element) RETURN p LIMIT 1")
	public PageState addElement(@Param("page_id") long page_id, @Param("element_id") long element_id);

	@Query("MATCH (ps:PageState) WHERE id(ps)=$id SET ps.fullPageScreenshotUrlComposite = $composite_img_url RETURN ps")
	public void updateCompositeImageUrl(@Param("id") long id, @Param("composite_img_url") String composite_img_url);

	@Query("MATCH (p:PageState) WITH p MATCH (element:ElementState) WHERE id(p)=$page_state_id AND id(element) IN $element_id_list MERGE (p)-[:HAS]->(element) RETURN p LIMIT 1")
	public void addAllElements(@Param("page_state_id") long page_state_id, @Param("element_id_list") List<Long> element_id_list);
	
	@Query("MATCH (audit_record:AuditRecord)-[:FOR]->(page:PageState) WHERE id(audit_record)=$audit_record_id AND page.key=$page_key RETURN page LIMIT 1")
	public PageState findPageWithKey(@Param("audit_record_id") long audit_record_id, @Param("page_key") String key);

	@Query("MATCH (domain_audit:DomainAuditRecord) with domain_audit WHERE id(domain_audit)=$domain_audit_id MATCH (domain_audit)-[:FOR]->(page_state:PageState) WHERE page_state.url=$url MATCH page=(page_state)-[]->(:ElementState) RETURN page LIMIT 1")
	public PageState findByDomainAudit(@Param("domain_audit_id") long domainAuditRecordId, @Param("url") String url);

	@Query("MATCH (s:Step)-[:ENDS_WITH]->(page:PageState) WHERE id(s)=$step_id RETURN page")
	public PageState getEndPageForStep(@Param("step_id") long id);
}
