package com.looksee.models.audit;

import com.looksee.models.SimpleElement;
import com.looksee.models.audit.messages.UXIssueMessage;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * A mapping of issues to elements
 */
@Getter
@Setter
public class ElementIssueTwoWayMapping {
	private Collection<? extends UXIssueMessage> issues;
	private Collection<SimpleElement> elements;
	
	private Map<String, String> issuesElementMap; // 1 to 1 correlation
	private Map<String, Set<String>> elementIssues; // 1 to many correlation
	
	private AuditScore scores;
	private String pageSrc;
	
	/**
	 * Constructs an {@link ElementIssueTwoWayMapping}
	 *
	 * @param issues the issues
	 * @param elements the elements
	 * @param issue_element_map the issue element map
	 * @param element_issues the element issues
	 * @param audit_score the audit score
	 * @param page_src the page source
	 *
	 * precondition: issues != null
	 * precondition: elements != null
	 * precondition: issue_element_map != null
	 * precondition: element_issues != null
	 * precondition: audit_score != null
	 * precondition: page_src != null
	 */
	 public ElementIssueTwoWayMapping(
			Collection<? extends UXIssueMessage> issues,
			Collection<SimpleElement> elements,
			Map<String, String> issue_element_map,
			Map<String, Set<String>> element_issues,
			AuditScore audit_score,
			String page_src
	) {
		assert issues != null;
		assert elements != null;
		assert issue_element_map != null;
		assert element_issues != null;
		assert audit_score != null;
		assert page_src != null;

		setIssues(issues);
		setElements(elements);
		setIssuesElementMap(issue_element_map);
		setElementIssues(element_issues);
		setScores(audit_score);
		setPageSrc(page_src);
	}
}
