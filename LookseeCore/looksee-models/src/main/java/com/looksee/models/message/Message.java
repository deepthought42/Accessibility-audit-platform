package com.looksee.models.message;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.Getter;
import lombok.Setter;

/**
 * Core Message object that defines global fields that are to be used by apage_idll Message objects
 */
@Getter
@Setter
public abstract class Message {
	/**
	 * The message id
	 */
	private String messageId;

	/**
	 * The publish time
	 */
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime publishTime;

	/**
	 * The account id
	 */
	private long accountId;
	
	/**
	 * Creates a new Message
	 */
	public Message(){
		setAccountId(-1);
		this.messageId = UUID.randomUUID().toString();
		this.publishTime = LocalDateTime.now();
	}
	
	/**
	 * Constructs a {@link Message} with the given account id
	 *
	 * @param accountId the account id
	 */
	public Message(long accountId){
		this.messageId = UUID.randomUUID().toString();
		this.publishTime = LocalDateTime.now();
		
		setAccountId(accountId);
	}
}
