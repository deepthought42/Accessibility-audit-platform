package com.looksee.models.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.ExecutionStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Client facing audit record.
 *
 * invariant: status is always a valid {@link ExecutionStatus} short name or null
 * invariant: level is always a valid {@link AuditLevel} string or null
 */
@Getter
@Setter
public class AuditRecordDto {
    private long id;
    private String url;
	private String status;
	private String statusMessage;
	private String level;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	private LocalDateTime startTime;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	private LocalDateTime endTime;

	private double contentAuditScore;
	private double infoArchScore;
	private double aestheticScore;
	
	private String targetUserAge;
	private String targetUserEducation;

	/**
	 * Constructs an {@link AuditRecordDto} object
	 */
    public AuditRecordDto() {
		setStartTime(LocalDateTime.now());
		setStatus(ExecutionStatus.UNKNOWN);
		setUrl("");
		setStatusMessage("");
		setLevel(AuditLevel.UNKNOWN);
		setContentAuditScore(0.0);
		setInfoArchScore(0.0);
		setAestheticScore(0.0);
	}
	
	/**
	 * Constructs an {@link AuditRecordDto} object
	 *
	 * @param id id of the audit record
	 * @param status status of the audit record
	 * @param level level of the audit record
	 * @param startTime start time of the audit record
	 * @param aestheticScore aesthetic score of the audit record
	 * @param contentAuditScore content audit score of the audit record
	 * @param infoArchScore info architecture score of the audit record
	 * @param created_at creation time of the audit record
	 * @param endTime end time of the audit record
	 * @param url url of the audit record
	 *
	 * precondition: status != null
	 * precondition: level != null
	 * precondition: startTime != null
	 * precondition: created_at != null
	 * precondition: url != null
	 */
	public AuditRecordDto(long id,
						ExecutionStatus status,
						AuditLevel level,
						LocalDateTime startTime,
						double aestheticScore,
						double contentAuditScore,
						double infoArchScore,
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
		setAestheticScore(aestheticScore);
		setContentAuditScore(contentAuditScore);
		setInfoArchScore(infoArchScore);
		setStartTime(created_at);
		setEndTime(endTime);
		setUrl(url);
	}

	/**
	 * Generates a key for the audit record
	 * 
	 * @return {@link String} representing the key
	 */
	public String generateKey() {
		return "auditrecord:" + UUID.randomUUID().toString() + org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	/**
	 * Gets the status of the audit record
	 * 
	 * @return {@link ExecutionStatus}
	 */
	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	/**
	 * Sets the status of the audit record
	 *
	 * @param status {@link ExecutionStatus}
	 *
	 * precondition: status != null
	 */
	public void setStatus(ExecutionStatus status) {
		assert status != null;

		this.status = status.getShortName();
	}

	/**
	 * Gets the level of the audit record
	 * 
	 * @return {@link AuditLevel}
	 */
	public AuditLevel getLevel() {
		return AuditLevel.create(level);
	}

	/**
	 * Sets the level of the audit record
	 *
	 * @param level {@link AuditLevel}
	 *
	 * precondition: level != null
	 */
	public void setLevel(AuditLevel level) {
		assert level != null;

		this.level = level.toString();
	}
	
	@Override
	public String toString() {
		return this.getId()+", "+this.getUrl()+", "+this.getStatus()+", "+this.getStatusMessage();
	}
	
	/**
	 * Checks if the audit record is complete
	 * 
	 * @return true if the audit record is complete, false otherwise
	 */
	public boolean isComplete() {
		return false;
	}
}
