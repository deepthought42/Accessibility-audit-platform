package com.looksee.models.audit;

import com.looksee.models.SimplePage;
import com.looksee.models.enums.ExecutionStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Record detailing an set of {@link Audit audits}.
 */
@NoArgsConstructor
@Getter
@Setter
public class PageAudits {
	private ElementIssueTwoWayMapping elementIssueMap;
	private SimplePage simplePage;
	private String status;
	private long auditRecordId;
	
	/**
	 * Constructor
	 * @param status {@link ExecutionStatus} of the audit
	 * @param element_issue_map {@link ElementIssueTwoWayMapping} of the audit
	 * @param page_state {@link SimplePage} of the audit
	 * @param audit_record_id id of the audit record
	 * 
	 * precondition: element_issue_map != null;
	 * precondition: page_state != null;
	 * precondition: status != null;
	 */
	public PageAudits(
			ExecutionStatus status,
			ElementIssueTwoWayMapping element_issue_map,
			SimplePage page_state, long audit_record_id
	) {
		assert element_issue_map != null;
		assert page_state != null;
		assert status != null;
		
		setAuditRecordId(audit_record_id);
		setElementIssueMap(element_issue_map);
		setSimplePage(page_state);
		setStatus(status);
	}

	/**
	 * Generates a key for the audit
	 * @return key for the audit
	 */
	public String generateKey() {
		return "auditrecord:"+UUID.randomUUID().toString()+org.apache.commons.codec.digest.DigestUtils.sha256Hex(System.currentTimeMillis() + "");
	}

	/**
	 * Retrieves the {@link ExecutionStatus} for the audit
	 * @return {@link ExecutionStatus} for the audit
	 */
	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	/**
	 * Sets the {@link ExecutionStatus} for the audit
	 * @param status {@link ExecutionStatus} for the audit
	 */
	public void setStatus(ExecutionStatus status) {
		this.status = status.getShortName();
	}
}
