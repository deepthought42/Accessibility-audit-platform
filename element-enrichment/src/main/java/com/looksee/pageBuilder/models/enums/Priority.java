package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all types of {@link Audit audits} that exist in the system
 */
public enum Priority {
	HIGH("high"), 
	MEDIUM("medium"), 
	LOW("low"),
	NONE("none");
	

	private String shortName;

	Priority (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static Priority create (String value) {
    	assert value != null;
    	assert !value.isEmpty();
    
        for(Priority v : values()) {
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
