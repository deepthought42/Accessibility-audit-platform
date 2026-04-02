package com.looksee.journeyMapCleanup.models.message;

import lombok.Getter;
import lombok.Setter;

/**
 * Intended to contain information regarding the progress of journey 
 *   mapping for a domain audit.
 */
public class DomainAuditMessage extends Message {

	@Getter
	@Setter
	private long domainAuditRecordId;
	
	public DomainAuditMessage() {	}
	
	public DomainAuditMessage(
			long account_id,
			long domain_audit_record_id
	) {
		super(account_id);
		setDomainAuditRecordId(domain_audit_record_id);
	}
}
