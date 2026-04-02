package com.looksee.journeyErrors.models.message;

import com.looksee.journeyErrors.models.enums.BrowserType;
import com.looksee.journeyErrors.models.journeys.Journey;

/**
 * 
 */
public class JourneyCandidateMessage extends DomainAuditMessage {

	private long map_id;
	private Journey journey;
	private BrowserType browser;
	
	public JourneyCandidateMessage() {}
	
	public JourneyCandidateMessage(Journey journey, 
								   BrowserType browser_type, 
								   long account_id, 
								   long audit_record_id, 
								   long map_id)
	{
		super(account_id, audit_record_id);
		setJourney(journey);
		//setSteps(steps);
		setBrowser(browser_type);
		setMapId(map_id);
	}

	public JourneyCandidateMessage clone(){
		return new JourneyCandidateMessage(getJourney(), 
								  getBrowser(), 
								  getAccountId(), 
								  getDomainAuditRecordId(),
								  getMapId());
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

	public long getMapId() {
		return map_id;
	}

	public void setMapId(long map_id) {
		this.map_id = map_id;
	}
	
}
