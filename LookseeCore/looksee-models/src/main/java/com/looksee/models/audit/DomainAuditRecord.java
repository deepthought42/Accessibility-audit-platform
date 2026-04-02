package com.looksee.models.audit;

import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.enums.JourneyStatus;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Record detailing an set of {@link Audit audits}.
 *
 * This class represents a record of an audit performed on a domain. It extends {@link AuditRecord}
 * and contains a set of {@link PageAuditRecord} objects, which represent the audits performed on
 * each page of the domain.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>pageAuditRecords is never null (initialized to empty set by default).</li>
 *   <li>The audit level for a DomainAuditRecord is always DOMAIN.</li>
 * </ul>
 */
@Node
@Getter
@Setter
public class DomainAuditRecord extends AuditRecord {
	
	private int totalPages;
	private Map<String, JourneyStatus> journeyStatusMap;
	
	/**
	 * The page audit records of the domain audit record
	 */
	@Relationship(type = "HAS")
	private Set<PageAuditRecord> pageAuditRecords;


	/**
	 * Constructs a new {@link DomainAuditRecord}
	 */
	public DomainAuditRecord() {
		super();
		setPageAuditRecords(new HashSet<>());
	}
	
	/**
	 * Constructs a new {@link DomainAuditRecord} with the given parameters.
	 *
	 * @param status The status of the audit
	 * @param audit_list The list of audit names
	 *
	 * precondition: status != null;
	 * precondition: audit_list != null;
	 */
	public DomainAuditRecord(ExecutionStatus status,
							Set<AuditName> audit_list) {
		super();
		assert status != null;
		assert audit_list != null;

		setPageAuditRecords(new HashSet<>());
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

	/**
	 * Generates a unique key for the domain audit record
	 *
	 * @return A unique key for the domain audit record
	 */
	public String generateKey() {
		return "domainauditrecord:"+UUID.randomUUID().toString()+org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	/**
	 * Adds a page audit record to the domain audit record
	 *
	 * @param audit The page audit record to add
	 *
	 * precondition: audit != null
	 */
	public void addAudit(PageAuditRecord audit) {
		assert audit != null;
		this.pageAuditRecords.add( audit );
	}
	
	/**
	 * Adds a set of page audit records to the domain audit record
	 *
	 * @param audits The set of page audit records to add
	 *
	 * precondition: audits != null
	 */
	public void addAudits(Set<PageAuditRecord> audits) {
		assert audits != null;
		this.pageAuditRecords.addAll( audits );
	}
}
