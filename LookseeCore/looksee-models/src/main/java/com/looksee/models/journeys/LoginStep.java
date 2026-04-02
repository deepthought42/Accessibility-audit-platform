package com.looksee.models.journeys;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.TestUser;
import com.looksee.models.enums.Action;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.enums.StepType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A Step is the increment of work that start with a {@link PageState} contians an {@link ElementState}
 * 	 that has an {@link Action} performed on it and results in an end {@link PageState}.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>testUser is never null when constructed via the parameterized constructor.</li>
 *   <li>usernameElement is never null when constructed via the parameterized constructor.</li>
 *   <li>passwordElement is never null when constructed via the parameterized constructor.</li>
 *   <li>submitElement is never null when constructed via the parameterized constructor.</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("LOGIN")
@Getter
@Setter
@NoArgsConstructor
@Node
public class LoginStep extends Step{

	/**
	 * The test user that is used to login
	 */
	@Relationship(type = "USES")
	private TestUser testUser;
	
	/**
	 * The username element that is used to login
	 */
	@Relationship(type = "USERNAME_INPUT")
	private ElementState usernameElement;
	
	/**
	 * The password element that is used to login
	 */
	@Relationship(type = "PASSWORD_INPUT")
	private ElementState passwordElement;
	
	/**
	 * The submit element that is used to login
	 */
	@Relationship(type = "SUBMIT")
	private ElementState submitElement;
	
	/**
	 * Constructor for {@link LoginStep}
	 *
	 * @param start_page the start page
	 * @param end_page the end page
	 * @param username_element the username element
	 * @param password_element the password element
	 * @param submit_btn the submit button
	 * @param user the test user
	 * @param status the status of the journey
	 *
	 * precondition: start_page != null
	 * precondition: end_page != null
	 * precondition: username_element != null
	 * precondition: password_element != null
	 * precondition: submit_btn != null
	 * precondition: user != null
	 * precondition: status != null
	 */
	public LoginStep(PageState start_page,
					PageState end_page,
					ElementState username_element,
					ElementState password_element,
					ElementState submit_btn,
					TestUser user,
					JourneyStatus status)
	{
		assert start_page != null;
		assert end_page != null;
		assert username_element != null;
		assert password_element != null;
		assert submit_btn != null;
		assert user != null;
		assert status != null;
		setStartPage(start_page);
		setEndPage(end_page);
		setUsernameElement(username_element);
		setPasswordElement(password_element);
		setSubmitElement(submit_btn);
		setTestUser(user);
		setStatus(status);
		setKey(generateKey());
		if(JourneyStatus.CANDIDATE.equals(status)) {
			setCandidateKey(generateCandidateKey());
		}
	}

	/**
	 * Generates a key for the login step
	 * @return the key for the login step
	 */
	@Override
	public String generateKey() {
		String key = "";
		if(getStartPage() != null) {
			key += getStartPage().getId();
		}
		if(getEndPage() != null) {
			key += getEndPage().getId();
		}
		if(usernameElement != null) {
			key += usernameElement.getId();
		}
		if(passwordElement != null) {
			key += passwordElement.getId();
		}
		if(submitElement != null) {
			key += submitElement.getId();
		}
		if(testUser != null) {
			key += testUser.getId();
		}
		return "loginstep"+key;
	}
	
	/**
	 * Generates a candidate key for the login step
	 * @return the candidate key for the login step
	 */
	@Override
	public String generateCandidateKey() {
		return generateKey();
	}
	
	/**
	 * Returns a string representation of the login step
	 * @return a string representation of the login step
	 */
	@Override
	public String toString() {
		return "key = "+getKey()+",\n start_page = "+getStartPage()+"\n username element ="+getUsernameElement()+"\n password element ="+getPasswordElement()+"\n submit element = "+getSubmitElement()+"\n  end page = "+getEndPage();
	}
	
	/**
	 * Clones the login step
	 * @return the cloned login step
	 */
	@Override
	public LoginStep clone() {
		return new LoginStep(getStartPage(),
							getEndPage(),
							getUsernameElement(),
							getPasswordElement(),
							getSubmitElement(),
							getTestUser(),
							getStatus());
	}

	/**
	 * Returns the type of the login step
	 * @return the type of the login step
	 */
	@Override
	StepType getStepType() {
		return StepType.LOGIN;
	}
}
