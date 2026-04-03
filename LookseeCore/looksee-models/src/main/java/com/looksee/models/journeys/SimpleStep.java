package com.looksee.models.journeys;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.Action;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.enums.StepType;

import lombok.Getter;
import lombok.Setter;

/**
 * A SimpleStep is a step that is used to perform an action on an element.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>action is never null.</li>
 *   <li>actionInput is never null.</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("SIMPLE")
@Getter
@Setter
@Node
public class SimpleStep extends Step {

	/**
	 * The element that is used to perform the action
	 */
	@Relationship(type = "HAS", direction = Direction.OUTGOING)
	private ElementState elementState;

	/**
	 * The action that is used to perform the action
	 */
	private String action;

	/**
	 * The input that is used to perform the action
	 */
	private String actionInput;

	/**
	 * Creates a new SimpleStep
	 */
	public SimpleStep() {
		super();
		setActionInput("");
		setAction(Action.UNKNOWN);
		setStatus(JourneyStatus.CANDIDATE);
	}

	/**
	 * Creates a new SimpleStep with the given action and input string
	 * @param action the action to perform
	 * @param input_string the input string to perform the action on
	 *
	 * precondition: action != null
	 * precondition: input_string != null
	 */
	public SimpleStep(Action action, String input_string) {
		super();
		assert action != null;
		assert input_string != null;
		setActionInput(input_string);
		setAction(action);
	}

	/**
	 * Creates a new SimpleStep with the given start page, element, action, input string, end page, and status
	 * @param start_page the start page
	 * @param element the element to perform the action on
	 * @param action the action to perform
	 * @param action_input the input string to perform the action on
	 * @param end_page the end page
	 * @param status the status of the step
	 *
	 * precondition: start_page != null
	 * precondition: action != null
	 * precondition: action_input != null
	 * precondition: status != null
	 */
	@JsonCreator
	public SimpleStep(@JsonProperty("startPage") PageState start_page,
						@JsonProperty("elementState") ElementState element,
						@JsonProperty("action") Action action,
						@JsonProperty("actionInput") String action_input,
						@JsonProperty("endPage") PageState end_page,
						@JsonProperty("status") JourneyStatus status)
	{
		assert start_page != null;
		assert action != null;
		assert action_input != null;
		assert status != null;
		setStartPage(start_page);
		setElementState(element);
		setAction(action);
		setActionInput(action_input);
		setEndPage(end_page);
		setStatus(status);
		setKey(generateKey());
		if(JourneyStatus.CANDIDATE.equals(status)) {
			setCandidateKey(generateCandidateKey());
		}
	}

	/**
	 * Clones the simple step
	 * @return the cloned simple step
	 */
	public Step clone() {
		return new SimpleStep(getStartPage(),
							getElementState(),
							getAction(),
							getActionInput(),
							getEndPage(),
							getStatus());
	}

	/**
	 * Returns the action that is used to perform the action
	 * @return the action that is used to perform the action
	 */
	public Action getAction() {
		return Action.create(action);
	}

	/**
	 * Sets the action that is used to perform the action
	 * @param action the action to perform
	 */
	public void setAction(Action action) {
		this.action = action.getShortName();
	}

	/**
	 * Generates a key for the simple step
	 * @return the key for the simple step
	 */
	@Override
	public String generateKey() {
		String key = "";
		if(this.getStartPage() != null) {
			key += this.getStartPage().getId();
		}
		
		if(this.elementState != null) {
			key += this.elementState.getId();
		}
		
		if(this.getEndPage() != null) {
			key += this.getEndPage().getId();
		}

		return "simplestep"+key+this.action+this.actionInput;
	}

	/**
	 * Generates a candidate key for the simple step
	 * @return the candidate key for the simple step
	 */
	@Override
	public String generateCandidateKey() {
		return generateKey();
	}

	/**
	 * Returns a string representation of the simple step
	 * @return a string representation of the simple step
	 */
	@Override
	public String toString() {
		return "key = "+getKey()+",\n start_page = "+getStartPage()+"\n element ="+getElementState()+"\n end page = "+getEndPage();
	}

	/**
	 * Returns the type of the simple step
	 * @return the type of the simple step
	 */
	@Override
	public StepType getStepType() {
		return StepType.SIMPLE;
	}
}
