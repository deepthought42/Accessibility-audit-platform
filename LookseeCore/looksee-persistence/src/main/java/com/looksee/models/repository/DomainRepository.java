package com.looksee.models.repository;

import com.looksee.audits.performance.PerformanceInsight;
import com.looksee.models.Domain;
import com.looksee.models.Element;
import com.looksee.models.Form;
import com.looksee.models.PageLoadAnimation;
import com.looksee.models.PageState;
import com.looksee.models.Test;
import com.looksee.models.TestAction;
import com.looksee.models.TestRecord;
import com.looksee.models.TestUser;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.journeys.Redirect;
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
 * {@link Domain} objects
 */
@Repository
@Retry(name = "neoforj")
public interface DomainRepository extends Neo4jRepository<Domain, Long> {

	/**
	 * Finds a domain by its key
	 *
	 * @param key the key of the domain
	 * @param username the username of the user
	 * @return the domain
	 *
	 * precondition: key != null
	 * precondition: username != null
	 */
	@Query("MATCH (a:Account{username:$username})-[:HAS_DOMAIN]->(d:Domain{key:$key}) RETURN d LIMIT 1")
	public Domain findByKey(@Param("key") String key, @Param("username") String username);
	
	/**
	 * Finds a domain by its host for a user
	 *
	 * @param host the host of the domain
	 * @param username the username of the user
	 * @return the domain
	 *
	 * precondition: host != null
	 * precondition: username != null
	 */
	@Query("MATCH (a:Account{username:$username})-[:HAS_DOMAIN]->(d:Domain{host:$host}) RETURN d LIMIT 1")
	public Domain findByHostForUser(@Param("host") String host, @Param("username") String username);
	
	/**
	 * Finds a domain by its host
	 *
	 * @param host the host of the domain
	 * @return the domain
	 *
	 * precondition: host != null
	 */
	@Query("MATCH (d:Domain{host:$host}) RETURN d LIMIT 1")
	public Domain findByHost(@Param("host") String host);
	
	/**
	 * Finds all pages for a domain
	 *
	 * @param host the host of the domain
	 * @return the pages
	 *
	 * precondition: host != null
	 */
	@Query("MATCH (d:Domain{host:$host})-[:HAS]->(p:PageState) RETURN p")
	public Set<PageState> getPages(@Param("host") String host);

	/**
	 * Finds a domain by its URL
	 *
	 * @param url the URL of the domain
	 * @return the domain
	 *
	 * precondition: url != null
	 */
	@Query("MATCH (d:Domain{url:$url}) RETURN d LIMIT 1")
	public Domain findByUrl(@Param("url") String url);

	/**
	 * Finds all page states for a domain
	 *
	 * @param domain_id the ID of the domain
	 * @return the page states
	 */
	@Query("MATCH (d:Domain)-[]->(p:PageState) WHERE id(d)=$domain_id RETURN p")
	public Set<PageState> getPageStates(@Param("domain_id") long domain_id);

	/**
	 * Finds all element states for a domain
	 *
	 * @param url the URL of the domain
	 * @param username the username of the user
	 * @return the element states
	 */
	@Query("MATCH (:Account{username:$username})-[:HAS_DOMAIN]-(d:Domain{url:$url}) MATCH (d)-[]->(t:Test) MATCH (t)-[]->(e:ElementState) OPTIONAL MATCH b=(e)-->() RETURN b")
	public Set<Element> getElementStates(@Param("url") String url, @Param("username") String username);
	
