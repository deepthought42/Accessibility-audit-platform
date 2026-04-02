package com.looksee.pageBuilder.models.journeys;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.looksee.pageBuilder.models.enums.JourneyStatus;
import com.looksee.pageBuilder.models.journeys.LoginStep;
import com.looksee.pageBuilder.models.ElementState;
import com.looksee.pageBuilder.models.PageState;
import com.looksee.pageBuilder.models.TestUser;
import com.looksee.pageBuilder.models.enums.Action;
import com.looksee.pageBuilder.models.enums.JourneyStatus;
import com.looksee.pageBuilder.models.enums.StepType;

/**
 * A Step is the increment of work that start with a {@link PageState} contians an {@link ElementState} 
 * 	 that has an {@link Action} performed on it and results in an end {@link PageState}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("LOGIN")
@Node
public class LoginStep extends Step{

	@Relationship(type = "USES")
	private TestUser testUser;
	
	@Relationship(type = "USERNAME_INPUT")
	private ElementState usernameElement;
	
	@Relationship(type = "PASSWORD_INPUT")
	private ElementState passwordElement;
	
	@Relationship(type = "SUBMIT")
	private ElementState submitElement;

	public LoginStep() {}
	
	public LoginStep(PageState start_page,
					 PageState end_page,
					 ElementState username_element,
					 ElementState password_element,
					 ElementState submit_btn,
					 TestUser user, 
					 JourneyStatus status) 
	{
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
	
	
	public ElementState getUsernameElement() {
		return usernameElement;
	}
	
	public void setUsernameElement(ElementState username_input) {
		this.usernameElement = username_input;
	}
	
	public ElementState getPasswordElement() {
		return passwordElement;
	}
	
	public void setPasswordElement(ElementState password_input) {
		this.passwordElement = password_input;
	}
	
	public TestUser getTestUser() {
		return testUser;
	}
	
	public void setTestUser(TestUser user) {
		this.testUser = user;
	}
	

	public ElementState getSubmitElement() {
		return submitElement;
	}

	public void setSubmitElement(ElementState submit_element) {
		this.submitElement = submit_element;
	}

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
	
	@Override
	public String generateCandidateKey() {
		return generateKey();
	}
	
	@Override
	public String toString() {
		return "key = "+getKey()+",\n start_page = "+getStartPage()+"\n username element ="+getUsernameElement()+"\n password element ="+getPasswordElement()+"\n submit element = "+getSubmitElement()+"\n  end page = "+getEndPage();
	}
	
	@Override
	public LoginStep clone() {
		return new LoginStep(getStartPage(), getEndPage(), getUsernameElement(), getPasswordElement(), getSubmitElement(), getTestUser(), getStatus());
	}

	@Override
	StepType getStepType() {
		return StepType.LOGIN;
	}
}
