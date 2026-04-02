package com.looksee.pageBuilder.models;

import java.util.List;


/**
 * 
 */
public class PageLoadAnimation extends LookseeObject {
	
	private List<String> imageUrls;
	private List<String> imageChecksums;
	private String pageUrl;
	
	public PageLoadAnimation(){}

	/**
	 * 
	 * @param image_urls 
	 * 
	 * @pre image_urls != null
	 */
	public PageLoadAnimation(List<String> image_urls, List<String> image_checksums, String page_url) {
		assert image_urls != null;
		setImageUrls(image_urls);
		setImageChecksums(image_checksums);
		setPageUrl(page_url);
		setKey(generateKey());
	}

	@Override
	public String generateKey() {
		return "pageloadanimation:"+getPageUrl();
	}

	public List<String> getImageChecksums() {
		return imageChecksums;
	}

	public void setImageChecksums(List<String> image_checksums) {
		this.imageChecksums = image_checksums;
	}

	public List<String> getImageUrls() {
		return imageUrls;
	}

	public void setImageUrls(List<String> image_urls) {
		this.imageUrls = image_urls;
	}

	public String getPageUrl() {
		return pageUrl;
	}

	public void setPageUrl(String page_url) {
		this.pageUrl = page_url;
	}
}
