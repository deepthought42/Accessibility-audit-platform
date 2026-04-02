package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enum values for various Captcha results on landable page states
 */
public enum CaptchaResult {
	CAPTCHA_BLOCKING("CAPTCHA_BLOCKING"), 
	CAPTCHA_MATCHED("CAPTCHA_MATCHED"), 
	CAPTCHA_NEEDED("CAPTCHA_NEEDED"), 
	CAPTCHA_NOT_NEEDED("CAPTCHA_NOT_NEEDED"), 
	CAPTCHA_UNMATCHED("CAPTCHA_UNMATCHED"),
	CAPTCHA_UNSET("CAPTCHA_UNSET");
	
	private String shortName;

	CaptchaResult (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static CaptchaResult create(String value) {
        if(value == null) {
            return CAPTCHA_UNSET;
        }
        for(CaptchaResult v : values()) {
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
