package com.looksee.models;

import com.looksee.models.enums.FormType;
import com.looksee.models.message.BugMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Represents a form tag and its associated input elements in a web browser.
 *
 * <p><b>Class Invariants:</b>
 * <ul>
 *   <li>formTag is never null and represents a valid HTML form element</li>
 *   <li>formFields list is never null and contains only valid form input elements</li>
 *   <li>submitField may be null if form has no explicit submit button</li>
 *   <li>name may be null but if set must be non-empty</li>
 *   <li>type is never null and contains a valid FormType value</li>
 *   <li>bugMessages list is never null</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <ul>
 *   <li>Models HTML form elements and their contained input fields</li>
 *   <li>Tracks form metadata like name and type</li>
 *   <li>Maintains relationships between form elements in graph database</li>
 *   <li>Used for form analysis and validation</li>
 * </ul>
 */
@NoArgsConstructor
@Getter
@Setter
public class Form extends LookseeObject{
	private static Logger log = LoggerFactory.getLogger(Form.class);

	private Long memoryId;
	private String name;
	private String type;

	@Relationship(type = "HAS")
	private List<BugMessage> bugMessages;
	
	@Relationship(type = "DEFINED_BY")
	private ElementState formTag;
	
	@Relationship(type = "HAS")
	private List<ElementState> formFields;

	@Relationship(type = "HAS_SUBMIT")
	private ElementState submitField;


	/**
	 * Constructs a new {@link Form}
	 *
	 * @param form_tag the form tag of the form
	 * @param form_fields the form fields of the form
	 * @param submit_field the submit field of the form
	 * @param name the name of the form
	 *
	 * precondition: form_tag != null
	 * precondition: form_fields != null
	 */
	public Form(ElementState form_tag,
				List<ElementState> form_fields,
				ElementState submit_field,
				String name){
		assert form_tag != null;
		assert form_fields != null;

		setFormTag(form_tag);
		setFormFields(form_fields);
		setSubmitField(submit_field);
		setType(determineFormType());
		setName(name);
		setKey(generateKey());
	}
	
	/**
	 * Generates key for form based on element within it and the key of the 
	 * form tag itself
	 *
	 * @return key for form
	 */
	@Override
	public String generateKey() {
		return "form"+getFormTag();
	}

	/**
	 * Determines the {@link FormType} of form based on the attributes of the
	 * form tag
	 *
	 * @return the type of form
	 */
	private FormType determineFormType(){
		Map<String, String> attributes = this.formTag.getAttributes();
		for(String attr: attributes.keySet()){
			String vals = attributes.get(attr);
			if(vals.contains("register")
				|| (vals.contains("sign") && vals.contains("up"))){
				return FormType.REGISTRATION;
			}
			else if(vals.contains("login")
					|| (vals.contains("sign") && vals.contains("in"))){
				return FormType.LOGIN;
			}
			else if(vals.contains("search")){
				return FormType.SEARCH;
			}
			else if(vals.contains("reset") && vals.contains("password")){
				return FormType.PASSWORD_RESET;
			}
			else if(vals.contains("payment") || vals.contains("credit")){
				return FormType.PAYMENT;
			}
		}

		if(submitField != null && (submitField.getAllText().toLowerCase().contains("login")
				|| submitField.getAllText().toLowerCase().contains("sign-in")
				|| submitField.getAllText().toLowerCase().contains("sign in"))) {
			return FormType.LOGIN;
		}
		else if(submitField != null && (submitField.getAllText().toLowerCase().contains("register")
				|| submitField.getAllText().toLowerCase().contains("sign-up")
				|| submitField.getAllText().toLowerCase().contains("sign up"))) {
			return FormType.REGISTRATION;
		}
		
		boolean contains_username = false;
		boolean contains_password = false;
		boolean contains_password_confirmation = false;
		for(ElementState element : formFields) {
			Map<String, String> element_attributes = element.getAttributes();
			for(String attr_val: element_attributes.values()){
				if(attr_val.contains("username") || attr_val.contains("email")){
					contains_username = true;
				}
				else if(attr_val.contains("password") && !(attr_val.contains("confirmation") || (attr_val.contains("confirm") && attr_val.contains("password")))){
					contains_password = true;
				}
				else if(attr_val.contains("password") && (attr_val.contains("confirmation") || attr_val.contains("confirm"))){
					contains_password_confirmation = true;
				}
			}
		}
		
		if(contains_username && contains_password && !contains_password_confirmation) {
			return FormType.LOGIN;
		}
		else if(contains_username && contains_password && contains_password_confirmation) {
			return FormType.REGISTRATION;
		}
		
		return FormType.UNKNOWN;
	}
	
	/**
	 * Checks if {@link Form forms} are equal
	 *
	 * @param o object to compare to
	 *
	 * @return true if forms are equal, false otherwise
	 */
	@Override
	public boolean equals(Object o){
		if (this == o) return true;
        if (!(o instanceof Form)) return false;
        
        Form that = (Form)o;
		return this.getKey().equals(that.getKey());
	}
	
	/**
	 * Adds a form field to the form
	 *
	 * @param form_field form field to add
	 *
	 * @return true if form field was added, false otherwise
	 *
	 * precondition: form_field != null
	 */
	public boolean addFormField(ElementState form_field) {
		assert form_field != null;

		return this.formFields.add(form_field);
	}
	
	/**
	 * Adds a list of form fields to the form
	 *
	 * @param form_field list of form fields to add
	 *
	 * @return true if form fields were added, false otherwise
	 *
	 * precondition: form_field != null
	 */
	public boolean addFormFields(List<ElementState> form_field) {
		assert form_field != null;

		return this.formFields.addAll(form_field);
	}

	/**
	 * Returns the {@link FormType} of the form
	 *
	 * @return the type of form
	 */
	public FormType getType() {
		return FormType.valueOf(type.toUpperCase());
	}

	/**
	 * Sets the type of the form
	 *
	 * @param type the type of form
	 *
	 * precondition: type != null
	 */
	public void setType(FormType type) {
		assert type != null;

		this.type = type.toString();
	}
	
	@Override
	public Form clone(){
		return new Form(formTag, formFields, submitField, name);
	}

	/**
	 * Sets the {@link BugMessage}s of the form
	 *
	 * @param bug_messages the bug messages to set
	 *
	 * precondition: bug_messages != null
	 */
	public void setBugMessages(List<BugMessage> bug_messages) {
		assert bug_messages != null;
		if(this.bugMessages == null) {
			this.bugMessages = new ArrayList<>();
		}
		this.bugMessages = bug_messages;
	}

	/**
	 * Adds a {@link BugMessage} to the form
	 *
	 * @param bug_message the bug message to add
	 *
	 * precondition: bug_message != null
	 */
	public void addBugMessage(BugMessage bug_message) {
		assert bug_message != null;
		if(this.bugMessages == null) {
			this.bugMessages = new ArrayList<>();
		}
		log.warn("bug meesages  :: "+this.bugMessages);
		this.bugMessages.add(bug_message);
	}

	/**
	 * Removes a {@link BugMessage} from the form
	 *
	 * @param msg the bug message to remove
	 *
	 * precondition: msg != null
	 */
	public void removeBugMessage(BugMessage msg) {
		assert msg != null;
		int idx = bugMessages.indexOf(msg);
		this.bugMessages.remove(idx);
	}
}
