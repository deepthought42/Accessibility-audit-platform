package com.looksee.pageBuilder.models.journeys;


import org.springframework.data.neo4j.core.schema.Node;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.looksee.pageBuilder.models.enums.JourneyStatus;
import com.looksee.pageBuilder.models.journeys.LandingStep;
import com.looksee.pageBuilder.models.ElementState;
import com.looksee.pageBuilder.models.PageState;
import com.looksee.pageBuilder.models.enums.Action;
import com.looksee.pageBuilder.models.enums.JourneyStatus;
import com.looksee.pageBuilder.models.enums.StepType;

/**
 * A Step is the increment of work that start with a {@link PageState} contians an {@link ElementState} 
 * 	 that has an {@link Action} performed on it and results in an end {@link PageState}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("LANDING")
@Node
public class LandingStep extends Step {
	
	public LandingStep() {
		super();
	}
	
	public LandingStep(PageState start_page, JourneyStatus status) 
	{
		setStartPage(start_page);
		setStatus(status);
		if(JourneyStatus.CANDIDATE.equals(status)) {
			setCandidateKey(generateCandidateKey());
		}
		setKey(generateKey());
	}

	@Override
	public LandingStep clone() {
		return new LandingStep(getStartPage(), getStatus());
	}
	
	@Override
	public String generateKey() {
		return "landingstep"+getStartPage().getId();
	}

	@Override
	public String generateCandidateKey() {
		return generateKey();
	}
	
	@Override
	public String toString() {
		return "key = "+getKey()+",\n start_page = "+getStartPage();
	}

	@Override
	StepType getStepType() {
		return StepType.LANDING;
	}
}
