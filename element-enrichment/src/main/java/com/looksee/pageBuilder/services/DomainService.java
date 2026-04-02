package com.looksee.pageBuilder.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.pageBuilder.models.AuditRecord;
import com.looksee.pageBuilder.models.DesignSystem;
import com.looksee.pageBuilder.models.Domain;
import com.looksee.pageBuilder.models.DomainAuditRecord;
import com.looksee.pageBuilder.models.Element;
import com.looksee.pageBuilder.models.Form;
import com.looksee.pageBuilder.models.PageState;
import com.looksee.pageBuilder.models.TestUser;
import com.looksee.pageBuilder.models.repository.DomainRepository;

@Service
public class DomainService {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private DomainRepository domain_repo;
	

	public Set<Domain> getDomains() {
		return domain_repo.getDomains();
	}
	
	public Set<TestUser> getTestUsers(long domain_id) {
		return domain_repo.getTestUsers(domain_id);
	}

	public Domain findByHostForUser(String host, String username) {
		return domain_repo.findByHostForUser(host, username);
	}
	
	public Domain findByHost(String host) {
		return domain_repo.findByHost(host);
	}

	public Domain findByUrl(String url) {
		return domain_repo.findByUrl(url);
	}
	
	public Domain save(Domain domain) {
		return domain_repo.save(domain);	
	}
	
	public int getTestCount(long account_id, String url) {
		return domain_repo.getTestCount(account_id, url);
	}

	public Optional<Domain> findById(long domain_id) {
		return domain_repo.findById(domain_id);
	}

	public boolean deleteTestUser(long domain_id, long user_id) {
		return domain_repo.deleteTestUser(domain_id, user_id) > 0;
	}

	public Set<Form> getForms(long account_id, String url) {
		return domain_repo.getForms(account_id, url);
	}
	
	public int getFormCount(long account_id, String url) {
		return domain_repo.getFormCount(account_id, url);
	}

	public Set<Element> getElementStates(String url, String username) {
		return domain_repo.getElementStates(url, username);
	}

	public Set<PageState> getPageStates(long domain_id) {
		return domain_repo.getPageStates(domain_id);
	}

	public Domain findByKey(String key, String username) {
		return domain_repo.findByKey(key, username);
	}

	/**
	 * Creates a relationship between existing {@link PageVersion} and {@link Domain} records
	 * 
	 * @param url {@link Domain} url
	 * @param page_key key of {@link PageVersion} object
	 * @return
	 * 
	 * @pre host != null
	 * @pre !host.isEmpty()
	 * @pre page_version_key != null
	 * @pre !page_version_key.isEmpty()
	 * 
	 */
	public boolean addPage(long domain_id, long page_id) {
		//check if page already exists. If it does then return true;
		Optional<PageState> page = domain_repo.getPage(domain_id, page_id);
		if(page.isPresent()) {
			return true;
		}
		
		return domain_repo.addPage(domain_id, page_id) != null;
	}

	@Deprecated
	public Optional<DomainAuditRecord> getMostRecentAuditRecord(String host) {
		assert host != null;
		assert !host.isEmpty();
		
		return domain_repo.getMostRecentAuditRecord(host);
	}
	
	public Optional<DomainAuditRecord> getMostRecentAuditRecord(long id) {
		return domain_repo.getMostRecentAuditRecord(id);
	}

	public Set<PageState> getPages(String domain_host) {
		return domain_repo.getPages(domain_host);
	}

	public Domain findByPageState(String page_state_key) {
		return domain_repo.findByPageState(page_state_key);
	}

	/**
	 * Creates graph edge connection {@link AuditRecord} to {@link Domain domain} 
	 * 
	 * @param domain_key
	 * @param audit_record_key
	 * 
	 * @pre domain_key != null;
	 * @pre !domain_key.isEmpty();
	 * @pre audit_record_key != null;
	 * @pre !audit_record_key.isEmpty();
	 */
	public void addAuditRecord(long domain_id, String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		//check if audit record is already attached to domain

		domain_repo.addAuditRecord(domain_id, audit_record_key);
	}

	public Set<AuditRecord> getAuditRecords(String domain_key) {
		return domain_repo.getAuditRecords(domain_key);
	}

	public Domain findByAuditRecord(long audit_record_id) {
		return domain_repo.findByAuditRecord(audit_record_id);
	}

	public DesignSystem updateExpertiseSettings(long domain_id, String expertise) {
		return domain_repo.updateExpertiseSetting(domain_id, expertise);
	}

	public List<DomainAuditRecord> getAuditRecordHistory(long domain_id) {
		return domain_repo.getAuditRecordHistory(domain_id);
	}

	public Optional<DesignSystem> getDesignSystem(long domain_id) {
		return domain_repo.getDesignSystem(domain_id);
	}

	public DesignSystem addDesignSystem(long domain_id, long design_system_id) {
		return domain_repo.addDesignSystem(domain_id, design_system_id);
	}

	public DesignSystem updateWcagSettings(long domain_id, String wcag_level) {
		return domain_repo.updateWcagSettings(domain_id, wcag_level);
	}

	public DesignSystem updateAllowedImageCharacteristics(long domain_id, List<String> allowed_image_characteristics) {
		return domain_repo.updateAllowedImageCharacteristics(domain_id, allowed_image_characteristics);
	}

	public List<TestUser> findTestUsers(long domain_id) {
		return domain_repo.findTestUsers(domain_id);
	}

	public void addTestUser(long domain_id, long test_user_id) {
		domain_repo.addTestUser(domain_id, test_user_id);	
	}
}
