package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link TemplateType template types} that exist in the system
 */
public enum TemplateType {
    /**
     * Represents an unknown template type.
     */
	UNKNOWN("unknown"),

    /**
     * Represents an atom template type.
     */
	ATOM("atom"),

    /**
     * Represents a molecule template type.
     */
	MOLECULE("molecule"),

    /**
     * Represents an organism template type.
     */
	ORGANISM("organism"),

    /**
     * Represents a template template type.
     */
	TEMPLATE("template");
	
	private String shortName;

    TemplateType(String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the template type.
     * @return the short name of the template type
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a TemplateType from a string.
     * @param value the string to create the TemplateType from
     * @return the TemplateType
     */
    @JsonCreator
    public static TemplateType create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(TemplateType v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the template type.
     * @return the short name of the template type
     */
    public String getShortName() {
        return shortName;
    }
}
