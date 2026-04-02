package com.looksee.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an account is not found
 */
@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "Unable to find account")
public class UnknownAccountException extends Exception {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -407342019498708399L;
}
