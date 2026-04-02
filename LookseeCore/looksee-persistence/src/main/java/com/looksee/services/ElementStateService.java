package com.looksee.services;

import com.looksee.exceptions.ExistingRuleException;
import com.looksee.models.Domain;
import com.looksee.models.Element;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.repository.ElementStateRepository;
import com.looksee.models.rules.Rule;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Service for managing element states in the database
 *
 * This service requires non-null element_repo and page_state_service dependencies.
 *
 * The service is responsible for:
 * - Providing CRUD operations for ElementState entities
 * - Managing relationships between ElementStates and PageStates
 * - Handling element state persistence and retrieval
 * - Enforcing data integrity constraints for element states
 */
@NoArgsConstructor
@Service
public class ElementStateService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ElementStateService.class);

	@Autowired
	private ElementStateRepository element_repo;

	@Autowired
	private PageStateService page_state_service;
	
	/**
	 * saves element state to database
	 * @param element element state
	 * @return saved record of element state
	 *
	 * precondition: element != null
	 */
	@Deprecated
	public ElementState save(ElementState element) {
		assert element != null;

		ElementState element_record = element_repo.findByKey(element.getKey());
		if(element_record == null) {
			return element_repo.save(element);
		}
		
		return element_record;
	}
	
	/**
	 * Save element state to database
	 *
	 * @param page_state_id page state id
	 * @param element element state
	 * @return saved element state
	 *
	 * precondition: page_state_id > 0
	 * precondition: element != null
	 */
	@Retryable
	public ElementState save(long page_state_id, ElementState element) {
		assert page_state_id > 0;
		assert element != null;

		ElementState element_record = element_repo.findByPageStateAndKey(page_state_id, element.getKey());
		if(element_record == null) {
			element_record = element_repo.save(element);
			element_repo.addElement(page_state_id, element_record.getId());
		}
		
		return element_record;
	}
	
	/**
	 * Save form element
	 *
	 * @param element element state
	 * @return saved element state
	 *
	 * precondition: element != null
	 */
	public ElementState saveFormElement(ElementState element){
		assert element != null;
		ElementState element_record = element_repo.findByKey(element.getKey());
		if(element_record == null){
			element_record = element_repo.save(element);
		}
		else{
			if(element.getScreenshotUrl() != null && !element.getScreenshotUrl().isEmpty()) {
				element_record.setScreenshotUrl(element.getScreenshotUrl());
				element_record.setXpath(element.getXpath());
	
				element_record = element_repo.save(element_record);
			}
		}
		return element_record;
	}

	/**
	 * Find element by key
	 *
	 * @param key element key
	 * @return element state
	 *
	 * precondition: key != null
	 * precondition: key is not empty
	 */
	public ElementState findByKey(String key){
		assert key != null;
		assert !key.isEmpty();

		return element_repo.findByKey(key);
	}

	/**
	 * Remove rule from form element
	 *
	 * @param user_id user id
	 * @param element_key element key
	 * @param rule_key rule key
	 *
	 * precondition: user_id != null
	 * precondition: user_id is not empty
	 * precondition: element_key != null
	 * precondition: element_key is not empty
	 * precondition: rule_key != null
	 */
	public void removeRule(String user_id, String element_key, String rule_key){
		assert user_id != null;
		assert !user_id.isEmpty();
		assert element_key != null;
		assert !element_key.isEmpty();
		assert rule_key != null;

		element_repo.removeRule(user_id, element_key, rule_key);
	}

	/**
	 * Find element by id
	 *
	 * @param id element id
	 * @return element state
	 *
	 * precondition: id > 0
	 */
	public ElementState findById(long id) {
		assert id > 0;
		return element_repo.findById(id).get();
	}

	/**
	 * Get rules for a given element
	 *
	 * @param user_id user id
	 * @param element_key element key
	 * @return set of rules
	 *
	 * precondition: user_id != null
	 * precondition: user_id is not empty
	 * precondition: element_key != null
	 * precondition: element_key is not empty
	 */
	public Set<Rule> getRules(String user_id, String element_key) {
		assert user_id != null;
		assert !user_id.isEmpty();
		assert element_key != null;
		assert !element_key.isEmpty();

		return element_repo.getRules(user_id, element_key);
	}

	/**
	 * Add rule to form element
	 *
	 * @param username username
	 * @param element_key element key
	 * @param rule rule
	 * @return set of rules
	 *
	 * precondition: username != null
	 * precondition: username is not empty
	 * precondition: element_key != null
	 * precondition: element_key is not empty
	 * precondition: rule != null
	 */
	public Set<Rule> addRuleToFormElement(String username, String element_key, Rule rule) {
		assert username != null;
		assert !username.isEmpty();
		assert element_key != null;
		assert !element_key.isEmpty();
		assert rule != null;

		//Check that rule doesn't already exist
		Rule rule_record = element_repo.getElementRule(username, element_key, rule.getKey());
		if(rule_record == null) {
			rule_record = element_repo.addRuleToFormElement(username, element_key, rule.getKey());
			return element_repo.getRules(username, element_key);
		}
		else {
			throw new ExistingRuleException(rule.getType().toString());
		}
	}

	/**
	 * Find element by outer HTML
	 *
	 * @param account_id account id
	 * @param snippet snippet of outer HTML
	 * @return element state
	 *
	 * precondition: account_id > 0
	 * precondition: snippet != null
	 * precondition: snippet is not empty
	 */
	public ElementState findByOuterHtml(long account_id, String snippet) {
		assert account_id > 0;
		assert snippet != null;
		assert !snippet.isEmpty();

		return element_repo.findByOuterHtml(account_id, snippet);
	}

	/**
	 * Clear bug messages for a given form element
	 *
	 * @param account_id account id
	 * @param form_key form key
	 *
	 * precondition: account_id > 0
	 * precondition: form_key != null
	 * precondition: form_key is not empty
	 */
	public void clearBugMessages(long account_id, String form_key) {
		assert account_id > 0;
		assert form_key != null;
		assert !form_key.isEmpty();

		element_repo.clearBugMessages(account_id, form_key);
	}

	/**
	 * Fetch elements that are the child of the given parent element for a
	 * given user
	 *
	 * @param user_id the id of the user
	 * @param element_key the key of the element
	 *
	 * @return the list of child elements
	 *
	 * precondition: user_id != null
	 * precondition: user_id is not empty
	 * precondition: element_key != null
	 * precondition: element_key is not empty
	 */
	public List<ElementState> getChildElementsForUser(String user_id,
														String element_key)
	{
		assert user_id != null;
		assert !user_id.isEmpty();
		assert element_key != null;
		assert !element_key.isEmpty();

		return element_repo.getChildElementsForUser(user_id, element_key);
	}
	
	/**
	 * Fetch elements that are the child of the given parent element for a given page
	 *
	 * @param page_key the key of the page
	 * @param xpath the xpath of the parent element
	 *
	 * @return the list of child elements
	 *
	 * precondition: page_key != null
	 * precondition: page_key is not empty
	 * precondition: xpath != null
	 * precondition: xpath is not empty
	 */
	public List<ElementState> getChildElements(String page_key, String xpath) {
		assert page_key != null;
		assert !page_key.isEmpty();
		assert xpath != null;
		assert !xpath.isEmpty();
		
		List<ElementState> element_states = page_state_service.getElementStates(page_key);
		
		// get elements that are the the child of the element state
		List<ElementState> child_element_states = new ArrayList<>();
		for(ElementState element : element_states) {
			if(!element.getXpath().contentEquals(xpath) && element.getXpath().contains(xpath)) {
				child_element_states.add(element);
			}
		}
		
		return child_element_states;
	}
	
	/**
	 * Fetch element that is the child of the given parent element for a given page
	 *
	 * @param parent_key the key of the parent element
	 * @param child_element_key the key of the child element
	 *
	 * @return the list of child elements
	 *
	 * precondition: parent_key != null
	 * precondition: parent_key is not empty
	 * precondition: child_element_key != null
	 * precondition: child_key is not empty
	 */
	public List<ElementState> getChildElementForParent(String parent_key,
													String child_element_key)
	{
		assert parent_key != null;
		assert child_element_key != null;
		assert !parent_key.isEmpty();
		assert !child_element_key.isEmpty();

		return element_repo.getChildElementForParent(parent_key, child_element_key);
	}

	/**
	 * gets parent element for given {@link Element} within the given {@link PageState}
	 * 
	 * @param page_state_key the key of the page state
	 * @param element_state_key the key of the element state
	 *
	 * @return the element state
	 *
	 * precondition: page_state_key != null
	 * precondition: page_state_key is not empty
	 * precondition: element_state_key != null
	 * precondition: element_state_key is not empty
	 */
	public ElementState getParentElement(String page_state_key,
											String element_state_key)
	{
		assert page_state_key != null;
		assert element_state_key != null;
		assert !page_state_key.isEmpty();
		assert !element_state_key.isEmpty();

		return element_repo.getParentElement(page_state_key, element_state_key);
	}
	
	/**
	 * Adds a child element to the parent element
	 *
	 * @param parent_element_key the key of the parent element
	 * @param child_element_key the key of the child element
	 *
	 * precondition: parent_element_key != null
	 * precondition: parent_element_key is not empty
	 * precondition: child_element_key != null
	 * precondition: child_element_key is not empty
	 */
	public void addChildElement(String parent_element_key,
								String child_element_key)
	{
		assert parent_element_key != null;
		assert child_element_key != null;
		assert !parent_element_key.isEmpty();
		assert !child_element_key.isEmpty();

		//check if element has child already
		if(getChildElementForParent(parent_element_key, child_element_key).isEmpty()) {
			element_repo.addChildElement(parent_element_key, child_element_key);
		}
	}

	/**
	 * Fetch element that is the parent of the given child element for a given page
	 *
	 * @param page_state_key the key of the page state
	 * @param child_key the key of the child element
	 *
	 * @return the element state
	 *
	 * precondition: page_state_key != null
	 * precondition: page_state_key is not empty
	 * precondition: child_key != null
	 * precondition: child_key is not empty
	 */
	public ElementState findByPageStateAndChild(String page_state_key,
												String child_key)
	{
		assert page_state_key != null;
		assert child_key != null;
		assert !page_state_key.isEmpty();
		assert !child_key.isEmpty();
		
		return element_repo.findByPageStateAndChild(page_state_key, child_key);
	}

	/**
	 * Fetch element that is the parent of the given child element for a given page
	 *
	 * @param page_state_key the key of the page state
	 * @param xpath the xpath of the child element
	 *
	 * @return the element state
	 *
	 * precondition: page_state_key != null
	 * precondition: page_state_key is not empty
	 * precondition: xpath != null
	 * precondition: xpath is not empty
	 */
	public ElementState findByPageStateAndXpath(String page_state_key,
												String xpath)
	{
		assert page_state_key != null;
		assert xpath != null;
		assert !page_state_key.isEmpty();
		assert !xpath.isEmpty();
		
		return element_repo.findByPageStateAndXpath(page_state_key, xpath);
	}

	/**
	 * Saves a list of element states to the database
	 *
	 * @param element_states the list of element states
	 * @param page_state_id the id of the page state
	 *
	 * @return the list of saved element states
	 *
	 * precondition: element_states != null
	 * precondition: page_state_id > 0
	 */
	public List<ElementState> saveAll(List<ElementState> element_states,
										long page_state_id)
	{
		assert element_states != null;
		assert page_state_id > 0;

		return element_states.parallelStream()
								.map(element -> save(page_state_id, element))
								.collect(Collectors.toList());
	}

	/**
	 * Returns subset of element keys that exist within the database
	 *
	 * @param page_state_id the id of the page state
	 * @return the list of existing keys
	 *
	 * precondition: page_state_id > 0
	 */
	public List<String> getAllExistingKeys(long page_state_id) {
		assert page_state_id > 0;

		return element_repo.getAllExistingKeys(page_state_id);
	}

	/**
	 * Returns a list of {@link ElementState} objects that exist within the database
	 *
	 * @param existing_keys the set of existing keys
	 * @return the list of element states
	 *
	 * precondition: existing_keys != null
	 */
	public List<ElementState> getElements(Set<String> existing_keys) {
		assert existing_keys != null;

		return element_repo.getElements(existing_keys);
	}

	/**
	 * Finds an {@link ElementState} by the domain audit id and key
	 *
	 * @param domain_audit_id the id of the domain audit
	 * @param element the element to find
	 * @return the element state
	 * @throws Exception if the element state is not found
	 *
	 * precondition: domain_audit_id > 0
	 * precondition: element != null
	 */
	public ElementState findByDomainAuditAndKey(long domain_audit_id, ElementState element) throws Exception {
		assert domain_audit_id > 0;
		assert element != null;

		return element_repo.findByDomainAuditAndKey(domain_audit_id, element.getKey());
	}
	
	/**
	 * Checks if an element exists in another page state with a lower scroll offset
	 *
	 * @param element the element to check
	 * @return true if the element exists in another page state with a lower scroll offset, false otherwise
	 *
	 * precondition: element != null
	 */
	@Deprecated
	public boolean doesElementExistInOtherPageStateWithLowerScrollOffset(Element element){
		assert element != null;
		return false;
	}

	/**
	 * Gets the parent element for a given element
	 *
	 * @param user_id the id of the user
	 * @param domain the domain
	 * @param page_key the key of the page
	 * @param element_state_key the key of the element
	 * @return the parent element
	 *
	 * precondition: user_id != null
	 * precondition: user_id is not empty
	 * precondition: domain != null
	 * precondition: page_key != null
	 * precondition: page_key is not empty
	 * precondition: element_state_key != null
	 * precondition: element_state_key is not empty
	 */
	@Deprecated
	public ElementState getParentElement(String user_id, Domain domain, String page_key, String element_state_key) {
		assert user_id != null;
		assert !user_id.isEmpty();
		assert domain != null;
		assert page_key != null;
		assert !page_key.isEmpty();
		assert element_state_key != null;
		assert !element_state_key.isEmpty();

		return element_repo.getParentElement(user_id, domain, page_key, element_state_key);
	}

	/**
	 * Retrieves all visible leaf elements for a given page state
	 *
	 * @param page_state_id the id of the page state
	 * @return a list of visible leaf elements
	 *
	 * precondition: page_state_id > 0
	 */
	public List<ElementState> getVisibleLeafElements(long page_state_id) {
		assert page_state_id > 0;

		return element_repo.getVisibleLeafElements(page_state_id);
	}
	
	/**
	 * Saves a list of element states to the database
	 *
	 * NOTE: This is best for a database with significant memory as the size of data can be difficult to process all at once
	 * on smaller machines
	 *
	 * @param element_states the list of element states to save
	 * @return {@link List} of {@link ElementState} ids
	 *
	 * precondition: element_states != null
	 */
	public List<ElementState> saveElements(List<ElementState> element_states) {
		assert element_states != null;

		return element_states.parallelStream()
									.map(element -> save(element))
									.collect(Collectors.toList());
	}

	/**
	 * Finds an {@link ElementState} by the domain map id and key
	 *
	 * @param domain_map_id the id of the domain map
	 * @param element the element to find
	 * @return the element state
	 * @throws Exception if the element state is null or not found in the database
	 *
	 * precondition: domain_map_id > 0
	 * precondition: element != null
	 */
	@Retryable
	public ElementState findByDomainMapAndKey(long domain_map_id, ElementState element) throws Exception {
		assert domain_map_id > 0;
		assert element != null;

		return element_repo.findByDomainMapAndKey(domain_map_id, element.getKey());
	}

	/**
	 * Finds an {@link ElementState} by the page id and css selector
	 *
	 * @param id the id of the page
	 * @param cssSelector the css selector of the element
	 * @return the element state
	 *
	 * precondition: id > 0
	 * precondition: cssSelector != null
	 * precondition: cssSelector is not empty
	 */
    public ElementState findByPageAndCssSelector(long id, String cssSelector) {
        assert id > 0;
        assert cssSelector != null;
        assert !cssSelector.isEmpty();

        return element_repo.findByPageAndCssSelector(id, cssSelector);
    }
}