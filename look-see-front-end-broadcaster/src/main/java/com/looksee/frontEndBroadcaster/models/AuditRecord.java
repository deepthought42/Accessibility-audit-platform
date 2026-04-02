package com.looksee.frontEndBroadcaster.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Node;

import com.looksee.frontEndBroadcaster.models.enums.AuditLevel;
import com.looksee.frontEndBroadcaster.models.enums.AuditName;
import com.looksee.frontEndBroadcaster.models.enums.ExecutionStatus;

/**
 * Record detailing an set of {@link Audit audits}.
 */
@Node
public class AuditRecord extends LookseeObject {
	private String url;
	
	private String status;
	private String statusMessage;
	private String level;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private double contentAuditProgress;
	private String contentAuditMsg;
	
	private double infoArchitectureAuditProgress;
	private String infoArchMsg;
	
	private double aestheticAuditProgress;
	private String aestheticMsg;
	
	private double dataExtractionProgress;
	private String dataExtractionMsg;

	private String targetUserAge;
	private String targetUserEducation;
	
	private List<AuditName> auditLabels;


	//DESIGN SYSTEM VALUES
	private List<String> colors;
	
	public AuditRecord() {
		setStartTime(LocalDateTime.now());
		setStatus(ExecutionStatus.UNKNOWN);
		setUrl("");
		setStatusMessage("");
		setLevel(AuditLevel.UNKNOWN);
		setContentAuditProgress(0.0);
		setContentAuditMsg("");
		setInfoArchitectureAuditProgress(0.0);
		setInfoArchMsg("");
		setAestheticAuditProgress(0.0);
		setAestheticMsg("");
		setDataExtractionProgress(0.0);
		setDataExtractionMsg("");
		setColors(new ArrayList<String>());
	}
	
	/**
	 * Constructor
	 * @param level TODO
	 * 
	 */
	public AuditRecord(long id, 
					   ExecutionStatus status, 
					   AuditLevel level, 
					   String key, 
					   LocalDateTime startTime,
					   double aestheticAuditProgress, 
					   String aestheticMsg, 
					   String contentAuditMsg, 
					   double contentAuditProgress,
					   String infoArchMsg, 
					   double infoArchAuditProgress, 
					   String dataExtractionMsg, 
					   double dataExtractionProgress,
					   LocalDateTime created_at, 
					   LocalDateTime endTime, 
					   String url
	) {
		setId(id);
		setStatus(status);
		setLevel(level);
		setKey(key);
		setStartTime(endTime);
		setAestheticAuditProgress(dataExtractionProgress);
		setAestheticMsg(aestheticMsg);
		setContentAuditMsg(contentAuditMsg);
		setContentAuditProgress(contentAuditProgress);
		setInfoArchMsg(infoArchMsg);
		setInfoArchitectureAuditProgress(infoArchAuditProgress);
		setDataExtractionMsg(dataExtractionMsg);
		setDataExtractionProgress(dataExtractionProgress);
		setCreatedAt(created_at);
		setEndTime(endTime);
		setColors(new ArrayList<String>());
		setUrl(url);
	}

	public String generateKey() {
		return "auditrecord:" + UUID.randomUUID().toString() + org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status.getShortName();
	}

	public AuditLevel getLevel() {
		return AuditLevel.create(level);
	}

	public void setLevel(AuditLevel level) {
		this.level = level.toString();
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime start_time) {
		this.startTime = start_time;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime end_time) {
		this.endTime = end_time;
	}
	
	public double getContentAuditProgress() {
		return contentAuditProgress;
	}

	public void setContentAuditProgress(double content_audit_progress) {
		this.contentAuditProgress = content_audit_progress;
	}

	public double getInfoArchitechtureAuditProgress() {
		return infoArchitectureAuditProgress;
	}

	public void setInfoArchitectureAuditProgress(double info_arch_audit_progress) {
		this.infoArchitectureAuditProgress = info_arch_audit_progress;
	}

	public double getAestheticAuditProgress() {
		return aestheticAuditProgress;
	}

	public void setAestheticAuditProgress(double aesthetic_audit_progress) {
		this.aestheticAuditProgress = aesthetic_audit_progress;
	}

	public String getContentAuditMsg() {
		return contentAuditMsg;
	}

	public void setContentAuditMsg(String content_audit_msg) {
		this.contentAuditMsg = content_audit_msg;
	}
	
	public String getInfoArchMsg() {
		return infoArchMsg;
	}

	public void setInfoArchMsg(String info_arch_msg) {
		this.infoArchMsg = info_arch_msg;
	}

	public String getAestheticMsg() {
		return aestheticMsg;
	}

	public void setAestheticMsg(String aesthetic_msg) {
		this.aestheticMsg = aesthetic_msg;
	}

	public double getDataExtractionProgress() {
		return dataExtractionProgress;
	}

	public void setDataExtractionProgress(double data_extraction_progress) {
		this.dataExtractionProgress = data_extraction_progress;
	}

	public String getDataExtractionMsg() {
		return dataExtractionMsg;
	}

	public void setDataExtractionMsg(String data_extraction_msg) {
		this.dataExtractionMsg = data_extraction_msg;
	}

	public String getTargetUserAge() {
		return targetUserAge;
	}

	public void setTargetUserAge(String target_user_age) {
		this.targetUserAge = target_user_age;
	}

	public String getTargetUserEducation() {
		return targetUserEducation;
	}

	public void setTargetUserEducation(String target_user_education) {
		this.targetUserEducation = target_user_education;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String status_message) {
		this.statusMessage = status_message;
	}

	public List<AuditName> getAuditLabels() {
		return auditLabels;
	}

	public void setAuditLabels(List<AuditName> auditLabels) {
		this.auditLabels = auditLabels;
	}
	
	@Override
	public String toString() {
		return this.getId()+", "+this.getKey()+", "+this.getUrl()+", "+this.getStatus()+", "+this.getStatusMessage();
	}
	
	public boolean isComplete() {
		return (this.getAestheticAuditProgress() >= 1.0
				&& this.getContentAuditProgress() >= 1.0
				&& this.getInfoArchitechtureAuditProgress() >= 1.0
				&& this.getDataExtractionProgress() >= 1.0);
	}
	
	@Override
	public AuditRecord clone() {
		return new AuditRecord(getId(),
							   getStatus(),
							   getLevel(),
							   getKey(),
							   getStartTime(),
							   getAestheticAuditProgress(), 
							   getAestheticMsg(), 
							   getContentAuditMsg(), 
							   getContentAuditProgress(), 
							   getInfoArchMsg(), 
							   getInfoArchitechtureAuditProgress(),
							   getDataExtractionMsg(), 
							   getDataExtractionProgress(), 
							   getCreatedAt(), 
							   getEndTime(),
							   getUrl());
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public List<String> getColors() {
		return colors;
	}

	public void setColors(List<String> colors) {
		this.colors = colors;
	}
	
	public boolean addColor(String color){
		if(!getColors().contains(color)) {
			return getColors().add(color);
		}
		
		return true;	
	}
}
