package com.looksee.models.journeys;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.Getter;
import lombok.Setter;

/**
 * Core Message object that defines global fields that are to be used by all Message objects.
 *
 * <p><b>Class Invariants:</b>
 * <ul>
 *   <li>messageId is always a valid UUID string</li>
 *   <li>publishTime is never null</li>
 *   <li>accountId is -1 or a positive number</li>
 *   <li>domainId is a positive number when set</li>
 *   <li>domainAuditRecordId is a positive number when set</li>
 * </ul>
 */
@Getter
@Setter
public abstract class Message {
	/**
	 * The message ID
	 */
	private String messageId;
	
	/**
	 * The publish time
	 */
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime publishTime;

	/**
	 * The account ID
	 */
	private long accountId;
	
	/**
	 * The domain ID
	 */
	private long domainId;
	
	/**
	 * The domain audit record ID
	 */
	private long domainAuditRecordId;
	
	/**
	 * Creates a new message.
	 */
	public Message(){
		setAccountId(-1);
		this.messageId = UUID.randomUUID().toString();
		this.publishTime = LocalDateTime.now();
	}
	
	/**
	 * Creates a new message.
	 * @param account_id the account id
	 * @param audit_record_id the audit record id
	 * @param domain_id the domain id
	 */
	public Message(long account_id, long audit_record_id, long domain_id){
		this.messageId = UUID.randomUUID().toString();
		this.publishTime = LocalDateTime.now();
		
		setAccountId(account_id);
		setDomainAuditRecordId(audit_record_id);
		setDomainId(domain_id);
	}
}
