package com.looksee.models.audit;

import com.looksee.models.LookseeObject;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;


/**
 * Represents an audit record for evaluating various aspects of website design and accessibility.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>The category, subcategory, name, and level must not be null.</li>
 *   <li>Points must be non-negative and not exceed totalPossiblePoints.</li>
 *   <li>Messages and labels collections must not be null.</li>
 *   <li>URL, description and whyItMatters must not be null or empty strings.</li>
 * </ul>
 */
@Getter
@Setter
public class Audit extends LookseeObject {

	private String category;
	private String subcategory;
	private String name;
	private String level;
	private int points;
	private int totalPossiblePoints;
	private String url;
	private boolean accessible;
	private String description;
	private String whyItMatters;
	private Set<UXIssueMessage> messages;
	private Set<String> labels;
	
	/**
	 * Construct empty action object
	 */
	public Audit(){
		super();
		setMessages(new HashSet<>());
	}
	
	/**
	 * Constructs an {@link Audit} with the given parameters
	 *
	 * @param category the category
	 * @param subcategory the subcategory
	 * @param name the name
	 * @param points the points
	 * @param ux_issues the ux issues
	 * @param level the level
	 * @param total_possible_points the total possible points
	 * @param url the url
	 * @param why_it_matters the why it matters
	 * @param description the description
	 * @param is_accessible the accessibility flag
	 *
	 * precondition: category != null
	 * precondition: subcategory != null
	 * precondition: name != null
	 * precondition: ux_issues != null
	 * precondition: level != null
	 * precondition: url != null
	 * precondition: why_it_matters != null
	 * precondition: description != null
	 */
	public Audit(
			AuditCategory category,
			AuditSubcategory subcategory,
			AuditName name,
			int points,
			Set<UXIssueMessage> ux_issues,
			AuditLevel level,
			int total_possible_points,
			String url,
			String why_it_matters,
			String description,
			boolean is_accessible
	) {
		super();

		assert category != null;
		assert subcategory != null;
		assert name != null;
		assert ux_issues != null;
		assert level != null;
		assert url != null;
		assert why_it_matters != null;
		assert description != null;
		
		setName(name);
		setCategory(category);
		setSubcategory(subcategory);
		setPoints(points);
		setTotalPossiblePoints(total_possible_points);
		setMessages(ux_issues);
		setCreatedAt(LocalDateTime.now());
		setLevel(level);
		setUrl(url);
		setWhyItMatters(why_it_matters);
		setDescription(description);
		setAccessible(is_accessible);
		setKey(generateKey());
	}

	/**
	 * Clones the audit
	 *
	 * @return the cloned audit
	 */
	public Audit clone() {
		return new Audit(getCategory(),
						getSubcategory(),
						getName(),
						getPoints(),
						getMessages(),
						getLevel(),
						getTotalPossiblePoints(),
						getUrl(),
						getWhyItMatters(),
						getDescription(),
						isAccessible());
	}

	/**
	 * Generates a key for the audit
	 *
	 * @return string of hashCodes identifying unique fingerprint of object by the contents of the object
	 */
	public String generateKey() {
		return "audit"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(this.getName().toString()+this.getCategory().toString()+this.getLevel().toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString(){
		return this.getKey();
	}

	/**
	 * Gets the category of the audit
	 *
	 * @return the category
	 */
	public AuditCategory getCategory() {
		return AuditCategory.create(category);
	}

	/**
	 * Sets the category of the audit
	 *
	 * @param category the category
	 */
	public void setCategory(AuditCategory category) {
		this.category = category.toString();
	}
	
	/**
	 * Gets the name of the audit
	 *
	 * @return the name
	 */
	public AuditName getName() {
		return AuditName.create(name);
	}
	
	/**
	 * Sets the name of the audit
	 *
	 * @param subcategory the subcategory
	 */
	public void setName(AuditName subcategory) {
		this.name = subcategory.getShortName();
	}
	
	/**
	 * Gets the level of the audit
	 *
	 * @return the level
	 */
	public AuditLevel getLevel() {
		return AuditLevel.create(level);
	}

	/**
	 * Sets the level of the audit
	 *
	 * @param level the level
	 */
	public void setLevel(AuditLevel level) {
		this.level = level.toString();
	}

	/**
	 * Gets the subcategory of the audit
	 *
	 * @return the subcategory
	 */
	public AuditSubcategory getSubcategory() {
		return AuditSubcategory.create(subcategory);
	}

	/**
	 * Sets the subcategory of the audit
	 *
	 * @param subcategory the subcategory
	 */
	public void setSubcategory(AuditSubcategory subcategory) {
		this.subcategory = subcategory.toString();
	}
}
