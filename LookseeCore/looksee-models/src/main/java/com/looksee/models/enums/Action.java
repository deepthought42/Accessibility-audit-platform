package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Represents an action that can be performed on a page.
 */
public enum Action {
    /**
     * Represents a click action.
     */
	CLICK("click"),
    /**
     * Represents a double click action.
     */
	DOUBLE_CLICK("doubleClick"),
    /**
     * Represents a hover action.
     */
	HOVER("hover"),
    /**
     * Represents a click and hold action.
     */
	CLICK_AND_HOLD("clickAndHold"),
    /**
     * Represents a context click action.
     */
	CONTEXT_CLICK("contextClick"),
    /**
     * Represents a release action.
     */
	RELEASE("release"),
    /**
     * Represents a send keys action.
     */
	SEND_KEYS("sendKeys"),
    /**
     * Represents a mouse over action.
     */
	MOUSE_OVER("mouseover"),
    /**
     * Represents an unknown action.
     */
	UNKNOWN("unknown");
	
	/**
	 * The short name of the action.
	 */
	private String shortName;

	/**
	 * Constructs a new Action with the specified short name.
	 * @param shortName the short name of the action
	 */
	Action (String shortName) {
        this.shortName = shortName;
    }

	/**
	 * Returns the short name of the action.
	 * @return the short name of the action
	 */
    @Override
    public String toString() {
        return shortName;
    }

	/**
	 * Creates an Action from a string.
	 * @param value the string to create the Action from
	 * @return the Action
	 */
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

	/**
	 * Returns the short name of the action.
	 * @return the short name of the action
	 */
    public String getShortName() {
        return shortName;
    }
}
