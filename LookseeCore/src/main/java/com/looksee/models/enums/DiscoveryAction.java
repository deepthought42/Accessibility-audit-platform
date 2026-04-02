package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link DiscoveryAction discovery actions} that exist in the system
 */
public enum DiscoveryAction {
	/**
     * Represents a start discovery action.
     */
    START("start"),

    /**
     * Represents a stop discovery action.
     */
    STOP("stop");
	
	private String shortName;

	DiscoveryAction (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the discovery action.
     * @return the short name of the discovery action
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a DiscoveryAction from a string.
     * @param value the string to create the DiscoveryAction from
     * @return the DiscoveryAction
     */
    @JsonCreator
    public static DiscoveryAction create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(DiscoveryAction v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the discovery action.
     * @return the short name of the discovery action
     */
    public String getShortName() {
        return shortName;
    }
}
