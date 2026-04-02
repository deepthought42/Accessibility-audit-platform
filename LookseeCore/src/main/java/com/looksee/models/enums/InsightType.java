package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link InsightType insight types} that exist in the system
 */
public enum InsightType {
    /**
     * Represents a performance insight type.
     */
	PERFORMANCE("PERFORMANCE"),

    /**
     * Represents an accessibility insight type.
     */
	ACCESSIBILITY("ACCESSIBILITY"),

    /**
     * Represents a SEO insight type.
     */
	SEO("SEO"),

    /**
     * Represents a PWA insight type.
     */
	PWA("PWA"),

    /**
     * Represents a security insight type.
     */
	SECURITY("SECURITY"),

    /**
     * Represents an unknown insight type.
     */
	UNKNOWN("UNKNOWN");
	
	private String shortName;

    /**
     * Constructs a new InsightType with the specified short name.
     * @param shortName the short name of the insight type
     */
    InsightType (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the insight type.
     * @return the short name of the insight type
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates an InsightType from a string.
     * @param value the string to create the InsightType from
     * @return the InsightType
     */
    @JsonCreator
    public static InsightType create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(InsightType v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the insight type.
     * @return the short name of the insight type
     */
    public String getShortName() {
        return shortName;
    }
}
