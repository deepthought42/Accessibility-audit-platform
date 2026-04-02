package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FormType {
	LOGIN("LOGIN"), 
	REGISTRATION("REGISTRATION"), 
	CONTACT_COMPANY("CONTACT_COMPANY"), 
	SUBSCRIBE("SUBSCRIBE"), 
	LEAD("LEAD"), 
	SEARCH("SEARCH"), 
	PASSWORD_RESET("PASSWORD_RESET"), 
	PAYMENT("PAYMENT"), 
	UNKNOWN("UNKNOWN");
	
	private String shortName;

    FormType(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static FormType create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(FormType v : values()) {
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
