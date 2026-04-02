package com.looksee.dto;


/**
 * Data transfer object for {@link Domain} object that is designed to comply with
 * the data format for browser extensions
 */
public class DomainDto {
	private long id;
	private String url;
	private double progress;

	/*
	private int page_count;
	private int pages_audited;
	private double content_score;
	private double content_progress;
	private double info_architecture_score;
	private double info_architecture_progress;
	private double accessibility_score;
	private double accessibility_progress;
	private double aesthetics_score;
	private double aesthetics_progress;
	private double data_extraction_progress;
	private boolean is_audit_running;
	private String message;
	 */
	
	public DomainDto(){}

	public DomainDto(
			long id,
			String url, 
			double progress
	){
		setId(id);
		setUrl(url);
		setProgress(progress);
		/*
		setStatus(status);		
		setPageCount(page_count);
		setContentScore(content_score);
		setContentProgress(content_progress);
		setInfoArchitectureScore(info_architecture_score);
		setInfoArchitectureProgress(info_architecture_progress);
		setAccessibilityScore(accessibility_score);
		setAccessibilityProgress(accessibility_progress);
		setAestheticsScore(aesthetics_score);
		setAestheticsProgress(aesthetics_progress);
		setIsAuditRunning(is_audit_running);
		setPagesAudited(audited_page_count);
		setDataExtractionProgress(data_extraction_progress);
		setMessage(message);
		*/
	}
	
	/*
	public DomainDto(
			long id,
			String url,
			int page_count,
			int audited_page_count,
			double content_score,
			double content_progress,
			double info_architecture_score,
			double info_architecture_progress,
			double accessibility_score,
			double accessibility_progress,
			double aesthetics_score,
			double aesthetics_progress, 
			boolean is_audit_running, 
			double data_extraction_progress,
			String message,
			ExecutionStatus status
	){
		setId(id);
		setUrl(url);
		
		setPageCount(page_count);
		setContentScore(content_score);
		setContentProgress(content_progress);
		setInfoArchitectureScore(info_architecture_score);
		setInfoArchitectureProgress(info_architecture_progress);
		setAccessibilityScore(accessibility_score);
		setAccessibilityProgress(accessibility_progress);
		setAestheticsScore(aesthetics_score);
		setAestheticsProgress(aesthetics_progress);
		setIsAuditRunning(is_audit_running);
		setPagesAudited(audited_page_count);
		setDataExtractionProgress(data_extraction_progress);
		setMessage(message);
		setStatus(status);
	}
	 */

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public double getProgress() {
		return progress;
	}

	public void setProgress(double progress) {
		this.progress = progress;
	}

	/*
	public double getContentScore() {
		return content_score;
	}

	public void setContentScore(double content_score) {
		this.content_score = content_score;
	}

	public double getInfoArchitectureScore() {
		return info_architecture_score;
	}

	public void setInfoArchitectureScore(double info_architecture_score) {
		this.info_architecture_score = info_architecture_score;
	}

	public double getAccessibilityScore() {
		return accessibility_score;
	}

	public void setAccessibilityScore(double accessibility_score) {
		this.accessibility_score = accessibility_score;
	}

	public double getAestheticsScore() {
		return aesthetics_score;
	}

	public void setAestheticsScore(double aesthetics_score) {
		this.aesthetics_score = aesthetics_score;
	}

	public int getPageCount() {
		return page_count;
	}

	public void setPageCount(int page_count) {
		this.page_count = page_count;
	}

	public boolean getIsAuditRunning() {
		return is_audit_running;
	}

	public void setIsAuditRunning(boolean is_audit_running) {
		this.is_audit_running = is_audit_running;
	}

	public double getContentProgress() {
		return content_progress;
	}

	public void setContentProgress(double content_progress) {
		this.content_progress = content_progress;
	}

	public double getInfoArchitectureProgress() {
		return info_architecture_progress;
	}

	public void setInfoArchitectureProgress(double info_architecture_progress) {
		this.info_architecture_progress = info_architecture_progress;
	}

	public double getAccessibilityProgress() {
		return accessibility_progress;
	}

	public void setAccessibilityProgress(double accessibility_progress) {
		this.accessibility_progress = accessibility_progress;
	}

	public double getAestheticsProgress() {
		return aesthetics_progress;
	}

	public void setAestheticsProgress(double aesthetics_progress) {
		this.aesthetics_progress = aesthetics_progress;
	}

	public int getPagesAudited() {
		return pages_audited;
	}

	public void setPagesAudited(int pages_audited) {
		this.pages_audited = pages_audited;
	}

	public double getDataExtractionProgress() {
		return data_extraction_progress;
	}

	public void setDataExtractionProgress(double data_extraction_progress) {
		this.data_extraction_progress = data_extraction_progress;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public ExecutionStatus getStatus() {
		return ExecutionStatus.create(status);
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status.getShortName();
	}
	 */
}
