package com.looksee.journeyErrors.models.message;

import com.looksee.journeyErrors.models.enums.BrowserType;
import com.looksee.journeyErrors.models.enums.JourneyStatus;
import com.looksee.journeyErrors.models.journeys.Journey;

public class JourneyMessage extends DomainAuditMessage {

	private Journey journey;
	private JourneyStatus status;
	private BrowserType browser;
	
	public JourneyMessage( Journey journey, 
						   JourneyStatus status, 
						   BrowserType browser_type, 
						   long account_id, 
						   long audit_record_id){
		setJourney(journey);
		setStatus(status);
		setBrowser(browser_type);
	}
	
	public JourneyMessage clone(){
		return new JourneyMessage(journey.clone(),
								  getStatus(), 
								  getBrowser(), 
								  getAccountId(),
								  getDomainAuditRecordId());
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
