package com.looksee.services;

import com.looksee.models.ElementState;
import com.looksee.models.audit.messages.ColorContrastIssueMessage;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.enums.AuditName;
import com.looksee.models.repository.ColorContrastIssueMessageRepository;
import com.looksee.models.repository.UXIssueMessageRepository;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Contains business logic for interacting with and managing UX issue messages
 */
@Service
@Retry(name="neoforj")
public class UXIssueMessageService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(UXIssueMessageService.class);

	@Autowired
	private UXIssueMessageRepository issue_message_repo;
	
	@Autowired
	private ColorContrastIssueMessageRepository contrast_issue_message_repo;
	
	/**
	 * Save a {@link UXIssueMessage}
	 * @param ux_issue the {@link UXIssueMessage} to save
	 * @return the saved {@link UXIssueMessage}
	 * 
	 * precondition: ux_issue != null
	 */
	public UXIssueMessage save(UXIssueMessage ux_issue) {
		assert ux_issue != null;
		
		return issue_message_repo.save(ux_issue);
	}
	
	/**
	 * Save a {@link ColorContrastIssueMessage}
	 * @param ux_issue the {@link ColorContrastIssueMessage} to save
	 * @return the saved {@link ColorContrastIssueMessage}
	 * 
	 * precondition: ux_issue != null
	 */
	public ColorContrastIssueMessage saveColorContrast(ColorContrastIssueMessage ux_issue) {
		assert ux_issue != null;
		
		return contrast_issue_message_repo.save(ux_issue);
	}

	/**
	 * Find {@link UXIssueMessage} with a given key
	 * @param key used for identifying {@link UXIssueMessage}
	 *
	 * @return updated {@link UXIssueMessage} object
	 *
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public UXIssueMessage findByKey(String key) {
		assert key != null;
		assert !key.isEmpty();
		
		return issue_message_repo.findByKey(key);
	}
	
	/**
	 * Add recommendation string to observation with a given key
	 *
	 * @param key for finding observation to be updated
	 * @param recommendation to be added to observation
	 *
	 * @return updated UXIssueMessage record
	 *
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 * precondition: priority != null
	 * precondition: priority.isEmpty()
	 */
	public UXIssueMessage addRecommendation(String key, String recommendation) {
		assert key != null;
		assert !key.isEmpty();
		assert recommendation != null;
		assert !recommendation.isEmpty();
		
		UXIssueMessage observation = findByKey(key);
		return save(observation);
	}

	/**
	 * Update priority of observation with a given key
	 *
	 * @param key for finding observation to be updated
	 * @param priority to be set on observation
	 * @return updated UXIssueMessage record
	 *
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 * precondition: priority != null
	 * precondition: priority.isEmpty()
	 */
	public UXIssueMessage updatePriority(String key, String priority) {
		assert key != null;
		assert !key.isEmpty();
		assert priority != null;
		assert !priority.isEmpty();
		
		UXIssueMessage observation = findByKey(key);
    	return save(observation);
	}

	/**
	 * Get an element state by id
	 * @param id the id of the element state
	 * @return the element state
	 *
	 * precondition: id > 0
	 */
	public ElementState getElement(long id) {
		assert id > 0;
		
		return issue_message_repo.getElement(id);
	}

	/**
	 * Save all {@link UXIssueMessage}
	 * @param issue_messages the {@link UXIssueMessage} to save
	 * @return the saved {@link UXIssueMessage}
	 * 
	 * precondition: issue_messages != null
	 */
	public Iterable<UXIssueMessage> saveAll(List<UXIssueMessage> issue_messages) {
		assert issue_messages != null;
		
		return issue_message_repo.saveAll(issue_messages);
	}

	/**
	 * Get a good example for an issue
	 * @param issue_id the id of the issue
	 * @return the good example
	 *
	 * precondition: issue_id > 0
	 */
	public ElementState getGoodExample(long issue_id) {
		assert issue_id > 0;
		
		return issue_message_repo.getGoodExample(issue_id);
	}

	/**
	 * Add an element to an issue
	 * @param issue_id the id of the issue
	 * @param element_id the id of the element
	 *
	 * precondition: issue_id > 0
	 * precondition: element_id > 0
	 */
	public void addElement(long issue_id, long element_id) {
		assert issue_id > 0;
		assert element_id > 0;
		
		issue_message_repo.addElement(issue_id, element_id);
	}

	/**
	 * Add a page to an issue
	 * @param issue_id the id of the issue
	 * @param page_id the id of the page
	 *
	 * precondition: issue_id > 0
	 * precondition: page_id > 0
	 */
	public void addPage(long issue_id, long page_id) {
		assert issue_id > 0;
		assert page_id > 0;
		
		issue_message_repo.addPage(issue_id, page_id);
	}

	/**
	 * Find {@link UXIssueMessage} by audit name and element id
	 * @param audit_name the audit name
	 * @param element_id the id of the element
	 * @return the {@link UXIssueMessage}
	 *
	 * precondition: audit_name != null
	 * precondition: element_id > 0
	 */
	public Set<UXIssueMessage> findByNameForElement(AuditName audit_name, long element_id) {
		assert audit_name != null;
		assert element_id > 0;

		return issue_message_repo.findByNameForElement(audit_name, element_id);
	}

	/**
	 * Check if an audit has been executed for an element
	 * @param audit_name the audit name
	 * @param element_id the id of the element
	 * @return true if the audit has been executed, otherwise false
	 *
	 * precondition: audit_name != null
	 * precondition: element_id > 0
	 */
	public boolean hasAuditBeenExecuted(AuditName audit_name, long element_id) {
		assert audit_name != null;
		assert element_id > 0;

		int count = issue_message_repo.getNumberOfUXIssuesForElement(audit_name, element_id);
		return count > 0;
	}
}
