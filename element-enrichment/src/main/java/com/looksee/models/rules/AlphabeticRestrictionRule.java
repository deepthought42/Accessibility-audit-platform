package com.looksee.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.looksee.pageBuilder.models.Element;

/**
 * Defines a {@link Rule} where all letters a-z are not allowed regardless of case
 */
public class AlphabeticRestrictionRule extends Rule{
	
	public AlphabeticRestrictionRule() {
		setValue("[a-zA-Z]");
		setType(RuleType.ALPHABETIC_RESTRICTION);
		setKey(generateKey());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(Element elem) {
		Pattern pattern = Pattern.compile(getValue());

        Matcher matcher = pattern.matcher(elem.getText());
		return !matcher.matches();
	}
}
