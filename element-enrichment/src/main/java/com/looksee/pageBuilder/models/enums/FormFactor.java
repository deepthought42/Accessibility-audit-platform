package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enum values for various Captcha results on landable page states
 */
public enum FormFactor {
	UNKNOWN_FORM_FACTOR("UNKNOWN_FORM_FACTOR"), 
	DESKTOP("desktop"), 
	MOBILE("mobile"), 
	NONE("none");
	
	private String shortName;

	FormFactor (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static FormFactor create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(FormFactor v : values()) {
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
