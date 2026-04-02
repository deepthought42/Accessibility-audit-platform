package com.looksee.services;

import com.looksee.gcp.GoogleCloudStorage;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.Screenshot;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.ElementClassification;
import com.looksee.models.repository.AuditRecordRepository;
import com.looksee.models.repository.ElementStateRepository;
import com.looksee.models.repository.PageStateRepository;
import com.looksee.utils.HtmlGeneralizer;
import io.github.resilience4j.retry.annotation.Retry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Service layer object for interacting with {@link PageState} database layer
 */
@Service
@NoArgsConstructor
public class PageStateService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PageStateService.class.getName());
	
	@Autowired
	private PageStateRepository page_state_repo;

	@Autowired
	private ElementStateRepository element_state_repo;

	@Autowired
	private AuditRecordRepository audit_record_repo;

	@Autowired(required = false)
	private GoogleCloudStorage googleCloudStorage;

	/**
	 * Save a {@link PageState} object and its associated objects
	 * @param page_state the page state to save
	 * @return the saved page state
	 * @throws Exception if the page state is null
	 *
	 * precondition: page_state != null
	 */
	public PageState save(PageState page_state) throws Exception {
		assert page_state != null;

		PageState page_state_record = page_state_repo.findByKey(page_state.getKey());
		
		if(page_state_record == null) {
			log.warn("page state wasn't found in database. Saving new page state to neo4j");
			return page_state_repo.save(page_state);
		}

		return page_state_record;
	}
	
	/**
	 * Save a {@link PageState} object and its associated objects
	 * @param audit_record_id the id of the audit record
	 * @param page_state the page state to save
	 * @return the saved page state
	 * @throws Exception if the page state is null
	 *
	 * precondition: page_state != null
	 * precondition: audit_record_id > 0
	 */
	@Retry(name = "neoforj")
	public PageState save(long audit_record_id, PageState page_state) throws Exception {
		assert audit_record_id > 0;
		assert page_state != null;
		
		PageState page_state_record = page_state_repo.findPageWithKey(audit_record_id, page_state.getKey());
		if(page_state_record == null) {
			log.warn("page state wasn't found in database. Saving new page state to neo4j");
			return page_state_repo.save(page_state);
		}

		return page_state_record;
	}
	
	/**
	 * Find a page state by key
	 * @param page_key the key of the page state
	 * @return the page state
	 *
	 * precondition: page_key != null
	 * precondition: !page_key.isEmpty()
	 */
	public PageState findByKey(String page_key) {
		assert page_key != null;
		assert !page_key.isEmpty();

		PageState page_state = page_state_repo.findByKey(page_key);
		if(page_state != null){
			page_state.setElements(getElementStates(page_key));
		}
		return page_state;
	}
	
	/**
	 * Find a page state by screenshot checksum and page url
	 * @param user_id the user id
	 * @param url the url of the page
	 * @param screenshot_checksum the checksum of the screenshot
	 * @return the page state
	 *
	 * precondition: user_id != null
	 * precondition: url != null
	 * precondition: screenshot_checksum != null
	 * precondition: !user_id.isEmpty()
	 * precondition: !url.isEmpty()
	 * precondition: !screenshot_checksum.isEmpty()
	 */
	public List<PageState> findByScreenshotChecksumAndPageUrl(String user_id, String url, String screenshot_checksum){
		assert user_id != null;
		assert url != null;
		assert screenshot_checksum != null;
		assert !user_id.isEmpty();
		assert !url.isEmpty();
		assert !screenshot_checksum.isEmpty();
		
		return page_state_repo.findByScreenshotChecksumAndPageUrl(url, screenshot_checksum);
	}
	
	/**
	 * Find a page state by full page screenshot checksum
	 * @param screenshot_checksum the checksum of the screenshot
	 * @return the page state
	 *
	 * precondition: screenshot_checksum != null
	 * precondition: !screenshot_checksum.isEmpty()
	 */
	public List<PageState> findByFullPageScreenshotChecksum(String screenshot_checksum){
		assert screenshot_checksum != null;
		assert !screenshot_checksum.isEmpty();
		
		return page_state_repo.findByFullPageScreenshotChecksum(screenshot_checksum);
	}
	
	/**
	 * Find a page state by animation image checksum
	 * @param user_id the user id
	 * @param screenshot_checksum the checksum of the screenshot
	 * @return the page state
	 *
	 * precondition: user_id != null
	 * precondition: screenshot_checksum != null
	 * precondition: !user_id.isEmpty()
	 * precondition: !screenshot_checksum.isEmpty()
	 */
	public PageState findByAnimationImageChecksum(String user_id, String screenshot_checksum){
		assert user_id != null;
		assert screenshot_checksum != null;
		assert !user_id.isEmpty();
		assert !screenshot_checksum.isEmpty();
		
		return page_state_repo.findByAnimationImageChecksum(user_id, screenshot_checksum);
	}
	
	/**
	 * Get the element states for a page
	 * @param page_key the key of the page
	 * @return the element states
	 *
	 * precondition: page_key != null
	 * precondition: !page_key.isEmpty()
	 */
	public List<ElementState> getElementStates(String page_key){
		assert page_key != null;
		assert !page_key.isEmpty();
		
		return element_state_repo.getElementStates(page_key);
	}
	
	/**
	 * Get the element states for a page state
	 * @param page_state_id the id of the page state
	 * @return the element states
	 *
	 * precondition: page_state_id > 0
	 */
	public List<ElementState> getElementStates(long page_state_id){
		assert page_state_id > 0;
		
		return element_state_repo.getElementStates(page_state_id);
	}
	
	/**
	 * Get the link element states for a page state
	 * @param page_state_id the id of the page state
	 * @return the link element states
	 *
	 * precondition: page_state_id > 0
	 */
	public List<ElementState> getLinkElementStates(long page_state_id){
		assert page_state_id > 0;
		
		return element_state_repo.getLinkElementStates(page_state_id);
	}
	
	/**
	 * Get the screenshots for a page
	 * @param user_id the user id
	 * @param page_key the key of the page
	 * @return the screenshots
	 *
	 * precondition: user_id != null
	 * precondition: page_key != null
	 * precondition: !user_id.isEmpty()
	 * precondition: !page_key.isEmpty()
	 */
	public List<Screenshot> getScreenshots(String user_id, String page_key){
		assert user_id != null;
		assert page_key != null;
		assert !user_id.isEmpty();
		assert !page_key.isEmpty();
		
		List<Screenshot> screenshots = page_state_repo.getScreenshots(user_id, page_key);
		if(screenshots == null){
			return new ArrayList<Screenshot>();
		}
		return screenshots;
	}
	
	/**
	 * Find page states with form
	 * @param account_id the account id
	 * @param url the url of the page
	 * @param page_key the key of the page
	 * @return the page states
	 *
	 * precondition: account_id > 0
	 * precondition: url != null
	 * precondition: page_key != null
	 * precondition: !url.isEmpty()
	 * precondition: !page_key.isEmpty()
	 */
	public List<PageState> findPageStatesWithForm(long account_id, String url, String page_key) {
		assert account_id > 0;
		assert url != null;
		assert page_key != null;
		assert !url.isEmpty();
		assert !page_key.isEmpty();
		
		return page_state_repo.findPageStatesWithForm(account_id, url, page_key);
	}
	
	/**
	 * Get the expandable elements for a list of element states
	 * @param elements the element states
	 * @return the expandable elements
	 *
	 * precondition: elements != null
	 */
	public Collection<ElementState> getExpandableElements(List<ElementState> elements) {
		assert elements != null;

		List<ElementState> expandable_elements = new ArrayList<>();
		for(ElementState elem : elements) {
			if(ElementClassification.LEAF.equals(elem.getClassification())) {
				expandable_elements.add(elem);
			}
		}
		return expandable_elements;
	}
	
	/**
	 * Find page states by source checksum for a domain
	 * @param url the url of the page
	 * @param src_checksum the checksum of the source
	 * @return the page states
	 *
	 * precondition: url != null
	 * precondition: src_checksum != null
	 */
	public List<PageState> findBySourceChecksumForDomain(String url, String src_checksum) {
		assert url != null;
		assert src_checksum != null;
		
		return page_state_repo.findBySourceChecksumForDomain(url, src_checksum);
	}
	
	/**
	 * Get the audits for a page state
	 * @param page_state_key the key of the page state
	 * @return the audits
	 *
	 * precondition: page_state_key != null
	 * precondition: !page_state_key.isEmpty()
	 */
	public List<Audit> getAudits(String page_state_key){
		assert page_state_key != null;
		assert !page_state_key.isEmpty();
		
		return page_state_repo.getAudits(page_state_key);
	}

	/**
	 * Find an audit by subcategory and page state key
	 * @param subcategory the subcategory
	 * @param page_state_key the key of the page state
	 * @return the audit
	 *
	 * precondition: subcategory != null
	 * precondition: page_state_key != null
	 * precondition: !page_state_key.isEmpty()
	 */
	public Audit findAuditBySubCategory(AuditName subcategory, String page_state_key) {
		assert subcategory != null;
		assert page_state_key != null;
		assert !page_state_key.isEmpty();
		
		return page_state_repo.findAuditBySubCategory(subcategory.getShortName(), page_state_key);
	}
	
	
	/**
	 * Get the visible leaf elements for a page state
	 * @param page_state_key the key of the page state
	 * @return the visible leaf elements
	 *
	 * precondition: page_state_key != null
	 * precondition: !page_state_key.isEmpty()
	 */
	public List<ElementState> getVisibleLeafElements(String page_state_key) {
		assert page_state_key != null;
		assert !page_state_key.isEmpty();
		
		return element_state_repo.getVisibleLeafElements(page_state_key);
	}

	/**
	 * Find a page state by url
	 * @param url the url of the page
	 * @return the page state
	 *
	 * precondition: url != null
	 * precondition: !url.isEmpty()
	 */
	public PageState findByUrl(String url) {
		assert url != null;
		assert !url.isEmpty();

		return page_state_repo.findByUrl(url);
	}

	/**
	 * Add an element to a page
	 * @param page_id the id of the page
	 * @param element_id the id of the element
	 * @return true if {@link ElementState} is already connected to page. Otherwise, returns result of attempting to add element to page
	 *
	 * precondition: page_id > 0
	 * precondition: element_id > 0
	 */
	public boolean addElement(long page_id, long element_id) {
		assert page_id > 0;
		assert element_id > 0;
		
		if(getElementState(page_id, element_id).isPresent()) {
			return true;
		}
		
		return page_state_repo.addElement(page_id, element_id) != null;
	}

	/**
	 * Get the element state for a page and element
	 * @param page_id the id of the page
	 * @param element_id the id of the element
	 * @return the element state
	 *
	 * precondition: page_id > 0
	 * precondition: element_id > 0
	 */
	public Optional<ElementState> getElementState(long page_id, long element_id) {
		assert page_id > 0;
		assert element_id > 0;
		
		return element_state_repo.getElementState(page_id, element_id);
	}

	/**
	 * Find a page state by id
	 * @param page_id the id of the page
	 * @return the page state
	 *
	 * precondition: page_id > 0
	 */
	public Optional<PageState> findById(long page_id) {
		assert page_id > 0;
		
		return page_state_repo.findById(page_id);
	}

	/**
	 * Update the composite image url for a page state
	 * @param id the id of the page state
	 * @param composite_img_url the composite image url
	 *
	 * precondition: id > 0
	 * precondition: composite_img_url != null
	 */
	public void updateCompositeImageUrl(Long id, String composite_img_url) {
		assert id != null;
		assert composite_img_url != null;
		assert id > 0;
		
		page_state_repo.updateCompositeImageUrl(id, composite_img_url);
	}

	/**
	 * Add all elements to a page state
	 * @param page_state_id the id of the page state
	 * @param element_ids the ids of the elements
	 *
	 * precondition: page_state_id > 0
	 * precondition: element_ids != null
	 */
	public void addAllElements(long page_state_id, List<Long> element_ids) {
		assert page_state_id > 0;
		assert element_ids != null;
		
		page_state_repo.addAllElements(page_state_id, element_ids);
	}

	/**
	 * Find a page state by audit record id and key
	 * @param audit_record_id the id of the audit record
	 * @param key the key of the page state
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
		
		return page_state_repo.findPageWithKey(audit_record_id, key);
	}

	/**
	 * Get the page state for an audit record
	 * @param audit_record_id the id of the audit record
	 * @return the page state
	 *
	 * precondition: audit_record_id > 0
	 */
    public PageState getPageStateForAuditRecord(long audit_record_id) {
		assert audit_record_id > 0;

        return page_state_repo.getPageStateForAuditRecord(audit_record_id);
    }

	/**
	 * Retrieves an {@link PageAuditRecord} for the page with the given id
	 * @param id the id of the page audit record
	 * @return the page audit record
	 *
	 * precondition: id > 0
	 */
	public PageAuditRecord getAuditRecord(long id) {
		assert id > 0;

		return audit_record_repo.getAuditRecord(id);
	}

	/**
	 * Retrieves the number of element states for the page with the given id
	 * @param page_id the id of the page
	 * @return the number of element states
	 *
	 * precondition: page_id > 0
	 */
	public int getElementStateCount(long page_id) {
		assert page_id > 0;

		return element_state_repo.getElementStateCount(page_id);
	}

	/**
	 * Finds a page state by the domain map id and key
	 * @param domain_map_id the id of the domain map
	 * @param page_key the key of the page state
	 * @return the page state
	 *
	 * precondition: domain_map_id > 0
	 * precondition: page_key != null
	 * precondition: !page_key.isEmpty()
	 */
	public PageState findPageInDomainMap(long domain_map_id, String page_key) {
		assert domain_map_id > 0;
		assert page_key != null;
		assert !page_key.isEmpty();

		return page_state_repo.findByDomainMap(domain_map_id, page_key);
	}

	/**
	 * Finds a page state by the domain audit record id and page state id
	 * @param domainAuditRecordId the id of the domain audit record
	 * @param page_state_id the id of the page state
	 * @return the page state
	 *
	 * precondition: domainAuditRecordId > 0
	 * precondition: page_state_id > 0
	 */
	public PageState findByDomainAudit(long domainAuditRecordId, long page_state_id) {
		assert domainAuditRecordId > 0;
		assert page_state_id > 0;

		return page_state_repo.findByDomainAudit(domainAuditRecordId, page_state_id);
	}

	/**
	 * Finds a page state by the domain audit record id and current url
	 * @param domainAuditRecordId the id of the domain audit record
	 * @param current_url the current url
	 * @return the page state
	 *
	 * precondition: domainAuditRecordId > 0
	 * precondition: current_url != null
	 * precondition: !current_url.isEmpty()
	 */
	public PageState findByDomainAudit(long domainAuditRecordId, String current_url) {
		assert domainAuditRecordId > 0;
		assert current_url != null;
		assert !current_url.isEmpty();

		return page_state_repo.findByDomainAudit(domainAuditRecordId, current_url);
	}

    /**
     * Updates the element extraction complete status for a page state
     * @param page_id the id of the page state
     * @param is_complete the status of the element extraction
     * @throws Exception if the page state is not found
     *
     * precondition: page_id > 0
     */
	@Retryable
	@Synchronized
	public void updateElementExtractionCompleteStatus(Long page_id, boolean is_complete) throws Exception {
		assert page_id != null;
		assert page_id > 0;

		PageState page = page_state_repo.updateElementExtractionCompleteStatus(page_id, is_complete);
		if(page == null){
			throw new Exception("Page state with id = "+page_id+" was not found");
		}
	}

	/**
	 * Updates the element extraction complete status for a page state
	 * @param page_id the id of the page state
	 * @param is_complete the status of the element extraction
	 * @throws Exception if the page state is not found
	 *
	 * precondition: page_id > 0
	 */
	@Retryable
	@Synchronized
    public void updateInteractiveElementExtractionComplete(Long page_id, boolean is_complete) throws Exception {
		assert page_id != null;
		assert page_id > 0;

        PageState page = page_state_repo.updateInteractiveElementExtractionCompleteStatus(page_id, is_complete);
		if(page == null){
			throw new Exception("Page state with id = "+page_id+" was not found");
		}
    }

	/**
	 * Finds a page state by the audit record id
	 * @param pageAuditId the id of the audit record
	 * @return the page state
	 *
	 * precondition: pageAuditId > 0
	 */
    public PageState findByAuditRecordId(long pageAuditId) {
		assert pageAuditId > 0;
		
        return page_state_repo.findByAuditRecordId(pageAuditId);
    }

	/**
	 * Checks if a page is landable
	 * @param pageId the id of the page
	 * @return true if the page is landable, false otherwise
	 *
	 * precondition: pageId > 0
	 */
	public boolean isPageLandable(long pageId) {
		assert pageId > 0;

		return page_state_repo.isPageLandable(pageId) != null;
	}

	/**
	 * Resolves the page source content from cloud storage.
	 * If GoogleCloudStorage is not configured, returns the raw src field.
	 *
	 * @param pageState the page state whose source to resolve
	 * @return the HTML content
	 *
	 * precondition: pageState != null
	 */
	public String resolvePageSrc(PageState pageState) {
		assert pageState != null;

		String src = pageState.getSrc();
		if (src == null || src.isEmpty()) {
			return src;
		}
		if (googleCloudStorage != null) {
			return googleCloudStorage.getHtmlContent(src);
		}
		return src;
	}

	/**
	 * Uploads page source content to cloud storage and updates the page state's
	 * src field with the storage URL.
	 * If GoogleCloudStorage is not configured, sets the raw src directly.
	 *
	 * @param pageState the page state to update
	 * @param src the HTML source content
	 * @throws IOException if upload fails
	 *
	 * precondition: pageState != null
	 * precondition: src != null
	 */
	public void uploadAndSetPageSrc(PageState pageState, String src) throws IOException {
		assert pageState != null;
		assert src != null;

		if (googleCloudStorage != null && !src.isEmpty()) {
			String pathKey = HtmlGeneralizer.extractHost(pageState.getUrl()) + "/pages/" + pageState.getKey();
			pageState.setSrc(googleCloudStorage.uploadHtmlContent(src, pathKey));
		} else {
			pageState.setSrc(src);
		}
	}
}
