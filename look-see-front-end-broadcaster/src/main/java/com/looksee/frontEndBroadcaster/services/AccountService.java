package com.looksee.frontEndBroadcaster.services;

import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.frontEndBroadcaster.models.Account;
import com.looksee.frontEndBroadcaster.models.repository.AccountRepository;

/**
 * Contains business logic for interacting with and managing accounts
 *
 */
@Service
public class AccountService {

	@Autowired
	private AccountRepository account_repo;

	public void addDomainToAccount(long account_id, long domain_id){
		account_repo.addDomain(domain_id, account_id);
	}

	public Account findByEmail(String email) {
		assert email != null;
		assert !email.isEmpty();
		
		return account_repo.findByEmail(email);
	}

	public Account save(Account acct) {
		return account_repo.save(acct);
	}

	public Account findByUserId(String id) {
		return account_repo.findByUserId(id);
	}

	public void deleteAccount(long account_id) {
        account_repo.deleteAccount(account_id);
	}
	
	public void removeDomain(long account_id, long domain_id) {
		account_repo.removeDomain(account_id, domain_id);
	}

	public int getTestCountByMonth(String username, int month) {
		return account_repo.getTestCountByMonth(username, month);
	}

	public Optional<Account> findById(long id) {
		return account_repo.findById(id);
	}

	public Set<Account> findForAuditRecord(long id) {
		return account_repo.findAllForAuditRecord(id);
	}

	public int getPageAuditCountByMonth(long account_id, int month) {
		return account_repo.getPageAuditCountByMonth(account_id, month);
	}

	public Account findByCustomerId(String customer_id) {
		return account_repo.findByCustomerId(customer_id);
	}
	
	public int getDomainAuditCountByMonth(long account_id, int month) {
		return account_repo.getDomainAuditRecordCountByMonth(account_id, month);
	}
}
