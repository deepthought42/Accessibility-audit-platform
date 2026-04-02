package com.looksee.frontEndBroadcaster.models.message;

import com.looksee.frontEndBroadcaster.models.enums.AuditCategory;
import com.looksee.frontEndBroadcaster.models.enums.AuditLevel;

import lombok.Getter;
import lombok.Setter;

/**
 * Intended to contain information about progress of an audit
 */
public class AuditProgressUpdate extends Message {
	@Getter
	@Setter
	private long pageAuditId;
	
	@Getter
	@Setter
	private AuditCategory category;
	
	@Getter
	@Setter
	private AuditLevel level;
	
	@Getter
	@Setter
	private double progress;
	
	@Getter
	@Setter
	private String message;
	
	public AuditProgressUpdate() {	}
	
	public AuditProgressUpdate(
			long account_id,
			double progress,
			String message,
			AuditCategory category,
			AuditLevel level,
			long page_audit_id
	) {
		super(account_id);
		setProgress(progress);
		setMessage(message);
		setCategory(category);
		setLevel(level);
		setPageAuditId(page_audit_id);
	}
}
