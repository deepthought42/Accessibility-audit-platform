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
 * <p>invariant: when constructed with parameters, inherits all invariants from {@link ElementStateIssueMessage}</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Node
public class StockImageIssueMessage extends ElementStateIssueMessage {
	/**
	 * The stock image of the stock image issue message
	 */
	private boolean stockImage;
	
	/**
	 * Constructs a new {@link StockImageIssueMessage}
	 *
	 * @param priority the priority of the stock image issue message
	 * @param description the description of the stock image issue message
	 * @param recommendation the recommendation of the stock image issue message
	 * @param element the element of the stock image issue message
	 * @param category the category of the stock image issue message
	 * @param labels the labels of the stock image issue message
	 * @param wcagCompliance the wcag compliance of the stock image issue message
	 * @param title the title of the stock image issue message
	 * @param pointsAwarded the points awarded of the stock image issue message
	 * @param maxPoints the max points of the stock image issue message
	 * @param isStockImage the stock image of the stock image issue message
	 *
	 * precondition: priority != null
	 * precondition: category != null
	 * precondition: labels != null
	 */
	public StockImageIssueMessage(
			Priority priority,
			String description,
			String recommendation,
			ElementState element,
			AuditCategory category,
			Set<String> labels,
			String wcagCompliance,
			String title,
			int pointsAwarded,
			int maxPoints,
			boolean isStockImage
	) {
		super(	priority,
				description,
				recommendation,
				element,
				category,
				labels,
				wcagCompliance,
				title,
				pointsAwarded,
				maxPoints);

		assert priority != null;
		assert category != null;
		assert labels != null;

		setStockImage(isStockImage);
	}
}
