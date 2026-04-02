package com.looksee.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user's free trial has ended
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class FreeTrialExpiredException extends RuntimeException {

	private static final long serialVersionUID = 7200878662560716216L;

	/**
	 * Default constructor for {@link FreeTrialExpiredException}
	 */
	public FreeTrialExpiredException() {
		super("Your free trial has ended. Sign up for a plan.");
	}
}