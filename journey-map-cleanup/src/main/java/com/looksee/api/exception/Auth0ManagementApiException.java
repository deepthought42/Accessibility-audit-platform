package com.looksee.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
public class Auth0ManagementApiException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5133451250284340743L;

	public Auth0ManagementApiException(){
		super("An error occurred while updating user account");
	}
}
