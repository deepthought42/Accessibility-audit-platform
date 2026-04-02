package com.looksee.services;

import com.google.api.client.util.Value;
import com.looksee.models.Account;
import com.looksee.models.enums.SubscriptionPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

/**
 * Provides methods to check if an {@link Account} user has permission to access a restricted resource and verifying that
 * the {@link Account} user has not exceeded their usage.
 *
 */
@Service
@PropertySource("classpath:application.properties")
public class SubscriptionService {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Value("${stripe.agency_basic_price_id}")
	private String agency_basic_price_id;
	
	@Value("${stripe.agency_pro_price_id}")
	private String agency_pro_price_id;
	
	@Value("${stripe.company_basic_price_id}")
	private String company_basic_price_id;
	
	@Value("${stripe.company_pro_price_id}")
	private String company_pro_price_id;
	
	/**
	 * Default constructor for {@link SubscriptionService}
	 */
	public SubscriptionService(){}
	
	/**
	 * checks if user has exceeded limit for page audits based on their subscription
	 * 
	 * @param plan the subscription plan
	 * @param page_audit_cnt the number of page audits
	 * 
	 * @return true if user has exceeded limits for their {@link SubscriptionPlan}, otherwise false
	 * 
	 * precondition: plan != null
	 * precondition: page_audit_cnt >= 0
	 */
	public boolean hasExceededSinglePageAuditLimit(SubscriptionPlan plan, int page_audit_cnt) {
		assert plan != null;
		assert page_audit_cnt >= 0;
		
		if(plan.equals(SubscriptionPlan.FREE) && page_audit_cnt >= 100){
			return true;
		}
		else if(plan.equals(SubscriptionPlan.COMPANY_PRO) && page_audit_cnt >= 1000){
			return true;
		}
		else if(plan.equals(SubscriptionPlan.COMPANY_PREMIUM)){ //UNLIMITED
			return false;
		}
		else if(plan.equals(SubscriptionPlan.AGENCY_PREMIUM) ){ //UNLIMITED
			return false;
		}
		else if(plan.equals(SubscriptionPlan.UNLIMITED)){
			return false;
		}
		
		return false;
	}
	
	/**
	 * checks if user has exceeded limit for page limit for domain audit based on their subscription
	 *
	 * @param plan the subscription plan
	 * @param page_audit_count the number of page audits
	 *
	 * @return true if user has exceeded limits for their {@link SubscriptionPlan}, otherwise false
	 * 
	 * precondition: plan != null
	 * precondition: page_audit_count >= 0
	 */
	public boolean hasExceededDomainPageAuditLimit(SubscriptionPlan plan, int page_audit_count) {
		assert plan != null;
		assert page_audit_count >= 0;
		
		if(plan.equals(SubscriptionPlan.FREE) && page_audit_count >= 20){
			return true;
		}
		else if(plan.equals(SubscriptionPlan.COMPANY_PRO) && page_audit_count >= 200){
			return true;
		}
		else if(plan.equals(SubscriptionPlan.COMPANY_PREMIUM) && page_audit_count >= 500){
			return true;
		}
		else if(plan.equals(SubscriptionPlan.AGENCY_PREMIUM) && page_audit_count >= 2000){
			return true;
		}
		else if(plan.equals(SubscriptionPlan.UNLIMITED)){
			return false;
		}
		
		return false;
	}
	

	/**
	 * Checks if account has exceeded the allowed number of domain audits
	 *
	 * @param plan the subscription plan
	 * @param domain_audit_cnt the number of domain audits
	 * @return true if the account has exceeded the allowed number of domain audits, otherwise false
	 *	
	 * precondition: plan != null
	 * precondition: domain_audit_cnt >= 0
	 */
	public boolean hasExceededDomainAuditLimit(SubscriptionPlan plan, int domain_audit_cnt) {
		assert plan != null;
		assert domain_audit_cnt >= 0;

		if(plan.equals(SubscriptionPlan.FREE) && domain_audit_cnt >= 5){
			return true;
		}
		else if(plan.equals(SubscriptionPlan.COMPANY_PRO) && domain_audit_cnt >= 20){
			return true;
		}
		else if(plan.equals(SubscriptionPlan.COMPANY_PREMIUM) && domain_audit_cnt >= 100){
			return true;
		}
		else if(plan.equals(SubscriptionPlan.AGENCY_PREMIUM) && domain_audit_cnt >= 200){
			return true;
		}
		else if(plan.equals(SubscriptionPlan.UNLIMITED)){
			return true;
		}
		
		return false;
	}
}
