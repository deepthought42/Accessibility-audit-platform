package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link JourneyStatus journey statuses} that exist in the system
 */
public enum JourneyStatus {
    /**
     * Represents a candidate journey status.
     */
	CANDIDATE("CANDIDATE"),

    /**
     * Represents a reviewing journey status.
     */
	REVIEWING("REVIEWING"),

    /**
     * Represents a discarded journey status.
     */
	DISCARDED("DISCARDED"),

    /**
     * Represents a verified journey status.
     */
	VERIFIED("VERIFIED"),

    /**
     * Represents an error journey status.
     */
	ERROR("ERROR");
	
	private String shortName;

	JourneyStatus (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the journey status.
     * @return the short name of the journey status
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a JourneyStatus from a string.
     * @param value the string to create the JourneyStatus from
     * @return the JourneyStatus
     */
    @JsonCreator
    public static JourneyStatus create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(JourneyStatus v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the journey status.
     * @return the short name of the journey status
     */
    public String getShortName() {
        return shortName;
    }
}
