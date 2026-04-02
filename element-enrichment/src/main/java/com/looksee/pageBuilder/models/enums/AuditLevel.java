package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.looksee.pageBuilder.models.Audit;

/**
 * Defines all levels of {@link Audit audits} that exist in the system
 */
public enum AuditLevel {
	PAGE("PAGE"),
	DOMAIN("DOMAIN"),
	UNKNOWN("UNKNOWN");
	
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
