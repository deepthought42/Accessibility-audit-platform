package com.looksee.journeyMapCleanup.models.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Message used to indicate that a domain page has been built and data extracted
 */
@NoArgsConstructor
public class PageBuiltMessage extends Message{
	@Getter
	@Setter
	private long pageId;

	@Getter
	@Setter
	private long auditRecordId;
	
	public PageBuiltMessage(long account_id,
							long page_id,
							long audit_record_id)
	{
		super(account_id);
		setPageId(page_id);
		setAuditRecordId(audit_record_id);
	}
}

