package com.looksee.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user's subscription is insufficient
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientSubscriptionException extends Exception {

	/**
	 * Default constructor for {@link InsufficientSubscriptionException}
	 */
	private static final long serialVersionUID = -952134835989805493L;
	
	/**
	 * Default constructor for {@link InsufficientSubscriptionException}
	 */
	public InsufficientSubscriptionException() {
		super("Upgrade your account to run a competitive analysis");
	}
}
