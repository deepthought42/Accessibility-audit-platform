package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines the types of animations that exist in the system.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>Each enum value has a unique shortName</li>
 *   <li>shortName is never null or empty</li>
 *   <li>shortName is always uppercase</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li>This enum is immutable and thread-safe</li>
 * </ul>
 */
public enum AnimationType {
    /**
     * Represents a carousel animation.
     */
	CAROUSEL("CAROUSEL"),

    /**
     * Represents a loading animation.
     */
	LOADING("LOADING"),

    /**
     * Represents a continuous animation.
     */
	CONTINUOUS("CONTINUOUS");
	
	private String shortName;

    AnimationType (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the animation type.
     *
     * @return the short name
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates an {@linkplain AnimationType} from a string value
     *
     * @param value the value
     * @return the {@link AnimationType}
     */
    @JsonCreator
    public static AnimationType create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(AnimationType v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the animation type.
     *
     * @return the short name
     */
    public String getShortName() {
        return shortName;
    }
}
