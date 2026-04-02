package com.looksee.models.message;

import com.looksee.models.Domain;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request for a discovery action
 */
@Getter
@Setter
@NoArgsConstructor
public class DiscoveryActionRequest {
	private Domain domain;
	private String accountId;
	
	/**
	 * Constructs a {@link DiscoveryActionRequest}
	 *
	 * @param domain the domain
	 * @param accountId the account ID
	 *
	 * precondition: domain != null
	 * precondition: accountId != null
	 */
	public DiscoveryActionRequest(Domain domain, String accountId) {
		assert domain != null : "domain must not be null";
		assert accountId != null : "accountId must not be null";

		this.setDomain(domain);
		this.setAccountId(accountId);
	}
}
