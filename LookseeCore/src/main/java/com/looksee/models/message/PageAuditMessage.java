package com.looksee.models.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Messages that are primarily for PageAudits. Generally used for Single Page Audit messages
 */
@NoArgsConstructor
@Getter
@Setter
public class PageAuditMessage extends Message {
	
	/**
	 * The page audit id
	 */
	private long pageAuditId;
	
	/**
	 * Creates a new PageAuditMessage
	 * @param accountId the account id
	 * @param pageAuditId the page audit id
	 */
	public PageAuditMessage(long accountId,
							long pageAuditId
	) {
		super(accountId);
		setPageAuditId(pageAuditId);
	}
}

