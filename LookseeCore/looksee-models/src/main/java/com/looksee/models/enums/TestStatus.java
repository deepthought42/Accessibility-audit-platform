package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link TestStatus test statuses} that exist in the system
 */
public enum TestStatus {
    /**
     * Represents a passing test status.
     */
	PASSING("PASSING"),

    /**
     * Represents a failing test status.
     */
	FAILING("FAILING"),

    /**
     * Represents an unverified test status.
     */
	UNVERIFIED("UNVERIFIED"),

    /**
     * Represents a running test status.
     */
	RUNNING("RUNNING");
	
	private String shortName;

    TestStatus (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the test status.
     * @return the short name of the test status
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a TestStatus from a string.
     * @param value the string to create the TestStatus from
     * @return the TestStatus
     */
    @JsonCreator
    public static TestStatus create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(TestStatus v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the test status.
     * @return the short name of the test status
     */
    public String getShortName() {
        return shortName;
    }
}
