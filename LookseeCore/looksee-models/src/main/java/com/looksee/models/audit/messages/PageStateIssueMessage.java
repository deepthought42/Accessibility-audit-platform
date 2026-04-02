package com.looksee.models.audit.messages;

import com.looksee.models.Element;
import com.looksee.models.PageState;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * A observation of potential error for a given {@link Element element}.
 *
 * <p>invariant: pageState may be null for default-constructed instances</p>
 * <p>invariant: when constructed with parameters, inherits all invariants from {@link UXIssueMessage}</p>
 */
@Node
@Getter
@Setter
@NoArgsConstructor
public class PageStateIssueMessage extends UXIssueMessage {

	/**
	 * The page state of the page state issue message
	 */
	@Relationship(type = "FOR")
	private PageState pageState;
	
	/**
	 * Constructs a new {@link PageStateIssueMessage}
	 *
	 * @param page the page of the page state issue message
	 * @param description the description of the page state issue message
	 * @param recommendation the recommendation of the page state issue message
	 * @param priority the priority of the page state issue message
	 * @param category the category of the page state issue message
	 * @param labels the labels of the page state issue message
	 * @param wcag_compliance the wcag compliance of the page state issue message
	 * @param title the title of the page state issue message
	 * @param points_awarded the points awarded of the page state issue message
	 * @param max_points the max points of the page state issue message
	 *
	 * precondition: page != null
	 * precondition: priority != null
	 * precondition: category != null
	 * precondition: labels != null
	 */
	public PageStateIssueMessage(
				PageState page,
				String description,
				String recommendation,
				Priority priority,
				AuditCategory category,
				Set<String> labels,
				String wcag_compliance,
				String title,
				int points_awarded,
				int max_points
	) {
		super(	priority,
				description,
				ObservationType.PAGE_STATE,
				category,
				wcag_compliance,
				labels,
				"",
				title,
				points_awarded,
				max_points,
				recommendation);

		assert page != null;
		assert priority != null;
		assert category != null;
		assert labels != null;

		setPage(page);
	}

	/**
	 * Gets the page state of the page state issue message
	 *
	 * @return the page state of the page state issue message
	 */
	public PageState getElements() {
		return pageState;
	}


	/**
	 * Sets the page state of the page state issue message.
	 *
	 * @param page_state the page state of the page state issue message
	 *
	 * precondition: page_state != null
	 */
	public void setPage(PageState page_state) {
		assert page_state != null;
		this.pageState = page_state;
	}
}
