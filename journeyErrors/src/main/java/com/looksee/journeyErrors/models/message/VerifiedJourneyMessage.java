package com.looksee.journeyErrors.models.message;

import com.looksee.journeyErrors.models.enums.BrowserType;
import com.looksee.journeyErrors.models.enums.JourneyStatus;
import com.looksee.journeyErrors.models.journeys.Journey;

/**
 * 
 */
public class VerifiedJourneyMessage extends DomainAuditMessage {

	private Journey journey;
	private JourneyStatus status;
	private BrowserType browser;
	
	public VerifiedJourneyMessage() {}
	
	public VerifiedJourneyMessage( Journey journey, 
								   JourneyStatus status, 
								   BrowserType browser,
								   long account_id,
								   long audit_record_id)
	{
		super(account_id, audit_record_id);
		setJourney(journey);
		setStatus(status);
		setBrowser(browser);
	}
	
	public VerifiedJourneyMessage clone(){
		return new VerifiedJourneyMessage(	journey.clone(), 
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

	public Journey getJourney() {
		return journey;
	}

	public void setJourney(Journey journey) {
		this.journey = journey;
	}
}

