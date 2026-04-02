package com.looksee.models.rules;

import com.looksee.models.Element;

/**
 * Creates a read-only {@link Rule} on a {@link Element}
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>The value is always ""</li>
 *   <li>The type is always {@link RuleType#READ_ONLY}</li>
 * </ul>
 */
public class ReadOnlyRule extends Rule {
	/**
	 * Constructs a new {@link ReadOnlyRule} rule
	 */
	public ReadOnlyRule(){
		setValue("");
		setType(RuleType.READ_ONLY);
		setKey(generateKey());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Boolean evaluate(Element elem) {
		assert elem != null;
		
		//Check if field is read-only
		return elem.getAttributes().containsKey("readonly");
	}
}
