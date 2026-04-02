package com.looksee.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a user's payment is due
 */
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PaymentDueException extends Exception {
	private static final long serialVersionUID = 7200878662560716216L;

	/**
	 * Default constructor for {@link PaymentDueException}
	 */
	public PaymentDueException() {
		super("Make a payment to continue using Look-see");
	}

	/**
	 * Constructor for {@link PaymentDueException}
	 *
	 * @param message the message to display
	 *
	 * precondition: message != null
	 */
	public PaymentDueException(String message) {
		super(message);
		assert message != null : "message must not be null";
	}
}
