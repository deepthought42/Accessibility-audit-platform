package com.looksee.pageBuilder.models;

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
	
	public String getScreenshotUrl() {
		return screenshotUrl;
	}
	public void setScreenshotUrl(String screenshot_url) {
		this.screenshotUrl = screenshot_url;
	}
	
	public int getXLocation() {
		return xLocation;
	}
	public void setXLocation(int x_location) {
		this.xLocation = x_location;
	}
	
	public int getYLocation() {
		return yLocation;
	}
	public void setYLocation(int y_location) {
		this.yLocation = y_location;
	}
	
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getCssSelector() {
		return cssSelector;
	}

	public void setCssSelector(String css_selector) {
		this.cssSelector = css_selector;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isImageFlagged() {
		return imageFlagged;
	}

	public void setImageFlagged(boolean is_image_flagged) {
		this.imageFlagged = is_image_flagged;
	}

	public boolean isAdultContent() {
		return adultContent;
	}

	public void setAdultContent(boolean adult_content) {
		this.adultContent = adult_content;
	}	
}
