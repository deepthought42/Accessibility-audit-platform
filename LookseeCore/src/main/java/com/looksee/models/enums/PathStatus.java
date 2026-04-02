package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link PathStatus path statuses} that exist in the system
 *
 * ready - ready for expansion
 * expanded - path has already been expanded and is ready for exploration
 */
public enum PathStatus {
    /**
     * Represents a ready path status.
     */
	READY("ready"),

    /**
     * Represents an expanded path status.
     */
	EXPANDED("expanded"),

    /**
     * Represents an examined path status.
     */
	EXAMINED("examined");
	
	private String shortName;

	PathStatus (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the path status.
     * @return the short name of the path status
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a PathStatus from a string.
     * @param value the string to create the PathStatus from
     * @return the PathStatus
     */
    @JsonCreator
    public static PathStatus create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(PathStatus v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the path status.
     * @return the short name of the path status
     */
    public String getShortName() {
        return shortName;
    }
}
