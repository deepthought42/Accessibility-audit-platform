package com.looksee.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientSubscriptionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -952134835989805493L;
	
	public InsufficientSubscriptionException() {
		super("Upgrade your account to run a competitive analysis");
	}
}
