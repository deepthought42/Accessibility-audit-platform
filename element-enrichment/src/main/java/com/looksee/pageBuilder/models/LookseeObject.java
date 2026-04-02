package com.looksee.pageBuilder.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

/**
 * Universal object that contains values that are expected to exist on all persistable objects within the database
 * @author brand
 *
 */
@Node
public abstract class LookseeObject {
	
	//@Index(unique=true)
	@GeneratedValue
    @Id
	private Long id;

	//@Index(unique=false)
	@Property
	private String key;
	
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@Property
	private LocalDateTime createdAt;
	
	public LookseeObject() {
		setCreatedAt(LocalDateTime.now());
	}
	
	public LookseeObject(String key) {
		setKey(key);
		setCreatedAt(LocalDateTime.now());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.getKey();
	}
	
	/**
	 * @return string of hashCodes identifying unique fingerprint of object by the contents of the object
	 */
	public abstract String generateKey();

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime created_at) {
		this.createdAt = created_at;
	}
	
	public Long getId() {
		return this.id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
}
