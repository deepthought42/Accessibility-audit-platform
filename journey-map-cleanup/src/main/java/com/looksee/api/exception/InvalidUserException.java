package com.looksee.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class InvalidUserException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8418257650116390173L;

	public InvalidUserException(){
		super("Invalid user");
	}
}
