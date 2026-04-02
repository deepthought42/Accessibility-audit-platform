package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link BugType bug types} that exist in the system
 */
public enum BugType {
    /**
     * Represents a missing field bug.
     */
	MISSING_FIELD("MISSING FIELD"),

    /**
     * Represents an accessibility bug.
     */
	ACCESSIBILITY("ACCESSIBILITY"),

    /**
     * Represents a performance bug.
     */
	PERFORMANCE("PERFORMANCE"),

    /**
     * Represents a SEO bug.
     */
	SEO("SEO"),

    /**
     * Represents a best practices bug.
     */
	BEST_PRACTICES("BEST_PRACTICES");
	
	private String shortName;

    /**
     * Constructs a new BugType with the specified short name.
     * @param shortName the short name of the bug type
     */
    BugType (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the bug type.
     * @return the short name of the bug type
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a BugType from a string.
     * @param value the string to create the BugType from
     * @return the BugType
     */
    @JsonCreator
    public static BugType create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(BugType v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the bug type.
     * @return the short name of the bug type
     */
    public String getShortName() {
        return shortName;
    }
}
