package com.looksee.frontEndBroadcaster.models;

import lombok.Getter;
import lombok.Setter;

/**
 * A simplified data set for page consisting of full page and viewport screenshots, url and the height and width
 *  of the full page screenshot
 *
 */
public class SimplePage {
	
	@Getter
	@Setter
	private long id;

	@Getter
	@Setter
	private String url;

	@Getter
	@Setter
	private String screenshotUrl;

	@Getter
	@Setter
	private String fullPageScreenshotUrl;

	@Getter
	@Setter
	private long width;

	@Getter
	@Setter
	private long height;

	@Getter
	@Setter
	private String htmlSource;

	@Getter
	@Setter
	private String key;
	
	public SimplePage(
			String url, 
			String screenshot_url, 
			String full_page_screenshot_url, 
			long width, 
			long height, 
			String html_source,
			String page_state_key,
			long id
	) {
		setId(id);
		setUrl(url);
		setScreenshotUrl(screenshot_url);
		setFullPageScreenshotUrl(full_page_screenshot_url);
		setWidth(width);
		setHeight(height);
		setHtmlSource(html_source);
		setKey(page_state_key);
	}
}
