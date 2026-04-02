package com.looksee.journeyMapCleanup.models.journeys;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.neo4j.core.schema.Node;

import com.looksee.journeyMapCleanup.models.LookseeObject;
import com.looksee.journeyMapCleanup.models.enums.JourneyStatus;

import lombok.Getter;
import lombok.Setter;


/**
 * Represents the series of steps taken for an end to end journey
 */
@Node
public class Journey extends LookseeObject {

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
		setOrderedIds(new ArrayList<>());
		setKey(generateKey());
	}
	
	public Journey(JourneyStatus status) {
		super();
		setStatus(status);
		if(JourneyStatus.CANDIDATE.equals(status)) {
			setKey(getCandidateKey());
		}
		else {
			setKey(generateKey());
		}
	}
	
	public Journey(
				   List<Long> ordered_ids, 
				   JourneyStatus status) {
		super();
		setOrderedIds(ordered_ids);
		setStatus(status);
		if(JourneyStatus.CANDIDATE.equals(status)) {
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
		return "journey"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(StringUtils.join(getOrderedIds(), "|"));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Journey clone() {
		return new Journey(new ArrayList<>(getOrderedIds()), 
						   getStatus());
	}
}
