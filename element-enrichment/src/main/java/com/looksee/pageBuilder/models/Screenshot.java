package com.looksee.pageBuilder.models;

import org.springframework.data.neo4j.core.schema.Node;

/**
 * 
 */
@Node
public class Screenshot extends LookseeObject {
	
	private String browserName;
	private String url;
	private String checksum;
	private int width;
	private int height;
	
	public Screenshot(){}
	
	public Screenshot(String viewport, String browser_name, String checksum, int width, int height){
		setScreenshotUrl(viewport);
		setChecksum(checksum);
		setBrowser(browser_name);
		setKey(generateKey());
		setWidth(width);
		setHeight(height);
	}

	public String getScreenshotUrl() {
		return url;
	}

	public void setScreenshotUrl(String url) {
		this.url = url;
	}

	public String getBrowser() {
		return browserName;
	}

	public void setBrowser(String browser_name) {
		this.browserName = browser_name;
	}
	
	public String generateKey() {
		return "screenshot" + this.checksum;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
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
}
