package com.looksee.pageBuilder.models.journeys;

import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.looksee.pageBuilder.models.enums.JourneyStatus;
import com.looksee.pageBuilder.models.journeys.Step;
import com.looksee.pageBuilder.models.LookseeObject;
import com.looksee.pageBuilder.models.PageState;
import com.looksee.pageBuilder.models.enums.JourneyStatus;
import com.looksee.pageBuilder.models.enums.StepType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SimpleStep.class, name = "SIMPLE"),
    @JsonSubTypes.Type(value = LoginStep.class, name = "LOGIN"),
    @JsonSubTypes.Type(value = LandingStep.class, name = "LANDING") 
})
public abstract class Step extends LookseeObject{
	@Relationship(type = "STARTS_WITH", direction = Direction.OUTGOING)
	private PageState startPage;
	
	@Relationship(type = "ENDS_WITH", direction = Direction.OUTGOING)
	private PageState endPage;
	
	private String candidate_key;
	private String status;

	public JourneyStatus getStatus() {
		return JourneyStatus.create(status);
	}

	public void setStatus(JourneyStatus status) {
		this.status = status.toString();
	}
	
	abstract StepType getStepType();
	
	public PageState getStartPage() {
		return startPage;
	}

	public void setStartPage(PageState page_state) {
		this.startPage = page_state;
	}

	public PageState getEndPage() {
		return endPage;
	}

	public void setEndPage(PageState page_state) {
		this.endPage = page_state;
	}
	
	public String getCandidateKey() {
		return candidate_key;
	}

	public void setCandidateKey(String candidate_key) {
		this.candidate_key = candidate_key;
	}

	public abstract String generateCandidateKey();
	
	/**
	 * Perform deep clone of object
	 */
	public abstract Step clone();
}
