package com.looksee.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Auth0ManagementApiException is thrown when an error occurs while updating
 * user account.
 */
@ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
public class Auth0ManagementApiException extends Exception {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -5133451250284340743L;

	/**
	 * Constructor for Auth0ManagementApiException
	 */
	public Auth0ManagementApiException(){
		super("An error occurred while updating user account");
	}
}
