package com.looksee.pageBuilder.models.message;

import com.looksee.pageBuilder.models.enums.BrowserType;
import com.looksee.pageBuilder.models.enums.JourneyStatus;
import com.looksee.pageBuilder.models.journeys.Journey;

import lombok.Getter;
import lombok.Setter;


/**
 * 
 */
public class JourneyMessage extends Message {

	private Journey journey;
	private JourneyStatus status;
	private BrowserType browser;

	@Getter
	@Setter
	private long auditRecordId;

	public JourneyMessage(Journey journey,
					   JourneyStatus status, 
					   BrowserType browser_type, 
					   long account_id, 
					   long audit_record_id)
	{
		super(account_id);
		setJourney(journey);
		setStatus(status);
		setBrowser(browser_type);
		setAuditRecordId(audit_record_id);
	}
	
	public JourneyMessage clone(){
		return new JourneyMessage(journey.clone(),
								  getStatus(), 
								  getBrowser(),
								  getAccountId(),
								  getAuditRecordId());
	}

	public JourneyStatus getStatus() {
		return status;
	}

	private void setStatus(JourneyStatus status) {
		this.status = status;
	}

	public BrowserType getBrowser() {
		return browser;
	}

	public void setBrowser(BrowserType browser) {
		this.browser = browser;
	}

	public void setJourney(Journey journey) {
		this.journey = journey;
	}
	
	public Journey getJourney() {
		return this.journey;
	}
}
