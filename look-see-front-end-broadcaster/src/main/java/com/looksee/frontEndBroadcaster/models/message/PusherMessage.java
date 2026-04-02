package com.looksee.frontEndBroadcaster.models.message;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.Getter;
import lombok.Setter;

/**
 * Core PusherMessage object that defines global fields that are to be used by apage_idll PusherMessage objects
 */
public abstract class PusherMessage {

	@Getter
	@Setter
	private String messageId;
	
	@Getter
	@Setter
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime publishTime;
	
	public PusherMessage(){
		this.messageId = UUID.randomUUID().toString();
		this.publishTime = LocalDateTime.now();
	}
	
	/**
	 * 
	 * @param account_id
	 * @param domain eg. example.com
	 */
	public PusherMessage(long account_id){
		this.messageId = UUID.randomUUID().toString();
		this.publishTime = LocalDateTime.now();
	}
}
