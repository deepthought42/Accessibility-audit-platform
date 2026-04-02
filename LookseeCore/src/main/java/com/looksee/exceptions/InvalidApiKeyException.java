package com.looksee.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an API key is invalid
 */
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class InvalidApiKeyException extends Exception {
	private static final long serialVersionUID = -8173024252672818375L;

	/**
	 * Default constructor for {@link InvalidApiKeyException}
	 */
	public InvalidApiKeyException() {
		super("Invalid API key");
	}

	/**
	 * Constructor for {@link InvalidApiKeyException}
	 *
	 * @param message the message to display
	 *
	 * precondition: message != null
	 */
	public InvalidApiKeyException(String message) {
		super(message);
		assert message != null : "message must not be null";
	}
}
