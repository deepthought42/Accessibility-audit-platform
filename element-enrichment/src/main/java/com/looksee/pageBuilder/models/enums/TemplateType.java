package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TemplateType {
	UNKNOWN("unknown"), 
	ATOM("atom"), 
	MOLECULE("molecule"), 
	ORGANISM("organism"), 
	TEMPLATE("template");
	
	private String shortName;

    TemplateType(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

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

    public String getShortName() {
        return shortName;
    }
}
