package com.looksee.models;

import com.looksee.models.audit.AuditRecord;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Defines the type of package paid for, which domains are registered and which Users belong to the account
 *
 * <p><b>Class Invariants:</b>
 * <ul>
 *   <li>invariant: domains is never null</li>
 *   <li>invariant: audits is never null</li>
 *   <li>invariant: onboardedSteps is never null</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@Node
public class Account extends LookseeObject{

	private String userId;
	private String email;
	private String customerToken;
	private String subscriptionToken;
	private String subscriptionType;
	private String lastDomainUrl;
	private List<String> onboardedSteps;
	private String apiToken;
	private String name;
	
	@Relationship(type = "HAS")
	private Set<Domain> domains = new HashSet<>();

	@Relationship(type = "HAS")
	private Set<AuditRecord> audits = new HashSet<>();

	/**
	 * Constructor for {@link Account}
	 *
	 * @param userId the user_id of the account
	 * @param email the email of the account
	 * @param customerToken the customer_token of the account
	 * @param subscriptionToken the subscription_token of the account
	 *
	 * precondition: userId != null
	 * precondition: email != null
	 * precondition: customerToken != null
	 * precondition: subscriptionToken != null
	 */
	public Account(
			String userId,
			String email,
			String customerToken,
			String subscriptionToken
	){
		super();

		assert userId != null;
		assert email != null;
		assert customerToken != null;
		assert subscriptionToken != null;

		setUserId(userId);
		setEmail(email);
		setCustomerToken(customerToken);
		setSubscriptionToken(subscriptionToken);
		setOnboardedSteps(new ArrayList<String>());
		setName("");
	}

	/**
	 * Sets the onboarded steps for the account
	 *
	 * @param onboarded_steps the onboarded steps for the account
	 *
	 * precondition: onboarded_steps != null
	 */
	public void setOnboardedSteps(List<String> onboarded_steps) {
		if(onboarded_steps == null){
			this.onboardedSteps = new ArrayList<String>();
		}
		else{
			this.onboardedSteps = onboarded_steps;
		}
	}

	/**
	 * Adds an onboarding step to the account
	 *
	 * @param step_name the name of the onboarding step
	 *
	 * precondition: step_name != null
	 */
	public void addOnboardingStep(String step_name) {
		assert step_name != null;

		if(!this.onboardedSteps.contains(step_name)){
			this.onboardedSteps.add(step_name);
		}
	}

	/**
	 * Adds a domain to the account
	 *
	 * @param domain the domain to add
	 *
	 * precondition: domain != null
	 */
	public void addDomain(Domain domain) {
		assert domain != null;

		this.domains.add(domain);
	}

	/**
	 * Removes a domain from the account
	 *
	 * @param domain the domain to remove
	 *
	 * precondition: domain != null
	 */
	public void removeDomain(Domain domain) {
		assert domain != null;

		boolean domain_found = false;
		for(Domain curr_domain : this.domains){
			if(curr_domain.getKey().equals(domain.getKey())){
				domain_found = true;
				break;
			}
		}

		if(domain_found){
			this.domains.remove(domain);
		}
	}

	/**
	 * Adds an audit record to the account
	 *
	 * @param record the audit record to add
	 *
	 * precondition: record != null
	 */
	public void addAuditRecord(AuditRecord record) {
		assert record != null;

		this.audits.add(record);
	}

	/**
	 * Generates a key for the account
	 *
	 * @return the key for the account
	 */
	@Override
	public String generateKey() {
		return UUID.randomUUID().toString();
	}
}
