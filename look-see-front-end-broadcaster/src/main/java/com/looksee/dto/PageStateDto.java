package com.looksee.dto;

import com.looksee.frontEndBroadcaster.models.PageState;

import lombok.Getter;
import lombok.Setter;

/**
 * Data transfer object representing a {@link PageState} in a format readable for browser real-time audit feedback and browser extensions
 */
public class PageStateDto {

	@Getter
	@Setter
	private long auditRecordId;
	
	@Getter
	@Setter
	private String key;

	@Getter
	@Setter
	private String url;
	
	public PageStateDto(long audit_record_id, PageState page){
		setAuditRecordId(audit_record_id);
		setKey(page.getKey());
		setUrl(page.getUrl());
	}
}
