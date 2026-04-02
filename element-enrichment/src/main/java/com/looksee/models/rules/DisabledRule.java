package com.looksee.models.rules;

import org.slf4j.LoggerFactory;

import com.looksee.pageBuilder.models.Element;

import org.slf4j.Logger;

public class DisabledRule extends Rule{
	private static Logger log = LoggerFactory.getLogger(DisabledRule.class);
	
	public DisabledRule() {
		setType(RuleType.DISABLED);
		setValue("");
		setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(Element elem) {
		/* 
		 * Also check for 
		 * 
		 * display: none;
		 * visibility: hidden;
		 * 
		 */
	
		for(String attribute: elem.getAttributes().keySet()){
			if("disabled".contentEquals(attribute)){
				log.info("!DISABLED RULE TYPE....TODO : THIS FEATURE NEEDS A PROPER IMPLEMENTATION!!!");
				return  elem.getAttributes().get(attribute).length() == 0;
			}
		}
		return null;
	}
}
