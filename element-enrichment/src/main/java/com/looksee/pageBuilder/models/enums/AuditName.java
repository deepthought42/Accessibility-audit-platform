package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of {@link Audit audits} that exist in the system
 */
public enum AuditName {
	//color management
	COLOR_PALETTE("Color_Palette"),
	TEXT_BACKGROUND_CONTRAST("Text_Background_Contrast"),
	NON_TEXT_BACKGROUND_CONTRAST("Non_Text_Background_Contrast"),
	LINKS("Links"),
	TYPEFACES("Typefaces"),
	FONT("Font"),
	PADDING("Padding"),
	MARGIN("Margin"),
	MEASURE_UNITS("Measure_Units"),
	TITLES("Titles"),
	ALT_TEXT("Alt_Text"),
	PARAGRAPHING("Paragraphing"),
	METADATA("Metadata"),
	UNKNOWN("Unknown"), 
	IMAGE_COPYRIGHT("Image_Copyright"), 
	IMAGE_POLICY("Image Policy"), 
	READING_COMPLEXITY("Reading_Complexity");
	
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
