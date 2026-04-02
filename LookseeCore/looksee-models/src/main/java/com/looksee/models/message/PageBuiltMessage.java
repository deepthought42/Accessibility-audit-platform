package com.looksee.models.message;

import lombok.Getter;
import lombok.Setter;

/**
 * Message used to indicate that a domain page has been built and data extracted
 *
 * invariant: pageId >= 0 after parameterized construction
 * invariant: auditRecordId >= 0 after parameterized construction
 */
@Getter
@Setter
public class PageBuiltMessage extends Message{
	/**
	 * The page id
	 */
	private long pageId;

	/**
	 * The audit record id
	 */
	private long auditRecordId;
	
	/**
	 * Creates a new PageBuiltMessage
	 */
	public PageBuiltMessage() {
		super(-1);
	}
	
	/**
	 * Creates a new PageBuiltMessage
	 * @param accountId the account id
	 * @param pageId the page id
	 * @param auditRecordId the audit record id
	 *
	 * precondition: pageId >= 0
	 * precondition: auditRecordId >= 0
	 */
	public PageBuiltMessage(long accountId,
							long pageId,
							long auditRecordId)
	{
		super(accountId);

		assert pageId >= 0;
		assert auditRecordId >= 0;

		setAuditRecordId(auditRecordId);
		setPageId(pageId);
	}
}
