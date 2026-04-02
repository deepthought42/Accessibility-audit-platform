package com.looksee.frontEndBroadcaster.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.looksee.frontEndBroadcaster.models.Domain;
import com.looksee.frontEndBroadcaster.models.Element;
import com.looksee.frontEndBroadcaster.models.ElementState;
import com.looksee.frontEndBroadcaster.models.repository.ElementStateRepository;

import io.github.resilience4j.retry.annotation.Retry;

@Service
@Retry(name="neoforj")
public class ElementStateService {
	
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ElementStateService.class);

	@Autowired
	private ElementStateRepository element_repo;

	@Autowired
	private PageStateService page_state_service;
	
	/**
	 * saves element state to database
	 * 
	 * @param element
	 * @return saved record of element state
	 * 
	 * @pre element != null
	 */
	@Deprecated
	@Retryable
	public ElementState save(ElementState element) {
		assert element != null;

		ElementState element_record = element_repo.findByKey(element.getKey());
		if(element_record == null) {
			return element_repo.save(element);
		}
		
		return element_record;
	}
	
	/**
	 * saves element state to database
	 * 
	 * @param element
	 * @return saved record of element state
	 * 
	 * @pre element != null
	 */
	/*
	@Retry(name = "neoforj")
	public ElementState save(long page_state_id, ElementState element) {
		assert element != null;

		ElementState element_record = element_repo.findByPageStateAndKey(page_state_id, element.getKey());
		if(element_record == null) {
			element_record = element_repo.save(element);
			element_repo.addElement(page_state_id, element_record.getId());
		}
		
		return element_record;
	}
	*/
	
	/**
	 * saves element state to database
	 * 
	 * @param element
	 * @return saved record of element state
	 * 
	 * @pre element != null
	 */
	@Retryable
	public ElementState save(long domain_map_id, ElementState element) {
		assert element != null;

		ElementState element_record = element_repo.findByDomainMapAndKey(domain_map_id, element.getKey());
		if(element_record == null) {
			element_record = element_repo.save(element);
		}
		
		return element_record;
	}
	
	/**
	 * 
	 * @param element
	 * @return
	 * 
	 * @pre element != null
	 */
	public ElementState saveFormElement(ElementState element){
		assert element != null;
		ElementState element_record = element_repo.findByKey(element.getKey());
		if(element_record == null){
			element_record = element_repo.save(element);
		}
		/*
		else{
			if(element.getScreenshotUrl() != null && !element.getScreenshotUrl().isEmpty()) {
				element_record.setScreenshotUrl(element.getScreenshotUrl());
				element_record.setXpath(element.getXpath());
	
				element_record = element_repo.save(element_record);
			}
		}
		*/
		return element_record;
	}

	public ElementState findByKey(String key){
		return element_repo.findByKey(key);
	}
	
	public boolean doesElementExistInOtherPageStateWithLowerScrollOffset(Element element){
		return false;
	}

	public ElementState findById(long id) {
		return element_repo.findById(id).get();
	}

	public ElementState findByOuterHtml(long account_id, String snippet) {
		return element_repo.findByOuterHtml(account_id, snippet);
	}

	public List<ElementState> getChildElementsForUser(String user_id, String element_key) {
		return element_repo.getChildElementsForUser(user_id, element_key);
	}
	
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
	
	public List<ElementState> getChildElementForParent(String parent_key, String child_element_key) {
		return element_repo.getChildElementForParent(parent_key, child_element_key);
	}

	@Deprecated
	public ElementState getParentElement(String user_id, Domain domain, String page_key, String element_state_key) {
		return element_repo.getParentElement(user_id, domain, page_key, element_state_key);
	}

	/**
	 * Fetch element that is the parent of the given child element for a given page
	 * 
	 * @param page_state_key
	 * @param child_key
	 * 
	 * @return
	 * 
	 * @pre page_state_key != null
	 * @pre child_key != null
	 */
	public ElementState findByPageStateAndChild(String page_state_key, String child_key) {
		assert page_state_key != null;
		assert child_key != null;
		return element_repo.findByPageStateAndChild(page_state_key, child_key);
	}

	public ElementState findByPageStateAndXpath(String page_state_key, String xpath) {
		assert page_state_key != null;
		assert xpath != null;
		return element_repo.findByPageStateAndXpath(page_state_key, xpath);
	}

	/**
	 * Returns subset of element keys that exist within the database 
	 * 
	 * @param element_keys
	 * @return
	 */
	public List<String> getAllExistingKeys(long page_state_id) {
		return element_repo.getAllExistingKeys(page_state_id);
	}

	public List<ElementState> getElements(Set<String> existing_keys) {
		return element_repo.getElements(existing_keys);
	}
	
	public List<ElementState> getVisibleLeafElements(long page_state_id) {
		return element_repo.getVisibleLeafElements(page_state_id);
	}

	public ElementState findByDomainAuditAndKey(long domain_audit_id, ElementState element) throws Exception {
		return element_repo.findByDomainAuditAndKey(domain_audit_id, element.getKey());
	}

	public ElementState findByDomainMapAndKey(long domain_map_id, ElementState element) throws Exception {
		return element_repo.findByDomainMapAndKey(domain_map_id, element.getKey());
	}
}
