package com.looksee.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PaymentDueException extends Exception {	
	private static final long serialVersionUID = 7200878662560716216L;

	public PaymentDueException(String message) {
		super(message);
	}
}
