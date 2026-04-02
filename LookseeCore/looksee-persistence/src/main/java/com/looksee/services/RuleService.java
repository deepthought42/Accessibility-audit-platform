package com.looksee.services;

import com.looksee.exceptions.RuleValueRequiredException;
import com.looksee.models.repository.RuleRepository;
import com.looksee.models.rules.Rule;
import com.looksee.models.rules.RuleFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for {@link Rule} objects
 */
@Service
public class RuleService {
	
	@Autowired
	private RuleRepository rule_repo;

	/**
	 * Saves a rule
	 * @param rule the rule to save
	 * @return the saved rule
	 *
	 * precondition: rule != null
	 */
	public Rule save(Rule rule){
		assert rule != null;
		
		Rule rule_record = rule_repo.findByTypeAndValue(rule.getType().toString(), rule.getValue());
		if(rule_record == null) {
			return rule_repo.save(rule);
		}
		return rule_record;
	}

	/**
	 * Finds a rule
	 * @param rule_type the type of the rule
	 * @param value the value of the rule
	 * @return the rule
	 * @throws RuleValueRequiredException if the rule value is required
	 *
	 * precondition: rule_type != null
	 * precondition: !rule_type.isEmpty()
	 * precondition: value != null
	 * precondition: !value.isEmpty()
	 */
	public Rule findRule(String rule_type, String value) throws RuleValueRequiredException {
		assert rule_type != null;
		assert !rule_type.isEmpty();
		assert value != null;
		assert !value.isEmpty();

		Rule rule = rule_repo.findByTypeAndValue(rule_type, value);
		if(rule == null) {
			rule = RuleFactory.build(rule_type, value);
		}
		return rule;
	}
}
