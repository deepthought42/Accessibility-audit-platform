package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ElementClassification {
	TEMPLATE("TEMPLATE"), 
	LEAF("LEAF"), 
	SLIDER("SLIDER"), 
	ANCESTOR("ANCESTOR"), 
	UNKNOWN("UNKNOWN");
	
	private String shortName;

	ElementClassification(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static ElementClassification create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(ElementClassification v : values()) {
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
