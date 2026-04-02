package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Represents the type of audit
 */
public enum AuditType {
    /**
     * Represents the table audit type.
     */
	TABLE("table"),

    /**
     * Represents the filmstrip audit type.
     */
	FILMSTRIP("filmstrip"),
    
    /**
     * Represents the opportunity audit type.
     */
	OPPORTUNITY("opportunity"),

    /**
     * Represents the node audit type.
     */
	NODE("node"),

    /**
     * Represents the debug data audit type.
     */
	DEBUG_DATA("debugdata"),

    /**
     * Represents the unknown audit type.
     */
	UNKNOWN("unknown");
	
	private String shortName;

    /**
     * Constructs a new AuditType with the specified short name.
     * @param shortName the short name of the audit type
     */
    AuditType (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the audit type.
     * @return the short name of the audit type
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates an AuditType from a string.
     * @param value the string to create the AuditType from
     * @return the AuditType
     */
    @JsonCreator
    public static AuditType create (String value) {
        if(value == null) {
            return UNKNOWN;
        }
        for(AuditType v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the audit type.
     * @return the short name of the audit type
     */
    public String getShortName() {
        return shortName;
    }
}
