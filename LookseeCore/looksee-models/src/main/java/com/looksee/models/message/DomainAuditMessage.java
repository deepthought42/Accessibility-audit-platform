package com.looksee.models.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A DomainAuditMessage is a message that is used to audit a domain
 */
@NoArgsConstructor
@Getter
@Setter
public class DomainAuditMessage extends Message {

	/**
	 * The domain audit record id
	 */
	private long domainAuditRecordId;

	/**
	 * Creates a new DomainAuditMessage
	 * @param account_id the account id
	 * @param domain_audit_record_id the domain audit record id
	 */
	public DomainAuditMessage(
			long account_id,
			long domain_audit_record_id
	) {
		super(account_id);
		setDomainAuditRecordId(domain_audit_record_id);
	}
}
