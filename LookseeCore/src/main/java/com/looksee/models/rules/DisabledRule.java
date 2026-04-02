package com.looksee.models.rules;

import com.looksee.models.Element;

/**
 * A rule that checks if an element is disabled
 */
public class DisabledRule extends Rule{

	/**
	 * Constructs a new {@link DisabledRule} rule
	 */
	public DisabledRule() {
		setType(RuleType.DISABLED);
		setValue("");
		setKey(generateKey());
	}
	
	/**
	 * Evaluates the rule for the given element
	 *
	 * @param elem the element to evaluate
	 * @return true if the element is disabled, false otherwise
	 *
	 * precondition: elem != null
	 */
	public Boolean evaluate(Element elem) {
		assert elem != null;
		
		for(String attribute: elem.getAttributes().keySet()){
			if("disabled".contentEquals(attribute)){
				return  elem.getAttributes().get(attribute).length() == 0;
			}
		}
		return null;
	}
}
