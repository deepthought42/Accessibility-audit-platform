package com.looksee.models.audit.messages;

import com.looksee.models.LookseeObject;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Represents a UX issue message.
 *
 * <p>invariant: title may be null but when set should be non-empty</p>
 * <p>invariant: priority is always a valid {@link Priority} value when set</p>
 * <p>invariant: category is always a valid {@link AuditCategory} value when set</p>
 * <p>invariant: labels is non-null when set via constructor</p>
 * <p>invariant: maxPoints &gt; 0 when score is computed</p>
 */
@Node
@Getter
@Setter
@NoArgsConstructor
public class UXIssueMessage extends LookseeObject {
	private static Logger log = LoggerFactory.getLogger(UXIssueMessage.class);

	private String title;
	private String description;
	private String whyItMatters;
	private String recommendation;
	private String priority;
	private String type;
	private String category;
	private String wcagCompliance;
	private Set<String> labels;
	private int points;
	private int maxPoints;
	private int score;
	
	/**
	 * constructor for the ux issue message
	 * @param priority {@link Priority}
	 * @param description description of the ux issue message
	 * @param type {@link ObservationType}
	 * @param category {@link AuditCategory}
	 * @param wcagCompliance String with the WCAG compliance requirement
	 * @param labels labels of the ux issue message
	 * @param whyItMatters why it matters
	 * @param title title of the ux issue message
	 * @param points points of the ux issue message
	 * @param maxPoints max points of the ux issue message
	 * @param recommendation recommendation of the ux issue message
	 *
	 * precondition: priority != null
	 * precondition: type != null
	 * precondition: category != null
	 * precondition: labels != null
	 */
	public UXIssueMessage(
			Priority priority,
			String description,
			ObservationType type,
			AuditCategory category,
			String wcagCompliance,
			Set<String> labels,
			String whyItMatters,
			String title,
			int points,
			int maxPoints,
			String recommendation
	) {
		assert priority != null;
		assert type != null;
		assert category != null;
		assert labels != null;

		setPriority(priority);
		setDescription(description);
		setType(type);
		setCategory(category);
		setRecommendation(recommendation);
		setWcagCompliance(wcagCompliance);
		setLabels(labels);
		setWhyItMatters(whyItMatters);
		setTitle(title);
		setPoints(points);
		setMaxPoints(maxPoints);
		setScore( (int)((points/(double)maxPoints)*100) );
		setKey(generateKey());
	}
	
	/**
	 * Prints the UX issue message
	 */
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
	
	/**
	 * Gets the priority of the UX issue message
	 *
	 * @return the priority of the UX issue message
	 */
	public Priority getPriority() {
		return Priority.create(this.priority);
	}
	
	/**
	 * Sets the priority of the UX issue message.
	 *
	 * @param priority the priority of the UX issue message
	 *
	 * precondition: priority != null
	 */
	public void setPriority(Priority priority) {
		assert priority != null;
		this.priority = priority.getShortName();
	}
	
	/**
	 * Gets the type of the UX issue message
	 *
	 * @return the type of the UX issue message
	 */
	public ObservationType getType() {
		return ObservationType.create(type);
	}

	/**
	 * Sets the type of the UX issue message.
	 *
	 * @param type the type of the UX issue message
	 *
	 * precondition: type != null
	 */
	public void setType(ObservationType type) {
		assert type != null;
		this.type = type.getShortName();
	}

	/**
	 * Gets the category of the UX issue message
	 *
	 * @return the category of the UX issue message
	 */
	public AuditCategory getCategory() {
		return AuditCategory.create(category);
	}

	/**
	 * Sets the category of the UX issue message.
	 *
	 * @param category the category of the UX issue message
	 *
	 * precondition: category != null
	 */
	public void setCategory(AuditCategory category) {
		assert category != null;
		this.category = category.getShortName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String generateKey() {
		return "issuemessage"+UUID.randomUUID();
	}
}
