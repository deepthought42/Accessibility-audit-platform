package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link BrowserType browser types} that exist in the system
 */
public enum BrowserType {
    /**
     * Represents the Chrome browser type.
     */
	CHROME("chrome"),

    /**
     * Represents the Firefox browser type.
     */
	FIREFOX("firefox"),

    /**
     * Represents the Safari browser type.
     */
	SAFARI("safari"),

    /**
     * Represents the Internet Explorer browser type.
     */
	IE("ie");
	
	private String shortName;

	BrowserType(String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the browser type.
     * @return the short name of the browser type
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a BrowserType from a string.
     * @param value the string to create the BrowserType from
     * @return the BrowserType
     */
    @JsonCreator
    public static BrowserType create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(BrowserType v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the browser type.
     * @return the short name of the browser type
     */
    public String getShortName() {
        return shortName;
    }
}
