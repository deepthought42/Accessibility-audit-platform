package com.looksee.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.looksee.frontEndBroadcaster.models.enums.AuditLevel;
import com.looksee.frontEndBroadcaster.models.enums.ExecutionStatus;

import lombok.Getter;
import lombok.Setter;

/**
 * Client facing audit record.
 */
public class AuditRecordDto {
	@Getter
	@Setter
    private long id;

	@Getter
	@Setter
    private String key;

	@Getter
	@Setter
    private String url;

	@Getter
	@Setter
	private ExecutionStatus status;

	@Getter
	@Setter
	private AuditLevel level;

	@Getter
	@Setter
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	private LocalDateTime startTime;

	@Getter
	@Setter
	private double contentAuditScore;
	
	@Getter
	@Setter
	private double infoArchScore;
	
	@Getter
	@Setter
	private double aestheticScore;

    public AuditRecordDto() {
		setStartTime(LocalDateTime.now());
		setStatus(ExecutionStatus.UNKNOWN);
		setUrl("");
		setLevel(AuditLevel.UNKNOWN);
		setContentAuditScore(0.0);
		setInfoArchScore(0.0);
		setAestheticScore(0.0);
	}
	
	/**
	 * Constructor
	 * @param level TODO
	 * 
	 */
	public AuditRecordDto(long id,
					   ExecutionStatus status,
					   AuditLevel level,
					   LocalDateTime startTime,
					   double aestheticScore,
					   double contentAuditScore,
					   double infoArchScore,
					   LocalDateTime created_at,
					   String url
	) {
		setId(id);
		setStatus(status);
		setLevel(level);
		setStartTime(startTime);
		setAestheticScore(aestheticScore);
		setContentAuditScore(contentAuditScore);
		setInfoArchScore(infoArchScore);
		setStartTime(created_at);
		setUrl(url);
	}

	public String generateKey() {
		return "auditrecord:" + UUID.randomUUID().toString() + org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}
	
	@Override
	public String toString() {
		return this.getId()+", "+this.getUrl()+", "+this.getStatus();
	}
}
