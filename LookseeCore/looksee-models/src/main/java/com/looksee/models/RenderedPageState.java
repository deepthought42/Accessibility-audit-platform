package com.looksee.models;

import lombok.Getter;
import lombok.Setter;

/**
 * Rendered page state
 */
@Getter
@Setter
public class RenderedPageState {

	/**
	 * The page state
	 */
	private PageState pageState;

	/**
	 * Constructor for {@link RenderedPageState}
	 * @param page the {@link PageState} to render
	 *
	 * precondition: page != null
	 */
	public RenderedPageState(PageState page) {
		assert page != null : "page must not be null";

		this.pageState = page;
	}
}
