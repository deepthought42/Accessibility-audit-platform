package com.looksee.models.message;

import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.BrowserType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Message for different audit actions to perform and which audit types to perform them for.
 * 
 */
@NoArgsConstructor
@Getter
@Setter
public class AuditStartMessage extends Message{
	/**
	 * The url to audit
	 */
	private String url;

	/**
	 * The browser to audit
	 */
	private BrowserType browser;

	/**
	 * The audit id
	 */
	private long auditId;

	/**
	 * The audit type
	 */
	private AuditLevel type;
	
	/**
	 * Creates a new AuditStartMessage
	 * @param url the url to audit
	 * @param browser the browser to audit
	 * @param audit_id the audit id
	 * @param type the audit type
	 * @param account_id the account id
	 *
	 * precondition: url != null
	 * precondition: browser != null
	 * precondition: type != null
	 */
	public AuditStartMessage(String url,
							BrowserType browser,
							long audit_id,
							AuditLevel type,
							long account_id)
	{
		assert url != null : "url must not be null";
		assert browser != null : "browser must not be null";
		assert type != null : "type must not be null";

		setUrl(url);
		setBrowser(browser);
		setAuditId(audit_id);
		setAccountId(account_id);
		setType(type);
	}
}
