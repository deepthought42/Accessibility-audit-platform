package com.looksee.models.dto;

import com.looksee.models.Element;
import lombok.Getter;
import lombok.Setter;

/**
 * Data transfer object for {@link Element} object that stores data in a format for browser extension
 *
 * invariant: key != null after construction
 * invariant: xpath != null after construction
 */
@Getter
@Setter
public class ElementStateDto {

	private String key;
	private String xpath;
	
	/**
	 * Constructs an {@link ElementStateDto}
	 *
	 * @param elem the element
	 *
	 * precondition: elem != null
	 */
	public ElementStateDto(Element elem){
		assert elem != null;

		setKey(elem.getKey());
		setXpath(elem.getXpath());
	}
}
