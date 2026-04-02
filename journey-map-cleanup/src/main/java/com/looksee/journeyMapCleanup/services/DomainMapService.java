package com.looksee.journeyMapCleanup.services;


import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.looksee.journeyMapCleanup.models.journeys.DomainMap;
import com.looksee.journeyMapCleanup.models.repository.DomainMapRepository;

/**
 * Enables interacting with database for {@link InteractiveStep Steps}
 */
@Service
public class DomainMapService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DomainMapService.class);

	@Autowired
	private DomainMapRepository domain_map_repo;
	
	@Retryable
    public Set<DomainMap> getAllMapsWithinLastDay(int number_of_days) {
		return domain_map_repo.getAllMapsWithinLastDay(number_of_days);
    }
}
