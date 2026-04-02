package com.looksee.journeyMapCleanup.models.message;

import com.looksee.journeyMapCleanup.models.enums.BrowserType;
import com.looksee.journeyMapCleanup.models.journeys.Journey;

import lombok.Getter;
import lombok.Setter;

public class DiscardedJourneyMessage extends Message {

	@Getter
	@Setter
	private Journey journey;
	
	@Getter
	@Setter
	private BrowserType browserType;

	@Getter
	@Setter
	private long auditRecordId;
   
	public DiscardedJourneyMessage() {}
	
	public DiscardedJourneyMessage( Journey journey, 
									BrowserType browserType, 
									long accountId, 
									long auditRecordId) {
		super(accountId);
		setJourney(journey);
		setBrowserType(browserType);
		setAuditRecordId(auditRecordId);
	}
}
