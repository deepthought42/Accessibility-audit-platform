package com.looksee.pageBuilder.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ExecutionStatus {
	RUNNING("running"), 
	STOPPED("stopped"), 
	COMPLETE("complete"), 
	IN_PROGRESS("in_progress"), 
	ERROR("error"), 
	RUNNING_AUDITS("running audits"), 
	BUILDING_PAGE("building page"), 
	EXTRACTING_ELEMENTS("extracting_elements"), 
	UNKNOWN("unknown"), 
	EXCEEDED_SUBSCRIPTION("exceeded_subscription");
	
	private String shortName;

    ExecutionStatus (String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }

    @JsonCreator
    public static ExecutionStatus create(String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(ExecutionStatus v : values()) {
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
