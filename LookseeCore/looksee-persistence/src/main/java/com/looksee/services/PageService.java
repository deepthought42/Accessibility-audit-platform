package com.looksee.services;

import com.looksee.audits.performance.PerformanceInsight;
import com.looksee.models.Element;
import com.looksee.models.Page;
import com.looksee.models.PageState;
import com.looksee.models.repository.PageRepository;
import com.looksee.models.repository.PageStateRepository;
import com.looksee.models.repository.PerformanceInsightRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Methods for interacting with page object
 */
@Service
public class PageService {
	private static final Logger log = LoggerFactory.getLogger(PageService.class);
	
	@Autowired
	private PageRepository page_repo;
	
	@Autowired
	private PageStateRepository page_state_service;
	
	@Autowired
	private PerformanceInsightRepository performance_insight_repo;
	
	
	/**
	 * Saves {@link Page} to database
	 * 
	 * @param page {@link Page} to save
	 * @param user_id the user id
	 * @return {@link Page} object reference to database object
	 * 
	 * precondition: page != null;
	 * precondition: user_id != null;
	 * precondition: !user_id.isEmpty();
	 */
	public Page saveForUser(String user_id, Page page){
		assert page != null;
		assert user_id != null;
		assert !user_id.isEmpty();
		
		Page page_record = page_repo.findByKeyAndUser(user_id, page.getKey());
		if(page_record != null){
			page_record.setPageStates(page.getPageStates());
			return page_repo.save(page_record);
		}

		log.debug("Saving new page for user {} with key {}", user_id, page.getKey());
		return page_repo.save(page);
	}
	
	/**
	 * Saves {@link Page} to database
	 * 
	 * @param page {@link Page} to save
	 * 
	 * @return {@link Page} object reference to database object
	 * 
	 * precondition: page != null;
	 */
	public Page save(Page page){
		assert page != null;
		
		Page page_record = findByKey(page.getKey());
		if(page_record != null){
			page_record.setPageStates(page.getPageStates());
			return page_repo.save(page_record);
		}
		
		return page_repo.save(page);
	}
	
	/**
	 * Retrieve page from database using key and user ID
	 * 
	 * @param user_id user id of the user who owns the page
	 * @param key key of the {@link Page} to retrieve
	 * 
	 * @return {@link Page} record
	 * 
	 * precondition: key != null;
	 * precondition: !key.isEmpty();
	 * precondition: user_id != null
	 * precondition: !user_id.isEmpty()
	 */
	@Deprecated
	public Page findByKeyAndUser(String user_id, String key){
		assert key != null;
		assert !key.isEmpty();
		assert user_id != null;
		assert !user_id.isEmpty();
		
		return page_repo.findByKeyAndUser(user_id, key);
	}
	
	/**
	 * Retrieve page from database using key
	 * 
	 * @param key key of the {@link Page} to retrieve
	 * 
	 * @return {@link Page} record
	 * 
	 * precondition: key != null;
	 * precondition: !key.isEmpty();
	 */
	public Page findByKey( String key ){
		assert key != null;
		assert !key.isEmpty();
		
		return page_repo.findByKey(key);
	}

	/**
	 * Retrieve page from database using url
	 * 
	 * @param url url of the {@link Page} to retrieve
	 * 
	 * @return {@link Page} record
	 * 
	 * precondition: key != null;
	 * precondition: !key.isEmpty();
	 */
	public Page findByUrl( String url ){
		assert url != null;
		assert !url.isEmpty();
		
		return page_repo.findByUrl(url);
	}
	
	/**
	 * Adds a {@link PerformanceInsight} to a {@link Page} with a given key
	 *
	 * @param user_id user id of the user who owns the page
	 * @param domain_url domain url of the page
	 * @param page_key key of the {@link Page} to add the performance insight to
	 * @param performance_insight_key key of the {@link PerformanceInsight} to add
	 *
	 * @return true if the performance insight was added, false otherwise
	 *
	 * precondition: user_id != null
	 * precondition: !user_id.isEmpty()
	 * precondition: domain_url != null;
	 * precondition: !domain_url.isEmpty();
	 * precondition: page_key != null
	 * precondition: !page_key.isEmpty()
	 * precondition: performance_insight_key != null
	 * precondition: !performance_insight_key.isEmpty();
	 */
	public boolean addPerformanceInsight(String user_id, String domain_url, String page_key, String performance_insight_key) {
		assert user_id != null;
		assert !user_id.isEmpty();
		assert domain_url != null;
		assert !domain_url.isEmpty();
		assert page_key != null;
		assert !page_key.isEmpty();
		assert performance_insight_key != null;
		assert !performance_insight_key.isEmpty();
		
		//check if performance insight already exists for page
		PerformanceInsight performance_insight = page_repo.getPerformanceInsight(user_id, domain_url, page_key, performance_insight_key);
		if(performance_insight == null) {
			page_repo.addPerformanceInsight(user_id, domain_url, page_key, performance_insight_key);
			return true;
		}
		return false;
	}

