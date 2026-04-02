package com.looksee.models.message;

import lombok.NoArgsConstructor;

/**
 * Message for when a user exceeds their subscription limit
 */
@NoArgsConstructor
public class ExceededSubscriptionMessage extends Message{

	/**
	
	/**
	 * Constructor
	 *
	 * @param accountId the account id
	 */
	public ExceededSubscriptionMessage(long accountId) {
		setAccountId(accountId);
	}
	
}
