package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link ToneOfVoice tone of voices} that exist in the system
 */
public enum ToneOfVoice {
    /**
     * Represents a confident tone of voice.
     */
	CONFIDENT("confident"),

    /**
     * Represents a neutral tone of voice.
     */
	NEUTRAL("neutral"),
    
    /**
     * Represents a joyful tone of voice.
     */
	JOYFUL("joyful"),
    
    /**
     * Represents an optimistic tone of voice.
     */
	OPTIMISTIC("optimistic"),
    
    /**
     * Represents a friendly tone of voice.
     */
	FRIENDLY("friendly"),

    /**
     * Represents an urgent tone of voice.
     */
	URGENT("urgent"),

    /**
     * Represents an analytical tone of voice.
     */
	ANALYTICAl("analytical"),

    /**
     * Represents a respectful tone of voice.
     */
	RESPECTFUL("respectful"),

    /**
     * Represents an unknown tone of voice.
     */
	UNKNOWN("unknown");
	
	private String shortName;

	ToneOfVoice (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the tone of voice.
     * @return the short name of the tone of voice
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a ToneOfVoice from a string.
     * @param value the string to create the ToneOfVoice from
     * @return the ToneOfVoice
     */
    @JsonCreator
    public static ToneOfVoice create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(ToneOfVoice v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the tone of voice.
     * @return the short name of the tone of voice
     */
    public String getShortName() {
        return shortName;
    }
}
