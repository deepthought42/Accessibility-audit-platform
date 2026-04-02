package com.looksee.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user is invalid
 */
@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class InvalidUserException extends Exception {

	private static final long serialVersionUID = 8418257650116390173L;

	/**
	 * Default constructor for {@link InvalidUserException}
	 */
	public InvalidUserException(){
		super("Invalid user");
	}
}
