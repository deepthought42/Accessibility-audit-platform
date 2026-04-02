package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link DomainAction domain actions} that exist in the system
 */
public enum DomainAction {
    /**
     * Represents a create domain action.
     */
	CREATE("create"),

    /**
     * Represents a delete domain action.
     */
	DELETE("delete");
	
	private String shortName;

	DomainAction (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the domain action.
     * @return the short name of the domain action
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a DomainAction from a string.
     * @param value the string to create the DomainAction from
     * @return the DomainAction
     */
    @JsonCreator
    public static DomainAction create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(DomainAction v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the domain action.
     * @return the short name of the domain action
     */
    public String getShortName() {
        return shortName;
    }
}
