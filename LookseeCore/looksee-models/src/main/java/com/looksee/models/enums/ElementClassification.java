package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link ElementClassification element classifications} that
 * exist in the system
 */
public enum ElementClassification {
    /**
     * Represents a template element classification.
     */
	TEMPLATE("TEMPLATE"),

    /**
     * Represents a leaf element classification.
     */
	LEAF("LEAF"),

    /**
     * Represents a slider element classification.
     */
	SLIDER("SLIDER"),

    /**
     * Represents an ancestor element classification.
     */
	ANCESTOR("ANCESTOR"),

    /**
     * Represents an unknown element classification.
     */
	UNKNOWN("UNKNOWN");
	
	private String shortName;

	ElementClassification(String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the element classification.
     * @return the short name of the element classification
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates an ElementClassification from a string.
     * @param value the string to create the ElementClassification from
     * @return the ElementClassification
     */
    @JsonCreator
    public static ElementClassification create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(ElementClassification v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the element classification.
     * @return the short name of the element classification
     */
    public String getShortName() {
        return shortName;
    }
}
