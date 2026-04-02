package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link CrawlAction crawl actions} that exist in the system
 */
public enum CrawlAction {
    /**
     * Represents a start crawl action.
     */
	START("start"),

    /**
     * Represents a stop crawl action.
     */
	STOP("stop");
	
	private String shortName;

	CrawlAction (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the crawl action.
     * @return the short name of the crawl action
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a CrawlAction from a string.
     * @param value the string to create the CrawlAction from
     * @return the CrawlAction
     */
    @JsonCreator
    public static CrawlAction create (String value) {
        if(value == null) {
            throw new IllegalArgumentException();
        }
        for(CrawlAction v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the short name of the crawl action.
     * @return the short name of the crawl action
     */
    public String getShortName() {
        return shortName;
    }
}
