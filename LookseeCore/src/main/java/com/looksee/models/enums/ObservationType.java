package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link ObservationType observation types} that exist in the system
 */
public enum ObservationType {
    
    /**
     * Represents an element observation type.
     */
	ELEMENT("Element"),

    /**
     * Represents a typography observation type.
     */
	TYPOGRAPHY("Typography"),

    /**
     * Represents a color palette observation type.
     */
	COLOR_PALETTE("Color_Palette"),

    /**
     * Represents a property map observation type.
     */
	PROPERTY_MAP("Property_Map"),

    /**
     * Represents a style missing observation type.
     */
	STYLE_MISSING("Style_Missing"),

    /**
     * Represents a page state observation type.
     */
	PAGE_STATE("Page"),

    /**
     * Represents a typeface observation type.
     */
	TYPEFACE("Typeface"),

    /**
     * Represents a color contrast observation type.
     */
	COLOR_CONTRAST("Color_Contrast"),

    /**
     * Represents a security observation type.
     */
	SECURITY("Security"),

    /**
     * Represents a SEO observation type.
     */
	SEO("SEO"),

    /**
     * Represents a page language observation type.
     */
	PAGE_LANGUAGE("Page_Language"),

    /**
     * Represents a reflow observation type.
     */
	REFLOW("Reflow"),

    /**
     * Represents an unknown observation type.
     */
	UNKNOWN("Unknown");
	
	private String shortName;

	ObservationType (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the observation type.
     * @return the short name of the observation type
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates an ObservationType from a string.
     * @param value the string to create the ObservationType from
     * @return the ObservationType
     */
    @JsonCreator
    public static ObservationType create (String value) {
        if(value == null) {
            return UNKNOWN;
        }
        for(ObservationType v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the observation type.
     * @return the short name of the observation type
     */
    public String getShortName() {
        return shortName;
    }
}
