package com.looksee.models;

import java.util.Map;

/**
 * Represents the browser passing statuses
 */
public class BrowserPassingStatuses {
	private Map<String, Boolean> status_map;

	/**
	 * Gets the browser passing statuses
	 * 
	 * @return the browser passing statuses
	 */
	public Map<String, Boolean> getBrowserPassingStatuses() {
		return status_map;
	}

	/**
	 * Sets the browser passing statuses
	 *
	 * @param browser_passing_statuses the browser passing statuses
	 *
	 * precondition: browser_passing_statuses != null
	 */
	public void setBrowserPassingStatuses(Map<String, Boolean> browser_passing_statuses) {
		assert browser_passing_statuses != null : "browser_passing_statuses must not be null";
		this.status_map = browser_passing_statuses;
	}
}
