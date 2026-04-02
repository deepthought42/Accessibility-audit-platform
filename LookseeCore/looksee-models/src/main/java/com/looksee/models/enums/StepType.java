package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link StepType step types} that exist in the system
 */
public enum StepType {
    /**
     * Represents an unknown step type.
     */
	UNKNOWN("unknown"),

    /**
     * Represents a simple step type.
     */
	SIMPLE("SIMPLE"),

    /**
     * Represents a redirect step type.
     */
	REDIRECT("REDIRECT"),

    /**
     * Represents a login step type.
     */
	LOGIN("LOGIN"),

    /**
     * Represents a landing step type.
     */
	LANDING("LANDING");
	
	private String shortName;

    StepType(String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the step type.
     * @return the short name of the step type
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a StepType from a string.
     * @param value the string to create the StepType from
     * @return the StepType
     */
    @JsonCreator
    public static StepType create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(StepType v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the step type.
     * @return the short name of the step type
     */
    public String getShortName() {
        return shortName;
    }
}
