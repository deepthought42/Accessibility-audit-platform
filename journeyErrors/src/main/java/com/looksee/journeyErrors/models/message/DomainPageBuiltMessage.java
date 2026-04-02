package com.looksee.journeyErrors.models.message;

/**
 * Message used to indicate that a domain page has been built and data extracted
 */
public class DomainPageBuiltMessage extends DomainAuditMessage{
	private long pageId;
	private long pageAuditRecordId;
	
	public DomainPageBuiltMessage() {}
	
	public DomainPageBuiltMessage(long account_id, 
							long domain_audit_id,
							long page_id) 
	{
		super(account_id, domain_audit_id);
		setPageId(page_id);
	}
	
	public long getPageId() {
		return pageId;
	}
	public void setPageId(long page_id) {
		this.pageId = page_id;
	}

	public long getPageAuditRecordId() {
		return pageAuditRecordId;
	}

	public void setPageAuditRecordId(long pageAuditRecordId) {
		this.pageAuditRecordId = pageAuditRecordId;
	}

}
