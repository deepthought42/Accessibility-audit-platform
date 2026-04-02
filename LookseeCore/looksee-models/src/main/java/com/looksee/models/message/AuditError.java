package com.looksee.models.message;

import com.looksee.models.enums.AuditCategory;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents an error that occurred during an audit process.
 * This class extends {@link Message} to provide error information specific to audit operations.
 *
 * invariant: errorMessage != null after construction
 * invariant: auditCategory != null after construction
 * invariant: progress &gt;= 0 and progress &lt;= 1 after construction
 *
 * @see Message
 * @see AuditCategory
 */
@Getter
@Setter
public class AuditError extends Message {
	/**
	 * A descriptive message explaining the error that occurred during the audit
	 */
	private String errorMessage;

	/**
	 * The category of audit where the error occurred (e.g., COLOR_PALETTE, TEXT_BACKGROUND_CONTRAST)
	 * @see AuditCategory
	 */
	private AuditCategory auditCategory;

	/**
	 * The progress of the audit operation when the error occurred, represented as a value between 0 and 1
	 */
	private double progress;

	/**
	 * The unique identifier of the audit record associated with this error
	 */
	private long auditRecordId;
	
	/**
	 * Constructs a new AuditError with the specified parameters.
	 *
	 * @param accountId the ID of the account associated with this audit error
	 * @param auditRecordId the ID of the audit record where the error occurred
	 * @param errorMessage a descriptive message explaining the error
	 * @param auditCategory the category of audit where the error occurred
	 * @param progress the progress of the audit operation when the error occurred (0-1)
	 * 
	 * precondition: errorMessage != null
	 * precondition: auditCategory != null
	 * precondition: progress &gt;= 0 and progress &lt;= 1
	 *
	 * @throws IllegalArgumentException if progress is not between 0 and 1
	 */
	public AuditError(long accountId,
					long auditRecordId,
					String errorMessage,
					AuditCategory auditCategory,
					double progress
	) {
		super(accountId);

		assert errorMessage != null;
		assert auditCategory != null;

		if (progress < 0 || progress > 1) {
			throw new IllegalArgumentException("Progress must be between 0 and 1");
		}
		this.errorMessage = errorMessage;
		this.auditCategory = auditCategory;
		this.progress = progress;
		this.auditRecordId = auditRecordId;
	}
}
