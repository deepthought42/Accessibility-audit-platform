package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.looksee.models.audit.Audit;

/**
 * Defines all {@link AuditStage stages} of {@link Audit audits} that exist in the system
 */
public enum AuditStage {
    /**
     * Represents the pre-render stage of an audit.
     */
	PRERENDER("prerender"),

    /**
     * Represents the rendered stage of an audit.
     */
	RENDERED("rendered"),

    /**
     * Represents an unknown stage of an audit.
     */
	UNKNOWN("unknown");
	
	private String shortName;

    /**
     * Constructs a new AuditStage with the specified short name.
     * @param shortName the short name of the audit stage
     */
    AuditStage (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the audit stage.
     * @return the short name of the audit stage
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates an AuditStage from a string.
     * @param value the string to create the AuditStage from
     * @return the AuditStage
     */
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

    /**
     * Returns the short name of the audit stage.
     * @return the short name of the audit stage
     */
    public String getShortName() {
        return shortName;
    }
}
