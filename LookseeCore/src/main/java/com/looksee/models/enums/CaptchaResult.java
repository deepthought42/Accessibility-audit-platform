package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link CaptchaResult captcha results} that exist in the system
 */
public enum CaptchaResult {
    /**
     * Represents a captcha blocking result.
     */
	CAPTCHA_BLOCKING("CAPTCHA_BLOCKING"),

    /**
     * Represents a captcha matched result.
     */
	CAPTCHA_MATCHED("CAPTCHA_MATCHED"),

    /**
     * Represents a captcha needed result.
     */
	CAPTCHA_NEEDED("CAPTCHA_NEEDED"),

    /**
     * Represents a captcha not needed result.
     */
	CAPTCHA_NOT_NEEDED("CAPTCHA_NOT_NEEDED"),

    /**
     * Represents a captcha unmatched result.
     */
	CAPTCHA_UNMATCHED("CAPTCHA_UNMATCHED"),

    /**
     * Represents a captcha unset result.
     */
	CAPTCHA_UNSET("CAPTCHA_UNSET");
	
	private String shortName;

	CaptchaResult (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the captcha result.
     * @return the short name of the captcha result
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a CaptchaResult from a string.
     * @param value the string to create the CaptchaResult from
     * @return the CaptchaResult
     */
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

    /**
     * Returns the short name of the captcha result.
     * @return the short name of the captcha result
     */
    public String getShortName() {
        return shortName;
    }
}
