package com.looksee.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user's subscription limit is exceeded
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class SubscriptionExceededException extends RuntimeException {

	private static final long serialVersionUID = 2919850254180144335L;

	/**
	 * Default constructor for {@link SubscriptionExceededException}
	 */
	public SubscriptionExceededException() {
		super("Your usage has exceeded your subscription limit. Upgrade your plan to continue.");
	}

	/**
	 * Constructor for {@link SubscriptionExceededException}
	 *
	 * @param message the message to display
	 *
	 * precondition: message != null
	 */
	public SubscriptionExceededException(String message) {
		super(message);
		assert message != null : "message must not be null";
	}
}
