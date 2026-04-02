package com.looksee.journeyErrors.services;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.journeyErrors.models.enums.JourneyStatus;
import com.looksee.journeyErrors.models.journeys.Journey;
import com.looksee.journeyErrors.models.repository.JourneyRepository;

@Service
public class JourneyService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(JourneyService.class.getName());

	@Autowired
	private JourneyRepository journey_repo;
	
	public Optional<Journey> findById(long id) {
		return journey_repo.findById(id);
	}
	
	public Journey findByKey(String key) {
		return journey_repo.findByKey(key);
	}
	
	/**
	 * 
	 * @param journey
	 * @return
	 */
	public Journey save(Journey journey) {
		Journey journey_record = journey_repo.findByKey(journey.getKey());
		if(journey_record == null) {
			journey_record = journey_repo.findByCandidateKey(journey.getCandidateKey());
			if(journey_record == null) {
				journey_record = journey_repo.save(journey);
			}
		}
		
		return journey_record;
	}

	public Journey findByCandidateKey(String candidateKey) {
		return journey_repo.findByCandidateKey(candidateKey);
	}

	public Journey updateFields(long journey_id, JourneyStatus status, String key, List<Long> ordered_ids) {
		return journey_repo.updateFields(journey_id, status, key, ordered_ids);
	}

	public Journey addStep(long journey_id, long step_id) {
		return journey_repo.addStep(journey_id, step_id);
	}

	public Journey findByCandidateKey(long domain_map_id, String candidate_key) {
		return journey_repo.findByCandidateKey(domain_map_id, candidate_key);
	}

	public Journey updateStatus(long journey_id, JourneyStatus status) {
		return journey_repo.updateStatus(journey_id, status.toString());
	}
	
}
