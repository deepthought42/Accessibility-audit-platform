package com.looksee.models.rules;

import lombok.NoArgsConstructor;

/**
 * A factory for creating rules
 *
 * invariant: instances are stateless; all behavior is through static methods
 */
@NoArgsConstructor
public class RuleFactory {

	/**
	 * Builds a rule based on the type and value
	 *
	 * @param type the type of the rule
	 * @param value the value of the rule
	 * @return the rule
	 *
	 * precondition: type != null
	 * precondition: !type.isEmpty()
	 * precondition: value != null
	 */
	public static Rule build(String type, String value){
		assert type != null;
		assert !type.isEmpty();
		assert value != null;

		if(type.equalsIgnoreCase(RuleType.ALPHABETIC_RESTRICTION.toString())){
			return new AlphabeticRestrictionRule();
		}
		else if(type.equalsIgnoreCase(RuleType.DISABLED.toString())){
			return new DisabledRule();
		}
		else if(type.equalsIgnoreCase(RuleType.EMAIL_PATTERN.toString())){
			return new EmailPatternRule();
		}
		else if(type.equalsIgnoreCase(RuleType.MAX_LENGTH.toString())){
			return new NumericRule(RuleType.MAX_LENGTH, value);
		}
		else if(type.equalsIgnoreCase(RuleType.MAX_VALUE.toString())){
			return new NumericRule(RuleType.MAX_VALUE, value);
		}
		else if(type.equalsIgnoreCase(RuleType.MIN_LENGTH.toString())){
			return new NumericRule(RuleType.MIN_LENGTH, value);
		}
		else if(type.equalsIgnoreCase(RuleType.MIN_VALUE.toString())){
			return new NumericRule(RuleType.MIN_VALUE, value);
		}
		else if(type.equalsIgnoreCase(RuleType.NUMERIC_RESTRICTION.toString())){
			return new NumericRestrictionRule();
		}
		else if(type.equalsIgnoreCase(RuleType.PATTERN.toString())){
			return new PatternRule(value);
		}
		else if(type.equalsIgnoreCase(RuleType.READ_ONLY.toString())){
			return new ReadOnlyRule();
		}
		else if(type.equalsIgnoreCase(RuleType.REQUIRED.toString())){
			return new RequirementRule();
		}
		else if(type.equalsIgnoreCase(RuleType.SPECIAL_CHARACTER_RESTRICTION.toString())){
			return new SpecialCharacterRestriction();
		}
		return null;
	}
}
