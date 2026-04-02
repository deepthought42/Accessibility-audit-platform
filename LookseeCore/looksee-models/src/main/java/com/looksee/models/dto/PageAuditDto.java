package com.looksee.models.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.looksee.models.Domain;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.ExecutionStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Data transfer object for {@link Domain} object that is designed to comply with
 * the data format for browser extensions
 */
@Getter
@Setter
@AllArgsConstructor
public class PageAuditDto {
	private long id;
	private String url;
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
	private String level;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	private LocalDateTime startTime;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	private LocalDateTime endTime;
	
	private String targetUserAge;
	private String targetUserEducation;

	/**
	 * Constructor for {@link PageAuditDto}
	 */
	public PageAuditDto() {
		setStartTime(LocalDateTime.now());
		setStatus(ExecutionStatus.UNKNOWN);
		setUrl("");
		setMessage("");
		setLevel(AuditLevel.UNKNOWN);
		setContentProgress(0.0);
		setContentScore(0.0);
		setInfoArchitectureProgress(0.0);
		setInfoArchitectureScore(0.0);
		setAestheticsProgress(0.0);
		setAestheticsScore(0.0);
		setDataExtractionProgress(0.0);
	}
	/**
	 * Constructor for {@link PageAuditDto}
	 *
	 * @param id the id of the page audit
	 * @param url the url of the page
	 * @param content_score the content score
	 * @param content_progress the content progress
	 * @param info_architecture_score the information architecture score
	 * @param info_architecture_progress the information architecture progress
	 * @param accessibility_score the accessibility score
	 * @param accessibility_progress the accessibility progress
	 * @param aesthetics_score the aesthetics score
	 * @param aesthetics_progress the aesthetics progress
	 * @param data_extraction_progress the data extraction progress
	 * @param message the message
	 * @param status the status
	 *
	 * precondition: url != null
	 * precondition: message != null
	 * precondition: status != null
	 */
	public PageAuditDto(
			long id,
			String url,
			double content_score,
			double content_progress,
			double info_architecture_score,
			double info_architecture_progress,
			double accessibility_score,
			double accessibility_progress,
			double aesthetics_score,
			double aesthetics_progress,
			double data_extraction_progress,
			String message,
			ExecutionStatus status
	){
		assert url != null;
		assert message != null;
		assert status != null;

		setId(id);
		setUrl(url);
		setContentScore(content_score);
		setContentProgress(content_progress);
		setInfoArchitectureScore(info_architecture_score);
		setInfoArchitectureProgress(info_architecture_progress);
		setAccessibilityScore(accessibility_score);
		setAccessibilityProgress(accessibility_progress);
		setAestheticsScore(aesthetics_score);
		setAestheticsProgress(aesthetics_progress);
		setDataExtractionProgress(data_extraction_progress);
		setMessage(message);
		setStatus(status);
	}

	/**
	 * Constructor
	 * @param id the id of the page audit
	 * @param status the status of the page audit
	 * @param level the level of the page audit
	 * @param startTime the start time of the page audit
	 * @param aestheticAuditProgress the aesthetic audit progress
	 * @param aestheticScore the aesthetic score
	 * @param contentAuditScore the content audit score
	 * @param contentAuditProgress the content audit progress
	 * @param infoArchScore the information architecture score
	 * @param infoArchAuditProgress the information architecture audit progress
	 * @param dataExtractionProgress the data extraction progress
	 * @param created_at the created at time
	 * @param endTime the end time
	 * @param url the url of the page
	 *
	 * precondition: status != null
	 * precondition: level != null
	 * precondition: startTime != null
	 * precondition: created_at != null
	 * precondition: url != null
	 */
	public PageAuditDto(long id,
						ExecutionStatus status,
						AuditLevel level,
						LocalDateTime startTime,
						double aestheticAuditProgress,
						double aestheticScore,
						double contentAuditScore,
						double contentAuditProgress,
						double infoArchScore,
						double infoArchAuditProgress,
						double dataExtractionProgress,
						LocalDateTime created_at,
						LocalDateTime endTime,
						String url
	) {
		assert status != null;
		assert level != null;
		assert startTime != null;
		assert created_at != null;
		assert url != null;

		setId(id);
		setStatus(status);
		setLevel(level);
		setStartTime(endTime);
		setAestheticsProgress(dataExtractionProgress);
		setAestheticsScore(aestheticScore);
		setContentScore(contentAuditScore);
		setContentProgress(contentAuditProgress);
		setInfoArchitectureScore(infoArchScore);
		setInfoArchitectureProgress(infoArchAuditProgress);
		setDataExtractionProgress(dataExtractionProgress);
		setStartTime(created_at);
		setEndTime(endTime);
		setUrl(url);
	}

	/**
	 * Generate a key for the page audit
	 * @return the key for the page audit
	 */
	public String generateKey() {
		return "auditrecord:" + UUID.randomUUID().toString() + org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	/**
	 * Convert the page audit to a string
	 * @return the string representation of the page audit
	 */
	@Override
	public String toString() {
		return this.getId()+", "+this.getUrl()+", "+this.getStatus()+", "+this.getMessage();
	}
	
	/**
	 * Check if the page audit is complete
	 * @return true if the page audit is complete, false otherwise
	 */
	public boolean isComplete() {
		return (this.getAestheticsProgress() >= 1.0
				&& this.getContentProgress() >= 1.0
				&& this.getInfoArchitectureProgress() >= 1.0
				&& this.getDataExtractionProgress() >= 1.0);
	}

	/**
	 * Get the level of the page audit
	 * @return the level of the page audit
	 */
	public AuditLevel getLevel() {
		return AuditLevel.create(level);
	}

	/**
	 * Set the level of the page audit
	 * @param level the level of the page audit
	 *
	 * precondition: level != null
	 */
	public void setLevel(AuditLevel level) {
		assert level != null;

		this.level = level.getShortName();
	}

	/**
	 * Returns the status of the page audit.
	 * @return the status of the page audit
	 */
	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	/**
	 * Sets the status of the page audit.
	 * @param status the status of the page audit
	 */
	public void setStatus(ExecutionStatus status) {
		assert status != null;
		this.status = status.getShortName();
	}
}
