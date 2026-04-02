package com.looksee.models.rules;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.looksee.models.rules.RuleType;
import com.looksee.models.serializer.RuleDeserializer;
import com.looksee.pageBuilder.models.Element;
import com.looksee.pageBuilder.models.LookseeObject;

/**
 * Defines rule to be used to evaluate if a {@link FormField} has a value that satisfies the 
 * rule based on its {@link RuleType}
 *
 * @param <T> a generic value that is used to define the type of value returned
 */
@JsonDeserialize(using = RuleDeserializer.class)
public abstract class Rule extends LookseeObject {

	private String value;
	private String type;

	public RuleType getType() {
		return RuleType.create(this.type.toLowerCase());
	};

	public void setType(RuleType type) {
		this.type = type.getShortName();
	}
	
	public String getValue(){
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	
	
	/**
	 * evaluates the rule to determine if it is satisfied
	 * 
	 * @return boolean value indicating the rule is satisfied or not
	 */
	abstract Boolean evaluate(Element val);	

	
	@Override
	public String generateKey() {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(this.getType()+""+this.getValue());
	}
}
