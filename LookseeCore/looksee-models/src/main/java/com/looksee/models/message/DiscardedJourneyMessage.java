package com.looksee.models.message;

import com.looksee.browser.enums.BrowserType;
import com.looksee.models.journeys.Journey;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A DiscardedJourneyMessage is a message that is used to discard a journey
 */
@NoArgsConstructor
@Getter
@Setter
public class DiscardedJourneyMessage extends Message {

	private Journey journey;
	private BrowserType browserType;
	private long domainId;
	private long accountId;
	private long auditRecordId;

	/**
	 * Creates a new DiscardedJourneyMessage
	 * @param journey the journey to discard
	 * @param browserType the browser type
	 * @param domainId the domain id
	 * @param accountId the account id
	 * @param auditRecordId the audit record id
	 *
	 * precondition: journey != null
	 * precondition: browserType != null
	 */
	public DiscardedJourneyMessage(Journey journey,
									BrowserType browserType,
									long domainId,
									long accountId,
									long auditRecordId) {
		super(accountId);

		assert journey != null : "journey must not be null";
		assert browserType != null : "browserType must not be null";

		setJourney(journey);
		setBrowserType(browserType);
		setDomainId(domainId);
		setAuditRecordId(auditRecordId);
	}
}