	/**
	 * Retrieves a List of all {@link PerformanceInsight}s associated with a {@link Page} that has a given key
	 * 
	 * @param page_key key of the {@link Page} to retrieve insights for
	 * @return list of {@link PerformanceInsight}s
	 * 
	 * precondition: page_key != null
	 * precondition: !page_key.isEmpty()
	 */
	public List<PerformanceInsight> findAllInsights(String page_key) {
		assert page_key != null;
		assert !page_key.isEmpty();
		
		return page_repo.getAllPerformanceInsights(page_key);
	}
	
	/**
	 * Retrieves the latest {@link PerformanceInsight} for a {@link Page} with a given key
	 * 
	 * @param page_key key of the {@link Page} to retrieve the latest insight for
	 * @return latest {@link PerformanceInsight}
	 * 
	 * precondition: page_key != null
	 * precondition: !page_key.isEmpty()
	 */
	public PerformanceInsight findLatestInsight(String page_key) {
		assert page_key != null;
		assert !page_key.isEmpty();
		
		PerformanceInsight insight = page_repo.getLatestPerformanceInsight(page_key);
		if(insight == null) {
			return null;
		}
		insight.setAudits(performance_insight_repo.getAllAudits(page_key, insight.getKey()));
		return insight;
	}
	
	/**
	 * Adds a {@link PageState} to a {@link Page} with a given key
	 * 
	 * @param user_id the user id
	 * @param page_key key of the {@link Page} to add the page state to
	 * @param page_state {@link PageState} to add
	 * 
	 * precondition: page_key != null
	 * precondition: !page_key.isEmpty()
	 * precondition: page_state != null
	 */
	@Deprecated
	public void addPageState(String user_id, String page_key, PageState page_state) {
		assert user_id != null;
		assert !user_id.isEmpty();
		assert page_key != null;
		assert !page_key.isEmpty();
		assert page_state != null;
		
		Page page = page_repo.findByKeyAndUser(user_id, page_key);
		if(page == null) {
			log.warn("Unable to add page state {} to missing page {} for user {}", page_state.getKey(), page_key, user_id);
			return;
		}

		PageState page_state_record = page_state_service.findByKeyAndUsername(user_id, page_state.getKey());
		if(page_state_record == null) {
			page_state_record = page_state_service.save(page_state);
		}
		page.addPageState(page_state_record);
		page_repo.save(page);
	}
	
	/**
	 * Adds a {@link PageState} to a {@link Page} with a given key
	 * 
	 * @param page_key key of the {@link Page} to add the page state to
	 * @param page_state_id id of the {@link PageState} to add
	 * 
	 * precondition: page_key != null
	 * precondition: !page_key.isEmpty()
	 * precondition: page_state_id > 0
	 */
	public void addPageState(String page_key, long page_state_id) {
		assert page_key != null;
		assert !page_key.isEmpty();
		assert page_state_id > 0;
		
		Optional<PageState> page_state_record = page_repo.findPageStateForPage(page_key, page_state_id);
		if(!page_state_record.isPresent()) {
			page_repo.addPageState(page_key, page_state_id);
		}
	}

	/**
	 * Retrieves the most recent {@link PageState} for a {@link Page} with a given key
	 *
	 * @param key key of the {@link Page} to retrieve the most recent page state for
	 * @return most recent {@link PageState}
	 *
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public PageState getMostRecentPageState(String key) {
		assert key != null;
		assert !key.isEmpty();

		return page_repo.findMostRecentPageState(key);
	}

	/**
	 * Retrieves all {@link Element}s for a {@link Page} with a given key
	 *
	 * @param key key of the {@link Page} to retrieve the elements for
	 * @return list of {@link Element}s
	 *
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public List<Element> getElements(String key) {
		assert key != null;
		assert !key.isEmpty();

		return page_repo.getElements(key);
	}
}
