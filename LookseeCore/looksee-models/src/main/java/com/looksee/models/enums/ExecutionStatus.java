package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link ExecutionStatus execution statuses} that exist in the system
 */
public enum ExecutionStatus {
    /**
     * Represents a running execution status.
     */
	RUNNING("running"),

    /**
     * Represents a stopped execution status.
     */
	STOPPED("stopped"),

    /**
     * Represents a complete execution status.
     */
	COMPLETE("complete"),

    /**
     * Represents an in progress execution status.
     */
	IN_PROGRESS("in_progress"),

    /**
     * Represents an error execution status.
     */
	ERROR("error"),

    /**
     * Represents a running audits execution status.
     */
	RUNNING_AUDITS("running audits"),

    /**
     * Represents a building page execution status.
     */
	BUILDING_PAGE("building page"),

    /**
     * Represents an extracting elements execution status.
     */
	EXTRACTING_ELEMENTS("extracting_elements"),

    /**
     * Represents an exceeded subscription execution status.
     */
	EXCEEDED_SUBSCRIPTION("exceeded_subscription"),

    /**
     * Represents an unknown execution status.
     */
	UNKNOWN("unknown");
	
	private String shortName;

    /**
     * Constructs a new ExecutionStatus with the specified short name.
     * @param shortName the short name of the execution status
     */
    ExecutionStatus (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the execution status.
     * @return the short name of the execution status
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates an ExecutionStatus from a string.
     * @param value the string to create the ExecutionStatus from
     * @return the ExecutionStatus
     */
    @JsonCreator
    public static ExecutionStatus create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(ExecutionStatus v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the execution status.
     * @return the short name of the execution status
     */
    public String getShortName() {
        return shortName;
    }
}
