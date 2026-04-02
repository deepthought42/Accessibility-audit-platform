package com.looksee.pageBuilder.models.message;

import com.looksee.pageBuilder.models.enums.AuditLevel;
import com.looksee.pageBuilder.models.enums.BrowserType;

import lombok.Getter;
import lombok.Setter;

/**
 * Message for different audit actions to perform and which audit types to perform them for.
 * 
 */
public class AuditStartMessage extends Message{
	private String url;
	private BrowserType browser;
	private long audit_id;

	@Setter
	@Getter
	private AuditLevel type;

	public AuditStartMessage() {}
	
	public AuditStartMessage(String url, 
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

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public BrowserType getBrowser() {
		return browser;
	}

	private void setBrowser(BrowserType browser) {
		this.browser = browser;
	}

	public long getAuditId() {
		return audit_id;
	}

	public void setAuditId(long auditId) {
		this.audit_id = auditId;
	}
}
