package com.looksee.models.message;

/**
 * An interface for all messages
 */
public interface IMessage {
	/**
	 * Returns the account id
	 * @return the account id
	 */
	public long getAccountId();
	/**
	 * Returns the domain audit record id
	 * @return the domain audit record id
	 */
	public long getDomainAuditRecordId();

	/**
	 * Returns the domain id
	 * @return the domain id
	 */
	public long getDomainId();

	/**
	 * Returns the message id
	 * @return the message id
	 */
	public String getMessageId();

	/**
	 * Returns the publish time
	 * @return the publish time
	 */
	public String getPublishTime();
}
