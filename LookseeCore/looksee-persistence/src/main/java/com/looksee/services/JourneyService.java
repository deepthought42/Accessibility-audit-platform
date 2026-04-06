package com.looksee.services;

import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.journeys.Journey;
import com.looksee.models.journeys.Step;
import com.looksee.models.repository.JourneyRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Journey service contains business logic for interacting with and managing 
 * journeys
 */
@NoArgsConstructor
@Service
public class JourneyService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(JourneyService.class.getName());

	@Autowired
	private JourneyRepository journey_repo;
	
	/**
	 * Save a journey
	 *
	 * @param journey the journey to save
	 * @return the saved journey
	 *
	 * precondition: journey != null
	 */
	public Journey save(Journey journey) {
		assert journey != null;

		Journey journey_record = journey_repo.findByKey(journey.getKey());
		if(journey_record == null) {
			journey_record = journey_repo.findByCandidateKey(journey.getCandidateKey());
			if(journey_record == null) {
				journey_record = journey_repo.save(journey);
			}
			else {
				journey_record.setKey(journey.getKey());
				journey_repo.save(journey_record);
			}
		}
		return journey_record;
	}

	/**
	 * Find a journey by id
	 *
	 * @param id the id of the journey
	 * @return the journey
	 *
	 * precondition: id > 0
	 */
	public Optional<Journey> findById(long id) {
		assert id > 0;
		return journey_repo.findById(id);
	}
	
	/**
	 * Find a journey by key
	 *
	 * @param key the key of the journey
	 * @return the journey
	 *
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public Journey findByKey(String key) {
		assert key != null;
		assert !key.isEmpty();
		return journey_repo.findByKey(key);
	}

	/**
	 * Find a journey by candidate key
	 *
	 * @param candidateKey the candidate key of the journey
	 * @return the journey
	 *
	 * precondition: candidateKey != null
	 * precondition: !candidateKey.isEmpty()
	 */
	public Journey findByCandidateKey(String candidateKey) {
		assert candidateKey != null;
		assert !candidateKey.isEmpty();
		return journey_repo.findByCandidateKey(candidateKey);
	}
	
	/**
	 * Saves a journey
	 *
	 * @param domain_map_id the id of the domain map
	 * @param journey the journey to save
	 * @return the saved journey
	 *
	 * precondition: domain_map_id > 0
	 * precondition: journey != null
	 */
	@Retryable
	@Synchronized
	public Journey save(long domain_map_id, Journey journey) {
		assert domain_map_id > 0;
		assert journey != null;
		Journey journey_record = journey_repo.findByKeyOrCandidateKey(domain_map_id, journey.getKey(), journey.getCandidateKey());
		if(journey_record == null) {
			//journey_record = journey_repo.save(journey);
			journey_record = new Journey();
			journey_record.setOrderedIds(journey.getOrderedIds());
			journey_record.setStatus(journey.getStatus());
			journey_record.setKey(journey.generateKey());
			journey_record.setCandidateKey(journey.generateCandidateKey());
			journey_record = journey_repo.save(journey_record);
			journey_record.setSteps(journey.getSteps());

			for(Step step: journey.getSteps()){
				addStep(journey_record.getId(), step.getId());
			}
		}
		
		return journey_record;
	}
	
	/**
	 * Updates the status, key, and ordered_ids fields for {@link Journey} stored in database
	 * 
	 * @param journey_id the id of the journey
	 * @param status the status of the journey
	 * @param key the key of the journey
	 * @param ordered_ids the ordered ids of the journey
	 * 
	 * @return {@link Journey} after saving
	 *
	 * precondition: journey_id > 0
	 * precondition: status != null
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 * precondition: ordered_ids != null
	 */
	public Journey updateFields(long journey_id, JourneyStatus status, String key, List<Long> ordered_ids) {
		assert journey_id > 0;
		assert status != null;
		assert key != null;
		assert !key.isEmpty();
		assert ordered_ids != null;
		return journey_repo.updateFields(journey_id, status, key, ordered_ids);
	}

	/**
	 * Adds a step to a journey
	 *
	 * @param journey_id the id of the journey
	 * @param step_id the id of the step
	 * @return the journey
	 *
	 * precondition: journey_id > 0
	 * precondition: step_id > 0
	 */
	public Journey addStep(long journey_id, long step_id) {
		assert journey_id > 0;
		assert step_id > 0;
		return journey_repo.addStep(journey_id, step_id);
	}

	/**
	 * Finds a journey by the domain map id and key
	 *
	 * @param domain_map_id the id of the domain map
	 * @param key the key of the journey
	 * @return the journey
	 *
	 * precondition: domain_map_id > 0
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public Journey findByKey(long domain_map_id, String key) {
		assert domain_map_id > 0;
		assert key != null;
		assert !key.isEmpty();
		return journey_repo.findByKey(domain_map_id, key);
	}
	

	/**
	 * Updates the status of a journey
	 *
	 * @param journey_id the id of the journey
	 * @param status the status of the journey
	 * @return the updated journey
	 *
	 * precondition: journey_id > 0
	 * precondition: status != null
	 */
	@Synchronized
	@Retryable
	public Journey updateStatus(long journey_id, JourneyStatus status) {
		assert journey_id > 0;
		assert status != null;
		return journey_repo.updateStatus(journey_id, status.toString());
	}

	/**
	 * Gets all journeys for a domain map created within the last 30 minutes
	 *
	 * @param domain_map_id the ID of the domain map
	 * @return the set of journeys
	 *
	 * precondition: domain_map_id > 0
	 */
	@Retryable
	public Set<Journey> getDomainMapJourneys(long domain_map_id) {
		assert domain_map_id > 0;
		return journey_repo.getDomainMapJourneys(domain_map_id);
	}

	/**
	 * Changes journey status for journeys in a domain map
	 *
	 * @param map_id the ID of the domain map
	 * @param status the current status to match
	 * @param goal the target status to set
	 *
	 * precondition: map_id > 0
	 * precondition: status != null
	 * precondition: goal != null
	 */
	@Retryable
	public void changeJourneyStatus(long map_id, JourneyStatus status, JourneyStatus goal) {
		assert map_id > 0;
		assert status != null;
		assert goal != null;
		journey_repo.changeJourneyStatus(map_id, status.toString(), goal.toString());
	}
}
