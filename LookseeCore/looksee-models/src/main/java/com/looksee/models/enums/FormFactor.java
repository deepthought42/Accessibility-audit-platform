package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enum values for various Captcha results on landable page states
 */
public enum FormFactor {
    /**
     * Represents an unknown form factor.
     */
	UNKNOWN_FORM_FACTOR("UNKNOWN_FORM_FACTOR"),

    /**
     * Represents a desktop form factor.
     */
	DESKTOP("desktop"),

    /**
     * Represents a mobile form factor.
     */
	MOBILE("mobile"),

    /**
     * Represents a none form factor.
     */
	NONE("none");
	
	private String shortName;

	FormFactor (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the form factor.
     * @return the short name of the form factor
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a FormFactor from a string.
     * @param value the string to create the FormFactor from
     * @return the FormFactor
     */
    @JsonCreator
    public static FormFactor create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(FormFactor v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the form factor.
     * @return the short name of the form factor
     */
    public String getShortName() {
        return shortName;
    }
}
