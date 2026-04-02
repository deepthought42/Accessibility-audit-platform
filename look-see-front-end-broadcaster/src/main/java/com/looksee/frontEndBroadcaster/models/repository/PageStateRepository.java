package com.looksee.frontEndBroadcaster.models.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.frontEndBroadcaster.models.PageState;

/**
 * 
 */
@Repository
public interface PageStateRepository extends Neo4jRepository<PageState, Long> {
	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$key}) RETURN p LIMIT 1")
	public PageState findByKeyAndUsername(@Param("user_id") String user_id, @Param("key") String key);

	@Query("MATCH (ps:PageState{key:$key}) return ps LIMIT 1")
	public PageState findByKey(@Param("key") String key);

	@Deprecated
	@Query("MATCH (:Account{username:$user_id})-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState) MATCH a=(p)-[h:HAS]->() WHERE $screenshot_checksum IN p.screenshot_checksums RETURN a")
	public List<PageState> findByScreenshotChecksumsContainsForUserAndDomain(@Param("user_id") String user_id, @Param("url") String url, @Param("screenshot_checksum") String checksum );
	
	@Query("MATCH (p:PageState{url:$url})-[h:HAS]->() WHERE $screenshot_checksum IN p.screenshot_checksums RETURN a")
	public List<PageState> findByScreenshotChecksumAndPageUrl(@Param("url") String url, @Param("screenshot_checksum") String checksum );
	
	@Query("MATCH (p:PageState{full_page_checksum:$screenshot_checksum}) MATCH a=(p)-[h:HAS_CHILD]->() RETURN a")
	public List<PageState> findByFullPageScreenshotChecksum(@Param("screenshot_checksum") String checksum );

	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$page_key}) WHERE $screenshot_checksum IN p.animated_image_checksums RETURN p LIMIT 1")
	public PageState findByAnimationImageChecksum(@Param("user_id") String user_id, @Param("screenshot_checksum") String screenshot_checksum);

	@Query("MATCH (a:Account)-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState) MATCH (p)-[:HAS]->(f:Form{key:$form_key}) WHERE id(account)=$account_id RETURN p")
	public List<PageState> findPageStatesWithForm(@Param("account_id") long account_id, @Param("url") String url, @Param("form_key") String form_key);

	@Query("MATCH (d:Domain{url:$url})-[:HAS]->(ps:PageState{src_checksum:$src_checksum}) MATCH a=(ps)-[h:HAS]->() RETURN a")
	public List<PageState> findBySourceChecksumForDomain(@Param("url") String url, @Param("src_checksum") String src_checksum);

	@Query("ps:PageState{key:$page_state_key}) return p LIMIT 1")
	public PageState getParentPage(@Param("page_state_key") String page_state_key);

	@Query("MATCH (p:PageState{url:$url}) RETURN p ORDER BY p.created_at DESC LIMIT 1")
	public PageState findByUrl(@Param("url") String url);

	@Query("MATCH (ps:PageState) WHERE id(ps)=$id SET ps.fullPageScreenshotUrlComposite = $composite_img_url RETURN ps")
	public void updateCompositeImageUrl(@Param("id") long id, @Param("composite_img_url") String composite_img_url);

	@Query("MATCH (p:PageState) WITH p WHERE id(p)=$page_state_id MATCH (element:ElementState) WHERE id(element) IN $element_id_list MERGE (p)-[:HAS]->(element) RETURN p LIMIT 1")
	public void addAllElements(@Param("page_state_id") long page_state_id, @Param("element_id_list") List<Long> element_id_list);
	
	@Query("MATCH (domain_audit:DomainAuditRecord)-[]->(page_state:PageState) WHERE id(domain_audit)=$domain_audit_id RETURN page_state")
	public Set<PageState> getPageStatesForDomainAuditRecord(@Param("domain_audit_id") long domain_audit_id);
	
	@Query("MATCH (audit_record:DomainAuditRecord) WITH audit_record WHERE id(audit_record)=$audit_record_id MATCH (audit_record)-[:FOR]->(page:PageState) WHERE page.url=$page_url RETURN page")
	public PageState findPageWithUrl(@Param("audit_record_id") long audit_record_id, @Param("page_url") String page_url);

	@Query("MATCH (d:Domain)-[]->(p:PageState) WHERE id(d)=$domain_id RETURN p")
	public Set<PageState> getPageStates(@Param("domain_id") long domain_id);

	@Query("MATCH (d:Domain)-[]->(p:PageState) WHERE id(d)=$domain_id AND id(p)=$page_id RETURN p")
	public Optional<PageState> getPage(@Param("domain_id") long domain_id, @Param("page_id") long page_id);

	@Query("MATCH (d:Domain) WITH d MATCH (p:PageState) WHERE id(d)=$domain_id AND id(p)=$page_id MERGE (d)-[:HAS]->(p) RETURN p")
	public PageState addPage(@Param("domain_id") long domain_id, @Param("page_id") long page_id);
	
	@Query("MATCH (d:Domain{host:$host})-[:HAS]->(p:PageState) RETURN p")
	public Set<PageState> getPages(@Param("host") String host);

	@Query("MATCH (:Account{user_id:$user_id})-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(ps:PageState) MATCH (ps)-[:HAS]->(:Form{key:$key}) RETURN ps")
	public PageState getPageState(@Param("user_id") String user_id, @Param("url") String url, @Param("key") String key);

	@Query("MATCH (s:Step) WITH s MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:STARTS_WITH]->(p) RETURN p")
	public PageState addStartPageToStep(@Param("step_id") long id, @Param("page_state_id") long page_state_id);	

	@Query("MATCH (s:LoginStep)-[:STARTS_WITH]->(page:PageState) WHERE id(s)=$step_id RETURN page")
	public PageState getStartPageToLoginStep(@Param("step_id") long id);
	
	@Query("MATCH (s:Step) WITH s MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:ENDS_WITH]->(p) RETURN p")
	public PageState addEndPageToStep(@Param("step_id") long id, @Param("page_state_id") long page_state_id);

	@Query("MATCH (s:Step)-[:ENDS_WITH]->(page:PageState) WHERE id(s)=$step_id RETURN page")
	public PageState getEndPageForStep(@Param("step_id") long id);

	@Query("MATCH (:SimpleStep{key:$step_key})-[:ENDS_WITH]->(p:PageState) RETURN p")
	public PageState getEndPageForSimpleStep(@Param("step_key") String key);
	
	@Query("MATCH (:SimpleStep{key:$step_key})-[:STARTS_WITH]->(p:PageState) RETURN p")
	public PageState getStartPageForSimpleStep(@Param("step_key") String key);
	
	@Query("MATCH (a:Account{user_id:$user_id})-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test{key:$test_key}) MATCH (t)-[:HAS_RESULT]->(p:PageState) RETURN p")
	public PageState getResult(@Param("test_key") String test_key, @Param("url") String url, @Param("user_id") String user_id);

	@Query("MATCH (domain_audit:DomainAuditRecord) with domain_audit  WHERE id(domain_audit)=$domain_audit_id MATCH (domain_audit)-[*2]->(page_state:PageState) WHERE id(page_state)=$page_state_id RETURN page_state")
	public PageState findByDomainAudit(@Param("domain_audit_id") long domainAuditRecordId, @Param("page_state_id") long page_state_id);

	@Query("MATCH (page:PageState) WHERE page.key=$page_key RETURN page LIMIT 1")
	public PageState findPageWithKey(@Param("page_key") String key);

	@Query("MATCH (domain_audit:DomainAuditRecord)-[:FOR]->(page_state:PageState) WHERE id(domain_audit)=$domain_audit_id AND page_state.url=$url MATCH page=(page_state)-[]->(:ElementState) RETURN page LIMIT 1")
	public PageState findByDomainAudit(@Param("domain_audit_id") long domainAuditRecordId, @Param("url") String url);

	@Query("MATCH (page_state:PageState) WHERE id(page_state)=$page_id SET page_state.elementExtractionComplete=$is_complete RETURN page_state LIMIT 1")
	public PageState updateElementExtractionCompleteStatus(@Param("page_id") long page_id, @Param("is_complete") boolean is_complete);

	@Query("MATCH (map:DomainMap) WHERE id(map)=$domain_map_id MATCH (map)-[:HAS]->(page_state:PageState) WHERE page_state.key=$page_key RETURN page_state")
    public PageState findByDomainMap(@Param("domain_map_id") long domain_map_id, @Param("page_key") String page_key);
}
