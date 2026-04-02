package com.looksee.pageBuilder.models.message;

import lombok.Getter;
import lombok.Setter;

public class PageDataExtractionError extends Message {
	private String url;
	private String errorMessage;
	private long audit_record_id;
	
	@Getter
	@Setter
	private long auditRecordId;

	public PageDataExtractionError(long accountId, 
								   long auditRecordId, 
								   String url, 
								   String error_message) {
		super(accountId);
		setUrl(url);
		setAuditRecordId(auditRecordId);
		setErrorMessage(error_message);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String error_message) {
		this.errorMessage = error_message;
	}

	public long getAuditRecordId() {
		return audit_record_id;
	}

	public void setAuditRecordId(long audit_record_id) {
		this.audit_record_id = audit_record_id;
	}

}
