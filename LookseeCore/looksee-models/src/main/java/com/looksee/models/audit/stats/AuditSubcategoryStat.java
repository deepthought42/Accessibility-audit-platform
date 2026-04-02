package com.looksee.models.audit.stats;

import com.looksee.models.LookseeObject;
import com.looksee.models.enums.AuditName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the statistics of an audit subcategory.
 *
 * <p>invariant: subcategory is a valid {@link AuditName} string representation when set</p>
 * <p>invariant: startTime is non-null when constructed with parameters</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class AuditSubcategoryStat extends LookseeObject{
	
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private String subcategory;
	private long pagesCompleted;
	private String url;
	
	/**
	 * Constructs an {@link AuditSubcategoryStat} object.
	 *
	 * @param url url of the audit subcategory
	 *
	 * precondition: url != null
	 * precondition: !url.isEmpty()
	 */
	public AuditSubcategoryStat(String url) {
		assert url != null;
		assert !url.isEmpty();
		setUrl(url);
		setStartTime(LocalDateTime.now());
	}
	
	/**
	 * Constructs an {@link AuditSubcategoryStat} object
	 * 
	 * @param subcategory name of the audit subcategory
	 * @param start_time start time of the audit subcategory
	 * @param end_time end time of the audit subcategory
	 * @param page_count number of pages completed
	 *
	 * precondition: subcategory != null
	 * precondition: start_time != null
	 */
	public AuditSubcategoryStat(AuditName subcategory,
								LocalDateTime start_time,
								LocalDateTime end_time,
								int page_count) {
		assert subcategory != null;
		assert start_time != null;
		setStartTime(start_time);
		setEndTime(end_time);
		setPagesCompleted(page_count);
		setSubcategory(subcategory);
	}

	/**
	 * Sets the subcategory of the audit subcategory stat.
	 *
	 * @param subcategory {@link AuditName}
	 *
	 * precondition: subcategory != null
	 */
	public void setSubcategory(AuditName subcategory) {
		assert subcategory != null;
		this.subcategory = subcategory.toString();
	}
	
	/**
	 * Gets the subcategory of the audit subcategory stat
	 * 
	 * @return {@link AuditName}
	 */
	public AuditName getSubcategory() {
		return AuditName.valueOf(subcategory);
	}

	/**
	 * Generates a key for the audit subcategory stat
	 * 
	 * @return {@link String} representing the key
	 */
	@Override
	public String generateKey() {
		return "auditsubcategorystat"+org.apache.commons.codec.digest.DigestUtils.sha512Hex( startTime + url);
	}
}
