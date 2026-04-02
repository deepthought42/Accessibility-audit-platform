package com.looksee.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.Getter;
import lombok.Setter;

/**
 * Universal object that contains values that are expected to exist on all persistable objects within the database.
 *
 * invariant: createdAt != null
 * invariant: key != null after generateKey() is called
 */
@Node
@Getter
@Setter
public abstract class LookseeObject {
	
	@GeneratedValue
    @Id
	private Long id;
	private String key;
	
	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@Property
	@Getter
	private LocalDateTime createdAt;
	
	/**
	 * Constructs a new {@link LookseeObject}
	 */
	public LookseeObject() {
		setCreatedAt(LocalDateTime.now());
	}
	
	/**
	 * Constructs a new {@link LookseeObject} with the given key
	 *
	 * @param key the key of the object
	 *
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public LookseeObject(String key) {
		assert key != null;
		assert !key.isEmpty();
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
	 * Generate a key for the object
	 *
	 * @return string of hashCodes identifying unique fingerprint of object by the contents of the object
	 */
	public abstract String generateKey();
}
