package com.looksee.journeyMapCleanup.services;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.looksee.journeyMapCleanup.models.enums.JourneyStatus;
import com.looksee.journeyMapCleanup.models.journeys.Journey;
import com.looksee.journeyMapCleanup.models.repository.JourneyRepository;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.Synchronized;

@Service
@Retry(name="neoforj")
public class JourneyService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(JourneyService.class.getName());

	@Autowired
	private JourneyRepository journey_repo;
	
	public Optional<Journey> findById(long id) {
		return journey_repo.findById(id);
	}
	
	public Journey findByKey(long domain_map_id, String key) {
		return journey_repo.findByKey(domain_map_id, key);
	}
	
	/**
	 * 
	 * @param journey
	 * @return
	 */
	@Retryable
	public Journey save(long domain_map_id, Journey journey) {
		Journey journey_record = journey_repo.findByKey(domain_map_id, journey.getKey());
		if(journey_record == null) {
			journey_record = journey_repo.findByCandidateKey(domain_map_id, journey.getCandidateKey());
			if(journey_record == null) {
				journey_record = journey_repo.save(journey);
			}
			/*
			else {
				journey_record.setKey(journey.getKey());
				journey_record.setOrderedIds(journey.getOrderedIds());
				journey_record.setStatus(journey.getStatus());
				journey_repo.save(journey_record);
			}
			*/
		}
		/*
		else {
			journey_record.setKey(journey.getKey());
			journey_record.setOrderedIds(journey.getOrderedIds());
			journey_record.setStatus(journey.getStatus());
			journey_repo.save(journey_record);
		}
		*/
		
		return journey_record;
	}

	@Retryable
	public Journey updateFields(long journey_id, JourneyStatus status, String key, List<Long> ordered_ids) {
		return journey_repo.updateFields(journey_id, status.toString(), key, ordered_ids);
	}

	@Retryable
	public Journey addStep(long journey_id, long step_id) {
		return journey_repo.addStep(journey_id, step_id);
	}

	public Journey findByCandidateKey(long domain_map_id, String candidate_key) {
		return journey_repo.findByCandidateKey(domain_map_id, candidate_key);
	}

	@Synchronized
	@Retryable
	public Journey updateStatus(long journey_id, JourneyStatus status) {
		return journey_repo.updateStatus(journey_id, status.toString());
	}

	@Retryable
    public Set<Journey> getJourneysCreatedWithinLastHour(long domain_map_id) {
        return journey_repo.getJourneysCreatedWithinLastHour(domain_map_id);
    }

	@Retryable
    public void changeJourneyStatus(long map_id, JourneyStatus status, JourneyStatus goal) {
		journey_repo.changeJourneyStatus(map_id, status.toString(), goal.toString());
    }

	@Retryable
	public Set<Journey> getDomainMapJourneys(long id) {
		log.warn("looking up journeys for domain map = "+id);
		return journey_repo.getDomainMapJourneys(id);
	}
}
