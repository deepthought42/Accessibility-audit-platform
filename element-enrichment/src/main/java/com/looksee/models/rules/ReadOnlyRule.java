package com.looksee.models.rules;

import com.looksee.pageBuilder.models.Element;

/**
 * Creates a read-only {@link FormRule} on a {@link FormField}  
 *
 */
public class ReadOnlyRule extends Rule {
	public ReadOnlyRule(){
		setValue("");
		setType(RuleType.READ_ONLY);
		setKey(generateKey());
	}
	
	@Override
	public Boolean evaluate(Element elem) {
		//Check if field is read-only
		return elem.getAttributes().containsKey("readonly");
	}
}
