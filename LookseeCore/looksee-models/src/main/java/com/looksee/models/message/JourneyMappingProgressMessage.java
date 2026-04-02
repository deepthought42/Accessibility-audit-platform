package com.looksee.models.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Intended to contain information regarding the progress of journey 
 *   mapping for a domain audit.
 */
@Getter
@Setter
@NoArgsConstructor
public class JourneyMappingProgressMessage extends DomainAuditMessage {
	private int candidateCount;
	private int completedCount;
		
	/**
	 * Constructor for {@link JourneyMappingProgressMessage}
	 *
	 * @param account_id the id of the account
	 * @param audit_record_id the id of the audit record
	 * @param candidate_count the number of candidates
	 * @param completed_count the number of completed candidates
	 */
	public JourneyMappingProgressMessage(
			long account_id,
			long audit_record_id,
			int candidate_count,
			int completed_count
	) {
		super(account_id, audit_record_id);
		setCandidateCount(candidate_count);
		setCompletedCount(completed_count);
	}
}
