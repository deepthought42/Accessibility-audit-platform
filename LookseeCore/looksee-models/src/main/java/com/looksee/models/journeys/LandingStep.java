package com.looksee.models.journeys;

import org.springframework.data.neo4j.core.schema.Node;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.looksee.models.PageState;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.enums.StepType;

/**
 * A LandingStep is a step that is the first step in a journey.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>startPage is never null when constructed via the parameterized constructor.</li>
 *   <li>status is never null when constructed via the parameterized constructor.</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("LANDING")
@Node
public class LandingStep extends Step {
	
	/**
	 * Creates a new LandingStep
	 */
	public LandingStep() {
		super();
	}
	
	/**
	 * Creates a new LandingStep with the given start page and status
	 * @param start_page the start page of the landing step
	 * @param status the status of the landing step
	 * @throws IllegalArgumentException if the start page or status is null
	 *
	 * precondition: start_page != null
	 * precondition: status != null
	 */
	public LandingStep(PageState start_page, JourneyStatus status)
	{
		assert start_page != null;
		assert status != null;

		setStartPage(start_page);
		setStatus(status);
		if(JourneyStatus.CANDIDATE.equals(status)) {
			setCandidateKey(generateCandidateKey());
		}
		setKey(generateKey());
	}

	/**
	 * Clones the landing step
	 * @return the cloned landing step
	 */
	@Override
	public LandingStep clone() {
		return new LandingStep(getStartPage(), getStatus());
	}
	
	/**
	 * Generates a key for the landing step
	 * @return the key for the landing step
	 */
	@Override
	public String generateKey() {
		return "landingstep"+getStartPage().getId();
	}

	/**
	 * Generates a candidate key for the landing step
	 * @return the candidate key for the landing step
	 */
	@Override
	public String generateCandidateKey() {
		return generateKey();
	}
	
	/**
	 * Returns a string representation of the landing step
	 * @return a string representation of the landing step
	 */
	@Override
	public String toString() {
		return "key = "+getKey()+",\n start_page = "+getStartPage();
	}

	/**
	 * Returns the type of the landing step
	 * @return the type of the landing step
	 */
	@Override
	StepType getStepType() {
		return StepType.LANDING;
	}
}
