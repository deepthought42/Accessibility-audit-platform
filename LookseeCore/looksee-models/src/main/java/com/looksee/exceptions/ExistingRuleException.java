package com.looksee.exceptions;

/**
 * Exception thrown when a rule already exists for an element
 */
public class ExistingRuleException extends RuntimeException {
	private static final long serialVersionUID = 7200878662560716215L;

	/**
	 * Constructor for {@link ExistingRuleException}
	 *
	 * @param rule_type the type of rule that already exists
	 *
	 * precondition: rule_type != null
	 */
	public ExistingRuleException(String rule_type) {
		super("Element already has the " + rule_type + " rule applied.");
		assert rule_type != null : "rule_type must not be null";
	}
}