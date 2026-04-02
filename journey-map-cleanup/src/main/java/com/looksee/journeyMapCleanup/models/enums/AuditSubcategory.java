package com.looksee.journeyMapCleanup.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of {@link Audit audits} that exist in the system
 */
public enum AuditSubcategory {
	//color management
	WRITTEN_CONTENT("WRITTEN_CONTENT"),
	IMAGERY("IMAGERY"),
	VIDEOS("VIDEOS"),
	AUDIO("AUDIO"),
	MENU_ANALYSIS("MENU_ANALYSIS"),
	PERFORMANCE("PERFORMANCE"),
	SEO("SEO"),
	TYPOGRAPHY("TYPOGRAPHY"),
	COLOR_MANAGEMENT("COLOR_MANAGEMENT"),
	TEXT_CONTRAST("TEXT_CONTRAST"), 
	NON_TEXT_CONTRAST("NON_TEXT_CONTRAST"), 
	WHITESPACE("WHITESPACE"),
	BRANDING("BRANDING"),
	SECURITY("SECURITY"),
	LINKS("LINKS"),					
	NAVIGATION("NAVIGATION"), 
	INFORMATION_ARCHITECTURE("INFORMATION_ARCHITECTURE");
	
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
