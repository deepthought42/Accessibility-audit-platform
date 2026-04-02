package com.looksee.models;

import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Defines an action in name only
 */
@Getter
@Setter
@NoArgsConstructor
@Node
public class ActionOLD extends LookseeObject {
	
	private String name;
	private String value;
		
	/**
	 * Constructs an {@link ActionOLD} object with the given name
	 *
	 * @param action_name name of the action
	 *
	 * precondition: action_name != null
	 */
	public ActionOLD(String action_name) {
		assert action_name != null : "action_name must not be null";
		this.name = action_name;
		this.value = "";
		this.setKey(generateKey());
	}
	
	/**
	 * Constructs an {@link ActionOLD} object with the given name and value
	 *
	 * @param action_name name of the action
	 * @param value value of the action
	 *
	 * precondition: action_name != null
	 * precondition: value != null
	 */
	public ActionOLD(String action_name, String value) {
		assert action_name != null : "action_name must not be null";
		assert value != null : "value must not be null";
		setName(action_name);
		setValue(value);
		this.setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.name;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode(){
		return Objects.hashCode(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public ActionOLD clone() {
		ActionOLD action_clone = new ActionOLD(this.getName(), this.getValue());
		return action_clone;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return "action:"+getName() + ":"+ getValue();
	}
}
