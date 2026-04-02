package com.looksee.exceptions;

/**
 * Exception thrown when expected page and actual page do not match
 */
public class PagesAreNotMatchingException extends RuntimeException {
	private static final long serialVersionUID = 7200878662560716215L;

	/**
	 * Default constructor for {@link PagesAreNotMatchingException}
	 */
	public PagesAreNotMatchingException() {
		super("Expected page and actual page did not match.");
	}
}