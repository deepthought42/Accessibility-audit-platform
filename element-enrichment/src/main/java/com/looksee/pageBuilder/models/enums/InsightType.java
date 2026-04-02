package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 
 */
public enum InsightType {
	PERFORMANCE("PERFORMANCE"), 
	ACCESSIBILITY("ACCESSIBILITY"), 
	SEO("SEO"), 
	PWA("PWA"), 
	SECURITY("SECURITY"), 
	UNKNOWN("UNKNOWN");
	
	private String shortName;

    InsightType (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static InsightType create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(InsightType v : values()) {
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
