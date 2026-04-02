package com.looksee.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.looksee.models.Element;

/**
 * A rule that checks if an element's attribute value matches an email pattern
 */
public class EmailPatternRule extends Rule {

	/**
	 * Constructs a new {@link EmailPatternRule} rule
	 */
	public EmailPatternRule() {
		setType(RuleType.EMAIL_PATTERN);
		setValue("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$");
		setKey(super.generateKey());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(Element page_element) {
		for(String attribute: page_element.getAttributes().keySet()){
			if("vals".contentEquals(attribute)){
				String pattern = "/^" + page_element.getAttributes().get(attribute).toString() + " $/";
				Matcher matcher = Pattern.compile(getValue()).matcher(pattern);
				return matcher.matches();
			}
		}
		return null;
	}
}
