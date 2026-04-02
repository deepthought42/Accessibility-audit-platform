package com.looksee.frontEndBroadcaster.models.message;

public class PageAuditMessage extends Message {
	private long page_audit_id;
	
	public PageAuditMessage() {}
	
	public PageAuditMessage(long account_id,
							long page_audit_id
	) {
		super(account_id);
		setPageAuditId(page_audit_id);
	}

	public long getPageAuditId() {
		return page_audit_id;
	}

	public void setPageAuditId(long page_audit_id) {
		this.page_audit_id = page_audit_id;
	}
}
