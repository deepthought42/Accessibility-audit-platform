package com.looksee.frontEndBroadcaster.models.journeys;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.looksee.frontEndBroadcaster.models.LookseeObject;
import com.looksee.frontEndBroadcaster.models.enums.JourneyStatus;

import lombok.Getter;
import lombok.Setter;


/**
 * Represents the series of steps taken for an end to end journey
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Node
public class Journey extends LookseeObject {

	@Relationship(type = "HAS")

	@Getter
	@Setter
	private List<Step> steps;

	@Getter
	@Setter
	private List<Long> orderedIds;

	@Getter
	@Setter
	private String candidateKey;

	@Getter
	@Setter
	private JourneyStatus status;
	
	public Journey() {
		super();
		setSteps(new ArrayList<>());
		setOrderedIds(new ArrayList<>());
		setCandidateKey(generateCandidateKey());
		setKey(generateKey());
	}
	
	public Journey(List<Step> steps, JourneyStatus status) {
		super();
		setSteps(steps);
		setStatus(status);
		if(JourneyStatus.CANDIDATE.equals(status)) {
			setCandidateKey(generateCandidateKey());
			setKey(getCandidateKey());
		}
		else {
			setKey(generateKey());
		}
	}
	
	public Journey(List<Step> steps, 
				   List<Long> ordered_ids, 
				   JourneyStatus status) {
		super();
		setSteps(steps);
		setOrderedIds(ordered_ids);
		setStatus(status);
		if(JourneyStatus.CANDIDATE.equals(status)) {
			setCandidateKey(generateCandidateKey());
			setKey(getCandidateKey());
		}
		else {
			setKey(generateKey());
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		//return generateCandidateKey();//
		return "journey"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(StringUtils.join(getOrderedIds(), "|"));
	}

	/**
	 * generates a key using key values of each step in order
	 */
	public String generateCandidateKey() {
		List<String> ordered_keys = getSteps().stream()
								  		.map(step -> step.getKey())
								  		.collect(Collectors.toList());
		return "journey"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(StringUtils.join(ordered_keys, "|"));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Journey clone() {
		return new Journey(new ArrayList<>(getSteps()), 
						   new ArrayList<>(getOrderedIds()), 
						   getStatus());
	}
	
	public List<Step> getSteps() {
		return steps;
	}

	/**
	 * Sets {@link Step} sequence and updates ordered ID list
	 * @param steps
	 */
	public void setSteps(List<Step> steps) {
		this.steps = steps;
		
		List<Long> ordered_ids = steps.stream()
									  .map(step -> step.getId())
									  .collect(Collectors.toList());
		setOrderedIds(ordered_ids);
	}
}
