package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link BrowserEnvironment browser environments} that exist in 
 * the system
 */
public enum BrowserEnvironment {
    /**
     * Represents the test browser environment.
     */
	TEST("test"),
    /**
     * Represents the discovery browser environment.
     */
    DISCOVERY("discovery");
	
	private String shortName;

	BrowserEnvironment(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a BrowserEnvironment from a string.
     * @param value the string to create the BrowserEnvironment from
     * @return the BrowserEnvironment
     */
    @JsonCreator
    public static BrowserEnvironment create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(BrowserEnvironment v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the browser environment.
     * @return the short name of the browser environment
     */
    public String getShortName() {
        return shortName;
    }
}
