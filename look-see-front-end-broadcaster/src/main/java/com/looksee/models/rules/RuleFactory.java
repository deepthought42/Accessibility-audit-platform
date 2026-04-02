package com.looksee.models.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 *
 */
public class RuleFactory {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(RuleFactory.class);

	/**
	 * 
	 * @param type
	 * @param value
	 * @return
	 */
	public static Rule build(String type, String value){
		if(type.equalsIgnoreCase(RuleType.ALPHABETIC_RESTRICTION.toString())){
			return new AlphabeticRestrictionRule();
		}
		else if(type.equalsIgnoreCase(RuleType.DISABLED.toString())){
			return new DisabledRule();
		}
		else if(type.equalsIgnoreCase(RuleType.EMAIL_PATTERN.toString())){
			//log.info("Creating email pattern rule");
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
		//log.info("returning null rule");
		return null;
	}
}
