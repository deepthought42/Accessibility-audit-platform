package com.looksee.models.rules;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of rules that exist in the system
 */
public enum RuleType {
	PATTERN("pattern"), REQUIRED("required"), ALPHABETIC_RESTRICTION("alphabetic_restriction"), 
	SPECIAL_CHARACTER_RESTRICTION("special_character_restriction"), NUMERIC_RESTRICTION("numeric_restriction"), DISABLED("disabled"), 
	NO_VALIDATE("no_validate"), 
	READ_ONLY("read_only"), MIN_LENGTH("min_length"), MAX_LENGTH("max_length"), MIN_VALUE("min_value"), MAX_VALUE("max_value"), 
	EMAIL_PATTERN("email_pattern"), CLICKABLE("clickable"), DOUBLE_CLICKABLE("double_clickable"), MOUSE_RELEASE("mouse_release"),
	MOUSE_OVER("mouse_over"), SCROLLABLE("scrollabel");
	
	private String shortName;

    RuleType (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static RuleType create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(RuleType v : values()) {
            if(value.equals(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    public String getShortName() {
        return shortName;
    }
	//NO VALIDATE MAY NOT BE USEFUL
	/*
	 * NB: This list is complete since the system assumes that any type of absence of a restriction is either waiting to have a 
	 * restriction discovered, or doesn't have any restriction. For example, when a field only allows numbers, we assume that such a field
	 * will have alphabetic and special character restrictions
	 */

}
