package com.looksee.models.repository;

import com.looksee.models.Account;
import com.looksee.models.DiscoveryRecord;
import com.looksee.models.Domain;
import com.looksee.models.audit.AuditRecord;
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
 * Repository interface for Spring Data Neo4j to handle interactions with {@link Account} objects
 */
@Repository
@Retry(name = "neoforj")
public interface AccountRepository extends Neo4jRepository<Account, Long> {
	/**
	 * Find an account by email
	 * @param email the email of the account
	 * @return the account
	 */
	@Query("MATCH (account:Account{email:$email}) RETURN account LIMIT 1")
	public Account findByEmail(@Param("email") String email);

	/**
	 * Find an account by user_id
	 * @param user_id the user_id of the account
	 * @return the account
	 */
	@Query("MATCH (account:Account{user_id:$user_id}) RETURN account")
	public Account findByUserId(@Param("user_id") String user_id);
	
	/**
	 * Remove a domain from an account
	 * @param account_id the id of the account
	 * @param domain_id the id of the domain
	 */
	@Query("MATCH (account:Account)-[hd:HAS]->(domain:Domain) WHERE id(account)=$account_id AND id(domain)=$domain_id DELETE hd")
	public void removeDomain(@Param("account_id") long account_id, @Param("domain_id") long domain_id);

	/**
	 * Get the number of tests run by an account in a given month
	 * @param acct_key the key of the account
	 * @param month the month
	 * @return the number of tests run by the account in the given month
	 */
	@Query("MATCH (account:Account {username:$acct_key})-[]->(d:Domain) MATCH (d)-[:HAS_TEST]-(t:Test) MATCH (t)-[:HAS_TEST_RECORD]->(tr:TestRecord) WHERE datetime(tr.ran_at).month=$month RETURN COUNT(tr)")
	public int getTestCountByMonth(@Param("acct_key") String acct_key, 
									@Param("month") int month);

	/**
	 * Delete an account
	 * @param account_id the id of the account
	 */
	@Query("MATCH (account:Account) WHERE id(account)=$account_id DETACH DELETE account")
	public void deleteAccount(@Param("account_id") long account_id);

	/**
	 * Get an account by api_key
	 * @param api_key the api_key of the account
	 * @return the account
	 */
	@Query("MATCH (account:Account{api_key:$api_key}) RETURN account")
	public Account getAccountByApiKey(@Param("api_key") String api_key);

	/**
	 * Add a domain to an account
	 * @param domain_key the key of the domain
	 * @param email the email of the account
	 */
	@Query("MATCH (t:Account{email:$email}) WITH t MATCH (a:Domain{key:$domain_key}) MERGE (t)-[r:BELONGS_TO]->(a) RETURN r")
	public void addDomain(@Param("domain_key") String domain_key, @Param("email") String email);

	/**
	 * Find a domain by email and url
	 * @param email the email of the account
	 * @param url the url of the domain
	 * @return the domain
	 */
	@Query("MATCH (account:Account{email:$email})-[:HAS]->(domain:Domain{url:$url}) RETURN domain LIMIT 1")
	public Domain findDomain(@Param("email") String email, @Param("url") String url);

	/**
	 * Add an audit record to an account
	 * @param username the username of the account
	 * @param audit_record_id the id of the audit record
	 * @return the audit record
	 */
	@Query("MATCH (t:Account{username:$username}) WITH t MATCH (a:AuditRecord) WHERE id(a)=$audit_record_id MERGE (t)-[r:HAS]->(a) RETURN r")
	public AuditRecord addAuditRecord(@Param("username") String username, @Param("audit_record_id") long audit_record_id);

	/**
	 * Add an audit record to an account
	 * @param account_id the id of the account
	 * @param audit_record_id the id of the audit record
	 * @return the audit record
	 */
	@Query("MATCH (t:Account) WITH t MATCH (a:AuditRecord) WHERE id(a)=$audit_record_id AND id(t)=$account_id MERGE (t)-[r:HAS]->(a) RETURN a")
	public AuditRecord addAuditRecord(@Param("account_id") long account_id, @Param("audit_record_id") long audit_record_id);

	/**
	 * Find all accounts for an audit record
	 * @param audit_record_id the id of the audit record
	 * @return the account
	 */
	@Query("MATCH (account:Account)-[]->(audit_record:AuditRecord) WHERE id(audit_record)=$audit_record_id RETURN account LIMIT 1")
	public Account findAllForAuditRecord(@Param("audit_record_id") long audit_record_id);

