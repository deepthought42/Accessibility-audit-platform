package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link AuditStage stages} of {@link Audit audits} that exist in the system
 */
public enum AuditStage {
	PRERENDER("prerender"),
	RENDERED("rendered"),
	UNKNOWN("unknown");
	
	private String shortName;

    AuditStage (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static AuditStage create (String value) {
        if(value == null) {
            return UNKNOWN;
        }
        
        for(AuditStage v : values()) {
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
