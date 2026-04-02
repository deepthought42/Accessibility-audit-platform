package com.looksee.api.exception;

public class PagesAreNotMatchingException extends RuntimeException {
	private static final long serialVersionUID = 7200878662560716215L;

	public PagesAreNotMatchingException() {
		super("Expected page and actual page did not match.");
	}
}