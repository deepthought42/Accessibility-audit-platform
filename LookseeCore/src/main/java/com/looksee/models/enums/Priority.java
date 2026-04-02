package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link Priority priorities} that exist in the system
 */
public enum Priority {
    /**
     * Represents a high priority.
     */
	HIGH("high"),

    /**
     * Represents a medium priority.
     */
	MEDIUM("medium"),

    /**
     * Represents a low priority.
     */
	LOW("low"),

    /**
     * Represents a none priority.
     */
	NONE("none");
	

	private String shortName;

	Priority (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the priority.
     * @return the short name of the priority
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a Priority from a string.
     * @param value the string to create the Priority from
     * @return the Priority
     */
    @JsonCreator
    public static Priority create (String value) {
    	assert value != null;
    	assert !value.isEmpty();
    
        for(Priority v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the priority.
     * @return the short name of the priority
     */
    public String getShortName() {
        return shortName;
    }
}
