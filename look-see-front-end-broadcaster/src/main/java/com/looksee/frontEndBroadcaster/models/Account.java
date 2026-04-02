package com.looksee.frontEndBroadcaster.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.Getter;
import lombok.Setter;


/**
 * Defines the type of package paid for, which domains are registered and which Users belong to the account
 */
@Node
public class Account extends LookseeObject{

	@Getter
	@Setter
	private String userId;

	@Getter
	@Setter
	private String email;

	@Getter
	@Setter
	private String customerToken;

	@Getter
	@Setter
	private String subscriptionToken;

	@Getter
	@Setter
	private String subscriptionType;

	@Getter
	@Setter
	private String lastDomainUrl;
	
	@Getter
	@Setter
	private String apiToken;
	
	@Getter
	@Setter
	private String name;
	
	@Getter
	private List<String> onboardedSteps;
	
	@Getter
	@Setter
	@Relationship(type = "HAS")
	private Set<Domain> domains = new HashSet<>();

	@Getter
	@Setter
	@Relationship(type = "HAS")
	private Set<AuditRecord> auditRecords = new HashSet<>();

	public Account(){
		super();
	}

	/**
	 *
	 * @param username
	 * @param customer_token
	 * @param subscription_token
	 *
	 * @pre users != null
	 */
	public Account(
			String user_id, 
			String email, 
			String customer_token, 
			String subscription_token
	){
		super();
		setUserId(user_id);
		setEmail(email);
		setCustomerToken(customer_token);
		setSubscriptionToken(subscription_token);
		setOnboardedSteps(new ArrayList<String>());
		setName("");
	}

	public void setOnboardedSteps(List<String> onboarded_steps) {
		if(onboarded_steps == null){
			this.onboardedSteps = new ArrayList<String>();
		}
		else{
			this.onboardedSteps = onboarded_steps;
		}
	}

	public void addOnboardingStep(String step_name) {
		if(!this.onboardedSteps.contains(step_name)){
			this.onboardedSteps.add(step_name);
		}
	}

	public void addDomain(Domain domain) {
		this.domains.add(domain);
	}

	public void removeDomain(Domain domain) {
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

	public void addAuditRecord(AuditRecord record) {
		this.auditRecords.add(record);
	}

	@Override
	public String generateKey() {
		return UUID.randomUUID().toString();
	}
}
