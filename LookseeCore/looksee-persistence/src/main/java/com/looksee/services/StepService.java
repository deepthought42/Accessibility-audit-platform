package com.looksee.services;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.journeys.LandingStep;
import com.looksee.models.journeys.LoginStep;
import com.looksee.models.journeys.SimpleStep;
import com.looksee.models.journeys.Step;
import com.looksee.models.repository.LandingStepRepository;
import com.looksee.models.repository.LoginStepRepository;
import com.looksee.models.repository.PageStateRepository;
import com.looksee.models.repository.SimpleStepRepository;
import com.looksee.models.repository.StepRepository;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Contains business logic for interacting with and managing steps
 */
@Service
@Retry(name = "neoforj")
public class StepService {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(StepService.class);

	@Autowired
	private StepRepository step_repo;
	
	@Autowired
	private SimpleStepRepository simple_step_repo;

	@Autowired
	private LoginStepRepository login_step_repo;
	
	@Autowired
	private PageStateRepository page_state_repo;
	
	@Autowired
	private LandingStepRepository landing_step_repo;
	
	/**
	 * Find a step by key
	 * @param step_key the key of the step
	 * @return the step
	 *
	 * precondition: step_key != null
	 * precondition: !step_key.isEmpty()
	 */
	public Step findByKey(String step_key) {
		assert step_key != null;
		assert !step_key.isEmpty();

		return step_repo.findByKey(step_key);
	}

	/**
	 * Save a step
	 * @param step the step to save
	 * @return the saved step
	 *
	 * precondition: step != null
	 */
	public Step save(Step step) {
		assert step != null;
		
		if(step instanceof SimpleStep) {
			SimpleStep step_record = simple_step_repo.findByKey(step.getKey());
			SimpleStep simple_step = (SimpleStep)step;
			
			if(step_record != null) {
				step_record.setElementState(simple_step.getElementState());
				step_record.setStartPage(simple_step.getStartPage());
				step_record.setEndPage(simple_step.getEndPage());
				return step_record;
			}
			
			SimpleStep new_simple_step = new SimpleStep();
			new_simple_step.setAction(simple_step.getAction());
			new_simple_step.setActionInput(simple_step.getActionInput());
			new_simple_step.setStatus(simple_step.getStatus());
			new_simple_step.setCandidateKey(simple_step.getCandidateKey());
			new_simple_step.setKey(simple_step.generateKey());
			new_simple_step = simple_step_repo.save(new_simple_step);
			setStartPage(new_simple_step.getId(), simple_step.getStartPage().getId());
			new_simple_step.setStartPage(simple_step.getStartPage());
			
			if(simple_step.getEndPage() != null) {
				addEndPage(new_simple_step.getId(), simple_step.getEndPage().getId());
				new_simple_step.setEndPage(simple_step.getEndPage());
			}
			
			setElementState(new_simple_step.getId(), simple_step.getElementState().getId());
			new_simple_step.setElementState(simple_step.getElementState());
			
			return new_simple_step;
		}
		else if(step instanceof LoginStep) {
			LoginStep step_record = login_step_repo.findByKey(step.getKey());
			LoginStep login_step = (LoginStep)step;
			if(step_record != null) {
				step_record.setTestUser(login_step.getTestUser());
				step_record.setUsernameElement(login_step.getUsernameElement());
				step_record.setPasswordElement(login_step.getPasswordElement());
				step_record.setSubmitElement(login_step.getSubmitElement());
				step_record.setStartPage(login_step.getStartPage());
				step_record.setEndPage(login_step.getEndPage());

				return step_record;
			}
			
			
			LoginStep new_login_step = new LoginStep();
			new_login_step.setKey(login_step.generateKey());
			new_login_step.setCandidateKey(login_step.getCandidateKey());
			new_login_step.setStatus(login_step.getStatus());
			new_login_step = login_step_repo.save(new_login_step);
			
			setStartPage(new_login_step.getId(), login_step.getStartPage().getId());
			new_login_step.setStartPage(login_step.getStartPage());
			
			addEndPage(new_login_step.getId(), login_step.getEndPage().getId());
			new_login_step.setEndPage(login_step.getEndPage());
			new_login_step.setUsernameElement(login_step.getUsernameElement());
			new_login_step.setPasswordElement(login_step.getPasswordElement());
			new_login_step.setSubmitElement(login_step.getSubmitElement());
			new_login_step.setTestUser(login_step.getTestUser());

			return new_login_step;
		}
		else if(step instanceof LandingStep) {
			LandingStep landing_step_record = landing_step_repo.findByKey(step.getKey());
			
			if(landing_step_record != null) {
				landing_step_record.setStartPage(step.getStartPage());
				
				return landing_step_record;
			}
			else {
				LandingStep landing_step = (LandingStep)step;
				landing_step.setStatus(step.getStatus());
				landing_step.setKey(step.getKey());
				landing_step.setCandidateKey(step.getCandidateKey());
				
				Step saved_step = landing_step_repo.save(landing_step);
				setStartPage(saved_step.getId(), landing_step.getStartPage().getId());
				saved_step.setStartPage(landing_step.getStartPage());
				
				return saved_step;
			}
		}
		else {
			Step step_record = step_repo.findByKey(step.getKey());
			
			if(step_record != null) {
				step_record.setStartPage(step.getStartPage());
				step_record.setEndPage(step.getEndPage());
				
				return step_record;
			}
			else {
				Step saved_step = step_repo.save(step);
				//step_repo.addStartPage(saved_step.getId(), saved_step.getStartPage().getId());
				//step_repo.addEndPage(saved_step.getId(), saved_step.getEndPage().getId());
				saved_step.setStartPage(saved_step.getStartPage());
				saved_step.setEndPage(saved_step.getEndPage());
				
				return saved_step;
			}
		}
	}

