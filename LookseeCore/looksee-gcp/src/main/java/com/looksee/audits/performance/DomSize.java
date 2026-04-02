package com.looksee.audits.performance;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.CompositeProperty;


/**
 * Defines detail item for "dom-size"(Document Object Model size) object in the
 * Google PageSpeed API
 */
@Getter
@Setter
@NoArgsConstructor
public class DomSize extends AuditDetail {

	private String statistic;
	private String value;
	
	@CompositeProperty
	private Map<String, String> elementDefinition = new HashMap<>();
	
	/**
	 * Constructs a {@link DomSize} object
	 *
	 * @param statistic the statistic
	 * @param value the value
	 * @param elementDefinition the element definition
	 *
	 * precondition: statistic != null
	 * precondition: value != null
	 * precondition: elementDefinition != null
	 */
	public DomSize(String statistic, String value, Map<String, String> elementDefinition) {
		assert statistic != null : "statistic must not be null";
		assert value != null : "value must not be null";
		assert elementDefinition != null : "elementDefinition must not be null";

		setStatistic(statistic);
		setValue(value);
		setElementDefinition(elementDefinition);
	}

	@Override
	public String generateKey() {
		return "domsize"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(statistic + value + elementDefinition.toString());
	}
}
