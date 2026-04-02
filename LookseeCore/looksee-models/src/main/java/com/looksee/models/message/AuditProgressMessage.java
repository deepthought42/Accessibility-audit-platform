package com.looksee.models.message;

import com.looksee.models.enums.AuditStatus;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Intended to contain information regarding the progress of an audit
 *  category such as Content, Information Architecture, etc for a given page
 */
@Getter
@Setter
@NoArgsConstructor
public class AuditProgressMessage extends DomainAuditMessage {
	private long pageAuditRecordId;
	private Map<String, AuditStatus> auditStatuses;
	private Map<String, Float> auditScores;
	
	/**
	 * Constructor for AuditProgressMessage
	 *
	 * @param account_id the ID of the account
	 * @param audit_record_id the ID of the audit record
	 * @param audit_statuses the statuses of the audits
	 * @param audit_scores the scores of the audits
	 *
	 * precondition: audit_statuses != null
	 * precondition: audit_scores != null
	 */
	public AuditProgressMessage(
			long account_id,
			long audit_record_id,
			Map<String, AuditStatus> audit_statuses,
			Map<String, Float> audit_scores
	) {
		super(account_id, audit_record_id);

		assert audit_statuses != null : "audit_statuses must not be null";
		assert audit_scores != null : "audit_scores must not be null";

		setAuditStatuses(audit_statuses);
		setAuditScores(audit_scores);
	}
}
