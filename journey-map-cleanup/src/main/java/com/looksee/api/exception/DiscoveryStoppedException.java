package com.looksee.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DiscoveryStoppedException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2136313898636540125L;
}