package com.looksee.models.rules;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.looksee.models.Element;
import com.looksee.models.LookseeObject;
import com.looksee.models.serializer.RuleDeserializer;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Defines rule to be used to evaluate if a {@link Element} has a value that satisfies the
 * rule based on its {@link RuleType}
 *
 * invariant: type is always a valid {@link RuleType} short name or null
 */
@Getter
@Setter
@JsonDeserialize(using = RuleDeserializer.class)
@NoArgsConstructor
public abstract class Rule extends LookseeObject {

	private String value;
	private String type;

	/**
	 * Gets the type of the rule
	 *
	 * @return the type of the rule
	 */
	public RuleType getType() {
		return RuleType.create(this.type.toLowerCase());
	};

	/**
	 * Sets the type of the rule
	 *
	 * @param type the type of the rule
	 *
	 * precondition: type != null
	 */
	public void setType(RuleType type) {
		assert type != null;

		this.type = type.getShortName();
	}
	
	/**
	 * evaluates the rule to determine if it is satisfied
	 *
	 * @param val the element to evaluate the rule against
	 * @return boolean value indicating the rule is satisfied or not
	 *
	 * precondition: val != null
	 */
	public abstract Boolean evaluate(Element val);

	/**
	 * Generates a key for the rule
	 *
	 * @return the key for the rule
	 */
	@Override
	public String generateKey() {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(this.getType()+""+this.getValue());
	}
}
