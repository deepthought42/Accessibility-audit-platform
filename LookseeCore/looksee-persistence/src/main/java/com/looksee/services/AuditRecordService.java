package com.looksee.services;

import com.looksee.models.Account;
import com.looksee.models.Label;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.dto.AuditRecordDto;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.repository.AccountRepository;
import com.looksee.models.repository.AuditRecordRepository;
import com.looksee.models.repository.AuditRepository;
import com.looksee.utils.AuditUtils;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.IterableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Contains business logic for interacting with and managing audits
 */
@NoArgsConstructor
@Service
@Retry(name = "neoforj")
public class AuditRecordService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AuditRecordService.class);

	@Autowired
	private AuditRecordRepository audit_record_repo;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private AuditRepository audit_repo;

	@Autowired
	private AccountRepository account_repo;

	/**
	 * Save an audit record
	 *
	 * @param audit the audit record to save
	 * @return the saved audit record
	 *
	 * precondition: audit != null
	 */
	public AuditRecord save(AuditRecord audit) {
		assert audit != null;

		return audit_record_repo.save(audit);
	}
	
	/**
	 * Save an audit record with an account and domain
	 *
	 * @param audit the audit record to save
	 * @param account_id the id of the account
	 * @param domain_id the id of the domain
	 * @return the saved audit record
	 *
	 * precondition: audit != null
	 * precondition: account_id != null
	 * precondition: domain_id != null
	 */
	public AuditRecord save(AuditRecord audit, Long account_id, Long domain_id) {
		assert audit != null;
		assert account_id != null;
		assert domain_id != null;

		AuditRecord audit_record = audit_record_repo.save(audit);
		return audit_record;
	}

	/**
	 * Find an audit record by id
	 *
	 * @param id the id of the audit record
	 * @return the audit record
	 *
	 * precondition: id > 0
	 */
	public Optional<AuditRecord> findById(long id) {
		assert id > 0;

		return audit_record_repo.findById(id);
	}
	
	/**
	 * Find an audit record by key
	 *
	 * @param key the key of the audit record
	 * @return the audit record
	 *
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public AuditRecord findByKey(String key) {
		assert key != null;
		assert !key.isEmpty();

		return audit_record_repo.findByKey(key);
	}

	/**
	 * Find all audit records
	 *
	 * @return the list of audit records
	 */
	public List<AuditRecord> findAll() {
		return IterableUtils.toList(audit_record_repo.findAll());
	}
	
	/**
	 * Add an audit to an audit record
	 *
	 * @param audit_record_key the key of the audit record
	 * @param audit_key the key of the audit
	 *
	 * precondition: audit_record_key != null
	 * precondition: audit_key != null
	 * precondition: !audit_record_key.isEmpty()
	 * precondition: !audit_key.isEmpty()
	 */
	public void addAudit(String audit_record_key, String audit_key) {
		assert audit_record_key != null;
		assert audit_key != null;
		assert !audit_record_key.isEmpty();
		assert !audit_key.isEmpty();

		//check if audit already exists for page state
		Optional<Audit> audit = audit_record_repo.getAuditForAuditRecord(audit_record_key, audit_key);
		if(!audit.isPresent()) {
			audit_record_repo.addAudit(audit_record_key, audit_key);
		}
	}

	/**
	 * Add an audit to an audit record
	 *
	 * @param audit_record_id the id of the audit record
	 * @param audit_id the id of the audit
	 *
	 * precondition: audit_record_id > 0
	 * precondition: audit_id > 0
	 */
	public void addAudit(long audit_record_id, long audit_id) {
		assert audit_record_id > 0;
		assert audit_id > 0;

		audit_record_repo.addAudit(audit_record_id, audit_id);
	}
	
	/**
	 * Get all audits and issues for an audit
	 *
	 * @param audit_id the id of the audit
	 * @return the set of audits and issues
	 *
	 * precondition: audit_id > 0
	 */
	public Set<Audit> getAllAuditsAndIssues(long audit_id) {
		assert audit_id > 0;

		return audit_record_repo.getAllAuditsForPageAuditRecord(audit_id);
	}
	
	/**
	 * Find the most recent domain audit record
	 *
	 * @param id the id of the domain audit record
	 * @return the most recent domain audit record
	 *
	 * precondition: id > 0
	 */
	public Optional<DomainAuditRecord> findMostRecentDomainAuditRecord(long id) {
		assert id > 0;

		return audit_record_repo.findMostRecentDomainAuditRecord(id);
	}

	/**
	 * Find the most recent page audit record
	 *
	 * @param page_url the url of the page
	 * @return the most recent page audit record
	 *
	 * precondition: page_url != null
	 * precondition: !page_url.isEmpty()
	 */
	public Optional<PageAuditRecord> findMostRecentPageAuditRecord(String page_url) {
		assert page_url != null;
		assert !page_url.isEmpty();
		
		return audit_record_repo.getMostRecentPageAuditRecord(page_url);
	}

	/**
	 * Find the most recent audits for a page
	 *
	 * @param page_url the url of the page
	 * @return the most recent audits for the page
	 *
	 * precondition: page_url != null
	 * precondition: !page_url.isEmpty()
	 */
	public Set<Audit> findMostRecentAuditsForPage(String page_url) {
		assert page_url != null;
		assert !page_url.isEmpty();
		
		//get most recent page state
		PageState page_state = page_state_service.findByUrl(page_url);
		return audit_record_repo.getMostRecentAuditsForPage(page_state.getKey());
		//return audit_record_repo.findMostRecentDomainAuditRecord(page_url);
	}

	/**
	 * Get all color palette audits for an audit record
	 *
	 * @param audit_record_key the key of the audit record
	 * @return the set of color palette audits
	 *
	 * precondition: audit_record_key != null
	 * precondition: !audit_record_key.isEmpty()
	 */
	public Set<Audit> getAllColorPaletteAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageColorPaletteAudits(audit_record_key);
	}

	/**
	 * Get all text color contrast audits for an audit record
	 *
	 * @param audit_record_key the key of the audit record
	 * @return the set of text color contrast audits
	 *
	 * precondition: audit_record_key != null
	 * precondition: !audit_record_key.isEmpty()
	 */
	public Set<Audit> getAllTextColorContrastAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageTextColorContrastAudits(audit_record_key);
	}

	/**
	 * Get all non-text color contrast audits for an audit record
	 *
	 * @param audit_record_key the key of the audit record
	 * @return the set of non-text color contrast audits
	 *
	 * precondition: audit_record_key != null
	 * precondition: !audit_record_key.isEmpty()
	 */
	public Set<Audit> getAllNonTextColorContrastAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageNonTextColorContrastAudits(audit_record_key);
	}

	/**
	 * Get all typeface audits for an audit record
	 *
	 * @param audit_record_key the key of the audit record
	 * @return the set of typeface audits
	 *
	 * precondition: audit_record_key != null
	 * precondition: !audit_record_key.isEmpty()
	 */
	public Set<Audit> getAllTypefaceAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageTypefaceAudits(audit_record_key);
	}

	/**
	 * Get all link audits for an audit record
	 *
	 * @param audit_record_key the key of the audit record
	 * @return the set of link audits
	 *
	 * precondition: audit_record_key != null
	 * precondition: !audit_record_key.isEmpty()
	 */
	public Set<Audit> getAllLinkAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageLinkAudits(audit_record_key);
	}

	/**
	 * Get all title and header audits for an audit record
	 *
	 * @param audit_record_key the key of the audit record
	 * @return the set of title and header audits
	 *
	 * precondition: audit_record_key != null
	 * precondition: !audit_record_key.isEmpty()
	 */
	public Set<Audit> getAllTitleAndHeaderAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageTitleAndHeaderAudits(audit_record_key);
	}

	/**
	 * Get all alt text audits for an audit record
	 *
	 * @param audit_record_key the key of the audit record
	 * @return the set of alt text audits
	 *
	 * precondition: audit_record_key != null
	 * precondition: !audit_record_key.isEmpty()
	 */
	public Set<Audit> getAllAltTextAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageAltTextAudits(audit_record_key);
	}

	/**
	 * Get all margin audits for an audit record
	 *
	 * @param audit_record_key the key of the audit record
	 * @return the set of margin audits
	 *
	 * precondition: audit_record_key != null
	 * precondition: !audit_record_key.isEmpty()
	 */
	public Set<Audit> getAllMarginAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageMarginAudits(audit_record_key);
	}

	/**
	 * Get all page padding audits for an audit record
	 *
	 * @param audit_record_key the key of the audit record
	 * @return the set of page padding audits
	 *
	 * precondition: audit_record_key != null
	 * precondition: !audit_record_key.isEmpty()
	 */
	public Set<Audit> getAllPagePaddingAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPagePaddingAudits(audit_record_key);
	}

	/**
	 * Get all page paragraphing audits for an audit record
	 *
	 * @param audit_record_key the key of the audit record
	 * @return the set of page paragraphing audits
	 *
	 * precondition: audit_record_key != null
	 * precondition: !audit_record_key.isEmpty()
	 */
	public Set<Audit> getAllPageParagraphingAudits(String audit_record_key) {
		assert audit_record_key != null;
		assert !audit_record_key.isEmpty();
		
		return audit_record_repo.getAllPageParagraphingAudits(audit_record_key);
	}

	/**
	 * Get all page audits for an audit record
	 *
	 * @param audit_record_id the id of the audit record
	 * @return the set of page audits
	 *
	 * precondition: audit_record_id > 0
	 */
	public Set<PageAuditRecord> getAllPageAudits(long audit_record_id) {
		assert audit_record_id > 0;

		return audit_record_repo.getAllPageAudits(audit_record_id);
	}

	/**
	 * Get all audits for a page audit record
	 *
	 * @param page_audit_id the id of the page audit record
	 * @return the set of audits
	 *
	 * precondition: page_audit_id > 0
	 */
	public Set<Audit> getAllAuditsForPageAuditRecord(long page_audit_id) {
		assert page_audit_id > 0;

		return audit_record_repo.getAllAuditsForPageAuditRecord( page_audit_id);
	}

	/**
	 * Add a page audit to a domain audit
	 *
	 * @param domain_audit_record_id the id of the domain audit record
	 * @param page_audit_record_key the key of the page audit record
	 *
	 * precondition: domain_audit_record_id > 0
	 * precondition: page_audit_record_key != null
	 * precondition: !page_audit_record_key.isEmpty()
	 */
	public void addPageAuditToDomainAudit(long domain_audit_record_id, String page_audit_record_key) {
		assert domain_audit_record_id > 0;
		assert page_audit_record_key != null;
		assert !page_audit_record_key.isEmpty();
		
		//check if audit already exists for page state
		audit_record_repo.addPageAuditRecord(domain_audit_record_id, page_audit_record_key);
	}

	/**
	 * Add a page audit to a domain audit
	 *
	 * @param domain_audit_id the id of the domain audit
	 * @param page_audit_id the id of the page audit
	 *
	 * precondition: domain_audit_id > 0
	 * precondition: page_audit_id > 0
	 */
	public void addPageAuditToDomainAudit(long domain_audit_id, long page_audit_id) {
		assert domain_audit_id > 0;
		assert page_audit_id > 0;

		audit_record_repo.addPageAuditRecord(domain_audit_id, page_audit_id);
	}

	/**
	 * Get the most recent page audit record for a url
	 *
	 * @param url the url of the page
	 * @return the most recent page audit record
	 *
	 * precondition: url != null
	 * precondition: !url.isEmpty()
	 */
	public Optional<PageAuditRecord> getMostRecentPageAuditRecord(String url) {
		assert url != null;
		assert !url.isEmpty();
		
		return audit_record_repo.getMostRecentPageAuditRecord(url);
	}

	/**
	 * Get the page state for an audit record
	 *
	 * @param page_audit_key the key of the page audit record
	 * @return the page state
	 *
	 * precondition: page_audit_key != null
	 * precondition: !page_audit_key.isEmpty()
	 */
	public PageState getPageStateForAuditRecord(String page_audit_key) {
		assert page_audit_key != null;
		assert !page_audit_key.isEmpty();
		
		return audit_record_repo.getPageStateForAuditRecord(page_audit_key);
	}

	/**
	 * Get all content audits for a domain record
	 *
	 * @param id the id of the domain record
	 * @return the set of content audits
	 *
	 * precondition: id > 0
	 */
	public Set<Audit> getAllContentAuditsForDomainRecord(long id) {
		assert id > 0;

		return audit_record_repo.getAllContentAuditsForDomainRecord(id);
	}

	/**
	 * Get all information architecture audits for a domain record
	 *
	 * @param id the id of the domain record
	 * @return the set of information architecture audits
	 *
	 * precondition: id > 0
	 */
	public Set<Audit> getAllInformationArchitectureAuditsForDomainRecord(long id) {
		assert id > 0;

		return audit_record_repo.getAllInformationArchitectureAuditsForDomainRecord(id);
	}

	/**
	 * Get all accessibility audits for a domain record
	 *
	 * @param id the id of the domain record
	 * @return the set of accessibility audits
	 *
	 * precondition: id > 0
	 */
	public Set<Audit> getAllAccessibilityAuditsForDomainRecord(long id) {
		assert id > 0;

		return audit_record_repo.getAllAccessibilityAuditsForDomainRecord(id);
	}

	/**
	 * Get all aesthetic audits for a domain record
	 *
	 * @param id the id of the domain record
	 * @return the set of aesthetic audits
	 *
	 * precondition: id > 0
	 */
	public Set<Audit> getAllAestheticAuditsForDomainRecord(long id) {
		assert id > 0;

		return audit_record_repo.getAllAestheticsAuditsForDomainRecord(id);
	}

	/**
	 * Get all content audits for an audit record
	 *
	 * @param audit_record_id the id of the audit record
	 * @return the set of content audits
	 *
	 * precondition: audit_record_id > 0
	 */
	public Set<Audit> getAllContentAudits(long audit_record_id) {
		assert audit_record_id > 0;

		return audit_record_repo.getAllContentAudits(audit_record_id);
	}

	/**
	 * Get all information architecture audits for an audit record
	 *
	 * @param id the id of the audit record
	 * @return the set of information architecture audits
	 *
	 * precondition: id > 0
	 */
	public Set<Audit> getAllInformationArchitectureAudits(long id) {
		assert id > 0;

		return audit_record_repo.getAllInformationArchitectureAudits(id);
	}

	/**
	 * Get all accessibility audits for an audit record
	 *
	 * @param id the id of the audit record
	 * @return the set of accessibility audits
	 *
	 * precondition: id != null
	 * precondition: id > 0
	 */
	public Set<Audit> getAllAccessibilityAudits(Long id) {
		assert id != null;
		assert id > 0;

		return audit_record_repo.getAllAccessibilityAudits(id);
	}

	/**
	 * Get all aesthetic audits for an audit record
	 *
	 * @param id the id of the audit record
	 * @return the set of aesthetic audits
	 *
	 * precondition: id > 0
	 */
	public Set<Audit> getAllAestheticAudits(long id) {
		assert id > 0;

		return audit_record_repo.getAllAestheticsAudits(id);
	}

	/**
	 * Get the page state for an audit record
	 *
	 * @param page_audit_id the id of the page audit record
	 * @return the page state
	 *
	 * precondition: page_audit_id > 0
	 */
	public PageState getPageStateForAuditRecord(long page_audit_id) {
		assert page_audit_id > 0;

		return audit_record_repo.getPageStateForAuditRecord(page_audit_id);
	}

	/**
	 * Get all issues for an audit record
	 *
	 * @param audit_record_id the id of the audit record
	 * @return the set of issues
	 *
	 * precondition: audit_record_id > 0
	 */
	public Set<UXIssueMessage> getIssues(long audit_record_id) {
		assert audit_record_id > 0;

		return audit_record_repo.getIssues(audit_record_id);
	}

	/**
	 * Get all page states for a domain audit record
	 *
	 * @param audit_record_id the id of the audit record
	 * @return the set of page states
	 *
	 * precondition: audit_record_id > 0
	 */
	public Set<PageState> getPageStatesForDomainAuditRecord(long audit_record_id) {
		assert audit_record_id > 0;

		return audit_record_repo.getPageStatesForDomainAuditRecord(audit_record_id);
	}

	/**
	 * Add a page to an audit record
	 *
	 * @param audit_record_id the id of the audit record
	 * @param page_state_id the id of the page state
	 *
	 * precondition: audit_record_id > 0
	 * precondition: page_state_id > 0
	 */
	public void addPageToAuditRecord(long audit_record_id, long page_state_id) {
		assert audit_record_id > 0;
		assert page_state_id > 0;

		audit_record_repo.addPageToAuditRecord( audit_record_id, page_state_id );
	}

	/**
	 * Get the issue count by severity
	 *
	 * @param id the id of the audit record
	 * @param severity the severity of the issue
	 * @return the issue count
	 *
	 * precondition: id > 0
	 * precondition: severity != null
	 * precondition: !severity.isEmpty()
	 */
	public long getIssueCountBySeverity(long id, String severity) {
		assert id > 0;
		assert severity != null;
		assert !severity.isEmpty();

		return audit_record_repo.getIssueCountBySeverity(id, severity);
	}

	/**
	 * Get the page audit count for a domain audit record
	 *
	 * @param domain_audit_id the id of the domain audit record
	 * @return the page audit count
	 *
	 * precondition: domain_audit_id > 0
	 */
	public int getPageAuditCount(long domain_audit_id) {
		assert domain_audit_id > 0;

		return audit_record_repo.getPageAuditRecordCount(domain_audit_id);
	}

	/**
	 * Get all audits for an audit record
	 *
	 * @param id the id of the audit record
	 * @return the set of audits
	 *
	 * precondition: id > 0
	 */
	public Set<Audit> getAllAudits(long id) {
		assert id > 0;

		return audit_record_repo.getAllAudits(id);
	}

	/**
	 * Check if a domain audit is complete
	 *
	 * @param audit_record the audit record
	 * @return true if the domain audit is complete, false otherwise
	 *
	 * precondition: audit_record != null
	 */
	public boolean isDomainAuditComplete(AuditRecord audit_record) {
		assert audit_record != null;

		Set<PageAuditRecord> page_audits = audit_record_repo.getAllPageAudits(audit_record.getId());
		if(audit_record.getDataExtractionProgress() < 1.0) {
			return false;
		}
		//check all page audit records. If all are complete then the domain is also complete
		for(PageAuditRecord audit : page_audits) {
			if(!audit.isComplete()) {
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Get the domain audit record for a page record
	 *
	 * @param id the id of the page record
	 * @return the domain audit record
	 *
	 * precondition: id > 0
	 */
	public Optional<DomainAuditRecord> getDomainAuditRecordForPageRecord(long id) {
		assert id > 0;

		return audit_record_repo.getDomainForPageAuditRecord(id);
	}

	/**
	 * Get the labels for image elements
	 *
	 * @param id the id of the audit record
	 * @return the set of labels
	 *
	 * precondition: id > 0
	 */
	public Set<Label> getLabelsForImageElements(long id) {
		assert id > 0;

		return audit_record_repo.getLabelsForImageElements(id);
	}

	/**
	 * Get the design system for an audit record
	 *
	 * @param audit_record_id the id of the audit record
	 * @return the design system
	 *
	 * precondition: audit_record_id > 0
	 */
	public Optional<DesignSystem> getDesignSystem(long audit_record_id) {
		assert audit_record_id > 0;

		return audit_record_repo.getDesignSystem(audit_record_id);
	}

	/**
	 * Add a journey to an audit record
	 *
	 * @param audit_record_id the id of the audit record
	 * @param journey_id the id of the journey
	 * @return the audit record
	 *
	 * precondition: audit_record_id > 0
	 * precondition: journey_id > 0
	 */
	public AuditRecord addJourney(long audit_record_id, long journey_id) {
		assert audit_record_id > 0;
		assert journey_id > 0;

		return audit_record_repo.addJourney(audit_record_id, journey_id);
	}

	/**
	 * Updates the progress for the appropriate {@linkplain AuditCategory}
	 *
	 * @param auditRecordId the ID of the audit record to update
	 * @param category the category of the audit to update
	 * @param account_id the ID of the account
	 * @param domain_id the ID of the domain
	 * @param progress the progress to set
	 * @param message the message to set
	 *
	 * @return the updated audit record
	 *
	 * precondition: auditRecordId > 0
	 * precondition: category != null
	 * precondition: account_id > 0
	 * precondition: domain_id > 0
	 * precondition: message != null
	 */
	public AuditRecord updateAuditProgress(long auditRecordId,
											AuditCategory category,
											long account_id,
											long domain_id,
											double progress,
											String message)
	{
		assert auditRecordId > 0;
		assert category != null;
		assert account_id > 0;
		assert domain_id > 0;
		assert message != null;
		
		AuditRecord audit_record = findById(auditRecordId).get();
		audit_record.setDataExtractionProgress(1.0);
		audit_record.setStatus(ExecutionStatus.RUNNING_AUDITS);

		if(AuditCategory.CONTENT.equals(category)) {
			audit_record.setContentAuditProgress( progress );
		}
		else if(AuditCategory.AESTHETICS.equals(category)) {
			audit_record.setAestheticAuditProgress( progress);
		}
		else if(AuditCategory.INFORMATION_ARCHITECTURE.equals(category)) {
			audit_record.setInfoArchitectureAuditProgress( progress );
		}
		
		return save(audit_record, account_id, domain_id);
	}

	/**
	 * Retrieves {@link PageState} with given URL for {@link DomainAuditRecord}
	 *
	 * @param audit_record_id the {@linkplain DomainAuditRecord} id
	 * @param url the url
	 * @return the {@link PageState}
	 *
	 * precondition: audit_record_id > 0
	 * precondition: url != null
	 * precondition: !url.isEmpty()
	 */
	public PageState findPageWithUrl(long audit_record_id, String url) {
		assert audit_record_id > 0;
		assert url != null;
		assert !url.isEmpty();

		return audit_record_repo.findPageWithUrl(audit_record_id, url);
	}
	
	/**
	 * Add a domain map to an audit record
	 *
	 * @param domain_id the id of the domain
	 * @param domain_map_id the id of the domain map
	 *
	 * precondition: domain_id > 0
	 * precondition: domain_map_id > 0
	 */
	public void addDomainMap(long domain_id, long domain_map_id) {
		assert domain_id > 0;
		assert domain_map_id > 0;

		audit_record_repo.addDomainMap(domain_id, domain_map_id);
	}
	
	/**
	 * Find a page with a given key
	 *
	 * @param audit_record_id the id of the audit record
	 * @param key the key of the page
	 * @return the page state
	 *
	 * precondition: audit_record_id > 0
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public PageState findPageWithKey(long audit_record_id, String key) {
		assert audit_record_id > 0;
		assert key != null;
		assert !key.isEmpty();

		return page_state_service.findPageWithKey(audit_record_id, key);
	}

	/**
	 * Get the account for an audit record
	 *
	 * @param audit_record_id the id of the audit record
	 * @return the account
	 *
	 * precondition: audit_record_id > 0
	 */
	public Optional<Account> getAccount(long audit_record_id) {
		assert audit_record_id > 0;

		return account_repo.getAccount(audit_record_id);
	}

	/**
	 * Update the progress for all audit categories
	 *
	 * @param audit_record_id the id of the audit record
	 * @param content_progress the content audit progress
	 * @param info_architecture_progress the information architecture audit progress
	 * @param aesthetic_progress the aesthetic audit progress
	 * @param data_extraction_progress the data extraction progress
	 *
	 * precondition: audit_record_id > 0
	 */
	public void updateAuditProgress(long audit_record_id,
									double content_progress,
									double info_architecture_progress,
									double aesthetic_progress,
									double data_extraction_progress)
	{
		assert audit_record_id > 0;

		AuditRecord audit_record = findById(audit_record_id).get();
		audit_record.setDataExtractionProgress(data_extraction_progress);
		audit_record.setStatus(ExecutionStatus.RUNNING_AUDITS);

		audit_record.setContentAuditProgress( content_progress );
		audit_record.setAestheticAuditProgress( aesthetic_progress);
		audit_record.setInfoArchitectureAuditProgress( info_architecture_progress );
		
		audit_record_repo.updateProgress(audit_record_id, content_progress, info_architecture_progress, aesthetic_progress, data_extraction_progress);
	}

	/**
	 * Get the most recent audit record for a domain by id
	 *
	 * @param id the id of the domain
	 * @return the most recent audit record for the domain
	 *
	 * precondition: id > 0
	 */
	public Optional<AuditRecord> getMostRecentAuditRecordForDomain(long id) {
		assert id > 0;

		return audit_record_repo.getMostRecentAuditRecordForDomain(id);
	}

	/**
	 * Get the most recent audit record for a domain by host
	 *
	 * @param host the host of the domain
	 * @return the most recent audit record for the domain
	 * @deprecated Use {@link #getMostRecentAuditRecordForDomain(long)} instead
	 *
	 * precondition: host != null
	 * precondition: !host.isEmpty()
	 */
	@Deprecated
	public Optional<AuditRecord> getMostRecentAuditRecordForDomain(String host) {
		assert host != null;
		assert !host.isEmpty();
		
		return audit_record_repo.getMostRecentAuditRecordForDomain(host);
	}

	/**
	 * Get all audits for a domain audit
	 *
	 * @param domain_audit_record_id the id of the domain audit record
	 * @return the set of audits for the domain audit
	 *
	 * precondition: domain_audit_record_id > 0
	 */
	public Set<Audit> getAllAuditsForDomainAudit(long domain_audit_record_id) {
		assert domain_audit_record_id > 0;

		return audit_repo.getAllAuditsForDomainAudit(domain_audit_record_id);
	}

	/**
	 * Update the audit scores for an audit record
	 *
	 * @param audit_record_id the id of the audit record
	 * @param content_score the content score
	 * @param info_architecture_score the information architecture score
	 * @param aesthetic_score the aesthetic score
	 * @return true if the update was successful, false otherwise
	 *
	 * precondition: audit_record_id > 0
	 */
    public boolean updateAuditScores(long audit_record_id,
									double content_score,
									double info_architecture_score,
									double aesthetic_score) {
		assert audit_record_id > 0;

		try{
	audit_record_repo.updateScores(audit_record_id, content_score, info_architecture_score, aesthetic_score);
			return true;
		}catch(Exception e){
			return false;
		}
    }

	/**
	 * Get the number of journeys with a specific status for a domain audit
	 *
	 * @param domain_audit_id the id of the domain audit
	 * @param candidate the journey status to count
	 * @return the number of journeys with the specified status
	 *
	 * precondition: domain_audit_id > 0
	 * precondition: candidate != null
	 */
	public int getNumberOfJourneysWithStatus(long domain_audit_id, JourneyStatus candidate) {
		assert domain_audit_id > 0;
		assert candidate != null;

		return audit_record_repo.getNumberOfJourneysWithStatus(domain_audit_id, candidate.toString());
	}

	/**
	 * Get the total number of journeys for a domain audit
	 *
	 * @param domain_audit_id the id of the domain audit
	 * @return the total number of journeys
	 *
	 * precondition: domain_audit_id > 0
	 */
	public int getNumberOfJourneys(long domain_audit_id) {
		assert domain_audit_id > 0;

		return audit_record_repo.getNumberOfJourneys(domain_audit_id);
	}

	/**
	 * Find a page for an audit record
	 *
	 * @param audit_record_id the id of the audit record
	 * @return the page state
	 *
	 * precondition: audit_record_id > 0
	 */
	public PageState findPage(long audit_record_id) {
		assert audit_record_id > 0;

		return page_state_service.getPageStateForAuditRecord(audit_record_id);
	}
	
	/**
	 * Add {@link Audit} to {@link AuditRecord}
	 *
	 * @param audit_record_id the id of the audit record
	 * @param audit the audit to add
	 *
	 * precondition: audit_record_id > 0
	 * precondition: audit != null
	 */
	public void addAudit(long audit_record_id, Audit audit) {
		assert audit_record_id > 0;
		assert audit != null;

		//check if audit already exists for page state
		Optional<Audit> audit_opt = audit_repo.getAuditForAuditRecord(audit_record_id, audit.getKey());
		if(!audit_opt.isPresent()) {
			audit_record_repo.addAudit(audit_record_id, audit.getId());
		}
	}
	
	/**
	 * Retrieves {@link PageState} with given URL for {@link DomainAuditRecord}
	 *
	 * @param audit_record_id the id of the audit record
	 * @param page_id the id of the page
	 * @return the page state
	 *
	 * precondition: audit_record_id > 0
	 * precondition: page_id > 0
	 */
	public AuditRecord findPageWithId(long audit_record_id, long page_id) {
		assert audit_record_id > 0;
		assert page_id > 0;

		return audit_record_repo.findPageWithId(audit_record_id, page_id);
	}

	/**
	 * Checks if a page was already audited for a domain audit record
	 *
	 * @param domainAuditRecordId the id of the domain audit record
	 * @param pageId the id of the page
	 * @return true if the page was already audited, false otherwise
	 *
	 * precondition: domainAuditRecordId > 0
	 * precondition: pageId > 0
	 */
	public boolean wasPageAlreadyAudited(long domainAuditRecordId, long pageId) {
		assert domainAuditRecordId > 0;
		assert pageId > 0;

		return audit_record_repo.wasPageAlreadyAudited(domainAuditRecordId, pageId) != null;
	}

	/**
	 * Checks if a page was already audited for a page audit record
	 *
	 * @param pageAuditRecordId the id of the page audit record
	 * @param pageId the id of the page
	 * @return true if the page was already audited, false otherwise
	 *
	 * precondition: pageAuditRecordId > 0
	 * precondition: pageId > 0
	 */
	public boolean wasSinglePageAlreadyAudited(long pageAuditRecordId, long pageId) {
		assert pageAuditRecordId > 0;
		assert pageId > 0;

		return audit_record_repo.wasSinglePageAlreadyAudited(pageAuditRecordId, pageId) != null;
	}

	/**
	 * Get the number of journeys without a specific status for a domain audit
	 *
	 * @param domain_audit_id the id of the domain audit
	 * @param status the status of the journey
	 * @return the number of journeys without the specified status
	 *
	 * precondition: domain_audit_id > 0
	 * precondition: status != null
	 */
	public int getNumberOfJourneysWithoutStatus(long domain_audit_id, JourneyStatus status) {
		assert domain_audit_id > 0;
		assert status != null;

		return audit_record_repo.getNumberOfJourneysWithoutStatus(domain_audit_id, status.toString());
	}

	/**
	 * Find all audit records for an account
	 * 
	 * @param acct_id id of the account
	 * @return {@link List} of {@link AuditRecord}s
	 * 
	 * precondition: acct_id > 0
	 */
    public List<AuditRecord> findByAccountId(long acct_id) {
		assert acct_id > 0;

		return audit_record_repo.findAuditRecordByAccountId(acct_id);
    }

	/**
	 * Convert list of {@link AuditRecord audit_records} to list of {@link AuditRecordDto}
	 * 
	 * @param audits_records {@link List} of {@link AuditRecord}s to convert
	 * @return {@link List} of {@link AuditRecordDto}s
	 * 
	 * precondition: audits_records != null
	 */
    public List<AuditRecordDto> buildAudits(List<AuditRecord> audits_records) {
		assert audits_records != null;

		List<AuditRecordDto> auditDtoList = new ArrayList<>();
		for(AuditRecord audit_record: audits_records){
			auditDtoList.add(buildAudit(audit_record));
		}

		return auditDtoList;
	}

	/**
	 * Convert list of {@link AuditRecord audit_records} to list of {@link AuditRecordDto}
	 * 
	 * @param audit_record {@link AuditRecord} to convert
	 * @return {@link AuditRecordDto}
	 * 
	 * precondition: audit_record != null
	 */
    public AuditRecordDto buildAudit(AuditRecord audit_record) {
		assert audit_record != null;

		Set<Audit> audits = new HashSet<>();
		if(audit_record instanceof DomainAuditRecord){
			audits = getAllAuditsForDomainAudit(audit_record.getId());
		}
		else if(audit_record instanceof PageAuditRecord){
			audits = getAllAudits(audit_record.getId());
		}

		double content_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.CONTENT);
		double info_architecture_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.INFORMATION_ARCHITECTURE);
		double visual_design_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.AESTHETICS);

		//log.warn("audits found = "+audits.size());
		//log.warn("content score = "+content_score);

		AuditRecordDto audit_dto = new AuditRecordDto( audit_record.getId(),
													audit_record.getStatus(),
													audit_record.getLevel(),
													audit_record.getStartTime(),
													visual_design_score,
													content_score,
													info_architecture_score,
													audit_record.getCreatedAt(),
													audit_record.getEndTime(),
													audit_record.getUrl());

		return audit_dto;
	}
}
