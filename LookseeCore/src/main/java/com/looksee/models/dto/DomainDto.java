package com.looksee.models.dto;

import com.looksee.models.Domain;
import com.looksee.models.enums.ExecutionStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object for {@link Domain} object that is designed to comply with
 * the data format for browser extensions
 *
 * invariant: url may be null only when using the no-arg constructor
 * invariant: status may be null only when using the no-arg constructor
 */
@Getter
@Setter
@NoArgsConstructor
public class DomainDto {
	private long id;
	private String url;
	private int pageCount;
	private int pagesAudited;
	private double contentScore;
	private double contentProgress;
	private double infoArchitectureScore;
	private double infoArchitectureProgress;
	private double accessibilityScore;
	private double accessibilityProgress;
	private double aestheticsScore;
	private double aestheticsProgress;
	private double dataExtractionProgress;
	private boolean auditRunning;
	private String message;
	private String status;

	/**
	 * constructor for the domain dto
	 * @param id the id of the domain
	 * @param url the url of the domain
	 * @param progress the progress of the domain
	 *
	 * precondition: url != null
	 * precondition: progress >= 0
	 */
	public DomainDto(long id, String url, double progress) {
		assert url != null;
		assert progress >= 0;

		this.id = id;
		this.url = url;
		this.contentProgress = progress;
	}
	
	
	/**
	 * Constructs a new DomainDto with the specified fields.
	 *
	 * @param id the id of the domain
	 * @param url the url of the domain
	 * @param page_count the number of pages in the domain
	 * @param audited_page_count the number of pages audited in the domain
	 * @param content_score the content score of the domain
	 * @param content_progress the content progress of the domain
	 * @param info_architecture_score the info architecture score of the domain
	 * @param info_architecture_progress the info architecture progress of the domain
	 * @param accessibility_score the accessibility score of the domain
	 * @param accessibility_progress the accessibility progress of the domain
	 * @param aesthetics_score the aesthetics score of the domain
	 * @param aesthetics_progress the aesthetics progress of the domain
	 * @param is_audit_running the audit running status of the domain
	 * @param data_extraction_progress the data extraction progress of the domain
	 * @param message the message of the domain
	 * @param status the status of the domain
	 *
	 * precondition: id != null
	 * precondition: url != null
	 * precondition: page_count != null
	 * precondition: audited_page_count != null
	 * precondition: content_score != null
	 * precondition: content_progress != null
	 * precondition: info_architecture_score != null
	 * precondition: info_architecture_progress != null
	 * precondition: accessibility_score != null
	 * precondition: accessibility_progress != null
	 * precondition: aesthetics_score != null
	 * precondition: aesthetics_progress != null
	 */
	public DomainDto(
			long id,
			String url,
			int page_count,
			int audited_page_count,
			double content_score,
			double content_progress,
			double info_architecture_score,
			double info_architecture_progress,
			double accessibility_score,
			double accessibility_progress,
			double aesthetics_score,
			double aesthetics_progress,
			boolean is_audit_running,
			double data_extraction_progress,
			String message,
			ExecutionStatus status
	){
		assert id > 0;
		assert url != null;
		assert page_count > 0;
		assert audited_page_count > 0;
		assert content_score >= 0;
		assert content_progress >= 0;
		assert info_architecture_score >= 0;
		assert info_architecture_progress >= 0;
		assert accessibility_score >= 0;
		assert accessibility_progress >= 0;
		assert aesthetics_score >= 0;
		assert aesthetics_progress >= 0;
		assert data_extraction_progress >= 0;
		assert message != null;
		assert status != null;

		setId(id);
		setUrl(url);
		setPageCount(page_count);
		setContentScore(content_score);
		setContentProgress(content_progress);
		setInfoArchitectureScore(info_architecture_score);
		setInfoArchitectureProgress(info_architecture_progress);
		setAccessibilityScore(accessibility_score);
		setAccessibilityProgress(accessibility_progress);
		setAestheticsScore(aesthetics_score);
		setAestheticsProgress(aesthetics_progress);
		setAuditRunning(is_audit_running);
		setPagesAudited(audited_page_count);
		setDataExtractionProgress(data_extraction_progress);
		setMessage(message);
		setStatus(status);
	}

	/**
	 * Gets the status of the domain
	 *
	 * @return the status
	 */
	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	/**
	 * Sets the status of the domain
	 *
	 * @param status the status
	 *
	 * precondition: status != null
	 */
	public void setStatus(ExecutionStatus status) {
		assert status != null;
		
		this.status = status.getShortName();
	}
}
