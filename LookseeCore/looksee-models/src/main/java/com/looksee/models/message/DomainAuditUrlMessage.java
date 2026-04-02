package com.looksee.models.message;

import com.looksee.models.enums.BrowserType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A DomainAuditUrlMessage is a message that is used to audit a url
 */
@NoArgsConstructor
@Getter
@Setter
public class DomainAuditUrlMessage extends DomainAuditMessage {
	
	/**
	 * The url to audit
	 */
	private String url;

	/**
	 * The browser to audit
	 */
	private BrowserType browser;
	
	/**
	 * Creates a new DomainAuditUrlMessage
	 * @param account_id the account id
	 * @param domain_audit_id the domain audit id
	 * @param url the url to audit
	 * @param browser the browser to audit
	 *
	 * precondition: url != null
	 * precondition: browser != null
	 */
	public DomainAuditUrlMessage(long account_id,
								long domain_audit_id,
								String url,
								BrowserType browser)
	{
		super(account_id, domain_audit_id);

		assert url != null : "url must not be null";
		assert browser != null : "browser must not be null";

		setUrl(url);
		setBrowser(browser);
	}
}
