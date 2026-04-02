package com.looksee.services;

import com.looksee.audits.performance.PageSpeedAudit;
import com.looksee.audits.performance.PerformanceInsight;
import com.looksee.models.repository.PerformanceInsightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Methods for interacting with {@link PerformanceInsight} object
 */
@Service
public class PerformanceInsightService {
	private static Logger log = LoggerFactory.getLogger(PerformanceInsightService.class.getName());

	@Autowired
	private PerformanceInsightRepository performance_insight_repo;
	
	/**
	 * Saves {@link PerformanceInsight} to database
	 * 
	 * @param performance_insight {@link PerformanceInsight} to save
	 * 
	 * @return {@link PerformanceInsight} object reference to database object
	 * 
	 * precondition: performance_insight != null
	 */
	public PerformanceInsight save(PerformanceInsight performance_insight){
		assert performance_insight != null;
		
		PerformanceInsight performance_insight_record = findByKey(performance_insight.getKey());
		log.warn("performance insight record found :: "+performance_insight_record);
		if(performance_insight_record == null){
			performance_insight_record = performance_insight_repo.save(performance_insight);
		}
		
		return performance_insight_record;
	}
	
	/**
	 * Retrieve performance_insight from database using key
	 * 
	 * @param key key of the performance insight
	 * 
	 * @return {@link PerformanceInsight} record with given key
	 * 
	 * precondition: key != null
	 * precondition: !key.isEmpty()
	 */
	public PerformanceInsight findByKey(String key){
		assert key != null;
		assert !key.isEmpty();
		
		return performance_insight_repo.findByKey(key);
	}

	/**
	 * Adds an audit to a performance insight
	 * 
	 * @param user_id user id
	 * @param domain_url domain url
	 * @param performance_insight_key performance insight key
	 * @param audit_key audit key
	 * 
	 * precondition: user_id != null
	 * precondition: !user_id.isEmpty()
	 * precondition: domain_url != null
	 * precondition: !domain_url.isEmpty()
	 * precondition: performance_insight_key != null
	 * precondition: !performance_insight_key.isEmpty()
	 * precondition: audit_key != null
	 * precondition: !audit_key.isEmpty()
	 */
	public void addAudit(String user_id, String domain_url, String performance_insight_key, String audit_key) {
		assert user_id != null;
		assert !user_id.isEmpty();
		assert domain_url != null;
		assert !domain_url.isEmpty();
		assert performance_insight_key != null;
		assert !performance_insight_key.isEmpty();
		assert audit_key != null;
		assert !audit_key.isEmpty();
		
		//check that audit doesn't already exist for insight
		PageSpeedAudit audit_record = performance_insight_repo.findAuditByKey(user_id, domain_url, performance_insight_key, audit_key);
		log.warn("audit record returned :: " + audit_record);
		if(audit_record == null) {
			log.warn("adding audit record :: "+audit_key);
			log.warn("user id :: "+user_id);
			log.warn("domain url :: " + domain_url);
			log.warn("speed insight key :: " + performance_insight_key);
			log.warn("Audit key :: "+audit_key);
			performance_insight_repo.addAudit(user_id, domain_url, performance_insight_key, audit_key);
		}
	}
}
