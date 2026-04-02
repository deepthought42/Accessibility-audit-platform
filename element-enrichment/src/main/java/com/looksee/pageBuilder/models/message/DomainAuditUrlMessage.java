package com.looksee.pageBuilder.models.message;

import com.looksee.pageBuilder.models.enums.BrowserType;

/**
 * Message for different audit actions to perform and which audit types to perform them for.
 * 
 */
public class DomainAuditUrlMessage extends DomainAuditMessage {
	
	private String url;
	private BrowserType browser;
	
	public DomainAuditUrlMessage() {}
	
	public DomainAuditUrlMessage( long account_id,
				  	   long domain_audit_id,
				  	   String url, 
				  	   BrowserType browser)
	{
		super(account_id, domain_audit_id);
		setUrl(url);
		setBrowser(browser);
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

	public void setBrowser(BrowserType browser) {
		this.browser = browser;
	}
}
