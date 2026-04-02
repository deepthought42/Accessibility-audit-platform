package com.looksee.models.rules;

import com.looksee.models.Element;

/**
 * A rule that checks if an element is clickable
 */
public class Clickable extends Rule {

	/**
	 * Constructs a new {@link Clickable} rule
	 */
	public Clickable(){
		setType(RuleType.CLICKABLE);
		setValue("");
		setKey(generateKey());
	}

	/**
	 * Evaluates the rule for the given element
	 *
	 * @param val the element to evaluate
	 * @return true if the element is clickable, false otherwise
	 *
	 * precondition: val != null
	 */
	@Override
	public Boolean evaluate(Element val) {
		assert false;
		// TODO Auto-generated method stub
		return null;
	}
}
