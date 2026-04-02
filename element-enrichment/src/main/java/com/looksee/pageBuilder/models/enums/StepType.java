package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StepType {
	UNKNOWN("unknown"), 
	SIMPLE("SIMPLE"), 
	LOGIN("LOGIN"), 
	REDIRECT("REDIRECT"),
	LANDING("LANDING");
	
	private String shortName;

    StepType(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static StepType create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(StepType v : values()) {
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
