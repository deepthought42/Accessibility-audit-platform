package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of {@link Audit audits} that exist in the system
 */
public enum AuditCategory {
	CONTENT("CONTENT"), 
	INFORMATION_ARCHITECTURE("INFORMATION_ARCHITECTURE"), 
	AESTHETICS("AESTHETICS"),
	ACCESSIBILITY("ACCESSIBILITY");
	
	private String shortName;

    AuditCategory (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static AuditCategory create (String value) {
        for(AuditCategory v : values()) {
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
