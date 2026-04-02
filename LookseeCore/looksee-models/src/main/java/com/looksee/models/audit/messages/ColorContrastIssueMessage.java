package com.looksee.models.audit.messages;

import com.looksee.models.ElementState;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;


/**
 * A observation of potential error for a given color palette.
 *
 * <p>invariant: foregroundColor is non-null and non-empty when set via constructor</p>
 * <p>invariant: backgroundColor is non-null and non-empty when set via constructor</p>
 * <p>invariant: contrast is a non-negative value</p>
 */
@NoArgsConstructor
@Getter
@Setter
@Node
public class ColorContrastIssueMessage extends ElementStateIssueMessage{

	private double contrast;
	private String foregroundColor;
	private String backgroundColor;
	private String fontSize;
	
	/**
	 * Constructs new instance
	 *
	 * @param priority the priority of the color contrast issue
	 * @param description the description of the color contrast issue
	 * @param contrast the contrast of the color contrast issue
	 * @param foreground_color the foreground color of the color contrast issue
	 * @param background_color the background color of the color contrast issue
	 * @param element the element of the color contrast issue
	 * @param category the category of the color contrast issue
	 * @param labels the labels of the color contrast issue
	 * @param wcag_compliance the WCAG compliance of the color contrast issue
	 * @param title the title of the color contrast issue
	 * @param font_size the font size of the color contrast issue
	 * @param points_earned the points earned of the color contrast issue
	 * @param max_points the max points of the color contrast issue
	 * @param recommendation the recommendation of the color contrast issue
	 *
	 * precondition: priority != null
	 * precondition: recommendation != null
	 * precondition: !recommendation.isEmpty()
	 * precondition: element != null
	 * precondition: foreground_color != null
	 * precondition: !foreground_color.isEmpty()
	 * precondition: assert background_color != null
	 * precondition: !background_color.isEmpty()
	 *
	 */
	public ColorContrastIssueMessage(
			Priority priority,
			String description,
			double contrast,
			String foreground_color,
			String background_color,
			ElementState element,
			AuditCategory category,
			Set<String> labels,
			String wcag_compliance,
			String title,
			String font_size,
			int points_earned,
			int max_points,
			String recommendation
	) {
		assert priority != null;
		assert recommendation != null;
		assert !recommendation.isEmpty();
		assert element != null;
		assert foreground_color != null;
		assert !foreground_color.isEmpty();
		assert background_color != null;
		assert !background_color.isEmpty();
		assert category != null;
		assert labels != null;

		setPriority(priority);
		setDescription(description);
		setRecommendation(recommendation);
		setContrast(contrast);
		setForegroundColor(foreground_color);
		setBackgroundColor(background_color);
		setElement(element);
		setCategory(category);
		setLabels(labels);
		setType(ObservationType.COLOR_CONTRAST);
		setWcagCompliance(wcag_compliance);
		setTitle(title);
		setFontSize(font_size);
		setPoints(points_earned);
		setMaxPoints(max_points);
		setKey(this.generateKey());
	}
}
