package com.looksee.pageBuilder.models.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.pageBuilder.models.AuditRecord;
import com.looksee.pageBuilder.models.DesignSystem;
import com.looksee.pageBuilder.models.Domain;
import com.looksee.pageBuilder.models.DomainAuditRecord;
import com.looksee.pageBuilder.models.Element;
import com.looksee.pageBuilder.models.Form;
import com.looksee.pageBuilder.models.PageLoadAnimation;
import com.looksee.pageBuilder.models.PageState;
import com.looksee.pageBuilder.models.TestUser;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * 
 */
@Repository
@Retry(name = "neoforj")
public interface DomainRepository extends Neo4jRepository<Domain, Long> {
	
	@Query("MATCH (a:Account{username:$username})-[:HAS_DOMAIN]->(d:Domain{key:$key}) RETURN d LIMIT 1")
	public Domain findByKey(@Param("key") String key, @Param("username") String username);
	
	@Query("MATCH (a:Account{username:$username})-[:HAS_DOMAIN]->(d:Domain{host:$host}) RETURN d LIMIT 1")
	public Domain findByHostForUser(@Param("host") String host, @Param("username") String username);
	
	@Query("MATCH (d:Domain{host:$host}) RETURN d LIMIT 1")
	public Domain findByHost(@Param("host") String host);
	
	@Query("MATCH (d:Domain{host:$host})-[:HAS]->(p:PageState) RETURN p")
	public Set<PageState> getPages(@Param("host") String host);

	@Query("MATCH (d:Domain{url:$url}) RETURN d LIMIT 1")
	public Domain findByUrl(@Param("url") String url);

	@Query("MATCH (d:Domain)-[]->(p:PageState) WHERE id(d)=$domain_id RETURN p")
	public Set<PageState> getPageStates(@Param("domain_id") long domain_id);

