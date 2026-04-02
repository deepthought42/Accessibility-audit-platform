package com.looksee.pageBuilder.models.message;

import com.looksee.pageBuilder.models.enums.AuditLevel;
import com.looksee.pageBuilder.models.enums.BrowserType;

import lombok.Getter;
import lombok.Setter;

/**
 * Message for different audit actions to perform and which audit types to perform them for.
 * 
 */
public class UrlMessage extends Message{
	@Getter
	@Setter
	private String url;

	@Getter
	@Setter
	private BrowserType browser;

	@Getter
	@Setter
	private long auditId;

	@Setter
	@Getter
	private AuditLevel type;

	public UrlMessage() {}
	
	public UrlMessage(String url, 
					  BrowserType browser,
					  long audit_id,
					  AuditLevel type,
					  long account_id)
	{
		setUrl(url);
		setBrowser(browser);
		setAuditId(audit_id);
		setAccountId(account_id);
		setType(type);
	}
}
