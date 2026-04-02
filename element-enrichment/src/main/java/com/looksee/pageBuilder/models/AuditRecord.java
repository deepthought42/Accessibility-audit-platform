package com.looksee.pageBuilder.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Node;

import com.looksee.pageBuilder.models.enums.AuditLevel;
import com.looksee.pageBuilder.models.enums.AuditName;
import com.looksee.pageBuilder.models.enums.ExecutionStatus;

/**
 * Record detailing an set of {@link Audit audits} and the outcomes of those
 *  audits. Outcomes include progress of audits and final scores of audits 
 */
@Node
public class AuditRecord extends LookseeObject {
	private String url;
	private String status;
	private String statusMessage;
	private String level;
	private String targetUserAge;
	private String targetUserEducation;

	private LocalDateTime startTime;
	private LocalDateTime endTime;
	
	private double contentAuditProgress;
	private double contentAuditScore;
	private double infoArchitectureAuditProgress;
	private double infoArchScore;
	private double aestheticAuditProgress;
	private double aestheticScore;
	private double dataExtractionProgress;

	private Set<AuditName> auditLabels;

	//DESIGN SYSTEM VALUES
	private List<String> colors;
	
	public AuditRecord() {
		setStartTime(LocalDateTime.now());
		setStatus(ExecutionStatus.UNKNOWN);
		setUrl("");
		setStatusMessage("");
		setLevel(AuditLevel.UNKNOWN);
		setContentAuditProgress(0.0);
		setContentAuditScore(0.0);
		setInfoArchitectureAuditProgress(0.0);
		setInfoArchScore(0.0);
		setAestheticAuditProgress(0.0);
		setAestheticScore(0.0);
		setDataExtractionProgress(0.0);
		setColors(new ArrayList<String>());
	}
	
	/**
	 * Constructor
	 * 
	 * @param id
	 * @param status
	 * @param level
	 * @param key
	 * @param startTime
	 * @param aestheticScore
	 * @param aestheticAuditProgress
	 * @param contentAuditScore
	 * @param contentAuditProgress
	 * @param infoArchScore
	 * @param infoArchAuditProgress
	 * @param dataExtractionProgress
	 * @param created_at
	 * @param endTime
	 * @param url
	 */
	public AuditRecord(long id, 
					   ExecutionStatus status, 
					   AuditLevel level, 
					   String key, 
					   LocalDateTime startTime,
					   double aestheticScore, 
					   double aestheticAuditProgress, 
					   double contentAuditScore, 
					   double contentAuditProgress,
					   double infoArchScore, 
					   double infoArchAuditProgress, 
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
		setAestheticScore(aestheticScore);
		setContentAuditScore(contentAuditScore);
		setContentAuditProgress(contentAuditProgress);
		setInfoArchScore(infoArchScore);
		setInfoArchitectureAuditProgress(infoArchAuditProgress);
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

	public double getContentAuditScore() {
		return contentAuditScore;
	}

	public void setContentAuditScore(double content_audit_score) {
		this.contentAuditScore = content_audit_score;
	}
	
	public double getInfoArchScore() {
		return infoArchScore;
	}

	public void setInfoArchScore(double info_arch_score) {
		this.infoArchScore = info_arch_score;
	}

	public double getAestheticScore() {
		return aestheticScore;
	}

	public void setAestheticScore(double aesthetic_score) {
		this.aestheticScore = aesthetic_score;
	}

	public double getDataExtractionProgress() {
		return dataExtractionProgress;
	}

	public void setDataExtractionProgress(double data_extraction_progress) {
		this.dataExtractionProgress = data_extraction_progress;
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

	public Set<AuditName> getAuditLabels() {
		return auditLabels;
	}

	public void setAuditLabels(Set<AuditName> auditLabels) {
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
							   getAestheticScore(), 
							   getAestheticAuditProgress(), 
							   getContentAuditScore(), 
							   getContentAuditProgress(), 
							   getInfoArchScore(), 
							   getInfoArchitechtureAuditProgress(),
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