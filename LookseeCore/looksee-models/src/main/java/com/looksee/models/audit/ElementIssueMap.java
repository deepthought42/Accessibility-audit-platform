package com.looksee.models.audit;

import com.looksee.models.SimpleElement;
import com.looksee.models.audit.messages.UXIssueMessage;

import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * A mapping of issues to elements
 */
@Getter
@Setter
//deprecated on 8-24-2021
@Deprecated
public class ElementIssueMap {
	private Set<UXIssueMessage> issues;
	private SimpleElement element;

	/**
	 * Constructs an {@link ElementIssueMap}
	 *
	 * @param issues the issues
	 * @param elements the elements
	 *
	 * precondition: issues != null
	 * precondition: elements != null
	 */
	public ElementIssueMap(
			Set<UXIssueMessage> issues,
			SimpleElement elements
	) {
		assert issues != null;
		assert elements != null;

		setIssues(issues);
		setElement(elements);
	}
}
