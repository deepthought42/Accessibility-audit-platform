package com.looksee.pageBuilder.models.message;

import com.looksee.pageBuilder.models.enums.AuditCategory;

import lombok.Getter;
import lombok.Setter;

public class AuditError extends Message{
	private String error_message;
	private AuditCategory audit_category;
	private double progress;

	@Getter
	@Setter
	private long auditRecordId;
	
	public AuditError(long accountId, 
					  long auditRecordId, 
					  String error_message,
					  AuditCategory category, 
					  double progress
	) {
		super(accountId);
		setErrorMessage(error_message);
		setAuditCategory(category);
		setProgress(progress);
		setAuditRecordId(auditRecordId);
	}

	public String getErrorMessage() {
		return error_message;
	}

	public void setErrorMessage(String error_message) {
		this.error_message = error_message;
	}

	public AuditCategory getAuditCategory() {
		return audit_category;
	}

	public void setAuditCategory(AuditCategory audit_category) {
		this.audit_category = audit_category;
	}

	public double getProgress() {
		return progress;
	}

	public void setProgress(double progress) {
		this.progress = progress;
	}

}
