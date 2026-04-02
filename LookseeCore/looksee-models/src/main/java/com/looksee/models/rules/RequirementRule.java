package com.looksee.models.rules;

import com.looksee.models.Element;

/**
 * A rule that checks if an element is required
 */
public class RequirementRule extends Rule{
	/**
	 * Constructs Rule
	 */
	public RequirementRule(){
		setValue("");
		setType(RuleType.REQUIRED);
		this.setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(Element elem) {
		return elem.getAttributes().containsKey("required");
	}
}
