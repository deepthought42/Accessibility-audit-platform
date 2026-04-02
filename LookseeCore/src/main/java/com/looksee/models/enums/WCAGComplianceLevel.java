package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link WCAGComplianceLevel WCAG compliance levels} that exist in 
 * the system
 */
public enum WCAGComplianceLevel {
    /**
     * Represents a WCAG compliance level.
     */
	A("A"),

    /**
     * Represents a WCAG compliance level.
     */
	AA("AA"),

    /**
     * Represents a WCAG compliance level.
     */
	AAA("AAA"),

    /**
     * Represents an unknown WCAG compliance level.
     */
	UNKNOWN("UNKNOWN");
	
	private String shortName;

	WCAGComplianceLevel(String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the WCAG compliance level.
     * @return the short name of the WCAG compliance level
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a WCAGComplianceLevel from a string.
     * @param value the string to create the WCAGComplianceLevel from
     * @return the WCAGComplianceLevel
     */
    @JsonCreator
    public static WCAGComplianceLevel create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(WCAGComplianceLevel v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        
        return UNKNOWN;
    }

    /**
     * Returns the short name of the WCAG compliance level.
     * @return the short name of the WCAG compliance level
     */
    public String getShortName() {
        return shortName;
    }
}
