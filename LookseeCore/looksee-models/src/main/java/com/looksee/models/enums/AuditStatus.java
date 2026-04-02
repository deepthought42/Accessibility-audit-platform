package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.looksee.models.audit.Audit;

/**
 * Defines all {@link AuditStatus status} of {@link Audit audits} that exist in the system
 */
public enum AuditStatus {
    /**
     * Represents an audit that has started
     */
	STARTED("STARTED"),

    /**
     * Represents an audit that has stopped
     */
	STOPPED("STOPPED"),

    /**
     * Represents an audit that has completed
     */
	COMPLETE("COMPLETE");
	
	private String shortName;

    /**
     * Constructor for {@link AuditStatus}
     *
     * @param shortName the short name of the audit status
     */
    AuditStatus (String shortName) {
        this.shortName = shortName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates an {@linkplain AuditStatus} from a string value
     *
     * @param value the value
     * @return the {@link AuditStatus}
     */
    @JsonCreator
    public static AuditStatus create (String value) {
        for(AuditStatus v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Get the short name of the audit status
     *
     * @return the short name of the audit status
     */
    public String getShortName() {
        return shortName;
    }
}
