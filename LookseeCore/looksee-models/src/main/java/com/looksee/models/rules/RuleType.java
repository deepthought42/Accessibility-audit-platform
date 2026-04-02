package com.looksee.models.rules;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of rules that exist in the system
 */
public enum RuleType {
    /**
     * A pattern rule
     */
	PATTERN("pattern"),
    /**
     * A required rule
     */
    REQUIRED("required"),
    /**
     * An alphabetic restriction rule
     */
    ALPHABETIC_RESTRICTION("alphabetic_restriction"),
    /**
     * A special character restriction rule
     */
	SPECIAL_CHARACTER_RESTRICTION("special_character_restriction"),
    /**
     * A numeric restriction rule
     */
    NUMERIC_RESTRICTION("numeric_restriction"),
    /**
     * A disabled rule
     */
    DISABLED("disabled"),
    /**
     * A no validate rule
     */
	NO_VALIDATE("no_validate"),
    /**
     * A read only rule
     */
	READ_ONLY("read_only"),
    /**
     * A min length rule
     */
    MIN_LENGTH("min_length"),
    /**
     * A max length rule
     */
    MAX_LENGTH("max_length"),
    /**
     * A min value rule
     */
    MIN_VALUE("min_value"),
    /**
     * A max value rule
     */
    MAX_VALUE("max_value"),
    /**
     * An email pattern rule
     */
    EMAIL_PATTERN("email_pattern"),
    /**
     * A clickable rule
     */
    CLICKABLE("clickable"),
    /**
     * A double clickable rule
     */
    DOUBLE_CLICKABLE("double_clickable"),
    /**
     * A mouse release rule
     */
    MOUSE_RELEASE("mouse_release"),
    /**
     * A mouse over rule
     */
    MOUSE_OVER("mouse_over"),
    /**
     * A scrollable rule
     */
    SCROLLABLE("scrollable");
	
	private String shortName;

    /**
     * Constructs a new {@link RuleType}
     *
     * @param shortName the short name of the rule type
     */
    RuleType (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the rule type
     *
     * @return the short name of the rule type
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a new {@link RuleType}
     *
     * @param value the value of the rule type
     * @return the rule type
     */
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

    /**
     * Returns the short name of the rule type
     *
     * @return the short name of the rule type
     */
    public String getShortName() {
        return shortName;
    }
}
