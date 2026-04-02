package com.looksee.models.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.looksee.models.Element;

/**
 * Verifies that an element doesn't have any special characters in its value
 *
 */
public class SpecialCharacterRestriction extends Rule {
	/**
	 * Constructs a new {@link SpecialCharacterRestriction} rule
	 */
	public SpecialCharacterRestriction() {
		setValue("[^<>!@#$%&*()]");
		setType(RuleType.SPECIAL_CHARACTER_RESTRICTION);
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
