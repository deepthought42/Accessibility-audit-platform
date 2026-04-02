package com.looksee.models.audit;

import com.looksee.models.PageState;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.ExecutionStatus;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Record detailing a set of {@link Audit audits} performed on a page.
 * This class extends {@link AuditRecord} to store audit results specific to a single page.
 * Each record contains the audits performed, the page state being audited, and statistics about the audit process.
 *
 * <p><b>Class Invariants:</b>
 * <ul>
 *   <li>The audits set must never be null (initialized to empty set if not provided)</li>
 *   <li>The key must be unique for each record</li>
 *   <li>The audit level must always be PAGE</li>
 * </ul>
 */
@Getter
@Setter
@Node
public class PageAuditRecord extends AuditRecord {
	/**
	 * The set of audits performed on the page.
	 * Each audit represents a specific check or evaluation performed.
	 */
	@Relationship(type = "HAS")
	private Set<Audit> audits;
	
	/**
	 * The page state that was audited.
	 * Contains the state of the page at the time of the audit.
	 */
	@Relationship(type = "FOR")
	private PageState pageState;

	/**
	 * The total number of elements found during the audit.
	 */
	private long elementsFound;

	/**
	 * The number of elements that were actually reviewed during the audit.
	 */
	private long elementsReviewed;
	
	/**
	 * Creates a new empty page audit record.
	 * Initializes the audits set to an empty HashSet and generates a unique key.
	 */
	public PageAuditRecord() {
		setAudits(new HashSet<>());
		setKey(generateKey());
	}
	
	/**
	 * Creates a new page audit record with the specified parameters.
	 *
	 * @param status the execution status of the audit
	 * @param audits the set of audits performed. Must not be null
	 * @param page_state the state of the page being audited. Must not be null
	 * @param is_part_of_domain_audit whether this audit is part of a larger domain audit
	 * @param audit_list the set of audit types that were performed
	 * @throws IllegalArgumentException if status, audits, or page_state is null
	 *
	 * precondition: status != null
	 * precondition: audits != null
	 * precondition: page_state != null
	 * precondition: audit_list != null
	 */
	public PageAuditRecord(
			ExecutionStatus status,
			Set<Audit> audits,
			PageState page_state,
			boolean is_part_of_domain_audit,
			Set<AuditName> audit_list
	) {
		assert audits != null;
		assert status != null;
		assert page_state != null;
		assert audit_list != null;

		setAudits(audits);
		setPageState(page_state);
		setStatus(status);
		setLevel(AuditLevel.PAGE);
		setAuditLabels(audit_list);
		setKey(generateKey());
	}

	/**
	 * Generates a unique key for this audit record.
	 * The key is created using a SHA-256 hash of the current timestamp.
	 *
	 * @return a unique string key for this audit record
	 */
	public String generateKey() {
		return "pageauditrecord:" + org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + " ");
	}

	/**
	 * Adds an audit to the record.
	 *
	 * @param audit the audit to add
	 *
	 * precondition: audit != null
	 */
	public void addAudit(Audit audit) {
		assert audit != null;
		audits.add(audit);
	}

	/**
	 * Removes an audit from the record.
	 *
	 * @param audit the audit to remove
	 *
	 * precondition: audit != null
	 */
	public void removeAudit(Audit audit) {
		assert audit != null;
		audits.remove(audit);
	}

	/**
	 * Adds a set of audits to the record.
	 *
	 * @param audits the set of audits to add
	 *
	 * precondition: audits != null
	 */
	public void addAudits(Set<Audit> audits) {
		assert audits != null;
		audits.addAll(audits);
	}
}
