package com.looksee.models.audit.stats;

import com.looksee.models.enums.ExecutionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain audit statistics.
 *
 * <p>invariant: status is non-null when set via the full constructor</p>
 * <p>invariant: score values are between 0.0 and 100.0 inclusive</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class DomainAuditStats extends AuditStats{
	
	private long id;
	private int journeysExplored;
	private int journeysTotal;
	private int pageCount;
	private double accessibilityScore;
	private double contentScore;
	private double writtenContentScore;
	private double imageryScore;
	private double infoArchitectureScore;
	private double seoScore;
	private double linkScore;
	private double aestheticScore;
	private double textContrastScore;
	private double nonTextContrastScore;
	private ExecutionStatus status;
	
	/**
	 * Constructs a {@link DomainAuditStats} object
	 *
	 * @param audit_record_id the ID of the audit record
	 */
	public DomainAuditStats(long audit_record_id) {
		setId(audit_record_id);
	}
	
	/**
	 * Constructs a {@link DomainAuditStats} object
	 *
	 * @param audit_record_id the ID of the audit record
	 * @param journeys_explored the number of journeys explored
	 * @param journeys_total the total number of journeys
	 * @param accessibility_score the accessibility score
	 * @param content_score the content score
	 * @param written_content_score the written content score
	 * @param imagery_score the imagery score
	 * @param info_arch_score the info architecture score
	 * @param seo_score the SEO score
	 * @param aesthetic_score the aesthetic score
	 * @param text_contrast_score the text contrast score
	 * @param non_text_contrast_score the non-text contrast score
	 * @param status the status
	 * @param link_score the link score
	 *
	 * precondition: status != null
	 */
	public DomainAuditStats(
			long audit_record_id,
			int journeys_explored,
			int journeys_total,
			double accessibility_score,
			double content_score,
			double written_content_score,
			double imagery_score,
			double info_arch_score,
			double seo_score,
			double aesthetic_score,
			double text_contrast_score,
			double non_text_contrast_score,
			ExecutionStatus status,
			double link_score
	) {
		assert status != null;
		setId(audit_record_id);
		setJourneysExplored(journeys_explored);
		setJourneysTotal(journeys_total);
		setAccessibilityScore(accessibility_score);
		setContentScore(content_score);
		setWrittenContentScore(written_content_score);
		setImageryScore(imagery_score);
		setInfoArchitectureScore(info_arch_score);
		setSeoScore(seo_score);
		setAestheticScore(aesthetic_score);
		setTextContrastScore(text_contrast_score);
		setNonTextContrastScore(non_text_contrast_score);
		setLinkScore(link_score);
		setStatus(status);
	}
}
