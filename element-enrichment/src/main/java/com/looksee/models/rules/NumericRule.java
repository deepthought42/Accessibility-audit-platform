package com.looksee.models.rules;

import org.apache.commons.lang3.StringUtils;

import com.looksee.pageBuilder.models.Element;


/**
 * Defines a min/max value or length {@link Rule} on a {@link Element}
 */
public class NumericRule extends Rule{
	public NumericRule(){}
	
	/**
	 * @param type
	 * @param value the length of the value allowed written as a {@linkplain String}. (eg. "3" -> length 3)
	 */
	public NumericRule(RuleType type, String value){
		setType(type);
		setValue(value);
		setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(Element elem) {
		for(String attribute: elem.getAttributes().keySet()){
			if("val".equals(attribute)){
				String field_value = elem.getAttributes().get(attribute).toString();
				if(this.getType().equals(RuleType.MAX_LENGTH)){
					return field_value.length() <= Integer.parseInt(this.getValue());
				}
				else if(this.getType().equals(RuleType.MIN_LENGTH)){
					return field_value.length() >= Integer.parseInt(this.getValue());
				}
				else if(this.getType().equals(RuleType.MIN_VALUE)){
					return Integer.parseInt(field_value) >= Integer.parseInt(this.getValue());
				}
				else if(this.getType().equals(RuleType.MAX_VALUE)){
					return Integer.parseInt(field_value)  <= Integer.parseInt(this.getValue());
				}
			}
		}
		return false;
	}
	
	public static String generateRandomAlphabeticString(int str_length){
		return StringUtils.repeat("a", str_length);
	}
}
