package com.looksee.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when discovery has been stopped
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DiscoveryStoppedException extends Exception {
	private static final long serialVersionUID = -2136313898636540125L;

	/**
	 * Default constructor for {@link DiscoveryStoppedException}
	 */
	public DiscoveryStoppedException() {
		super("Discovery has been stopped");
	}

	/**
	 * Constructor for {@link DiscoveryStoppedException}
	 *
	 * @param message the message to display
	 *
	 * precondition: message != null
	 */
	public DiscoveryStoppedException(String message) {
		super(message);
		assert message != null : "message must not be null";
	}
}