package com.looksee.models.dto;

import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.ExecutionStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object for the audit update message
 *
 * invariant: status is always a valid {@link ExecutionStatus} short name or null
 * invariant: level != null after parameterized construction
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditUpdateDto {
	private long id;
	private AuditLevel level;
	private double contentScore;
	private double contentProgress;
	private double infoArchitectureScore;
	private double infoArchitectureProgress;
	private double accessibilityScore;
	private double accessibilityProgress;
	private double aestheticsScore;
	private double aestheticsProgress;
	private double dataExtractionProgress;
	private String message;
	private String status;
	
	/**
	 * Constructs a new AuditUpdateDto with the specified fields.
	 *
	 * @param id the id of the audit
	 * @param level the level of the audit
	 * @param content_score the content score of the audit
	 * @param content_progress the content progress of the audit
	 * @param info_architecture_score the info architecture score of the audit
	 * @param info_architecture_progress the info architecture progress of the audit
	 * @param accessibility_score the accessibility score of the audit
	 * @param aesthetics_score the aesthetics score of the audit
	 * @param aesthetics_progress the aesthetics progress of the audit
	 * @param data_extraction_progress the data extraction progress of the audit
	 * @param message the message of the audit
	 * @param status the status of the audit
	 *
	 * precondition: level != null
	 * precondition: message != null
	 * precondition: status != null
	 */
	public AuditUpdateDto(
			long id,
			AuditLevel level,
			double content_score,
			double content_progress,
			double info_architecture_score,
			double info_architecture_progress,
			double accessibility_score,
			double aesthetics_score,
			double aesthetics_progress,
			double data_extraction_progress,
			String message,
			ExecutionStatus status
	){
		assert level != null;
		assert message != null;
		assert status != null;

		setId(id);
		setLevel(level);
		setContentScore(content_score);
		setContentProgress(content_progress);
		setInfoArchitectureScore(info_architecture_score);
		setInfoArchitectureProgress(info_architecture_progress);
		setAccessibilityScore(accessibility_score);
		setAestheticsScore(aesthetics_score);
		setAestheticsProgress(aesthetics_progress);
		setDataExtractionProgress(data_extraction_progress);
		setMessage(message);
		setStatus(status);
	}

	/**
	 * Get the status of the audit
	 *
	 * @return the status of the audit
	 */
	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	/**
	 * Set the status of the audit
	 *
	 * @param status the status of the audit
	 *
	 * precondition: status != null
	 */
	public void setStatus(ExecutionStatus status) {
		assert status != null;

		this.status = status.getShortName();
	}
}
