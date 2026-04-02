package com.looksee.services;

import com.looksee.exceptions.MissingSubscriptionException;
import com.looksee.exceptions.UnknownAccountException;
import com.looksee.models.Account;
import com.looksee.models.DiscoveryRecord;
import com.looksee.models.Domain;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.repository.AccountRepository;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Contains business logic for interacting with and managing accounts
 */
@NoArgsConstructor
@Service
public class AccountService {

	@Autowired
	private AccountRepository account_repo;
	
	/**
	 * Adds a domain to an account
	 *
	 * @param acct the account to add the domain to
	 * @param domain the domain to add
	 *
	 * precondition: acct != null
	 * precondition: domain != null
	 */
	public void addDomainToAccount(Account acct, Domain domain){
		assert acct != null;
		assert domain != null;
		
		boolean domain_exists_for_acct = false;
		for(Domain acct_domain : acct.getDomains()){
			if(acct_domain.equals(domain)){
				domain_exists_for_acct = true;
			}
		}
		
		if(!domain_exists_for_acct){
			account_repo.addDomain(domain.getKey(), acct.getEmail());
			acct.addDomain(domain);
			account_repo.save(acct);
		}
	}

	/**
	 * Finds an account by email
	 *
	 * @param email the email of the account
	 * @return the account
	 *
	 * precondition: email != null
	 * precondition: !email.isEmpty()
	 */
	public Account findByEmail(String email) {
		assert email != null;
		assert !email.isEmpty();
		
		return account_repo.findByEmail(email);
	}

	/**
	 * Saves an account
	 *
	 * @param acct the account to save
	 * @return the saved account
	 *
	 * precondition: acct != null
	 */
	public Account save(Account acct) {
		assert acct != null;
		
		return account_repo.save(acct);
	}

	/**
	 * Finds an account by user ID
	 *
	 * @param id the user ID of the account
	 * @return the account
	 *
	 * precondition: id != null
	 * precondition: !id.isEmpty()
	 */
	public Account findByUserId(String id) {
		assert id != null;
		assert !id.isEmpty();
		
		return account_repo.findByUserId(id);
	}

	/**
	 * Deletes an account
	 *
	 * @param account_id the ID of the account to delete
	 * 
	 * precondition: account_id > 0
	 */
	public void deleteAccount(long account_id) {
		assert account_id > 0;
		
        account_repo.deleteAccount(account_id);
	}
	
	/**
	 * Removes a domain from an account
	 *
	 * @param account_id the ID of the account
	 * @param domain_id the ID of the domain to remove
	 * 
	 * precondition: account_id > 0
	 * precondition: domain_id > 0
	 */
	public void removeDomain(long account_id, long domain_id) {
		assert account_id > 0;
		assert domain_id > 0;
		
		account_repo.removeDomain(account_id, domain_id);
	}

	/**
	 * Retrieves the number of tests performed by a user in a given month
	 *
	 * @param username the username of the user
	 * @param month the month to retrieve the test count for
	 * @return the number of tests performed by the user in the given month
	 *
	 * precondition: username != null
	 * precondition: !username.isEmpty()
	 * precondition: month >= 0
	 */
	public int getTestCountByMonth(String username, int month) {
		assert username != null;
		assert !username.isEmpty();
		assert month >= 0;
		
		return account_repo.getTestCountByMonth(username, month);
	}

	/**
	 * Finds an account by ID
	 *
	 * @param id the ID of the account
	 * @return the account
	 *
	 * precondition: id > 0
	 * 
	 */
	public Optional<Account> findById(long id) {
		assert id > 0;
		
		return account_repo.findById(id);
	}

	/**
	 * Finds a domain by email and URL
	 *
	 * @param email the email of the account
	 * @param url the URL of the domain
	 * @return the domain
	 *
	 * precondition: email != null
	 * precondition: !email.isEmpty()
	 * precondition: url != null
	 * precondition: !url.isEmpty()
	 * 
	 */
	public Domain findDomain(String email, String url) {
		assert email != null;
		assert !email.isEmpty();
		assert url != null;
		assert !url.isEmpty();
		
		return account_repo.findDomain(email, url);
	}

	/**
	 * Adds an audit record to an account
	 *
	 * @param username the username of the account
	 * @param audit_record_id the ID of the audit record to add
	 * @return the audit record
	 *
	 * precondition: username != null
	 * precondition: !username.isEmpty()
	 * precondition: audit_record_id > 0
	 * 
	 */
	public AuditRecord addAuditRecord(String username, long audit_record_id) {
		assert username != null;
		assert !username.isEmpty();
		
		return account_repo.addAuditRecord(username, audit_record_id);
	}
	
	/**
	 * Adds an audit record to an account
	 *
	 * @param id the ID of the account
	 * @param audit_record_id the ID of the audit record to add
	 * @return the audit record
	 * 
	 * precondition: id > 0
	 * precondition: audit_record_id > 0
	 */
	public AuditRecord addAuditRecord(long id, long audit_record_id) {
		assert id > 0;
		assert audit_record_id > 0;
		
		return account_repo.addAuditRecord(id, audit_record_id);
	}