	@Query("MATCH (:Account{username:$username})-[:HAS_DOMAIN]-(d:Domain{url:$url}) MATCH (d)-[]->(t:Test) MATCH (t)-[]->(e:ElementState) OPTIONAL MATCH b=(e)-->() RETURN b")
	public Set<Element> getElementStates(@Param("url") String url, @Param("username") String username);
	
	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:Page) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form) MATCH a=(f)-[:DEFINED_BY]->() MATCH b=(f)-[:HAS]->(e) OPTIONAL MATCH c=(e)-->() WHERE id(account)=$account_id return a,b,c")
	public Set<Form> getForms(@Param("account_id") long account_id, @Param("url") String url);
	
	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{url:$url}) MATCH (d)-[]->(p:PageState) MATCH (p)-[]->(ps:PageState) MATCH (ps)-[]->(f:Form) WHERE id(account)=$account_id RETURN COUNT(f)")
	public int getFormCount(@Param("account_id") long account_id, @Param("url") String url);

	@Query("MATCH(account:Account)-[]-(d:Domain{host:$host}) MATCH (d)-[:HAS_TEST]->(t:Test) WHERE id(account)=$account_id  RETURN COUNT(t)")
	public int getTestCount(@Param("account_id") long account_id, @Param("host") String host);

	@Query("MATCH (d:Domain)-[:HAS_TEST_USER]->(t:TestUser) WHERE id(d)=$domain_id RETURN t")
	public Set<TestUser> getTestUsers(@Param("domain_id") long domain_id);

	@Query("MATCH (d:Domain)-[r:HAS_TEST_USER]->(t:TestUser{username:$username}) WHERE id(d)=$domain_id AND id(t)=$user_id DELETE r,t return count(t)")
	public int deleteTestUser(@Param("domain_id") long domain_id, @Param("user_id") long user_id);

	@Query("MATCH (account:Account)-[:HAS_DOMAIN]->(d:Domain{host:$url}) MATCH (d)-[:HAS_TEST]->(:Test) MATCH (t)-[]->(p:PageLoadAnimation) WHERE id(account)=$account_id RETURN p")
	public Set<PageLoadAnimation> getAnimations(@Param("account_id") long account_id, @Param("url") String url);

	@Query("MATCH (d:Domain) WITH d MATCH (p:PageState) WHERE id(d)=$domain_id AND id(p)=$page_id MERGE (d)-[:HAS]->(p) RETURN p")
	public PageState addPage(@Param("domain_id") long domain_id, @Param("page_id") long page_id);

	@Query("MATCH (d:Domain{url:$url})-[]->(audit:DomainAuditRecord) RETURN audit ORDER BY audit.created_at DESC LIMIT 1")
	public Optional<DomainAuditRecord> getMostRecentAuditRecord(@Param("url") String url);

	@Query("MATCH(d:Domain) WITH d MATCH (audit:DomainAuditRecord)-[:HAS]->(d) WITH audit WHERE id(d)=$id RETURN audit ORDER BY audit.created_at DESC LIMIT 1")
	public Optional<DomainAuditRecord> getMostRecentAuditRecord(@Param("id") long id);

	@Query("MATCH (d:Domain)-[*]->(:PageState{key:$page_state_key}) RETURN d LIMIT 1")
	public Domain findByPageState(@Param("page_state_key") String page_state_key);

	@Query("MATCH (d:Domain) WITH d MATCH (audit:AuditRecord{key:$audit_record_key}) WHERE id(d) = $domain_id MERGE (d)-[:HAS]->(audit) RETURN audit")
	public void addAuditRecord(@Param("domain_id") long domain_id, @Param("audit_record_key") String audit_record_key);

	@Query("MATCH (d:Domain{key:$domain_key})-[]->(audit:AuditRecord) RETURN audit")
	public Set<AuditRecord> getAuditRecords(@Param("domain_key") String domain_key);

	@Query("MATCH (d:Domain{key:$domain_key})<-[]-(audit:AuditRecord{key:$audit_record_key}) RETURN audit")
	public AuditRecord getAuditRecords(@Param("domain_key") String domain_key, @Param("audit_record_key") String audit_record_key);

	@Query("MATCH (d:Domain)-[:HAS]->(audit_record:AuditRecord) WHERE id(audit_record)=$audit_record_id RETURN d LIMIT 1")
	public Domain findByAuditRecord(@Param("audit_record_id") long audit_record_id);

	@Query("MATCH (domain:Domain) RETURN domain")
	public Set<Domain> getDomains();
	
	@Query("MATCH (d:Domain)-[]->(p:PageState) WHERE id(d)=$domain_id AND id(p)=$page_id RETURN p")
	public Optional<PageState> getPage(@Param("domain_id") long domain_id, @Param("page_id") long page_id);

	@Query("MATCH (d:Domain)-[:USES]->(setting:DesignSystem) WHERE id(d)=$domain_id SET setting.audience_proficiency=$audience_proficiency RETURN setting")
	public DesignSystem updateExpertiseSetting(@Param("domain_id") long domain_id, @Param("audience_proficiency") String audience_proficiency);
	
	@Query("MATCH (d:Domain)-[]->(setting:DesignSystem) WHERE id(d)=$domain_id SET setting.wcag_compliance_level=$wcag_level RETURN setting")
	public DesignSystem updateWcagSettings(@Param("domain_id") long domain_id, @Param("wcag_level") String wcag_level);

	@Query("MATCH (d:Domain)-[]->(setting:DesignSystem) WHERE id(d)=$domain_id SET setting.allowed_image_characteristics=$image_characteristics RETURN setting")
	public DesignSystem updateAllowedImageCharacteristics(@Param("domain_id") long domain_id, @Param("image_characteristics") List<String> allowed_image_characteristics);

	@Query("MATCH(d:Domain) WITH d WHERE id(d)=$domain_id MATCH (ar:DomainAuditRecord)-[]->(d) MATCH y=(ar)-[:HAS]->(page_audit:PageAuditRecord) MATCH z=(page_audit)-[:HAS]->(audit:Audit) RETURN y,z")
	public List<DomainAuditRecord> getAuditRecordHistory(@Param("domain_id") long domain_id);

	@Query("MATCH (domain:Domain)-[:USES]->(ds:DesignSystem) WHERE id(domain)=$domain_id RETURN ds LIMIT 1")
	public Optional<DesignSystem> getDesignSystem(@Param("domain_id") long domain_id);

	@Query("MATCH (d:Domain) WITH d MATCH (design:DesignSystem) WHERE id(d)=$domain_id AND id(design)=$design_system_id MERGE (d)-[:USES]->(design) RETURN design")
	public DesignSystem addDesignSystem(@Param("domain_id") long domain_id, @Param("design_system_id") long design_system_id);

	@Query("MATCH (d:Domain)-[]->(user:TestUser) WHERE id(d)=$domain_id RETURN user")
	public List<TestUser> findTestUsers(@Param("domain_id") long domain_id);

	@Query("MATCH (d:Domain) WITH d MATCH (user:TestUser) WHERE id(d)=$domain_id AND id(user)=$test_user_id MERGE (d)-[:HAS_TEST_USER]->(user) RETURN user")
	public void addTestUser(@Param("domain_id") long domain_id, @Param("test_user_id") long test_user_id);
}
