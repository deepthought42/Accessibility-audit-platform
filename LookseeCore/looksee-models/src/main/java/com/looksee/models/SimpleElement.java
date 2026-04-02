package com.looksee.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores a simple element.
 *
 * invariant: key != null
 * invariant: cssSelector != null
 */
@Getter
@Setter
@NoArgsConstructor
public class SimpleElement {
	
	private String key;
	private String screenshotUrl;
	private int xLocation;
	private int yLocation;
	private int width;
	private int height;
	private String text;
	private String cssSelector;
	private boolean imageFlagged;
	private boolean adultContent;
	
	/**
	 * Constructs a new {@link SimpleElement}
	 *
	 * @param key the key of the simple element
	 * @param screenshot_url the screenshot url of the simple element
	 * @param x the x location of the simple element
	 * @param y the y location of the simple element
	 * @param width the width of the simple element
	 * @param height the height of the simple element
	 * @param css_selector the css selector of the simple element
	 * @param text the text of the simple element
	 * @param is_image_flagged the image flagged of the simple element
	 * @param is_adult_content the adult content of the simple element
	 *
	 * precondition: key != null
	 * precondition: css_selector != null
	 */
	public SimpleElement(String key,
						String screenshot_url,
						int x,
						int y,
						int width,
						int height,
						String css_selector,
						String text,
						boolean is_image_flagged,
						boolean is_adult_content) {
		assert key != null;
		assert css_selector != null;
		setKey(key);
		setScreenshotUrl(screenshot_url);
		setXLocation(x);
		setYLocation(y);
		setWidth(width);
		setHeight(height);
		setCssSelector(css_selector);
		setText(text);
		setImageFlagged(is_image_flagged);
		setAdultContent(is_adult_content);
	}
}
