package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BrowserType {
	CHROME("chrome"), 
	FIREFOX("firefox"), 
	SAFARI("safari"), 
	IE("ie");
	
	private String shortName;

	BrowserType(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static BrowserType create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(BrowserType v : values()) {
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
