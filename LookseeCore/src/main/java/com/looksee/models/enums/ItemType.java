package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of {@link ItemType} that exist in the system
 */
public enum ItemType {
    /**
     * Represents a text item type.
     */
	TEXT("text"),

    /**
     * Represents a bytes item type.
     */
	BYTES("bytes"),

    /**
     * Represents a numeric item type.
     */
	NUMERIC("numeric"),

    /**
     * Represents a URL item type.
     */
	URL("url") ;
	
	private String shortName;

    ItemType (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates an ItemType from a string.
     * @param value the string to create the ItemType from
     * @return the ItemType
     */
    @JsonCreator
    public static ItemType create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(ItemType v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the item type.
     * @return the short name of the item type
     */
    public String getShortName() {
        return shortName;
    }
}
