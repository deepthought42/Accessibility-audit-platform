package com.looksee.models.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A ElementExtractionError is a message that is used to report an error in element extraction
 */
@NoArgsConstructor
@Getter
@Setter
public class ElementExtractionError extends Message{
	private String pageUrl;
	private String message;
	private long pageId;

	/**
	 * Creates a new ElementExtractionError
	 * @param account_id the account id
	 * @param page_state_id the page state id
	 * @param page_url the page url
	 * @param msg the message
	 *
	 * precondition: page_url != null
	 * precondition: msg != null
	 */
	public ElementExtractionError(long account_id,
									long page_state_id,
									String page_url,
									String msg
	) {
		super(account_id);

		assert page_url != null : "page_url must not be null";
		assert msg != null : "msg must not be null";

		setPageId(page_state_id);
		setMessage(msg);
		setPageUrl(page_url);
	}
}
