package com.looksee.models;

import lombok.Getter;
import lombok.Setter;

/**
 * Stores a label
 */
@Getter
@Setter
public class Label extends LookseeObject{
	private String description;
	private float score;
	
	/**
	 * Constructs a new {@link Label}
	 */
	public Label() {
		setDescription("");
		setScore(0.0F);
	}
	
	/**
	 * Constructs a new {@link Label}
	 *
	 * @param description the description of the label
	 * @param score the score of the label
	 *
	 * precondition: description != null
	 */
	public Label(String description, float score) {
		assert description != null : "description must not be null";

		setDescription(description);
		setScore(score);
		setKey(generateKey());
	}

	/**
	 * Generates a key for the label
	 *
	 * @return the key for the label
	 */
	@Override
	public String generateKey() {
		return "label::"+getDescription();
	}
}