	/**
	 * Checks if page state is listed as a the start page for a journey step
	 * 
	 * @param page_state the page state
	 * @param domain_map_id the id of the domain map
	 * @return the steps with start page
	 * 
	 * precondition: page_state != null
	 * precondition: page_state.getId() != null
	 * precondition: domain_map_id > 0
	 */
	public List<Step> getStepsWithStartPage(PageState page_state, long domain_map_id) {
		assert page_state != null;
		assert page_state.getId() != null;
				
		return step_repo.getStepsWithStartPage(domain_map_id, page_state.getId());
	}

	/**
	 * Get steps with start page
	 * @param domainAuditRecordId the id of the domain audit record
	 * @param page_state the page state
	 * @return the steps with start page
	 *
	 * precondition: domainAuditRecordId > 0
	 * precondition: page_state != null
	 * precondition: page_state.getKey() != null
	 * precondition: !page_state.getKey().isEmpty()
	 */
	public List<Step> getStepsWithStartPage(long domainAuditRecordId, PageState page_state) {
		assert domainAuditRecordId > 0;
		assert page_state != null;
		assert page_state.getKey() != null;
		assert !page_state.getKey().isEmpty();
		
		return step_repo.getStepsWithStartPage(domainAuditRecordId, page_state.getKey());
	}

	/**
	 * Get the end page for a step
	 * @param id the id of the step
	 * @return the end page
	 *
	 * precondition: id > 0
	 */
	public PageState getEndPage(long id) {
		assert id > 0;
		
		return page_state_repo.getEndPageForStep(id);
	}
	
	/**
	 * Set the element state for a step
	 * @param step_id the id of the step
	 * @param element_id the id of the element
	 *
	 * precondition: step_id > 0
	 * precondition: element_id > 0
	 */
	public void setElementState(long step_id, long element_id) {
		assert step_id > 0;
		assert element_id > 0;
		
		step_repo.setElementState(step_id, element_id);
	}
	
	/**
	 * Add an end page to a step
	 * @param step_id the id of the step
	 * @param page_id the id of the page
	 *
	 * precondition: step_id > 0
	 * precondition: page_id > 0
	 */
	public void addEndPage(long step_id, long page_id) {
		assert step_id > 0;
		assert page_id > 0;
		
		step_repo.addEndPage(step_id, page_id);
	}

	/**
	 * Update the key for a step
	 * @param step_id the id of the step
	 * @param key the key to update
	 * @return the updated step
	 *
	 * precondition: step_id > 0
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public Step updateKey(long step_id, String key) {
		assert step_id > 0;
		assert key != null;
		assert !key.isEmpty();
		
		return step_repo.updateKey(step_id, key);
	}

	/**
	 * Set the start page for a step
	 * @param step_id the id of the step
	 * @param page_id the id of the page
	 *
	 * precondition: step_id > 0
	 * precondition: page_id > 0
	 */
	public void setStartPage(Long step_id, Long page_id) {
		assert step_id != null;
		assert step_id > 0;
		assert page_id != null;
		assert page_id > 0;
		
		step_repo.setStartPage(step_id, page_id);
	}
	
	/**
	 * Get the element state for a step
	 * @param step_key the key of the step
	 * @return the element state
	 *
	 * precondition: step_key != null
	 * precondition: !step_key.isEmpty()
	 */
	public ElementState getElementState(String step_key) {
		assert step_key != null;
		assert !step_key.isEmpty();
		
		return step_repo.getElementState(step_key);
	}

	/**
	 * Checks if page state is listed as a the start page for a journey step
	 * 
	 * @param page_state the page state
	 * @return the steps with start page
	 *
	 * precondition: page_state != null
	 * precondition: page_state.getId() != null
	 */
	public List<Step> getStepsWithStartPage(PageState page_state) {
		assert page_state != null;
		assert page_state.getId() != null;
		
		return step_repo.getStepsWithStartPage(page_state.getId());
	}

	/**
	 * Retrieve single step with given candidate key
	 * @param candidate_key the candidate key
	 * @param domain_map_id the id of the domain map
	 * @return the step
	 *
	 * precondition: candidate_key != null
	 * precondition: !candidate_key.isEmpty()
	 * precondition: domain_map_id > 0
	 */
	public Step findByCandidateKey(String candidate_key, long domain_map_id) {
		assert candidate_key != null;
		assert !candidate_key.isEmpty();
		assert domain_map_id > 0;

		return step_repo.findByCandidateKey(candidate_key, domain_map_id);
	}
}