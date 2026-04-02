package com.looksee.journeyMapCleanup.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all levels of {@link Audit audits} that exist in the system
 */
public enum AuditLevel {
	PAGE("page"),
	DOMAIN("domain"),
	UNKNOWN("unknown");
	
	private String shortName;

    AuditLevel (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static AuditLevel create (String value) {
        if(value == null) {
            return UNKNOWN;
        }
        
        for(AuditLevel v : values()) {
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
