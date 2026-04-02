package com.looksee.pageBuilder.models.message;

import com.looksee.pageBuilder.models.enums.BrowserType;
import com.looksee.pageBuilder.models.journeys.Journey;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 */
public class VerifiedJourneyMessage extends Message {

	@Getter
	@Setter
	private Journey journey;

	@Getter
	@Setter
	private BrowserType browser;

	@Getter
	@Setter
	private long auditRecordId;

	public VerifiedJourneyMessage() {}
	
	public VerifiedJourneyMessage( Journey journey, 
								   BrowserType browser,
								   long account_id, 
								   long audit_record_id)
	{
		super(account_id);
		setJourney(journey);
		setBrowser(browser);
		setAuditRecordId(audit_record_id);
	}
	
	public VerifiedJourneyMessage clone(){
		return new VerifiedJourneyMessage(	journey.clone(), 
											getBrowser(), 
											getAccountId(), 
											getAuditRecordId());
	}
}