package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link FormType form types} that exist in the system
 */
public enum FormType {
    /**
     * Represents a login form type.
     */
	LOGIN("LOGIN"),

    /**
     * Represents a registration form type.
     */
	REGISTRATION("REGISTRATION"),

    /**
     * Represents a contact company form type.
     */
	CONTACT_COMPANY("CONTACT_COMPANY"),
    
    /**
     * Represents a subscribe form type.
     */
	SUBSCRIBE("SUBSCRIBE"),

    /**
     * Represents a lead form type.
     */
	LEAD("LEAD"),

    /**
     * Represents a search form type.
     */
	SEARCH("SEARCH"),

    /**
     * Represents a password reset form type.
     */
	PASSWORD_RESET("PASSWORD_RESET"),

    /**
     * Represents a payment form type.
     */
	PAYMENT("PAYMENT"),

    /**
     * Represents an unknown form type.
     */
	UNKNOWN("UNKNOWN");
	
	private String shortName;

    /**
     * Constructs a new FormType with the specified short name.
     * @param shortName the short name of the form type
     */
    FormType(String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the form type.
     * @return the short name of the form type
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a FormType from a string.
     * @param value the string to create the FormType from
     * @return the FormType
     */
    @JsonCreator
    public static FormType create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(FormType v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the form type.
     * @return the short name of the form type
     */
    public String getShortName() {
        return shortName;
    }
}
