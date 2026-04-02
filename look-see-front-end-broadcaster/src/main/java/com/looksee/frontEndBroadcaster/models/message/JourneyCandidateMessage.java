package com.looksee.frontEndBroadcaster.models.message;

import com.looksee.frontEndBroadcaster.models.enums.BrowserType;
import com.looksee.frontEndBroadcaster.models.journeys.Journey;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 */
public class JourneyCandidateMessage extends Message {

	@Getter
	@Setter
	private long mapId;

	@Getter
	@Setter
	private Journey journey;

	@Getter
	@Setter
	private BrowserType browser;
	
	@Getter
	@Setter
	private long auditRecordId;
	
	public JourneyCandidateMessage() {}
	
	public JourneyCandidateMessage(Journey journey, 
								   BrowserType browser_type, 
								   long account_id, 
								   long audit_record_id, 
								   long map_id)
	{
		super(account_id);
		setJourney(journey);
		setBrowser(browser_type);
		setMapId(map_id);
		setAuditRecordId(audit_record_id);
	}

	public JourneyCandidateMessage clone(){
		return new JourneyCandidateMessage(getJourney(), 
								  getBrowser(), 
								  getAccountId(), 
								  getAuditRecordId(),
								  getMapId());
	}
}
