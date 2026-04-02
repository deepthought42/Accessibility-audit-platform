package com.looksee.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class InvalidApiKeyException extends Exception {	
	private static final long serialVersionUID = -8173024252672818375L;

	public InvalidApiKeyException(String message) {
		super(message);
	}
}
