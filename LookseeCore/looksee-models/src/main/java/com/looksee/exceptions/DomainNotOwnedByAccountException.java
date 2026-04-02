package com.looksee.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a domain is not owned by the account
 */
@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class DomainNotOwnedByAccountException extends Exception {

	private static final long serialVersionUID = 8418257650116390173L;

	/**
	 * Default constructor for {@link DomainNotOwnedByAccountException}
	 */
	public DomainNotOwnedByAccountException(){
		super("Domain is not owned by your account");
	}
}
