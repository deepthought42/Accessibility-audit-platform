package com.looksee.services;


import com.looksee.models.journeys.DomainMap;
import com.looksee.models.repository.DomainMapRepository;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Contains business logic for interacting with and managing domain maps
 */
@NoArgsConstructor
@Service
@Retry(name = "neoforj")
public class DomainMapService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainMapService.class);

	@Autowired
	private DomainMapRepository domain_map_repo;
	
	/**
	 * Find a domain map by key
	 *
	 * @param journey_map_key the key of the domain map
	 * @return the domain map
	 *
	 * precondition: journey_map_key != null
	 * precondition: !journey_map_key.isEmpty()
	 */
	public DomainMap findByKey(String journey_map_key) {
		assert journey_map_key != null;
		assert !journey_map_key.isEmpty();
		return domain_map_repo.findByKey(journey_map_key);
	}

	/**
	 * Save a domain map
	 *
	 * @param domain_map the domain map to save
	 * @return the saved domain map
	 *
	 * precondition: domain_map != null
	 */
	public DomainMap save(DomainMap domain_map) {
		assert domain_map != null;
		return domain_map_repo.save(domain_map);
	}

	/**
	 * Find a domain map by domain id
	 *
	 * @param domain_id the id of the domain
	 * @return the domain map
	 *
	 * precondition: domain_id > 0
	 */
	public DomainMap findByDomainId(long domain_id) {
		assert domain_id > 0;
		return domain_map_repo.findByDomainId(domain_id);
	}

	/**
	 * Add a journey to a domain map
	 *
	 * @param journey_id the id of the journey
	 * @param domain_map_id the id of the domain map
	 *
	 * precondition: journey_id > 0
	 * precondition: domain_map_id > 0
	 */
	public void addJourneyToDomainMap(long journey_id, long domain_map_id) {
		assert journey_id > 0;
		assert domain_map_id > 0;
		domain_map_repo.addJourneyToDomainMap(journey_id, domain_map_id);
	}
	
	/**
	 * Find a domain map by domain audit id
	 *
	 * @param domain_audit_id the id of the domain audit
	 * @return the domain map
	 *
	 * precondition: domain_audit_id > 0
	 */
	public DomainMap findByDomainAuditId(long domain_audit_id) {
		assert domain_audit_id > 0;
		
		return domain_map_repo.findByDomainAuditId(domain_audit_id);
	}

	/**
	 * Add a page to a domain map
	 *
	 * @param domain_map_id the id of the domain map
	 * @param page_state_id the id of the page state
	 *
	 * precondition: domain_map_id > 0
	 * precondition: page_state_id > 0
	 */
    public void addPageToDomainMap(long domain_map_id, long page_state_id) {
		assert domain_map_id > 0;
		assert page_state_id > 0;
		
        domain_map_repo.addPageToDomainMap(domain_map_id, page_state_id);
    }
}
