package com.looksee.frontEndBroadcaster.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Action {
	CLICK("CLICK"), 
	DOUBLE_CLICK("DOUBLE_CLICK"), 
	HOVER("HOVER"), 
	CLICK_AND_HOLD("CLICK_AND_HOLD"), 
	CONTEXT_CLICK("CONTEXT_CLICK"), 
	RELEASE("RELEASE"),
	SEND_KEYS("SEND_KEYS"),
	MOUSE_OVER("MOUSE_OVER"), 
	UNKNOWN("UNKNOWN");
	

	private String shortName;

	Action (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static Action create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(Action v : values()) {
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