	/**
	 * Finds all forms for a domain
	 *
	 * @param account_id the ID of the account
	 * @param url the URL of the domain
	 * @return the forms
	 */
	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form) MATCH a=(f)-[:DEFINED_BY]->() MATCH b=(f)-[:HAS]->(e) OPTIONAL MATCH c=(e)-->() WHERE id(account)=$account_id return a,b,c")
	public Set<Form> getForms(@Param("account_id") long account_id, @Param("url") String url);
	
	/**
	 * Finds the count of forms for a domain
	 *
	 * @param account_id the ID of the account
	 * @param url the URL of the domain
	 * @return the count of forms
	 */
	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form) WHERE id(account)=$account_id RETURN COUNT(f)")
	public int getFormCount(@Param("account_id") long account_id, @Param("url") String url);

	/**
	 * Finds the count of tests for a domain
	 *
	 * @param account_id the ID of the account
	 * @param host the host of the domain
	 * @return the count of tests
	 */
	@Query("MATCH(account:Account)-[]-(d:Domain{host:$host}) MATCH (d)-[:HAS_TEST]->(t:Test) WHERE id(account)=$account_id  RETURN COUNT(t)")
	public int getTestCount(@Param("account_id") long account_id, @Param("host") String host);

	/**
	 * Finds all test users for a domain
	 *
	 * @param domain_id the ID of the domain
	 * @return the test users
	 */
	@Query("MATCH (d:Domain)-[:HAS_TEST_USER]->(t:TestUser) WHERE id(d)=$domain_id RETURN t")
	public Set<TestUser> getTestUsers(@Param("domain_id") long domain_id);

	/**
	 * Deletes a test user from a domain
	 *
	 * @param domain_id the ID of the domain
	 * @param user_id the ID of the test user
	 * @return the count of test users deleted
	 */
	@Query("MATCH (d:Domain)-[r:HAS_TEST_USER]->(t:TestUser{username:$username}) WHERE id(d)=$domain_id AND id(t)=$user_id DELETE r,t return count(t)")
	public int deleteTestUser(@Param("domain_id") long domain_id, @Param("user_id") long user_id);

	/**
	 * Finds all page load animations for a domain
	 *
	 * @param account_id the ID of the account
	 * @param url the URL of the domain
	 * @return the page load animations
	 */
	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{host:$url}) MATCH (d)-[:HAS_TEST]->(:Test) MATCH (t)-[]->(p:PageLoadAnimation) WHERE id(account)=$account_id RETURN p")
	public Set<PageLoadAnimation> getAnimations(@Param("account_id") long account_id, @Param("url") String url);

	/**
	 * Adds a page to a domain
	 *
	 * @param domain_id the ID of the domain
	 * @param page_id the ID of the page
	 * @return the page
	 */
	@Query("MATCH (d:Domain) WITH d MATCH (p:PageState) WHERE id(d)=$domain_id AND id(p)=$page_id MERGE (d)-[:HAS]->(p) RETURN p")
	public PageState addPage(@Param("domain_id") long domain_id, @Param("page_id") long page_id);

	/**
	 * Finds the most recent audit record for a domain
	 *
	 * @param id the ID of the domain
	 * @return the most recent audit record
	 */
	@Query("MATCH(d:Domain) WITH d MATCH (audit:DomainAuditRecord)-[:HAS]->(d) WITH audit WHERE id(d)=$id RETURN audit ORDER BY audit.created_at DESC LIMIT 1")
	public Optional<DomainAuditRecord> getMostRecentAuditRecord(@Param("id") long id);

	/**
	 * Finds a domain by its page state key
	 *
	 * @param page_state_key the key of the page state
	 * @return the domain
	 */
	@Query("MATCH (d:Domain)-[*]->(:PageState{key:$page_state_key}) RETURN d LIMIT 1")
	public Domain findByPageState(@Param("page_state_key") String page_state_key);

	/**
	 * Adds an audit record to a domain
	 *
	 * @param domain_id the ID of the domain
	 * @param audit_record_key the key of the audit record
	 */
	@Query("MATCH (d:Domain) WITH d MATCH (audit:AuditRecord{key:$audit_record_key}) WHERE id(d) = $domain_id MERGE (d)-[:HAS]->(audit) RETURN audit")
	public void addAuditRecord(@Param("domain_id") long domain_id, @Param("audit_record_key") String audit_record_key);

	/**
	 * Finds all audit records for a domain
	 *
	 * @param domain_key the key of the domain
	 * @return the audit records
	 */
	@Query("MATCH (d:Domain{key:$domain_key})-[]->(audit:AuditRecord) RETURN audit")
	public Set<AuditRecord> getAuditRecords(@Param("domain_key") String domain_key);

	/**
	 * Finds an audit record for a domain
	 *
	 * @param domain_key the key of the domain
	 * @param audit_record_key the key of the audit record
	 * @return the audit record
	 *
	 * <b>Preconditions:</b>
	 * <ul>
	 *   <li>domain_key != null</li>
	 *   <li>audit_record_key != null</li>
	 * </ul>
	 */
	@Query("MATCH (d:Domain{key:$domain_key})<-[]-(audit:AuditRecord{key:$audit_record_key}) RETURN audit")
	public AuditRecord getAuditRecords(@Param("domain_key") String domain_key, @Param("audit_record_key") String audit_record_key);

	/**
	 * Finds a domain by its audit record ID
	 *
	 * @param audit_record_id the ID of the audit record
	 * @return the domain
	 */
	@Query("MATCH (d:Domain)-[:HAS]->(audit_record:AuditRecord) WHERE id(audit_record)=$audit_record_id RETURN d LIMIT 1")
	public Domain findByAuditRecord(@Param("audit_record_id") long audit_record_id);

	/**
	 * Finds all domains
	 *
	 * @return the domains
	 */
	@Query("MATCH (domain:Domain) RETURN domain")
	public Set<Domain> getDomains();
	
	/**
	 * Finds a page by its domain ID and page ID
	 *
	 * @param domain_id the ID of the domain
	 * @param page_id the ID of the page
	 * @return the page
	 */
	@Query("MATCH (d:Domain)-[]->(p:PageState) WHERE id(d)=$domain_id AND id(p)=$page_id RETURN p")
	public Optional<PageState> getPage(@Param("domain_id") long domain_id, @Param("page_id") long page_id);

	/**
	 * Updates the audience proficiency of a domain
	 *
	 * @param domain_id the ID of the domain
	 * @param audience_proficiency the new audience proficiency
	 * @return the design system
	 */
	@Query("MATCH (d:Domain)-[:USES]->(setting:DesignSystem) WHERE id(d)=$domain_id SET setting.audience_proficiency=$audience_proficiency RETURN setting")
	public DesignSystem updateExpertiseSetting(@Param("domain_id") long domain_id, @Param("audience_proficiency") String audience_proficiency);
	
	/**
	 * Updates the WCAG compliance level of a domain
	 *
	 * @param domain_id the ID of the domain
	 * @param wcag_level the new WCAG compliance level
	 * @return the design system
	 */
	@Query("MATCH (d:Domain)-[]->(setting:DesignSystem) WHERE id(d)=$domain_id SET setting.wcag_compliance_level=$wcag_level RETURN setting")
	public DesignSystem updateWcagSettings(@Param("domain_id") long domain_id, @Param("wcag_level") String wcag_level);

	/**
	 * Updates the allowed image characteristics of a domain
	 *
	 * @param domain_id the ID of the domain
	 * @param allowed_image_characteristics the new allowed image characteristics
	 * @return the design system
	 */
	@Query("MATCH (d:Domain)-[]->(setting:DesignSystem) WHERE id(d)=$domain_id SET setting.allowed_image_characteristics=$image_characteristics RETURN setting")
	public DesignSystem updateAllowedImageCharacteristics(@Param("domain_id") long domain_id, @Param("image_characteristics") List<String> allowed_image_characteristics);

	/**
	 * Finds the audit record history for a domain
	 *
	 * @param domain_id the ID of the domain
	 * @return the audit record history
	 */
	@Query("MATCH(d:Domain) WITH d WHERE id(d)=$domain_id MATCH (ar:DomainAuditRecord)-[]->(d) MATCH y=(ar)-[:HAS]->(page_audit:PageAuditRecord) MATCH z=(page_audit)-[:HAS]->(audit:Audit) RETURN y,z")
	public List<DomainAuditRecord> getAuditRecordHistory(@Param("domain_id") long domain_id);

	/**
	 * Finds the design system for a domain
	 *
	 * @param domain_id the ID of the domain
	 * @return the design system
	 */
	@Query("MATCH (domain:Domain)-[:USES]->(ds:DesignSystem) WHERE id(domain)=$domain_id RETURN ds LIMIT 1")
	public Optional<DesignSystem> getDesignSystem(@Param("domain_id") long domain_id);

	/**
	 * Adds a design system to a domain
	 *
	 * @param domain_id the ID of the domain
	 * @param design_system_id the ID of the design system
	 * @return the design system
	 */
	@Query("MATCH (d:Domain) WITH d MATCH (design:DesignSystem) WHERE id(d)=$domain_id AND id(design)=$design_system_id MERGE (d)-[:USES]->(design) RETURN design")
	public DesignSystem addDesignSystem(@Param("domain_id") long domain_id, @Param("design_system_id") long design_system_id);

	/**
	 * Finds all test users for a domain
	 *
	 * @param domain_id the ID of the domain
	 * @return the test users
	 */
	@Query("MATCH (d:Domain)-[]->(user:TestUser) WHERE id(d)=$domain_id RETURN user")
	public List<TestUser> findTestUsers(@Param("domain_id") long domain_id);

	/**
	 * Adds a test user to a domain
	 *
	 * @param domain_id the ID of the domain
	 * @param test_user_id the ID of the test user
	 */
	@Query("MATCH (d:Domain) WITH d MATCH (user:TestUser) WHERE id(d)=$domain_id AND id(user)=$test_user_id MERGE (d)-[:HAS_TEST_USER]->(user) RETURN user")
	public void addTestUser(@Param("domain_id") long domain_id, @Param("test_user_id") long test_user_id);

	/**
	 * Finds all tests for a domain
	 *
	 * @param account_id the ID of the account
	 * @param host the host of the domain
	 * @return the tests
	 */
	@Query("MATCH(account:Account)-[]-(d:Domain{host:$domain_host}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH a=(t)-[:HAS_RESULT]->(p) MATCH b=(t)-[]->() MATCH c=(p)-[]->() OPTIONAL MATCH y=(t)-->(:Group) WHERE id(account)=$account_id RETURN a,b,y,c as d")
	public Set<Test> getTests(@Param("account_id") long account_id, @Param("domain_host") String host);

	/**
	 * Finds all redirects for a domain
	 *
	 * @param account_id the ID of the account
	 * @param host the host of the domain
	 * @return the redirects
	 */
	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Redirect) WHERE id(account)=$account_id RETURN a")
	public Set<Redirect> getRedirects(@Param("account_id") long account_id, @Param("url") String host);
	
	/**
	 * Finds all test records for a domain
	 *
	 * @param account_id the ID of the account
	 * @param url the URL of the domain
	 * @return the test records
	 */
	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH b=(t)-[:HAS_TEST_RECORD]->(tr) WHERE id(account)=$account_id RETURN tr")
	public Set<TestRecord> getTestRecords(@Param("account_id") long account_id, @Param("url") String url);
	
	/**
	 * Finds all domains for an account
	 *
	 * @param account_id the ID of the account
	 * @return the domains
	 */
	@Query("MATCH (account:Account)-[:HAS]->(domain:Domain) WHERE id(account)=$account_id RETURN domain")
	Set<Domain> getDomainsForAccount(@Param("account_id") long account_id);

	/**
	 * Finds all actions for a domain
	 *
	 * @param account_id the ID of the account
	 * @param url the URL of the domain
	 * @return the actions
	 */
	@Query("MATCH(account:Account)-[]-(d:Domain{url:$url}) MATCH (d)-[:HAS_TEST]->(t:Test) MATCH (t)-[:HAS_PATH_OBJECT]->(a:Action) WHERE id(account)=$account_id RETURN a")
	public Set<TestAction> getActions(@Param("account_id") long account_id, @Param("url") String url);

	/**
	 * Finds the most recent audit record for a domain
	 *
	 * @param url the URL of the domain
	 * @return the most recent audit record
	 */
	@Query("MATCH (d:Domain{url:$url})-[]->(audit:DomainAuditRecord) RETURN audit ORDER BY audit.created_at DESC LIMIT 1")
	public Optional<DomainAuditRecord> getMostRecentAuditRecord(@Param("url") String url);

	/**
	 * Finds all performance insights for a domain
	 *
	 * @param account_id the ID of the account
	 * @param url the URL of the domain
	 * @param page_url the URL of the page
	 * @return the performance insights
	 */
	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState{url:$page_url}) MATCH (p)-[:HAS]->(pi:PerformanceInsight) WHERE id(account)=$account_id RETURN pi")
	public Set<PerformanceInsight> getPerformanceInsights(@Param("account_id") long account_id, @Param("url") String url, @Param("page_url") String page_url);

	/**
	 * Finds the most recent performance insight for a domain
	 *
	 * @param account_id the ID of the account
	 * @param url the URL of the domain
	 * @param page_url the URL of the page
	 * @return the most recent performance insight
	 */
	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState{url:$page_url}) MATCH (p)-[:HAS]->(pi:PerformanceInsight) WHERE id(account)=$account_id ORDER BY pi.executed_at DESC LIMIT 1")
	public PerformanceInsight getMostRecentPerformanceInsight(@Param("account_id") long account_id, @Param("url") String url, @Param("page_url") String page_url);

	/**
	 * Finds a domain by its account ID and URL
	 *
	 * @param account_id the ID of the account
	 * @param url the URL of the domain
	 * @return the domain
	 */
	@Query("MATCH (account:Account)-[:HAS]->(domain:Domain) WHERE id(account)=$account_id AND domain.url=$url RETURN domain LIMIT 1")
    public Domain findByAccountId(@Param("account_id") long account_id, @Param("url") String url);
}
