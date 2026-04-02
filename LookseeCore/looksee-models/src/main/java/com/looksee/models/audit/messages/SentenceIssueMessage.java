package com.looksee.models.audit.messages;

import com.looksee.models.Element;
import com.looksee.models.ElementState;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.Priority;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * A observation of potential error for a given {@link Element element}.
 *
 * <p>invariant: wordCount &gt;= 0</p>
 * <p>invariant: when constructed with parameters, inherits all invariants from {@link ElementStateIssueMessage}</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class SentenceIssueMessage extends ElementStateIssueMessage {
	
	/**
	 * The word count of the sentence issue message
	 */
	private int wordCount;
	
	/**
	 * Constructs a new {@link SentenceIssueMessage}
	 *
	 * @param priority the priority of the sentence issue message
	 * @param description the description of the sentence issue message
	 * @param recommendation the recommendation of the sentence issue message
	 * @param element the element of the sentence issue message
	 * @param category the category of the sentence issue message
	 * @param labels the labels of the sentence issue message
	 * @param wcag_compliance the wcag compliance of the sentence issue message
	 * @param title the title of the sentence issue message
	 * @param points_awarded the points awarded of the sentence issue message
	 * @param max_points the max points of the sentence issue message
	 * @param word_count the word count of the sentence issue message
	 *
	 * precondition: priority != null
	 * precondition: category != null
	 * precondition: labels != null
	 */
	public SentenceIssueMessage(
			Priority priority,
			String description,
			String recommendation,
			ElementState element,
			AuditCategory category,
			Set<String> labels,
			String wcag_compliance,
			String title,
			int points_awarded,
			int max_points,
			int word_count
	) {
		super(	priority,
				description,
				recommendation,
				element,
				category,
				labels,
				wcag_compliance,
				title,
				points_awarded,
				max_points);

		assert priority != null;
		assert category != null;
		assert labels != null;

		setWordCount(word_count);
	}
}
