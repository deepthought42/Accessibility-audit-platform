package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FormStatus {
	DISCOVERED("discovered"), 
	ACTION_REQUIRED("action_required"), 
	CLASSIFIED("classified");
	
	private String shortName;

	FormStatus(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static FormStatus create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(FormStatus v : values()) {
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
