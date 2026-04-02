package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BugType {
	MISSING_FIELD("MISSING FIELD"), 
	ACCESSIBILITY("ACCESSIBILITY"), 
	PERFORMANCE("PERFORMANCE"), 
	SEO("SEO"), 
	BEST_PRACTICES("BEST_PRACTICES");
	
	private String shortName;

    BugType (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static BugType create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(BugType v : values()) {
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
