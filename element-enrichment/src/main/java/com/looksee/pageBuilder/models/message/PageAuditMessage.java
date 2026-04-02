package com.looksee.pageBuilder.models.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Messages that are primarily for PageAudits. Generally used for Single Page Audit messages
 */
@NoArgsConstructor
public class PageAuditMessage extends Message {
	
	@Getter
	@Setter
	private long pageAuditId;
		
	public PageAuditMessage(long account_id,
							long page_audit_id
	) {
		super(account_id);
		setPageAuditId(page_audit_id);
	}
}

