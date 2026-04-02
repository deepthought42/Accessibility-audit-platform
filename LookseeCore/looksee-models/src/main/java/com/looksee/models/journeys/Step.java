package com.looksee.models.journeys;

import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.looksee.models.ElementState;
import com.looksee.models.LookseeObject;
import com.looksee.models.PageState;
import com.looksee.models.enums.Action;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.enums.StepType;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * A Step is the increment of work that start with a {@link PageState} contians
 * an {@link ElementState} that has an {@link Action} performed on it and
 * results in an end {@link PageState}
 *
 * <p><b>Class Invariants:</b>
 * <ul>
 *   <li>startPage is never null</li>
 *   <li>endPage is never null</li>
 *   <li>candidate_key is never null</li>
 *   <li>status is never null</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SimpleStep.class, name = "SIMPLE"),
    @JsonSubTypes.Type(value = LoginStep.class, name = "LOGIN"),
    @JsonSubTypes.Type(value = LandingStep.class, name = "LANDING")
})
@NoArgsConstructor
public abstract class Step extends LookseeObject{
	/**
	 * The page that the step starts with
	 */
	@Getter
	@Setter
	@Relationship(type = "STARTS_WITH", direction = Direction.OUTGOING)
	private PageState startPage;

	/**
	 * The page that the step ends with
	 */
	@Getter
	@Setter
	@Relationship(type = "ENDS_WITH", direction = Direction.OUTGOING)
	private PageState endPage;
	
	/**
	 * The candidate key for the step
	 */
	@Getter
	@Setter
	private String candidateKey;
	
	/**
	 * The status of the step
	 */
	private String status;

	/**
	 * Returns the status of the step
	 * @return the status of the step
	 */
	public JourneyStatus getStatus() {
		return JourneyStatus.create(status);
	}

	/**
	 * Sets the status of the step
	 * @param status the status of the step
	 *
	 * precondition: status != null
	 */
	public void setStatus(JourneyStatus status) {
		assert status != null;

		this.status = status.toString();
	}
	
	/**
	 * Returns the type of the step
	 * @return the type of the step
	 */
	abstract StepType getStepType();

	/**
	 * Generates a candidate key for the step
	 * @return the candidate key for the step
	 */
	public abstract String generateCandidateKey();
	
	/**
	 * Perform deep clone of object
	 * @return the cloned step
	 */
	public abstract Step clone();
}
