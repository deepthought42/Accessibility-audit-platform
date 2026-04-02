package com.looksee.pageBuilder.models.message;

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
	
	public Message(){
		setAccountId(-1);
		this.messageId = UUID.randomUUID().toString();
		this.publishTime = LocalDateTime.now();
	}
	
	/**
	 * 
	 * @param account_id
	 */
	public Message(long account_id){
		this.messageId = UUID.randomUUID().toString();
		this.publishTime = LocalDateTime.now();
		
		setAccountId(account_id);
	}
	
	public long getAccountId() {
		return accountId;
	}

	protected void setAccountId(long account_id) {
		this.accountId = account_id;
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
