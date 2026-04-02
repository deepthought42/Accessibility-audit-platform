package com.looksee.models.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SinglePageBuiltMessage is a message that is sent when a single page is built.
 * It contains the ID of the page that was built.
 */
@Getter
@Setter
@NoArgsConstructor
public class SinglePageBuiltMessage extends PageAuditMessage{
	private long pageId;
	
	/**
	 * Constructor for SinglePageBuiltMessage
	 *
	 * @param account_id the ID of the account
	 * @param page_id the ID of the page
	 * @param page_audit_record_id the ID of the page audit record
	 */
	public SinglePageBuiltMessage(long account_id,
							long page_id,
							long page_audit_record_id)
	{
		super(account_id, page_audit_record_id);
		setPageId(page_id);
	}
}