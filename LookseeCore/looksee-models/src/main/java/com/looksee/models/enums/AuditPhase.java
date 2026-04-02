package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.looksee.models.audit.Audit;

/**
 * Defines all phases of a website/web app {@link Audit audits}
 */
public enum AuditPhase {
	/**
	 * Extracts data from the page
	 */
	PAGE_DATA_EXTRACTION("PAGE_DATA_EXTRACTION"),
	
	/**
	 * Extracts data from the journey
	 */
	JOURNEY_DATA_EXTRACTION("JOURNEY_DATA_EXTRACTION"),

	/**
	 * Starts the audit
	 */
	START_AUDIT("INIT_AUDIT"),

	/**
	 * Updates the audit
     */
	AUDIT_UPDATE("AUDIT_UPDATE"),

	/**
	 * Completes the audit
	 */
	AUDIT_COMPLETE("AUDIT_COMPLETE"),

	/**
	 * Represents an unknown or unspecified audit phase
	 */
	UNKNOWN("UNKNOWN");
	
	private String shortName;

    /**
     * Constructs an {@link AuditPhase}
     * 
     * @param shortName short name of the {@link AuditPhase}
     */
    AuditPhase (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the {@link AuditPhase}
     * 
     * @return {@link String}
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates an {@link AuditPhase} from a string
     * 
     * @param value string to create an {@link AuditPhase} from
     * @return {@link AuditPhase}
     */
    @JsonCreator
    public static AuditPhase create (String value) {
        if(value == null) {
            return UNKNOWN;
        }
        
        for(AuditPhase v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the {@link AuditPhase}
     * 
     * @return {@link String}
     */
    public String getShortName() {
        return shortName;
    }
}
