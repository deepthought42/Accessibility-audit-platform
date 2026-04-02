package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.looksee.models.audit.Audit;

/**
 * Defines all types of {@link Audit audits} that exist in the system
 */
public enum AuditSubcategory {
	/**
     * Represents the written content subcategory.
     */
	WRITTEN_CONTENT("Written_Content"),

	/**
     * Represents the imagery subcategory.
     */
	IMAGERY("Imagery"),

	/**
     * Represents the videos subcategory.
     */
	VIDEOS("Videos"),

	/**
     * Represents the audio subcategory.
     */
	AUDIO("Audio"),

	/**
     * Represents the menu analysis subcategory.
     */
	MENU_ANALYSIS("Menu_Analysis"),

	/**
     * Represents the performance subcategory.
     */
	PERFORMANCE("Performance"),

	/**
     * Represents the SEO subcategory.
     */
	SEO("SEO"),

	/**
     * Represents the typography subcategory.
     */
	TYPOGRAPHY("Typography"),

	/**
     * Represents the color management subcategory.
     */
	COLOR_MANAGEMENT("Color_Management"),

	/**
     * Represents the text contrast subcategory.
     */
	TEXT_CONTRAST("Text Contrast"), // REMOVE THIS

	/**
     * Represents the non-text contrast subcategory.
     */
	NON_TEXT_CONTRAST("Non-Text_Contrast"), // REMOVE THIS

    /**
     * Represents the whitespace subcategory.
     */
	WHITESPACE("Whitespace"),

	/**
     * Represents the branding subcategory.
     */
	BRANDING("Branding"),

    /**
     * Represents the security subcategory.
     */
	SECURITY("Security"),

	/**
     * Represents the links subcategory.
     */
	LINKS("Links"),					//REMOVE THIS

    /**
     * Represents the information architecture subcategory.
     */
	NAVIGATION("Navigation"),

	/**
     * Represents the information architecture subcategory.
     */
	INFORMATION_ARCHITECTURE("Information_Architecture");
	
	private String shortName;

    /**
     * Constructs a new AuditSubcategory with the specified short name.
     * @param shortName the short name of the audit subcategory
     */
    AuditSubcategory (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the audit subcategory.
     * @return the short name of the audit subcategory
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates an AuditSubcategory from a string.
     * @param value the string to create the AuditSubcategory from
     * @return the AuditSubcategory
     */
    @JsonCreator
    public static AuditSubcategory create (String value) {

        for(AuditSubcategory v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the audit subcategory.
     * @return the short name of the audit subcategory
     */
    public String getShortName() {
        return shortName;
    }
}