	/**
	 * Finds an account for an audit record
	 *
	 * @param id the ID of the account
	 * @return the account
	 *
	 * precondition: id > 0
	 */
	public Account findForAuditRecord(long id) {
		assert id > 0;
		
		return account_repo.findAllForAuditRecord(id);
	}

	/**
	 * Finds the most recent page audits for an account
	 *
	 * @param account_id the ID of the account
	 * @return the most recent page audits
	 *
	 * precondition: account_id > 0
	 */
	public Set<PageAuditRecord> findMostRecentPageAudits(long account_id) {
		assert account_id > 0;
		
		return account_repo.findMostRecentAuditsByAccount(account_id);
	}

	/**
	 * Retrieves the number of page audits performed by a user in a given month
	 *
	 * @param account_id the ID of the account
	 * @param month the month to retrieve the page audit count for
	 * @return the number of page audits performed by the user in the given month
	 *
	 * precondition: account_id > 0
	 * precondition: month >= 0
	 */
	public int getPageAuditCountByMonth(long account_id, int month) {
		assert account_id > 0;
		assert month >= 0;
		
		return account_repo.getPageAuditCountByMonth(account_id, month);
	}

	/**
	 * Finds an account by customer ID
	 *
	 * @param customer_id the ID of the customer
	 * @return the account
	 *
	 * precondition: customer_id != null
	 * precondition: !customer_id.isEmpty()
	 */
	public Account findByCustomerId(String customer_id) {
		assert customer_id != null;
		assert !customer_id.isEmpty();
		
		return account_repo.findByCustomerId(customer_id);
	}
	
	/**
	 * Retrieves the number of domain audits performed by a user in a given month
	 *
	 * @param account_id the ID of the account
	 * @param month the month to retrieve the domain audit count for
	 * @return the number of domain audits performed by the user in the given month
	 *
	 * precondition: account_id > 0
	 * precondition: month >= 0
	 */
	public int getDomainAuditCountByMonth(long account_id, int month) {
		assert account_id > 0;
		assert month >= 0;
		
		return account_repo.geDomainAuditRecordCountByMonth(account_id, month);
	}

	/**
	 * Retrieves the domains for an account
	 *
	 * @param account_id the ID of the account
	 * @return the domains for the account
	 *
	 * precondition: account_id > 0
	 */
	public Set<Domain> getDomainsForAccount(long account_id) {
		assert account_id > 0;
		
		return account_repo.getDomainsForAccount(account_id);
	}

	/**
	 * Adds a domain to an account
	 *
	 * @param account_id the ID of the account
	 * @param domain_id the ID of the domain to add
	 *
	 * precondition: account_id > 0
	 * precondition: domain_id > 0
	 */
	public void addDomainToAccount(long account_id, long domain_id){
		assert account_id > 0;
		assert domain_id > 0;

		account_repo.addDomain(domain_id, account_id);
	}

	/**
	 * Retrieves the discovery records for an account in a given month
	 *
	 * @param username the username of the account
	 * @param month the month to retrieve the discovery records for
	 * @return the discovery records for the account in the given month
	 *
	 * precondition: username != null
	 * precondition: !username.isEmpty()
	 * precondition: month >= 0
	 */
	public Set<DiscoveryRecord> getDiscoveryRecordsByMonth(String username, int month) {
		assert username != null;
		assert !username.isEmpty();
		assert month >= 0;

		return account_repo.getDiscoveryRecordsByMonth(username, month);
	}

	/**
	 * Finds the most recent page audits for an account
	 *
	 * @param account_id the ID of the account
	 * @param limit the limit of audits to return
	 * @return the most recent page audits
	 *
	 * precondition: account_id > 0
	 * precondition: limit > 0
	 */
	public List<AuditRecord> findMostRecentPageAudits(long account_id, int limit) {
		assert account_id > 0;
		assert limit > 0;

		return account_repo.findMostRecentAuditsByAccount(account_id, limit);
	}

	/**
	 * Checks that there is an account associated with the given Principal and
	 * that the account has a subscription assigned
	 *
	 * @param userPrincipal user {@link Principal}
	 * @return the account
	 *
	 * precondition: userPrincipal != null
	 * precondition: !userPrincipal.getName().isEmpty()
	 *
	 * @throws UnknownAccountException if the account is not found
	 * @throws MissingSubscriptionException if the account has no subscription
	 */
    public Account retrieveAndValidateAccount(Principal userPrincipal)
		throws UnknownAccountException, MissingSubscriptionException
	{
		assert userPrincipal != null;
		assert !userPrincipal.getName().isEmpty();
		
		String acct_id = userPrincipal.getName();
		Account acct = findByUserId(acct_id);
		
		if(acct == null){
			throw new UnknownAccountException();
		}
		else if(acct.getSubscriptionToken() == null){
			throw new MissingSubscriptionException();
		}

		return acct;
    }
}
