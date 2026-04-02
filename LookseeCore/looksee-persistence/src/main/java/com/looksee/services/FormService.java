package com.looksee.services;

import com.looksee.models.Domain;
import com.looksee.models.Form;
import com.looksee.models.PageState;
import com.looksee.models.message.BugMessage;
import com.looksee.models.repository.BugMessageRepository;
import com.looksee.models.repository.ElementStateRepository;
import com.looksee.models.repository.FormRepository;
import com.looksee.models.repository.PageStateRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Form service
 */
@Service
public class FormService {
	private static Logger log = LoggerFactory.getLogger(FormService.class);

	@Autowired
	private FormRepository form_repo;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private BugMessageRepository bug_message_repo;
	
	@Autowired
	private ElementStateRepository element_state_repo;
	
	@Autowired
	private PageStateRepository page_state_repo;
	
	/**
	 * Gets the page state
	 *
	 * @param user_id the user ID
	 * @param url the URL
	 * @param form the form
	 * @return the page state
	 *
	 * precondition: user_id != null
	 * precondition: user_id is not empty
	 * precondition: url != null
	 * precondition: url is not empty
	 * precondition: form != null
	 */
	public PageState getPageState(String user_id, String url, Form form) {
		assert user_id != null;
		assert !user_id.isEmpty();
		assert url != null;
		assert !url.isEmpty();
		assert form != null;

		return page_state_repo.getPageState(user_id, url, form.getKey());
	}
	
	/**
	 * Finds a form by its key
	 *
	 * @param account_id the account ID
	 * @param url the URL
	 * @param key the key
	 * @return the form
	 *
	 * precondition: account_id > 0
	 * precondition: url != null
	 * precondition: url is not empty
	 * precondition: key != null
	 * precondition: key is not empty
	 */
	public Form findByKey(long account_id, String url, String key){
		assert account_id > 0;
		assert url != null;
		assert !url.isEmpty();
		assert key != null;
		assert !key.isEmpty();

		return form_repo.findByKeyForUserAndDomain(account_id, url, key);
	}

	/**
	 * Saves a form
	 *
	 * @param form the form
	 * @return the saved form
	 *
	 * precondition: form != null
	 */
	public Form save(Form form) {
		assert form != null;

		Form form_record = form_repo.findById(form.getId()).get();
		if(form_record == null){
			
			//List<Element> db_records = new ArrayList<Element>(form.getFormFields().size());
			//for(Element element : form.getFormFields()){
				//db_records.add(element_service.saveFormElement(element));
			//}
			
			//form.setFormFields(db_records);
			//form.setSubmitField(element_service.saveFormElement(form.getSubmitField()));
			//form.setFormTag(element_service.saveFormElement(form.getFormTag()));

			form_record = form_repo.save(form);
		}
		
		return form_record;
	}

	/**
	 * Finds a form by its ID
	 *
	 * @param user_id the user ID
	 * @param domain_id the domain ID
	 * @param form_id the form ID
	 * @return the form
	 *
	 * precondition: user_id != null
	 * precondition: user_id is not empty
	 * precondition: domain_id > 0
	 * precondition: form_id > 0
	 */
	public Form findById(String user_id, long domain_id, long form_id) {
		assert user_id != null;
		assert !user_id.isEmpty();
		assert domain_id > 0;
		assert form_id > 0;

		Optional<Form> opt_form = form_repo.findById(form_id);
		
		if(opt_form.isPresent()){
			Form form = opt_form.get();
			Optional<Domain> optional_domain = domain_service.findById(domain_id);
			log.info("Does the domain exist :: "+optional_domain.isPresent());
	    	if(optional_domain.isPresent()){
	    		Domain domain = optional_domain.get();
		    	
				form.setFormFields(element_state_repo.getElementStates(user_id, domain.getUrl(), form.getKey()));
				/*
				for(ElementState element : form.getFormFields()){
					element.setRules(element_service.getRules(user_id, element.getKey()));
				}
				*/
				form.setFormTag(element_state_repo.getFormElement(user_id, domain.getUrl(), form.getKey()));
				form.setSubmitField(element_state_repo.getSubmitElement(user_id, domain.getUrl(), form.getKey()));
				return form;
	    	}
	    	else {
	    		//throw domain not found exception
	    	}
		}
		return null;
	}
	
	
	/**
	 * Adds a bug message to a form
	 *
	 * @param form_id the form ID
	 * @param msg the bug message
	 * @return the form
	 *
	 * precondition: form_id > 0
	 * precondition: msg != null
	 */
	public Form addBugMessage(long form_id, BugMessage msg) {
		assert form_id > 0;
		assert msg != null;

		Optional<Form> opt_form = form_repo.findById(form_id);
		
		if(opt_form.isPresent()){
			boolean msg_exists = false;
			Form form = opt_form.get();
			//check if form has error message already
			for(BugMessage bug_msg : form.getBugMessages()) {
				if( bug_msg.equals(msg)) {
					msg_exists = true;
				}
			}
			if(!msg_exists) {
				BugMessage bug_msg = bug_message_repo.save(msg);
				form.addBugMessage(bug_msg);
				log.warn("form :: "+form.getBugMessages());
			}
			log.warn("form bug message size :: "+form.getBugMessages().size());
			log.warn("form name :: "+form.getName());
			log.warn("form memory id :: "+form.getMemoryId());
			log.warn("form fields :: "+form.getFormFields());
			log.warn("form tag  :: "+form.getFormTag());
			log.warn("form submit field :: "+form.getSubmitField());
			log.warn("form type :: "+form.getType());
			log.warn("form key :: "+form.getKey());
			log.warn("form repo   :: "+form_repo);
			
			return form_repo.save(form);
			
		}
		return null;
	}
	
	/**
	 * Removes a bug message from a form
	 *
	 * @param form_id the form ID
	 * @param msg the bug message
	 * @return the form
	 *
	 * precondition: form_id > 0
	 * precondition: msg != null
	 */
	public Form removeBugMessage(long form_id, BugMessage msg) {
		assert form_id > 0;
		assert msg != null;

		Optional<Form> opt_form = form_repo.findById(form_id);
		
		if(opt_form.isPresent()){
			Form form = opt_form.get();
			form.removeBugMessage(msg);
			
			return form_repo.save(form);
		}
		return null;
	}

	/**
	 * Clears the bug messages for a form
	 *
	 * @param user_id the user ID
	 * @param form_key the form key
	 * @return the form
	 *
	 * precondition: user_id != null
	 * precondition: user_id is not empty
	 * precondition: form_key != null
	 * precondition: form_key is not empty
	 */
	public Form clearBugMessages(String user_id, String form_key) {
		assert user_id != null;
		assert !user_id.isEmpty();
		assert form_key != null;
		assert !form_key.isEmpty();

		return form_repo.clearBugMessages(user_id, form_key);
	}
}
