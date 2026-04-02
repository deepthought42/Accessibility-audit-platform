package com.looksee.models.message;

import com.looksee.models.audit.DomainAuditRecord;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Intended to contain information regarding the identification of a new page
 *   to be audited as part of a domain {@link DomainAuditRecord audit}.
 */
@Getter
@Setter
@NoArgsConstructor
public class PageIdentifiedMessage extends DomainAuditMessage {
	private long pageId;
	private String url;
		
	/**
	 * Constructor for {@link PageIdentifiedMessage}
	 *
	 * @param account_id the id of the account
	 * @param domain_audit_record_id the id of the domain audit record
	 * @param page_id the id of the page
	 * @param url the url of the page
	 *
	 * precondition: url != null
	 */
	public PageIdentifiedMessage(
			long account_id,
			long domain_audit_record_id,
			long page_id,
			String url
	) {
		super(account_id, domain_audit_record_id);

		assert url != null : "url must not be null";

		setPageId(page_id);
		setUrl(url);
	}
}
