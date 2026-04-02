package com.looksee.models.repository;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.Screenshot;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.PageAuditRecord;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Spring Data Neo4j to handle interactions with
 * {@link PageState} objects
 */
@Repository
@Retry(name = "neoforj")
public interface PageStateRepository extends Neo4jRepository<PageState, Long> {
	/**
	 * Finds a page state by its key and username
	 *
	 * @param user_id the ID of the user
	 * @param key the key of the page state
	 * @return the page state
	 */
	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$key}) RETURN p LIMIT 1")
	public PageState findByKeyAndUsername(@Param("user_id") String user_id, @Param("key") String key);

	/**
	 * Finds a page state by its key
	 *
	 * @param key the key of the page state
	 * @return the page state
	 */
	@Query("MATCH (p:PageState{key:$key}) RETURN p LIMIT 1")
	public PageState findByKey(@Param("key") String key);

	/**
	 * Finds page states by their screenshot checksum and page URL
	 *
	 * @param url the URL of the page
	 * @param checksum the checksum of the screenshot
	 * @return the page states
	 */
	@Query("MATCH (p:PageState{url:$url})-[h:HAS]->() WHERE $screenshot_checksum IN p.screenshot_checksums RETURN a")
	public List<PageState> findByScreenshotChecksumAndPageUrl(@Param("url") String url, @Param("screenshot_checksum") String checksum );
	
	/**
	 * Finds page states by their full page screenshot checksum
	 *
	 * @param checksum the checksum of the full page screenshot
	 * @return the page states
	 */
	@Query("MATCH (p:PageState{full_page_checksum:$screenshot_checksum}) MATCH a=(p)-[h:HAS_CHILD]->() RETURN a")
	public List<PageState> findByFullPageScreenshotChecksum(@Param("screenshot_checksum") String checksum );
	
	/**
	 * Finds screenshots for a page state by its key and username
	 *
	 * @param user_id the ID of the user
	 * @param page_key the key of the page state
	 * @return the screenshots
	 */
	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$page_key}) MATCH (p)-[h:HAS]->(s:Screenshot) RETURN s")
	public List<Screenshot> getScreenshots(@Param("user_id") String user_id, @Param("page_key") String page_key);

	/**
	 * Finds a page state by its animated image checksum and username
	 *
	 * @param user_id the ID of the user
	 * @param screenshot_checksum the checksum of the animated image
	 * @return the page state
	 */
	@Query("MATCH (:Account{username:$user_id})-[*]->(p:PageState{key:$page_key}) WHERE $screenshot_checksum IN p.animated_image_checksums RETURN p LIMIT 1")
	public PageState findByAnimationImageChecksum(@Param("user_id") String user_id, @Param("screenshot_checksum") String screenshot_checksum);

	/**
	 * Finds page states with a form by their account ID, domain URL, and form key
	 *
	 * @param account_id the ID of the account
	 * @param url the URL of the domain
	 * @param form_key the key of the form
	 * @return the page states
	 */
	@Query("MATCH (a:Account)-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState) MATCH (p)-[:HAS]->(f:Form{key:$form_key}) WHERE id(account)=$account_id RETURN p")
	public List<PageState> findPageStatesWithForm(@Param("account_id") long account_id, @Param("url") String url, @Param("form_key") String form_key);

	/**
	 * Finds page states by their source checksum and domain URL
	 *
	 * @param url the URL of the domain
	 * @param src_checksum the checksum of the source
	 * @return the page states
	 */
	@Query("MATCH (d:Domain{url:$url})-[:HAS]->(ps:PageState{src_checksum:$src_checksum}) MATCH a=(ps)-[h:HAS]->() RETURN a")
	public List<PageState> findBySourceChecksumForDomain(@Param("url") String url, @Param("src_checksum") String src_checksum);
	
	/**
	 * Finds audits for a page state by its key
	 *
	 * @param page_state_key the key of the page state
	 * @return the audits
	 */
	@Query("MATCH (ps:PageState{key:$page_state_key})<-[]-(a:Audit) RETURN a")
	public List<Audit> getAudits(@Param("page_state_key") String page_state_key);

	/**
	 * Finds an audit by its subcategory and page state key
	 *
	 * @param subcategory the subcategory of the audit
	 * @param page_state_key the key of the page state
	 * @return the audit
	 */
	@Query("MATCH (p:PageState{key:$page_state_key})-[*]->(a:Audit{subcategory:$subcategory}) RETURN a")
	public Audit findAuditBySubCategory(@Param("subcategory") String subcategory, @Param("page_state_key") String page_state_key);
	
	/**
	 * Finds the parent page for a page state by its key
	 *
	 * @param page_state_key the key of the page state
	 * @return the parent page
	 */
	@Query("MATCH (p:PageState{key:$page_state_key}) return p LIMIT 1")
	public PageState getParentPage(@Param("page_state_key") String page_state_key);

	/**
	 * Finds the most recent page state by its URL
	 *
	 * @param url the URL of the page
	 * @return the page state
	 */
	@Query("MATCH (p:PageState{url:$url}) RETURN p ORDER BY p.created_at DESC LIMIT 1")
	public PageState findByUrl(@Param("url") String url);

	/**
	 * Adds an element to a page state
	 *
	 * @param page_id the ID of the page state
	 * @param element_id the ID of the element
	 * @return the page state
	 */
	@Query("MATCH (p:PageState) WITH p MATCH (element:ElementState) WHERE id(p)=$page_id AND id(element)=$element_id MERGE (p)-[:HAS]->(element) RETURN p LIMIT 1")
	public PageState addElement(@Param("page_id") long page_id, @Param("element_id") long element_id);

	/**
	 * Updates the composite image URL for a page state
	 *
	 * @param page_id the ID of the page state
	 * @param composite_img_url the URL of the composite image
	 */
	@Query("MATCH (ps:PageState) WHERE id(ps)=$page_id SET ps.fullPageScreenshotUrlComposite = $composite_img_url RETURN ps")
	public void updateCompositeImageUrl(@Param("page_id") long page_id, @Param("composite_img_url") String composite_img_url);

	/**
	 * Adds all elements to a page state
	 *
	 * @param page_state_id the ID of the page state
	 * @param element_id_list the IDs of the elements
	 */
	@Query("MATCH (p:PageState) WITH p MATCH (element:ElementState) WHERE id(p)=$page_state_id AND id(element) IN $element_id_list MERGE (p)-[:HAS]->(element) RETURN p LIMIT 1")
	public void addAllElements(@Param("page_state_id") long page_state_id, @Param("element_id_list") List<Long> element_id_list);
	
	/**
	 * Finds a page state by its audit record ID and key
	 *
	 * @param audit_record_id the ID of the audit record
	 * @param page_key the key of the page state
	 * @return the page state
	 */
	@Query("MATCH (audit_record:AuditRecord)-[:FOR]->(page:PageState) WHERE id(audit_record)=$audit_record_id AND page.key=$page_key RETURN page LIMIT 1")
	public PageState findPageWithKey(@Param("audit_record_id") long audit_record_id, @Param("page_key") String page_key);

	/**
	 * Finds a page state by its domain audit ID and URL
	 *
	 * @param domainAuditRecordId the ID of the domain audit
	 * @param url the URL of the page
	 * @return the page state
	 */
	@Query("MATCH (domain_audit:DomainAuditRecord) with domain_audit WHERE id(domain_audit)=$domain_audit_id MATCH (domain_audit)-[:FOR]->(page_state:PageState) WHERE page_state.url=$url MATCH page=(page_state)-[]->(:ElementState) RETURN page LIMIT 1")
	public PageState findByDomainAudit(@Param("domain_audit_id") long domainAuditRecordId, @Param("url") String url);

	/**
	 * Finds the end page for a step
	 *
	 * @param step_id the ID of the step
	 * @return the page state
	 */
	@Query("MATCH (s:Step)-[:ENDS_WITH]->(page:PageState) WHERE id(s)=$step_id RETURN page")
	public PageState getEndPageForStep(@Param("step_id") long step_id);

	/**
	 * Finds page states by screenshot checksums for a user and domain
	 *
	 * @param user_id the ID of the user
	 * @param url the URL of the domain
	 * @param checksum the checksum of the screenshot
	 * @return the page states
	 */
	@Deprecated
	@Query("MATCH (:Account{username:$user_id})-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState) MATCH a=(p)-[h:HAS]->() WHERE $screenshot_checksum IN p.screenshot_checksums RETURN a")
	public List<PageState> findByScreenshotChecksumsContainsForUserAndDomain(@Param("user_id") String user_id, @Param("url") String url, @Param("screenshot_checksum") String checksum );
	
	/**
	 * Retrieves page states for a domain audit record.
	 *
	 * @param domain_audit_id the ID of the domain audit record
	 * @return set of page states associated with the domain audit record
	 */
	@Query("MATCH (domain_audit:DomainAuditRecord)-[]->(page_state:PageState) WHERE id(domain_audit)=$domain_audit_id RETURN page_state")
	public Set<PageState> getPageStatesForDomainAuditRecord(@Param("domain_audit_id") long domain_audit_id);
	
	/**
	 * Finds a page with a specific URL within a domain audit record.
	 *
	 * @param audit_record_id the ID of the audit record
	 * @param page_url the URL of the page to find
	 * @return the page state if found
	 */
	@Query("MATCH (audit_record:DomainAuditRecord) WITH audit_record WHERE id(audit_record)=$audit_record_id MATCH (audit_record)-[:FOR]->(page:PageState) WHERE page.url=$page_url RETURN page")
	public PageState findPageWithUrl(@Param("audit_record_id") long audit_record_id, @Param("page_url") String page_url);

	/**
	 * Retrieves all page states for a domain.
	 *
	 * @param domain_id the ID of the domain
	 * @return set of all page states associated with the domain
	 */
	@Query("MATCH (d:Domain)-[]->(p:PageState) WHERE id(d)=$domain_id RETURN p")
	public Set<PageState> getPageStates(@Param("domain_id") long domain_id);

	/**
	 * Retrieves a specific page from a domain.
	 *
	 * @param domain_id the ID of the domain
	 * @param page_id the ID of the page
	 * @return Optional containing the page state if found
	 */
	@Query("MATCH (d:Domain)-[]->(p:PageState) WHERE id(d)=$domain_id AND id(p)=$page_id RETURN p")
	public Optional<PageState> getPage(@Param("domain_id") long domain_id, @Param("page_id") long page_id);

	/**
	 * Adds a page to a domain if it doesn't already exist.
	 *
	 * @param domain_id the ID of the domain
	 * @param page_id the ID of the page to add
	 * @return the page state that was added to the domain
	 */
	@Query("MATCH (d:Domain) WITH d MATCH (p:PageState) WHERE id(d)=$domain_id AND id(p)=$page_id MERGE (d)-[:HAS]->(p) RETURN p")
	public PageState addPage(@Param("domain_id") long domain_id, @Param("page_id") long page_id);
	
	/**
	 * Retrieves all pages for a specific host.
	 *
	 * @param host the host to get pages for
	 * @return set of all page states associated with the host
	 */
	@Query("MATCH (d:Domain{host:$host})-[:HAS]->(p:PageState) RETURN p")
	public Set<PageState> getPages(@Param("host") String host);

	/**
	 * Retrieves a page state for a specific user, domain, and form key.
	 *
	 * @param user_id the ID of the user
	 * @param url the URL of the domain
	 * @param key the key of the form
	 * @return the page state if found
	 */
	@Query("MATCH (:Account{user_id:$user_id})-[]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(ps:PageState) MATCH (ps)-[:HAS]->(:Form{key:$key}) RETURN ps")
	public PageState getPageState(@Param("user_id") String user_id, @Param("url") String url, @Param("key") String key);

	/**
	 * Adds a start page to a step.
	 *
	 * @param step_id the ID of the step
	 * @param page_state_id the ID of the page state to add as start page
	 * @return the page state that was added as start page
	 */
	@Query("MATCH (s:Step) MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:STARTS_WITH]->(p) RETURN p")
	public PageState addStartPageToStep(@Param("step_id") long step_id, @Param("page_state_id") long page_state_id);

	/**
	 * Retrieves the start page for a login step.
	 *
	 * @param step_id the ID of the login step
	 * @return the start page state of the login step
	 */
	@Query("MATCH (s:LoginStep)-[:STARTS_WITH]->(page:PageState) WHERE id(s)=$step_id RETURN page")
	public PageState getStartPageToLoginStep(@Param("step_id") long step_id);
	
	/**
	 * Adds an end page to a step.
	 *
	 * @param step_id the ID of the step
	 * @param page_state_id the ID of the page state to add as end page
	 * @return the page state that was added as end page
	 */
	@Query("MATCH (s:Step) MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:ENDS_WITH]->(p) RETURN p")
	public PageState addEndPageToStep(@Param("step_id") long step_id, @Param("page_state_id") long page_state_id);

	/**
	 * Retrieves the end page for a step.
	 *
	 * @param step_id the ID of the step
	 * @return the end page state of the step
	 */
	@Query("MATCH (s:Step)-[:ENDS_WITH]->(page:PageState) WHERE id(s)=$step_id RETURN page")
	public PageState getEndPage(@Param("step_id") long step_id);

	/**
	 * Retrieves the end page for a simple step by key.
	 *
	 * @param step_key the key of the simple step
	 * @return the end page state of the simple step
	 */
	@Query("MATCH (:SimpleStep{key:$step_key})-[:ENDS_WITH]->(p:PageState) RETURN p")
	public PageState getEndPageForSimpleStep(@Param("step_key") String step_key);
	
	/**
	 * Retrieves the start page for a simple step by key.
	 *
	 * @param step_key the key of the simple step
	 * @return the start page state of the simple step
	 */
	@Query("MATCH (:SimpleStep{key:$step_key})-[:STARTS_WITH]->(p:PageState) RETURN p")
	public PageState getStartPageForSimpleStep(@Param("step_key") String step_key);
	
	/**
	 * Retrieves the result page state for a test.
	 *
	 * @param test_key the key of the test
	 * @param url the URL of the domain
	 * @param user_id the ID of the user
	 * @return the result page state
	 */
	@Query("MATCH (a:Account{user_id:$user_id})-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test{key:$test_key}) MATCH (t)-[:HAS_RESULT]->(p:PageState) RETURN p")
	public PageState getResult(@Param("test_key") String test_key, @Param("url") String url, @Param("user_id") String user_id);
	
	/**
	 * Finds a page state by domain audit record and page state ID.
	 *
	 * @param domainAuditRecordId the ID of the domain audit record
	 * @param page_state_id the ID of the page state
	 * @return the page state if found
	 */
	@Query("MATCH (domain_audit:DomainAuditRecord) with domain_audit WHERE id(domain_audit)=$domain_audit_id MATCH (domain_audit)-[]->(page_audit:PageAuditRecord) MATCH (page_audit)-[]->(page_state:PageState) WHERE id(page_state)=$page_state_id RETURN page_state")
	public PageState findByDomainAudit(@Param("domain_audit_id") long domainAuditRecordId, @Param("page_state_id") long page_state_id);
	
	/**
	 * Retrieves the page state for a page audit record.
	 *
	 * @param page_audit_id the ID of the page audit record
	 * @return the page state associated with the page audit record
	 */
	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState) WHERE id(page_audit)=$page_audit_id RETURN page_state LIMIT 1")
	public PageState getPageStateForAuditRecord(@Param("page_audit_id") long page_audit_id);

	/**
	 * Retrieves the link element states for a page state.
	 *
	 * @param page_state_id the ID of the page state
	 * @return the link element states
	 */
	@Query("MATCH (p:PageState)-[:HAS]->(e:ElementState{name:'a'}) WHERE id(p)=$page_state_id RETURN DISTINCT e")
	public List<ElementState> getLinkElementStates(@Param("page_state_id") long page_state_id);

	/**
	 * Retrieves the visible leaf elements for a page state.
	 *
	 * @param page_state_key the key of the page state
	 * @return the visible leaf elements
	 */
	@Query("MATCH (p:PageState{key:$page_state_key})-[:HAS]->(e:ElementState{classification:'LEAF'}) where e.visible=true RETURN e")
	public List<ElementState> getVisibleLeafElements(@Param("page_state_key") String page_state_key);

	/**
	 * Retrieves the element state for a page state.
	 *
	 * @param page_id the ID of the page state
	 * @param element_id the ID of the element state
	 * @return the element state
	 */
	@Query("MATCH (p:PageState)-[:HAS]->(element:ElementState) WHERE id(p)=$page_id AND id(element)=$element_id RETURN element ORDER BY p.created_at DESC LIMIT 1")
	public Optional<ElementState> getElementState(@Param("page_id") long page_id, @Param("element_id") long element_id);

	/**
	 * Retrieves the page audit record for a page state.
	 *
	 * @param id the ID of the page state
	 * @return the page audit record
	 */
	@Query("MATCH (a:PageAuditRecord)-[:FOR]->(ps:PageState) WHERE id(ps)=$id RETURN a ORDER BY a.created_at DESC LIMIT 1")
	public PageAuditRecord getAuditRecord(@Param("id") long id);

	/**
	 * Updates the element extraction completion status for a page state.
	 *
	 * @param page_id the ID of the page state
	 * @param is_complete whether element extraction is complete
	 * @return the updated page state
	 */
	@Query("MATCH (page_state:PageState) WHERE id(page_state)=$page_id SET page_state.elementExtractionComplete=$is_complete RETURN page_state LIMIT 1")
	public PageState updateElementExtractionCompleteStatus(@Param("page_id") long page_id, @Param("is_complete") boolean is_complete);

	/**
	 * Finds a page state by domain map ID and page key.
	 *
	 * @param domain_map_id the ID of the domain map
	 * @param page_key the key of the page state
	 * @return the page state if found
	 */
	@Query("MATCH (map:DomainMap)-[:HAS]->(page_state:PageState) WHERE id(map)=$domain_map_id AND page_state.key=$page_key RETURN page_state")
    public PageState findByDomainMap(@Param("domain_map_id") long domain_map_id, @Param("page_key") String page_key);

	/**
	 * Updates the interactive element extraction completion status for a page state.
	 *
	 * @param page_id the ID of the page state
	 * @param is_complete whether interactive element extraction is complete
	 * @return the updated page state
	 */
	@Query("MATCH (page_state:PageState) WHERE id(page_state)=$page_id SET page_state.interactiveElementExtractionComplete=$is_complete RETURN page_state LIMIT 1")
    public PageState updateInteractiveElementExtractionCompleteStatus(@Param("page_id") long page_id, @Param("is_complete") boolean is_complete);

    /**
	 * Finds a page state by page audit record ID.
	 * 
	 * @param pageAuditId the ID of the page audit record
	 * @return the page state if found
	 */
	@Query("MATCH (page_audit:PageAuditRecord)-[]->(page_state:PageState) WHERE id(page_audit)=$id RETURN page_state")
    public PageState findByAuditRecordId(@Param("id") long pageAuditId);

	/**
	 * Retrieves the element states for a page state.
	 *
	 * @param page_key the key of the page state
	 * @return the element states
	 */
	@Query("MATCH (p:PageState{key:$page_key})-[:HAS]->(e:ElementState) RETURN DISTINCT e")
	public List<ElementState> getElementStates(@Param("page_key") String page_key);

	/**
	 * Retrieves the element states for a page state.
	 *
	 * @param page_state_id the ID of the page state
	 * @return the element states
	 */
	@Query("MATCH (p:PageState)-[:HAS]->(e:ElementState) WHERE id(p)=$page_state_id RETURN DISTINCT e")
	public List<ElementState> getElementStates(@Param("page_state_id") long page_state_id);

	/**
	 * Retrieves the page state for a landing step.
	 *
	 * @param pageId the ID of the page state
	 * @return the page state if found
	 */
	@Query("MATCH (step:LandingStep)-[:STARTS_WITH]->(ps:PageState) WHERE id(ps)=$page_id RETURN ps LIMIT 1")
	public PageState isPageLandable(@Param("page_id") long pageId);

	/**
	 * Retrieves the page state for a page audit record.
	 *
	 * @param page_audit_key the key of the page audit record
	 * @return the page state if found
	 */
	@Query("MATCH (page_audit:PageAuditRecord{key:$page_audit_key})-[]->(page_state:PageState) RETURN page_state LIMIT 1")
	@Deprecated
	public PageState getPageStateForAuditRecord(@Param("page_audit_key") String page_audit_key);

	/**
	 * Adds a start page to a step.
	 *
	 * @param step_id the ID of the step
	 * @param page_state_id the ID of the page state
	 * @return the page state if found
	 */
	@Query("MATCH (s:Step) WITH s MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:STARTS_WITH]->(p) RETURN p")
	public PageState addStartPage(@Param("step_id") long step_id, @Param("page_state_id") long page_state_id);

	/**
	 * Retrieves the start page for a login step.
	 *
	 * @param step_id the ID of the step
	 * @return the page state if found
	 */
	@Query("MATCH (s:LoginStep)-[:STARTS_WITH]->(page:PageState) WHERE id(s)=$step_id RETURN page")
	public PageState getStartPage(@Param("step_id") long step_id);

	/**
	 * Adds an end page to a step.
	 *
	 * @param step_id the ID of the step
	 * @param page_state_id the ID of the page state
	 * @return the page state if found
	 */
	@Query("MATCH (s:Step) WITH s MATCH (p:PageState) WHERE id(s)=$step_id AND id(p)=$page_state_id MERGE (s)-[:ENDS_WITH]->(p) RETURN p")
	public PageState addEndPage(@Param("step_id") long step_id, @Param("page_state_id") long page_state_id);

	/**
	 * Retrieves the end page for a simple step.
	 *
	 * @param step_key the key of the step
	 * @return the page state if found
	 */
	@Query("MATCH (:SimpleStep{key:$step_key})-[:STARTS_WITH]->(p:PageState) RETURN p")
	public PageState getEndPage(@Param("step_key") String step_key);

	/**
	 * Retrieves the start page for a simple step.
	 *
	 * @param step_key the key of the step
	 * @return the page state if found
	 */
	@Query("MATCH (:SimpleStep{key:$step_key})-[:ENDS_WITH]->(p:PageState) RETURN p")
	public PageState getStartPage(@Param("step_key") String step_key);
}
