package com.looksee.models.message;

import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.BrowserType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A UrlMessage is a message that is used to send a url to the browser
 *
 * invariant: url != null after parameterized construction
 * invariant: browser != null after parameterized construction
 * invariant: type != null after parameterized construction
 */
@NoArgsConstructor
@Getter
@Setter
public class UrlMessage extends Message{
	
	/**
	 * The url
	 */
	private String url;

	/**
	 * The browser
	 */
	private BrowserType browser;

	/**
	 * The audit id
	 */
	private long auditId;

	/**
	 * The type
	 */
	private AuditLevel type;
	
	/**
	 * Creates a new UrlMessage
	 * @param url the url
	 * @param browser the browser
	 * @param auditId the audit id
	 * @param type the type
	 * @param accountId the account id
	 *
	 * precondition: url != null
	 * precondition: browser != null
	 * precondition: type != null
	 */
	public UrlMessage(String url,
					  BrowserType browser,
					  long auditId,
					  AuditLevel type,
					  long accountId)
	{
		assert url != null;
		assert browser != null;
		assert type != null;

		setUrl(url);
		setBrowser(browser);
		setAuditId(auditId);
		setAccountId(accountId);
		setType(type);
	}
}
