package com.looksee.models.dto;

import com.looksee.models.PageState;
import lombok.Getter;
import lombok.Setter;

/**
 * Data transfer object representing a {@link PageState} in a format readable for browser extensions
 *
 * invariant: key != null after construction
 * invariant: url != null after construction
 */
@Getter
@Setter
public class PageStateDto {

	private String key;
	private String url;
	private String type;
	
	/**
	 * Constructor for PageStateDto
	 * @param page the page state
	 *
	 * precondition: page != null
	 */
	public PageStateDto(PageState page){
		assert page != null;

		setKey(page.getKey());
		setUrl(page.getUrl());
	}
}
