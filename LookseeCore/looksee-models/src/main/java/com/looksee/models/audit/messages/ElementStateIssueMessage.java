package com.looksee.models.audit.messages;


import com.looksee.models.ElementState;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;


/**
 * A observation of potential error for a given {@link ElementState element state}.
 *
 * <p>invariant: element may be null for default-constructed instances</p>
 * <p>invariant: when constructed with parameters, inherits all invariants from {@link UXIssueMessage}</p>
 */
@Node
@NoArgsConstructor
public class ElementStateIssueMessage extends UXIssueMessage {
	private static Logger log = LoggerFactory.getLogger(ElementStateIssueMessage.class);

	/**
	 * The element state of the element state issue message
	 */
	@Relationship(type = "FOR")
	private ElementState element;

	/**
	 * Constructs a new {@link ElementStateIssueMessage}
	 *
	 * @param priority the priority of the element state issue message
	 * @param description the description of the element state issue message
	 * @param recommendation the recommendation of the element state issue message
	 * @param element the element state of the element state issue message
	 * @param category the category of the element state issue message
	 * @param labels the labels of the element state issue message
	 * @param wcag_compliance the wcag compliance of the element state issue message
	 * @param title the title of the element state issue message
	 * @param points_awarded the points awarded of the element state issue message
	 * @param max_points the max points of the element state issue message
	 *
	 * precondition: priority != null
	 * precondition: category != null
	 * precondition: labels != null
	 */
	public ElementStateIssueMessage(
			Priority priority,
			String description,
			String recommendation,
			ElementState element,
			AuditCategory category,
			Set<String> labels,
			String wcag_compliance,
			String title,
			int points_awarded,
			int max_points
	) {
		super(	priority,
				description,
				ObservationType.ELEMENT,
				category,
				wcag_compliance,
				labels,
				"",
				title,
				points_awarded,
				max_points,
				recommendation);

		assert priority != null;
		assert category != null;
		assert labels != null;

		setElement(element);
	}

	/**
	 * Returns the element state of the element state issue message
	 *
	 * @return the element state of the element state issue message
	 */
	public ElementState getElement() {
		return element;
	}

	/**
	 * Sets the element state of the element state issue message.
	 *
	 * @param element the element state of the element state issue message
	 *
	 * precondition: element != null
	 */
	public void setElement(ElementState element) {
		assert element != null;
		this.element = element;
	}
	
	/**
	 * Prints the element state issue message
	 */
	@Override
	public void print() {
		log.warn("ux issue key :: "+getKey());
		log.warn("ux issue desc :: "+getDescription());
		log.warn("ux issue points :: "+getPoints());
		log.warn("ux issue max point :: "+getMaxPoints());
		log.warn("ux issue reco :: "+getRecommendation());
		log.warn("ux issue score :: "+getScore());
		log.warn("ux issue title ::"+ getTitle());
		log.warn("ux issue wcag :: "+getWcagCompliance());
		log.warn("ux issue why it matters :: "+getWhyItMatters());
		log.warn("ux issue category :: "+getCategory());
		log.warn("ux issue labels:: "+getLabels());
		log.warn("ux issue priority :: "+getPriority());
		log.warn("ux issue type :: "+getType());
		log.warn("------------------------------------------------------------------------------");
	}
}
