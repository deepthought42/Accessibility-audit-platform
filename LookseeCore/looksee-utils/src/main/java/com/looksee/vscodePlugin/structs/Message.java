package com.looksee.vscodePlugin.structs;

import com.looksee.models.Domain;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * A data wrapper for messages to be passed around between actors. This wrapper includes in the account
 * key for a request alongside data so that actors can keep track of who they are performing work for.
 * 
 * @param <T> data object that is being passed inside of message
 */
@Getter
@Setter
public class Message<T> {
	private final String accountKey;
	private final T data;
	private final Map<String, Object> options;
	private final Domain domain;
	
	/**
	 * Constructor for the Message class.
	 *
	 * @param accountKey the account key for the message
	 * @param data the data for the message
	 * @param options the options for the message
	 * @param domain the domain for the message
	 *
	 * precondition: accountKey != null
	 * precondition: data != null
	 * precondition: options != null
	 * precondition: domain != null
	 */
	public Message(String accountKey, T data, Map<String, Object> options, Domain domain){
		assert accountKey != null : "accountKey must not be null";
		assert data != null : "data must not be null";
		assert options != null : "options must not be null";
		assert domain != null : "domain must not be null";

		this.accountKey = accountKey;
		this.domain = domain;
		this.data = data;
		this.options = options;
	}
	
	/**
	 * Clones the message.
	 * 
	 * @return a clone of the message
	 */
	public Message<T> clone(){
		Message<T> msg = new Message<T>(this.getAccountKey(), this.getData(), this.getOptions(), this.getDomain());
		return msg;
		
	}
}
