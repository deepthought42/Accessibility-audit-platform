package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of {@link Audit audits} that exist in the system
 */
public enum AuditSubcategory {
	//color management
	WRITTEN_CONTENT("Written_Content"),
	IMAGERY("Imagery"),
	VIDEOS("Videos"),
	AUDIO("Audio"),
	MENU_ANALYSIS("Menu_Analysis"),
	PERFORMANCE("Performance"),
	SEO("SEO"),
	TYPOGRAPHY("Typography"),
	COLOR_MANAGEMENT("Color_Management"),
	TEXT_CONTRAST("Text Contrast"), // REMOVE THIS
	NON_TEXT_CONTRAST("Non-Text_Contrast"), // REMOVE THIS
	WHITESPACE("Whitespace"),
	BRANDING("Branding"),
	SECURITY("Security"),
	LINKS("Links"),					//REMOVE THIS
	NAVIGATION("Navigation"), 
	INFORMATION_ARCHITECTURE("Information_Architecture");
	
	private String shortName;

    AuditSubcategory (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static AuditSubcategory create (String value) {

        for(AuditSubcategory v : values()) {
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
