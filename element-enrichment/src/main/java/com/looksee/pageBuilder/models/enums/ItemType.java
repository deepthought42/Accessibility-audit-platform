package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of {@link ItemType} that exist in the system
 */
public enum ItemType {
	TEXT("text"), 
	BYTES("bytes"), 
	NUMERIC("numeric"), 
	MILLISECONDS("ms"), 
	URL("url") ;
	
	private String shortName;

    ItemType (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

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

    public String getShortName() {
        return shortName;
    }
}
