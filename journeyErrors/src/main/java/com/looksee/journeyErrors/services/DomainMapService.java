package com.looksee.journeyErrors.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.journeyErrors.models.journeys.DomainMap;
import com.looksee.journeyErrors.models.repository.DomainMapRepository;

import io.github.resilience4j.retry.annotation.Retry;

/**
 * Enables interacting with database for {@link InteractiveStep Steps}
 */
@Service
@Retry(name = "neoforj")
public class DomainMapService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainMapService.class);

	@Autowired
	private DomainMapRepository domain_map_repo;
	
	public DomainMap findByKey(String journey_map_key) {
		return domain_map_repo.findByKey(journey_map_key);
	}

	public DomainMap save(DomainMap domain_map) {
		assert domain_map != null;
		return domain_map_repo.save(domain_map);
	}

	public DomainMap findByDomainId(long domain_id) {
		return domain_map_repo.findByDomainId(domain_id);
	}

	public void addJourneyToDomainMap(long journey_id, long domain_map_id) {
		domain_map_repo.addJourneyToDomainMap(journey_id, domain_map_id);
	}

	public DomainMap findByDomainAuditId(long domain_audit_id) {
		return domain_map_repo.findByDomainAuditId(domain_audit_id);
	}
}
