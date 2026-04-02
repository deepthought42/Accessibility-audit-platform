package com.looksee.pageBuilder.models.message;

/**
 * Intended to contain information regarding the progress of journey 
 *   mapping for a domain audit.
 */
public class DomainAuditMessage extends Message {
	private long domainAuditRecordId;
	
	public DomainAuditMessage() {	}
	
	public DomainAuditMessage(
			long account_id,
			long domain_audit_record_id
	) {
		super(account_id);
		setDomainAuditRecordId(domain_audit_record_id);
	}

	/* GETTERS / SETTERS */
	public long getDomainAuditRecordId() {
		return domainAuditRecordId;
	}

	public void setDomainAuditRecordId(long audit_record_id) {
		this.domainAuditRecordId = audit_record_id;
	}
}
