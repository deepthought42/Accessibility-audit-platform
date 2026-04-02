package com.looksee.pageBuilder.models.journeys;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

/**
 * Core Message object that defines global fields that are to be used by apage_idll Message objects
 */
public abstract class Message {
	private String messageId;
	
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime publishTime;
	private long accountId;
	private long domainId;
	private long domainAuditRecordId;
	
	public Message(){
		setAccountId(-1);
		this.messageId = UUID.randomUUID().toString();
		this.publishTime = LocalDateTime.now();
	}
	
	/**
	 * 
	 * @param account_id
	 * @param audit_record_id TODO
	 * @param domain eg. example.com
	 */
	public Message(long account_id, long audit_record_id, long domain_id){
		this.messageId = UUID.randomUUID().toString();
		this.publishTime = LocalDateTime.now();
		
		setAccountId(account_id);
		setDomainAuditRecordId(audit_record_id);
		setDomainId(domain_id);
	}
	
	public long getAccountId() {
		return accountId;
	}

	protected void setAccountId(long account_id) {
		this.accountId = account_id;
	}

	public long getDomainAuditRecordId() {
		return domainAuditRecordId;
	}

	public void setDomainAuditRecordId(long audit_record_id) {
		this.domainAuditRecordId = audit_record_id;
	}

	public long getDomainId() {
		return domainId;
	}

	public void setDomainId(long domain_id) {
		this.domainId = domain_id;
	}
	
	public String getMessageId() {
		return messageId;
    }

    public void setMessageId(String messageId) {
    	this.messageId = messageId;
    }

    public LocalDateTime getPublishTime() {
    	return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
    	this.publishTime = publishTime;
    }
}
