package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * ready - ready for expansion
 * expanded - path has already been expanded and is ready for exploration
 */
public enum JourneyStatus {
	CANDIDATE("CANDIDATE"),
	REVIEWING("REVIEWING"),
	DISCARDED("DISCARDED"),
	VERIFIED("VERIFIED"),
	ERROR("ERROR");
	
	private String shortName;

	JourneyStatus (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static JourneyStatus create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(JourneyStatus v : values()) {
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
