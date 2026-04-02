package com.looksee.api.exception;

public class ExistingRuleException extends RuntimeException {
	private static final long serialVersionUID = 7200878662560716215L;

	public ExistingRuleException(String rule_type) {
		super("Element already has the " + rule_type + " rule applied.");
	}
}