	/**
	 * Find the most recent audits by account
	 * @param account_id the id of the account
	 * @return the most recent audits by the account
	 */
	@Query("MATCH (account:Account)-[]->(audit_record:PageAuditRecord) WHERE id(account)=$account_id RETURN audit_record ORDER BY audit_record.created_at DESC LIMIT 5")
	public Set<PageAuditRecord> findMostRecentAuditsByAccount(long account_id);

	/**
	 * Get the number of page audits by account in a given month
	 * @param account_id the id of the account
	 * @param month the month
	 * @return the number of page audits by the account in the given month
	 */
	@Query("MATCH (account:Account)-[]->(page_audit:PageAuditRecord) WHERE id(account)=$account_id AND datetime(page_audit.created_at).month=$month RETURN COUNT(page_audit)")
	int getPageAuditCountByMonth(@Param("account_id") long account_id, @Param("month") int month);

	/**
	 * Find an account by customer_id
	 * @param customer_id the customer_id of the account
	 * @return the account
	 */
	@Query("MATCH (account:Account{customer_id:$customer_id}) RETURN account")
	public Account findByCustomerId(@Param("customer_id") String customer_id);
	
	/**
	 * Get the number of domain audits by account in a given month
	 * @param account_id the id of the account
	 * @param month the month
	 * @return the number of domain audits by the account in the given month
	 */
	@Query("MATCH (account:Account)-[:HAS]->(domain:Domain) MATCH (domain)-[:HAS]->(audit_record:DomainAuditRecord) WHERE id(account)=$account_id AND datetime(audit_record.created_at).month=$month RETURN COUNT(audit_record)")
	public int geDomainAuditRecordCountByMonth(@Param("account_id") long account_id, @Param("month") int month);

	/**
	 * Get an account by audit record id
	 * @param audit_record_id the id of the audit record
	 * @return the account
	 */
	@Query("MATCH (account:Account)-[*]->(audit_record:AuditRecord) WHERE id(audit_record)=$audit_record_id RETURN account LIMIT 1")
	public Optional<Account> getAccount(@Param("audit_record_id") long audit_record_id);

	/**
	 * Get the domains for an account
	 * @param account_id the id of the account
	 * @return the domains for the account
	 */
	@Query("MATCH (account:Account)-[:HAS]->(domain:Domain) WHERE id(account)=$account_id RETURN domain")
	Set<Domain> getDomainsForAccount(@Param("account_id") long account_id);

	/**
	 * NOTE:: relic from old days. Remove at first chance
	 * @param username the username of the account
	 * @param month the month
	 * @return the discovery records for the given account and month
	 */
	@Query("MATCH (account:Account {username:$user_id})-[]->(d:DiscoveryRecord) WHERE datetime(d.started_at).month=$month return d")
	@Deprecated
	public Set<DiscoveryRecord> getDiscoveryRecordsByMonth(@Param("username") String username, 
															@Param("month") int month);
	/**
	 * Adds a domain to an account
	 * 
	 * @param domain_id the id of the domain
	 * @param acct_id the id of the account
	 */
	@Query("MATCH (t:Account) WHERE id(t)=$acct_id MATCH (a:Domain) WHERE id(a)=$domain_id MERGE (t)-[:HAS]->(a) RETURN t")
	public void addDomain(@Param("domain_id") long domain_id, @Param("acct_id") long acct_id);

	/**
	 * Retrieves up to a given limit of the most recent audits for a specified account
	 * 
	 * @param account_id the id of the account
	 * @param limit number of records to return
	 * @return the most recent audits for the given account
	 */
	@Query("MATCH (account:Account)-[]->(audit_record:AuditRecord) WHERE id(account)=$account_id RETURN audit_record ORDER BY audit_record.createdAt DESC LIMIT $limit")
	public List<AuditRecord> findMostRecentAuditsByAccount(@Param("account_id") long account_id, @Param("limit") int limit);

	/**
	 * Retrieves the number of domain audit records for a given account and month
	 * 
	 * @param account_id the id of the account
	 * @param month the month
	 * @return the number of domain audit records for the given account and month
	 */
	@Query("MATCH (account:Account)-[:HAS]->(domain:Domain) MATCH (domain)-[:HAS]->(audit_record:DomainAuditRecord) WHERE id(account)=$account_id AND datetime(audit_record.created_at).month=$month RETURN COUNT(audit_record)")
	public int getDomainAuditRecordCountByMonth(@Param("account_id") long account_id, @Param("month") int month);
}
