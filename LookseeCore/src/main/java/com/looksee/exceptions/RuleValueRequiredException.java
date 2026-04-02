package com.looksee.exceptions;

import com.looksee.models.rules.RuleType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a rule requires a value
 */
@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class RuleValueRequiredException extends RuntimeException {

	private static final long serialVersionUID = 7200878662560716216L;

	/**
	 * Constructor for {@link RuleValueRequiredException}
	 *
	 * @param type the type of rule that requires a value
	 *
	 * precondition: type != null
	 */
	public RuleValueRequiredException(RuleType type) {
		super("The provided rule " + type + " requires a value.");
		assert type != null : "type must not be null";
	}
}
