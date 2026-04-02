package com.looksee.audits.performance;

import com.looksee.models.LookseeObject;
import com.looksee.models.enums.InsightType;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Google Page speed audit
 */
@Getter
@Setter
@NoArgsConstructor
public class PageSpeedAudit extends LookseeObject {

	private String name;
	private String title;
	private String description; //definition
	private Double score;      //scoring
	private String scoreDisplayMode;
	private String displayValue;
	private String explanation; //meaning
	private String errorMessage;
	private Double numericValue;
	private String type;
	
	@Relationship(type = "HAS")
	private List<AuditDetail> details;
	
	/**
	 * Constructor for {@link PageSpeedAudit}
	 *
	 * @param name the name of the page speed audit
	 * @param description the description of the page speed audit
	 * @param display_value the display value of the page speed audit
	 * @param error_message the error message of the page speed audit
	 * @param explanation the explanation of the page speed audit
	 * @param numeric_value the numeric value of the page speed audit
	 * @param score_display_value the score display value of the page speed audit
	 * @param title the title of the page speed audit
	 *
	 * precondition: name != null
	 * precondition: title != null
	 */
	public PageSpeedAudit(
			String name,
			String description,
			String display_value,
			String error_message,
			String explanation,
			Double numeric_value,
			String score_display_value,
			String title
	) {
		super();

		assert name != null : "name must not be null";
		assert title != null : "title must not be null";

		setName(name);
		setDescription(description);
		setDisplayValue(display_value);
		setErrorMessage(error_message);
		setExplanation(explanation);
		setNumericValue(numeric_value);
		setScoreDisplayMode(score_display_value);
		setTitle(title);
		setKey(generateKey());
	}
	
	/**
	 * Generates a key for the page speed audit
	 * 
	 * @return the key for the page speed audit
	 */
	@Override
	public String generateKey() {
		return "pagespeedaudit"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(getTitle() + getDescription() + getDisplayValue() + getErrorMessage() + getExplanation() + getNumericValue() + getScoreDisplayMode() + getTitle());
	}
	
	/**
	 * Gets the type of the page speed audit
	 * @return the type of the page speed audit
	 */
	public InsightType getType() {
		return InsightType.create(type);
	}

	/**
	 * Sets the type of the page speed audit
	 * @param type the type of the page speed audit
	 *
	 * precondition: type != null
	 */
	public void setType(InsightType type) {
		assert type != null : "type must not be null";
		this.type = type.getShortName();
	}
}
