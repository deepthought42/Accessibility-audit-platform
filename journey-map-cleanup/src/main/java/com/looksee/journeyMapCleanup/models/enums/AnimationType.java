package com.looksee.journeyMapCleanup.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 
 */
public enum AnimationType {
	CAROUSEL("CAROUSEL"), 
	LOADING("LOADING"), 
	CONTINUOUS("CONTINUOUS");
	
	private String shortName;

    AnimationType (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static AnimationType create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(AnimationType v : values()) {
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
