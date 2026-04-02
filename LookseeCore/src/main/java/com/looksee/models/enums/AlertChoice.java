package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enum representing the choice of alert
 */
public enum AlertChoice {
    /**
     * Dismiss the alert
     */
	DISMISS("dismiss"),
    /**
     * Accept the alert
     */
    ACCEPT("accept");

	private String shortName;

	/**
	 * Constructor for the AlertChoice enum
	 *
	 * @param shortName the short name of the alert choice
	 */
	AlertChoice (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Create an AlertChoice from a string
     *
     * @param value the value of the alert choice
     * @return the AlertChoice
     */
    @JsonCreator
    public static AlertChoice create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(AlertChoice v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Get the short name of the alert choice
     *
     * @return the short name of the alert choice
     */
    public String getShortName() {
        return shortName;
    }
}
