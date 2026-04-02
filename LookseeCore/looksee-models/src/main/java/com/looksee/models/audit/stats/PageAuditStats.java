package com.looksee.models.audit.stats;

import com.looksee.models.enums.ExecutionStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Page audit stats for a page audit record.
 *
 * <p>invariant: status is a valid {@link ExecutionStatus} string representation when set</p>
 * <p>invariant: startTime is non-null when constructed with parameters</p>
 * <p>invariant: score values are between 0.0 and 100.0 inclusive</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class PageAuditStats extends AuditStats{
	
	private LocalDateTime startTime; //time that the page audit started

	private LocalDateTime endTime;

	private long auditRecordId;

	private String status;
	
	private double accessibilityScore;

	private double contentScore;
	
	//content sub-category score
	private int writtenContentIssueCount;
	private int imageryIssueCount;
	private int videoIssueCount;
	private int auditIssueCount;
	private int imageCopyrightIssueCount;

	private double writtenContentScore;

	private double imageryScore;

	private double videosScore;

	private double audioScore;
	
	private double infoArchitectureScore;
	
	//info architecture audit sub-categories
	private int seoIssueCount;
	private int menuIssueCount;
	private int performanceIssueCount;
	private int linkIssueCount;
	
	private double seoScore;

	private double menuAnalysisScore;

	private double performanceScore;

	private double linkScore;
	
	private double aestheticScore;
	
	//aesthetic audit sub-categories
	private int textContrastIssueCount;
	private int nonTextContrastIssueCount;
	private int typographyIssueCount;
	private int whitespaceIssueCount;
	private int brandingIssueCount;
	
	private double textContrastScore;
	private double nonTextContrastScore;
	private double typographyScore;
	private double whitespaceScore;
	private double brandingScore;
		
	private int totalIssues;
	private double overallScore;
	
	
	/**
	 * Constructor for {@link PageAuditStats}
	 * @param audit_record_id the id of the audit record
	 */
	public PageAuditStats(long audit_record_id) {
		setStartTime(LocalDateTime.now());
		setAuditRecordId(audit_record_id);
	}
	
	/**
	 * Constructor for {@link PageAuditStats}
	 * @param audit_record_id the id of the audit record
	 * @param start_time the start time of the audit
	 * @param end_time the end time of the audit
	 * @param accessibility_score the accessibility score
	 * @param content_score the content score
	 * @param written_content_score the written content score
	 * @param imagery_score the imagery score
	 * @param videos_score the videos score
	 * @param audio_score the audio score
	 * @param info_arch_score the information architecture score
	 * @param seo_score the SEO score
	 * @param menu_analysis_score the menu analysis score
	 * @param performance_score the performance score
	 * @param aesthetic_score the aesthetic score
	 * @param text_contrast_score the text contrast score
	 * @param non_text_contrast_score the non-text contrast score
	 * @param typography_score the typography score
	 * @param whitespace_score the whitespace score
	 * @param branding_score the branding score
	 * @param total_issues the total issues
	 * @param status the status of the audit
	 * @param link_score the link score
	 *
	 * precondition: start_time != null
	 * precondition: status != null
	 */
	public PageAuditStats(
			long audit_record_id,
			LocalDateTime start_time,
			LocalDateTime end_time,
			double accessibility_score,
			double content_score,
			double written_content_score,
			double imagery_score,
			double videos_score,
			double audio_score,
			double info_arch_score,
			double seo_score,
			double menu_analysis_score,
			double performance_score,
			double aesthetic_score,
			double text_contrast_score,
			double non_text_contrast_score,
			double typography_score,
			double whitespace_score,
			double branding_score,
			int total_issues,
			ExecutionStatus status,
			double link_score
	) {
		assert start_time != null;
		assert status != null;
		setAuditRecordId(audit_record_id);
		setStartTime(start_time);
		setEndTime(end_time);
		setAccessibilityScore(accessibility_score);
		setContentScore(content_score);
		setWrittenContentScore(written_content_score);
		setImageryScore(imagery_score);
		setVideosScore(videos_score);
		setAudioScore(audio_score);
		setSeoScore(seo_score);
		setMenuAnalysisScore(menu_analysis_score);
		setPerformanceScore(performance_score);
		setTextContrastScore(text_contrast_score);
		setNonTextContrastScore(non_text_contrast_score);
		setTypographyScore(typography_score);
		setWhitespaceScore(whitespace_score);
		setBrandingScore(branding_score);
		setTotalIssues(total_issues);
		setLinkScore(link_score);
		setStatus(status);
	}

	/**
	 * Get the status of the audit
	 * @return the status of the audit
	 */
	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	/**
	 * Set the status of the audit.
	 *
	 * @param status the status of the audit
	 *
	 * precondition: status != null
	 */
	public void setStatus(ExecutionStatus status) {
		assert status != null;
		this.status = status.toString();
	}
}
