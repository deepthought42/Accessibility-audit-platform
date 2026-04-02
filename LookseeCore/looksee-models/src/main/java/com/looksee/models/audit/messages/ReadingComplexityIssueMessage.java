package com.looksee.models.audit.messages;

import com.looksee.models.Element;
import com.looksee.models.ElementState;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.Priority;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * A observation of potential error for a given {@link Element element}.
 *
 * <p>invariant: easeOfReadingScore represents a valid readability metric value</p>
 * <p>invariant: when constructed with parameters, inherits all invariants from {@link ElementStateIssueMessage}</p>
 */
@Node
@Getter
@Setter
@NoArgsConstructor
public class ReadingComplexityIssueMessage extends ElementStateIssueMessage {
	/**
	 * The ease of reading score of the reading complexity issue message
	 */
	private double easeOfReadingScore;

	/**
	 * Constructs a new {@link ReadingComplexityIssueMessage}
	 *
	 * @param priority the priority of the reading complexity issue message
	 * @param description the description of the reading complexity issue message
	 * @param recommendation the recommendation of the reading complexity issue message
	 * @param element the element of the reading complexity issue message
	 * @param category the category of the reading complexity issue message
	 * @param labels the labels of the reading complexity issue message
	 * @param wcag_compliance the wcag compliance of the reading complexity issue message
	 * @param title the title of the reading complexity issue message
	 * @param points_awarded the points awarded of the reading complexity issue message
	 * @param max_points the max points of the reading complexity issue message
	 * @param ease_of_reading_score the ease of reading score of the reading complexity issue message
	 *
	 * precondition: priority != null
	 * precondition: category != null
	 * precondition: labels != null
	 */
	public ReadingComplexityIssueMessage(
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
			double ease_of_reading_score
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

		setEaseOfReadingScore(ease_of_reading_score);
	}

	/**
	 * Gets the ease of reading score of the reading complexity issue message
	 *
	 * @return the ease of reading score of the reading complexity issue message
	 */
	public double getEaseOfReadingScore() {
		return easeOfReadingScore;
	}

	/**
	 * Sets the ease of reading score of the reading complexity issue message
	 *
	 * @param ease_of_reading_score the ease of reading score of the reading complexity issue message
	 */
	public void setEaseOfReadingScore(double ease_of_reading_score) {
		this.easeOfReadingScore = ease_of_reading_score;
	}

}
