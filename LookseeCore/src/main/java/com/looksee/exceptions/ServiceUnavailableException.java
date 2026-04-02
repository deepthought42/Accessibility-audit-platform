package com.looksee.exceptions;

/**
 * Exception thrown when a service is unavailable
 */
public class ServiceUnavailableException extends RuntimeException {
	/**
	 * The serial version UID
	 */
	private static final long serialVersionUID = 794045239226319408L;

	/**
	 * Constructor for {@link ServiceUnavailableException}
	 *
	 * @param msg the message
	 *
	 * precondition: msg != null
	 */
	public ServiceUnavailableException(String msg) {
		super(msg);
		assert msg != null : "msg must not be null";
	}
}