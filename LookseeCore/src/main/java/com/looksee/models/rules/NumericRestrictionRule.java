package com.looksee.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.looksee.models.Element;

/**
 * Defines a {@link Rule} where the numbers 1-9 cannot appear in a given value when evaluated
 */
public class NumericRestrictionRule extends Rule {
	/**
	 * Constructs a new {@link NumericRestrictionRule} rule
	 */
	public NumericRestrictionRule() {
		setValue("[0-9]");
		setType(RuleType.NUMERIC_RESTRICTION);
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
