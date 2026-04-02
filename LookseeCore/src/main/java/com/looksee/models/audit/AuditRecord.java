package com.looksee.models.audit;

import com.looksee.models.LookseeObject;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.ExecutionStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Record detailing a set of {@link Audit audits} and the outcomes of those
 *  audits. Outcomes include progress of audits and final scores of audits.
 *
 * invariant: status != null
 * invariant: colors != null
 * invariant: auditLabels != null
 * invariant: contentAuditProgress >= 0.0
 * invariant: aestheticAuditProgress >= 0.0
 * invariant: infoArchitectureAuditProgress >= 0.0
 * invariant: dataExtractionProgress >= 0.0
 */
@Getter
@Setter
@Node
public class AuditRecord extends LookseeObject {
	
	private String status;
	private String level;
	private String url;

	private String statusMessage;
	private String targetUserAge;

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
	private String targetUserEducation;

	
	/**
	 * Constructs an empty {@link AuditRecord}
	 */
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
		setTargetUserEducation("");
		setTargetUserAge("");
		setAuditLabels(new HashSet<>());
	}
	
	/**
	 * Constructor
	 *
	 * @param id the id of the audit record
	 * @param status the status of the audit record
	 * @param level the level of the audit record
	 * @param key the key of the audit record
	 * @param startTime the start time of the audit record
	 * @param aestheticScore the aesthetic score of the audit record
	 * @param aestheticAuditProgress the aesthetic audit progress of the audit record
	 * @param contentAuditScore the content audit score of the audit record
	 * @param contentAuditProgress the content audit progress of the audit record
	 * @param infoArchScore the info architecture score of the audit record
	 * @param infoArchAuditProgress the info architecture audit progress of the audit record
	 * @param dataExtractionProgress the data extraction progress of the audit record
	 * @param created_at the creation time of the audit record
	 * @param endTime the end time of the audit record
	 * @param url the url of the audit record
	 *
	 * precondition: status != null
	 * precondition: level != null
	 * precondition: key != null
	 * precondition: url != null
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
		assert status != null;
		assert level != null;
		assert key != null;
		assert url != null;

		setId(id);
		setStatus(status);
		setLevel(level);
		setKey(key);
		setStartTime(startTime);
		setAestheticAuditProgress(aestheticAuditProgress);
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
		setTargetUserEducation("");
		setTargetUserAge("");
		setAuditLabels(new HashSet<>());
	}

	/**
	 * Generates a key for the audit record.
	 *
	 * @return the key for the audit record
	 */
	public String generateKey() {
		return "auditrecord:" + UUID.randomUUID().toString() + org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	/**
	 * Gets the status of the audit record.
	 *
	 * @return the status of the audit record
	 */
	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	/**
	 * Sets the status of the audit record.
	 *
	 * @param status the status to set
	 *
	 * precondition: status != null
	 */
	public void setStatus(ExecutionStatus status) {
		assert status != null;
		this.status = status.getShortName();
	}

	/**
	 * Gets the level of the audit record.
	 *
	 * @return the level of the audit record
	 */
	public AuditLevel getLevel() {
		return AuditLevel.create(level);
	}

	/**
	 * Sets the level of the audit record.
	 *
	 * @param level the level to set
	 *
	 * precondition: level != null
	 */
	public void setLevel(AuditLevel level) {
		assert level != null;
		this.level = level.toString();
	}
	
	@Override
	public String toString() {
		return this.getId()+", "+this.getKey()+", "+this.getUrl()+", "+this.getStatus()+", "+this.getStatusMessage();
	}
	
	/**
	 * Checks if the audit record is complete.
	 *
	 * @return true if the audit record is complete, false otherwise
	 */
	public boolean isComplete() {
		return (this.getAestheticAuditProgress() >= 1.0
				&& this.getContentAuditProgress() >= 1.0
				&& this.getInfoArchitectureAuditProgress() >= 1.0
				&& this.getDataExtractionProgress() >= 1.0);
	}
	
	/**
	 * Clones the {@link AuditRecord}
	 *
	 * @return the cloned {@link AuditRecord}
	 */
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
								getInfoArchitectureAuditProgress(),
								getDataExtractionProgress(),
								getCreatedAt(),
								getEndTime(),
								getUrl());
	}
	
	/**
	 * Adds a color to the {@link AuditRecord}.
	 * 
	 * @param color the color to add
	 * @return true if the color was added successfully, false otherwise
	 *
	 * precondition: color != null
	 * precondition: !color.isEmpty()
	 */
	public boolean addColor(String color){
		assert color != null;
		assert !color.isEmpty();
		if(!getColors().contains(color)) {
			return getColors().add(color);
		}
		
		return true;
	}
}