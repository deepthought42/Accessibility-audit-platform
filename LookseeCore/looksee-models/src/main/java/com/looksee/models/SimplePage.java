package com.looksee.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A simplified data set for page consisting of full page and viewport screenshots, url and the height and width
 * of the full page screenshot.
 *
 * invariant: url != null
 * invariant: screenshotUrl != null
 * invariant: fullPageScreenshotUrl != null
 * invariant: htmlSource != null
 * invariant: key != null
 */
@Getter
@Setter
@NoArgsConstructor
public class SimplePage {

	private long id;
	private String url;
	private String screenshotUrl;
	private String fullPageScreenshotUrl;
	private long width;
	private long height;
	private String htmlSource;
	private String key;
	
	/**
	 * Constructs a new {@link SimplePage}
	 *
	 * @param url the url of the simple page
	 * @param screenshot_url the screenshot url of the simple page
	 * @param full_page_screenshot_url the full page screenshot url of the simple page
	 * @param width the width of the simple page
	 * @param height the height of the simple page
	 * @param html_source the html source of the simple page
	 * @param page_state_key the key of the simple page
	 * @param id the id of the simple page
	 *
	 * precondition: url != null
	 * precondition: screenshot_url != null
	 * precondition: full_page_screenshot_url != null
	 * precondition: html_source != null
	 * precondition: page_state_key != null
	 */
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
		assert url != null;
		assert screenshot_url != null;
		assert full_page_screenshot_url != null;
		assert html_source != null;
		assert page_state_key != null;
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
