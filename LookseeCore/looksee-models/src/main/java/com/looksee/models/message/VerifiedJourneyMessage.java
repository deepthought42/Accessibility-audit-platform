package com.looksee.models.message;

import com.looksee.models.enums.BrowserType;
import com.looksee.models.journeys.Journey;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A VerifiedJourneyMessage is a message that is used to verify a journey
 */
@NoArgsConstructor
@Getter
@Setter
public class VerifiedJourneyMessage extends Message {

	/**
	 * The journey
	 */
	private Journey journey;

	/**
	 * The browser
	 */
	private BrowserType browser;

	/**
	 * The audit record id
	 */
	private long auditRecordId;

	/**
	 * Creates a new VerifiedJourneyMessage
	 * @param journey the journey
	 * @param browser the browser
	 * @param accountId the account id
	 * @param auditRecordId the audit record id
	 *
	 * precondition: journey != null
	 * precondition: browser != null
	 */
	public VerifiedJourneyMessage( Journey journey,
									BrowserType browser,
									long accountId,
									long auditRecordId)
	{
		super(accountId);

		assert journey != null : "journey must not be null";
		assert browser != null : "browser must not be null";

		setJourney(journey);
		setBrowser(browser);
		setAuditRecordId(auditRecordId);
	}
	
	public VerifiedJourneyMessage clone(){
		return new VerifiedJourneyMessage(	journey.clone(),
											getBrowser(),
											getAccountId(),
											getAuditRecordId());
	}
}