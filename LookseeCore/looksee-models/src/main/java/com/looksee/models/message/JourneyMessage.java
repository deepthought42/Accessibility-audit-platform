package com.looksee.models.message;

import com.looksee.browsing.enums.BrowserType;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.journeys.Journey;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A JourneyMessage is a message that is used to send a journey
 */
@NoArgsConstructor
@Getter
@Setter
public class JourneyMessage extends Message {

	private Journey journey;
	private JourneyStatus status;
	private BrowserType browser;
	private long auditRecordId;

	/**
	 * Creates a new JourneyMessage
	 * @param journey the journey
	 * @param status the status
	 * @param browserType the browser type
	 * @param accountId the account id
	 * @param auditRecordId the audit record id
	 *
	 * precondition: journey != null
	 * precondition: status != null
	 * precondition: browserType != null
	 */
	public JourneyMessage(Journey journey,
						JourneyStatus status,
						BrowserType browserType,
						long accountId,
						long auditRecordId)
	{
		super(accountId);

		assert journey != null : "journey must not be null";
		assert status != null : "status must not be null";
		assert browserType != null : "browserType must not be null";

		setJourney(journey);
		setStatus(status);
		setBrowser(browserType);
		setAuditRecordId(auditRecordId);
	}
	
	public JourneyMessage clone(){
		return new JourneyMessage(journey.clone(),
									getStatus(),
									getBrowser(),
									getAccountId(),
									getAuditRecordId());
	}
}
