package com.looksee.pageBuilder.models.message;

import lombok.Getter;
import lombok.Setter;

/**
 * Message used to indicate that a domain page has been built and data extracted
 */
public class PageBuiltMessage extends Message{
	@Getter
	@Setter
	private long pageId;

	@Getter
	@Setter
	private long auditRecordId;
	
	public PageBuiltMessage() {
		super(-1);
	}
	
	public PageBuiltMessage(long account_id,
							long page_id, 
							long audit_record_id) 
	{
		super(account_id);
		setAuditRecordId(audit_record_id);
		setPageId(page_id);
	}
}
