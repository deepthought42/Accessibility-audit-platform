package com.looksee.models.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Message used to indicate that a domain page has been built and data extracted
 */
@Getter
@Setter
@NoArgsConstructor
public class DomainPageBuiltMessage extends DomainAuditMessage{
	private long pageId;
	private long pageAuditRecordId;
	
	/**
	 * Constructs a new DomainPageBuiltMessage with the specified account id, domain audit id, and page id.
	 * 
	 * @param account_id the id of the account
	 * @param domain_audit_id the id of the domain audit
	 * @param page_id the id of the page
	 */
	public DomainPageBuiltMessage(long account_id,
								long domain_audit_id,
								long page_id)
	{
		super(account_id, domain_audit_id);
		setPageId(page_id);
	}
}
