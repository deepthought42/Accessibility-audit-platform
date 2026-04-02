package com.looksee.frontEndBroadcaster.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of {@link Audit audits} that exist in the system
 */
public enum AuditName {
	//color management
	COLOR_PALETTE("COLOR_PALETTE"),
	TEXT_BACKGROUND_CONTRAST("TEXT_BACKGROUND_CONTRAST"),
	NON_TEXT_BACKGROUND_CONTRAST("NON_TEXT_BACKGROUND_CONTRAST"),
	LINKS("LINKS"),
	TYPEFACES("TYPEFACES"),
	FONT("FONT"),
	PADDING("PADDING"),
	MARGIN("MARGIN"),
	MEASURE_UNITS("MEASURE_UNITS"),
	TITLES("TITLES"),
	ALT_TEXT("ALT_TEXT"),
	PARAGRAPHING("PARAGRAPHING"),
	METADATA("METADATA"),
	UNKNOWN("UNKNOWN"), 
	IMAGE_COPYRIGHT("IMAGE_COPYRIGHT"), 
	IMAGE_POLICY("IMAGE_POLICY"), 
	READING_COMPLEXITY("READING_COMPLEXITY"), 
	ENCRYPTED("ENCRYPTED"); //SECURITY
	
	private String shortName;

    AuditName(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static AuditName create(String value) {
        if(value == null) {
            return UNKNOWN;
        }
        for(AuditName v : values()) {
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
