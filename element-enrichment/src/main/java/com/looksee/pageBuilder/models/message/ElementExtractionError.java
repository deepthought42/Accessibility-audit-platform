package com.looksee.pageBuilder.models.message;


public class ElementExtractionError extends Message{
	private String page_url;
	private String message;
	private long page_id;
	
	public ElementExtractionError(long account_id, 		
								  long page_state_id, 
								  String page_url,
								  String msg
	) {
		super(account_id);
		setPageId(page_state_id);
		setMessage(msg);
		setPageUrl(page_url);
		
	}

	public String getPageUrl() {
		return page_url;
	}

	public void setPageUrl(String page_url) {
		this.page_url = page_url;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public long getPageId() {
		return page_id;
	}

	public void setPageId(long page_id) {
		this.page_id = page_id;
	}

}
