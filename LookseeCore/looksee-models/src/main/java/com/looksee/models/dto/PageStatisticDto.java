package com.looksee.models.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * A simplified data set for page consisting of full page and viewport
 * screenshots, url and the height and width of the full page screenshot
 */
@Getter
@Setter
public class PageStatisticDto {
	private long id;
	private String url;
	private long auditRecordId;
	private String screenshotUrl;
	private double contentScore;
	private double contentProgress;
	private double infoArchScore;
	private double infoArchProgress;
	private double accessibilityScore;
	private double accessibilityProgress;
	private double aestheticScore;
	private double aestheticProgress;
	private double dataExtractionProgress;
	private long elementsExtracted;
	private long elementsReviewed;
	private boolean isComplete;
	
	/**
	 * Constructor for PageStatisticDto
	 * @param id the id
	 * @param url the url
	 * @param screenshot_url the screenshot url
	 * @param content_score the content score
	 * @param content_progress the content progress
	 * @param info_arch_score the info arch score
	 * @param info_arch_progress the info arch progress
	 * @param accessibility_score the accessibility score
	 * @param accessibility_progress the accessibility progress
	 * @param aesthetic_score the aesthetic score
	 * @param aesthetic_progress the aesthetic progress
	 * @param audit_record_id the audit record id
	 * @param elements_reviewed the elements reviewed
	 * @param elements_extracted the elements extracted
	 * @param is_complete the is complete
	 * @param data_extraction_progress the data extraction progress
	 *
	 * precondition: url != null
	 * precondition: screenshot_url != null
	 */
	public PageStatisticDto(
			long id,
			String url,
			String screenshot_url,
			double content_score,
			double content_progress,
			double info_arch_score,
			double info_arch_progress,
			double accessibility_score,
			double accessibility_progress,
			double aesthetic_score,
			double aesthetic_progress,
			long audit_record_id,
			long elements_reviewed,
			long elements_extracted,
			boolean is_complete,
			double data_extraction_progress
	) {
		assert url != null;
		assert screenshot_url != null;

		setId(id);
		setUrl(url);
		setAuditRecordId(audit_record_id);
		setScreenshotUrl(screenshot_url);
		setContentScore(content_score);
		setContentProgress(content_progress);
		setInfoArchScore(info_arch_score);
		setInfoArchProgress(info_arch_progress);
		setAccessibilityScore(accessibility_score);
		setAccessibilityProgress(accessibility_progress);
		setAestheticScore(aesthetic_score);
		setAestheticProgress(aesthetic_progress);
		setElementsReviewed(elements_reviewed);
		setElementsExtracted(elements_extracted);
		setDataExtractionProgress(data_extraction_progress);
		setComplete(is_complete);
	}
}
