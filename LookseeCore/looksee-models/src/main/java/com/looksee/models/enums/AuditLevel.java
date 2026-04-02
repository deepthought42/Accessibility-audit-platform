package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.looksee.models.audit.Audit;

/**
 * Defines all levels of {@link Audit audits} that exist in the system.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>Each enum value has a unique shortName</li>
 *   <li>shortName is never null or empty</li>
 *   <li>shortName is always uppercase</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li>This enum is immutable and thread-safe</li>
 * </ul>
 */
public enum AuditLevel {
	/**
	 * Represents a page-level audit that evaluates individual web pages.
	 */
	PAGE("PAGE"),

	/**
	 * Represents a domain-level audit that evaluates entire websites/domains.
	 */
	DOMAIN("DOMAIN"),

	/**
	 * Represents an unknown or invalid audit level.
	 * Used as a fallback when audit level cannot be determined.
	 */
	UNKNOWN("UNKNOWN");
	
	private String shortName;

    /**
     * Constructs a new AuditLevel with the specified short name.
     * @param shortName the short name of the audit level
     */
    AuditLevel (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the audit level.
     * @return the short name of the audit level
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates an {@linkplain AuditLevel} from a string value
     *
     * @param value the value
     * @return the {@link AuditLevel}
     */
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
        throw new IllegalArgumentException("Invalid audit level: " + value);
    }

    /**
     * Gets the short name of the audit level
     *
     * @return the short name
     */
    public String getShortName() {
        return shortName;
    }
}
