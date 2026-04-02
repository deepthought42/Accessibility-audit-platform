package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link FormStatus form statuses} that exist in the system
 */
public enum FormStatus {
    /**
     * Represents a discovered form status.
     */
	DISCOVERED("discovered"),

    /**
     * Represents an action required form status.
     */
	ACTION_REQUIRED("action_required"),

    /**
     * Represents a classified form status.
     */
	CLASSIFIED("classified");
	
	private String shortName;

    /**
     * Constructs a new FormStatus with the specified short name.
     * @param shortName the short name of the form status
     */
	FormStatus(String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the form status.
     * @return the short name of the form status
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a FormStatus from a string.
     * @param value the string to create the FormStatus from
     * @return the FormStatus
     */
    @JsonCreator
    public static FormStatus create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(FormStatus v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the form status.
     * @return the short name of the form status
     */
    public String getShortName() {
        return shortName;
    }
}
