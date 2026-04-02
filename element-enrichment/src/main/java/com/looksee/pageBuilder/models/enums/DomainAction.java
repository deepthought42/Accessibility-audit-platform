package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DomainAction {
	CREATE("create"), 
	DELETE("delete");
	
	private String shortName;

	DomainAction (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static DomainAction create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(DomainAction v : values()) {
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
