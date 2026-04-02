package com.looksee.dto;

import com.looksee.frontEndBroadcaster.models.Element;

/**
 * Data transfer object for {@link Element} object that stores data in a format for browser extension
 */
public class ElementStateDto {

	private String key;
	private String xpath;
	
	public ElementStateDto(Element elem){
		setKey(elem.getKey());
		setXpath(elem.getXpath());
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getXpath() {
		return xpath;
	}

	public void setXpath(String xpath) {
		this.xpath = xpath;
	}
}
