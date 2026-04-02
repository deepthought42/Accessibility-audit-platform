package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * ready - ready for expansion
 * expanded - path has already been expanded and is ready for exploration
 */
public enum PathStatus {
	READY("ready"), 
	EXPANDED("expanded"), 
	EXAMINED("examined");
	
	private String shortName;

	PathStatus (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static PathStatus create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(PathStatus v : values()) {
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
