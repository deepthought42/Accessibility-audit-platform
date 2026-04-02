package com.looksee.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.looksee.pageBuilder.models.Element;


/**
 * Defines a regular expression based rule that applies to the entire text content(beginning to end) of a field.
 */
public class PatternRule extends Rule {
	public PatternRule(){}
	
	public PatternRule(String pattern){
		setValue(pattern);
		setType(RuleType.PATTERN);
		setKey(generateKey());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(Element elem) {
		for(String attribute: elem.getAttributes().keySet()){
			if("vals".contentEquals(attribute)){
				String pattern = "/^" + elem.getAttributes().get(attribute).toString() + " $/";
				Matcher matcher = Pattern.compile(getValue()).matcher(pattern);
			    return matcher.matches();
			}
		}
		return null;
	}
}
