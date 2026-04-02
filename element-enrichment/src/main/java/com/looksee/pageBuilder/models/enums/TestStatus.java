package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TestStatus {
	PASSING("PASSING"), 
	FAILING("FAILING"), 
	UNVERIFIED("UNVERIFIED"), 
	RUNNING("RUNNING");
	
	private String shortName;

    TestStatus (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static TestStatus create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(TestStatus v : values()) {
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
