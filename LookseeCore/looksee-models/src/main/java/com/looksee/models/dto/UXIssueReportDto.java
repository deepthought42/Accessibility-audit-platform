package com.looksee.models.dto;

import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for UX issue report
 */
@Getter
@Setter
@NoArgsConstructor
public class UXIssueReportDto {
	private String title;
	private String description;
	private String whyItMatters;
	private String recommendation;
	private String priority;
	private String type;
	private String category;
	private String wcagCompliance;
	private String elementSelector;
	private String pageUrl;
	
	private Set<String> labels;
	
	/**
	 * Constructor for {@link UXIssueReportDto}
	 * @param recommendation the recommendation
	 * @param priority the priority
	 * @param description the description
	 * @param type the type
	 * @param category the category
	 * @param wcag_compliance the WCAG compliance
	 * @param labels the labels
	 * @param why_it_matters the why it matters
	 * @param title the title
	 * @param element_selector the element selector
	 * @param url the url
	 *
	 * precondition: recommendation != null
	 * precondition: priority != null
	 * precondition: description != null
	 * precondition: type != null
	 * precondition: category != null
	 * precondition: wcag_compliance != null
	 * precondition: labels != null
	 */
	public UXIssueReportDto(
			String recommendation,
			Priority priority,
			String description,
			ObservationType type,
			AuditCategory category,
			String wcag_compliance,
			Set<String> labels,
			String why_it_matters,
			String title,
			String element_selector,
			String url
	) {
		assert recommendation != null;
		assert priority != null;
		assert description != null;
		assert type != null;
		assert category != null;
		assert wcag_compliance != null;
		assert labels != null;

		setRecommendation(recommendation);
		setPriority(priority);
		setDescription(description);
		setType(type);
		setCategory(category);
		setWcagCompliance(wcag_compliance);
		setLabels(labels);
		setWhyItMatters(why_it_matters);
		setElementSelector(element_selector);
		setTitle(title);
		setPageUrl(url);
	}
	
	/**
	 * Gets the {@link Priority priority}
	 * @return the priority
	 */
	public Priority getPriority() {
		return Priority.create(this.priority);
	}
	
	/**
	 * Sets the {@link Priority priority}
	 * @param priority the priority
	 *
	 * precondition: priority != null
	 */
	public void setPriority(Priority priority) {
		assert priority != null;

		this.priority = priority.getShortName();
	}
	
	/**
	 * Gets the {@link ObservationType type}
	 * @return the type
	 */
	public ObservationType getType() {
		return ObservationType.create(type);
	}

	/**
	 * Sets the {@link ObservationType type}
	 * @param type the type
	 *
	 * precondition: type != null
	 */
	public void setType(ObservationType type) {
		assert type != null;

		this.type = type.getShortName();
	}

	/**
	 * Gets the {@link AuditCategory category}
	 * @return the category
	 */
	public AuditCategory getCategory() {
		return AuditCategory.create(category);
	}

	/**
	 * Sets the {@link AuditCategory category}
	 * @param category the category
	 *
	 * precondition: category != null
	 */
	public void setCategory(AuditCategory category) {
		assert category != null;

		this.category = category.getShortName();
	}
}
