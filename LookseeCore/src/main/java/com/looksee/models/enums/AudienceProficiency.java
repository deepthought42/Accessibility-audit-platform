package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines the proficiency levels of the audience for the audit.
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
public enum AudienceProficiency {
    /**
     * Represents a general audience.
     */
	GENERAL("GENERAL"),

    /**
     * Represents a knowledgeable audience.
     */
	KNOWLEDGEABLE("KNOWLEDGEABLE"),

    /**
     * Represents an expert audience.
     */
	EXPERT("EXPERT"),

    /**
     * Represents an unknown audience.
     */
	UNKNOWN("UNKNOWN");
    
	private String shortName;

	AudienceProficiency (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the audience proficiency.
     *
     * @return the short name
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates an {@linkplain AudienceProficiency} from a string value
     *
     * @param value the value
     * @return the {@link AudienceProficiency}
     */
    @JsonCreator
    public static AudienceProficiency create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(AudienceProficiency v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns the short name of the audience proficiency.
     *
     * @return the short name
     */
    public String getShortName() {
        return shortName;
    }
}
