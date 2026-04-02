package com.looksee.pageBuilder.models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.looksee.pageBuilder.models.enums.AuditLevel;
import com.looksee.pageBuilder.models.enums.AuditName;
import com.looksee.pageBuilder.models.enums.ExecutionStatus;

/**
 * Record detailing an set of {@link Audit audits}.
 */
@Node
public class DomainAuditRecord extends AuditRecord {
	
	@Relationship(type = "HAS")
	private Set<PageAuditRecord> pageAuditRecords;	

	public DomainAuditRecord() {
		super();
		setAudits(new HashSet<>()); 
	}
	
	/**
	 * Constructor
	 * 
	 * @param audit_stats {@link AuditStats} object with statics for audit progress
	 * @param level TODO
	 * 
	 * @pre audit_stats != null;
	 */
	public DomainAuditRecord(ExecutionStatus status, 
							Set<AuditName> audit_list) {
		super();
		assert status != null;
		
		setAudits(new HashSet<>());
		setStatus(status);
		setLevel( AuditLevel.DOMAIN);
		setStartTime(LocalDateTime.now());
		setAestheticAuditProgress(0.0);
		setContentAuditProgress(0.0);
		setInfoArchitectureAuditProgress(0.0);
		setDataExtractionProgress(0.0);
		setAuditLabels(audit_list);
		setKey(generateKey());
	}

	public String generateKey() {
		return "domainauditrecord:"+UUID.randomUUID().toString()+org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	public Set<PageAuditRecord> getAudits() {
		return pageAuditRecords;
	}

	public void setAudits(Set<PageAuditRecord> audits) {
		this.pageAuditRecords = audits;
	}

	public void addAudit(PageAuditRecord audit) {
		this.pageAuditRecords.add( audit );
	}
	
	public void addAudits(Set<PageAuditRecord> audits) {
		this.pageAuditRecords.addAll( audits );
	}
}
