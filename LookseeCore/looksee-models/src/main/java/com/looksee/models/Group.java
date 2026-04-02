package com.looksee.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Defines a name and color used to group {@link Test}s.
 *
 * invariant: name != null
 * invariant: description != null
 */
@Node
@Getter
@Setter
@NoArgsConstructor
public class Group extends LookseeObject {

	private String name;
	private String description;
	
	/**
	 * Construct a new grouping
	 *
	 * @param name 		name of the group
	 *
	 * precondition: name != null
	 * precondition: !name.isEmpty()
	 */
	public Group(String name){
		assert name != null;
		assert !name.isEmpty();
		setName(name);
		setDescription("");
		setKey(generateKey());
	}
	
	/**
	 * Construct a new grouping
	 *
	 * @param name 		name of the group
	 * @param desc		description of the group
	 *
	 * precondition: name != null
	 * precondition: !name.isEmpty()
	 * precondition: desc != null
	 */
	public Group(String name, String desc){
		assert name != null;
		assert !name.isEmpty();
		assert desc != null;
		setName(name);
		setDescription(desc);
		setKey(generateKey());
	}

	/**
	 * Sets the name of the group
	 *
	 * @param name the name of the group
	 *
	 * precondition: name != null
	 * precondition: !name.isEmpty()
	 */
	public void setName(String name) {
		assert name != null;
		assert !name.isEmpty();
		String formatted_name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
		this.name = formatted_name;
	}

	/**
	 * {@inheritDoc}
	 */
	public String generateKey() {
		return "group:"+getName().toLowerCase();
	}
	
}
