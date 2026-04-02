package com.looksee.frontEndBroadcaster.models.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.looksee.frontEndBroadcaster.models.Account;
import com.looksee.frontEndBroadcaster.models.AuditRecord;
import com.looksee.frontEndBroadcaster.models.Domain;

import io.github.resilience4j.retry.annotation.Retry;

@Repository
@Retry(name = "neoforj")
public interface AccountRepository extends Neo4jRepository<Account, Long> {
	@Query("MATCH (account:Account{email:$email}) RETURN account LIMIT 1")
	public Account findByEmail(@Param("email") String username);

	@Query("MATCH (account:Account{userId:$user_id}) RETURN account")
	public Account findByUserId(@Param("user_id") String user_id);
	
	@Query("MATCH (account:Account)-[hd:HAS]->(domain:Domain) WHERE id(account)=$account_id AND id(domain)=$domain_id DELETE hd")
	public void removeDomain(@Param("account_id") long account_id, @Param("domain_id") long domain_id);

	@Query("MATCH (account:Account {username:$acct_key})-[]->(d:Domain) MATCH (d)-[:HAS_TEST]-(t:Test) MATCH (t)-[:HAS_TEST_RECORD]->(tr:TestRecord) WHERE datetime(tr.ran_at).month=$month RETURN COUNT(tr)")
	public int getTestCountByMonth(@Param("acct_key") String acct_key, 
								   @Param("month") int month);

	@Query("MATCH (account:Account) WHERE id(account)=$account_id DETACH DELETE account")
	public void deleteAccount(@Param("account_id") long account_id);

	@Query("MATCH (account:Account{apiKey:$api_key}) RETURN account")
	public Account getAccountByApiKey(@Param("api_key") String api_key);

	@Query("MATCH (t:Account) WHERE id(t)=$acct_id MATCH (a:Domain) WHERE id(a)=$domain_id MERGE (t)-[:HAS]->(a) RETURN t")
	public void addDomain(@Param("domain_id") long domain_id, @Param("acct_id") long acct_id);

	@Query("MATCH (account:Account{email:$email})-[:HAS]->(domain:Domain{url:$url}) RETURN domain LIMIT 1")
	public Domain findDomain(@Param("email") String email, @Param("url") String url);

	@Query("MATCH (t:Account) WHERE id(t)=$account_id MATCH (a:AuditRecord) WHERE id(a)=$audit_record_id MERGE (t)-[:HAS]->(a) RETURN t")
	public Account addAuditRecord(@Param("account_id") long account_id, @Param("audit_record_id") long audit_record_id);

	@Query("MATCH (account:Account)-[]->(audit_record:AuditRecord) WHERE id(audit_record)=$audit_record_id RETURN account")
	public Set<Account> findAllForAuditRecord(@Param("audit_record_id") long id);

	/**
	 * Retrieves up to a given limit of the most recent audits for a specified account
	 * 
	 * @param account_id 
	 * @param limit number of records to return
	 * @return
	 */
	@Query("MATCH (account:Account)-[]->(audit_record:AuditRecord) WHERE id(account)=$account_id RETURN audit_record ORDER BY audit_record.createdAt DESC LIMIT $limit")
	public List<AuditRecord> findMostRecentAuditsByAccount(@Param("account_id") long account_id, @Param("limit") int limit);

	@Query("MATCH (account:Account)-[]->(page_audit:PageAuditRecord) WHERE id(account)=$account_id AND datetime(page_audit.created_at).month=$month RETURN COUNT(page_audit)")
	public int getPageAuditCountByMonth(@Param("account_id") long account_id, @Param("month") int month);

	@Query("MATCH (account:Account{customer_id:$customer_id}) RETURN account")
	public Account findByCustomerId(@Param("customer_id") String customer_id);
	
	@Query("MATCH (account:Account)-[:HAS]->(domain:Domain) MATCH (domain)-[:HAS]->(audit_record:DomainAuditRecord) WHERE id(account)=$account_id AND datetime(audit_record.created_at).month=$month RETURN COUNT(audit_record)")
	public int getDomainAuditRecordCountByMonth(@Param("account_id") long account_id, @Param("month") int month);
	
	@Query("MATCH (account:Account)-[*]->(audit_record:AuditRecord) WHERE id(audit_record)=$audit_record_id RETURN account LIMIT 1")
	public Optional<Account> getAccount(@Param("audit_record_id") long audit_record_id);
}
