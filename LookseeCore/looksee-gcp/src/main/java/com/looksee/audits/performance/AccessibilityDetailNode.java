package com.looksee.audits.performance;

import com.looksee.models.ElementState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Defines an accessibility audit detail record.
 */
@Getter
@Setter
@NoArgsConstructor
public class AccessibilityDetailNode extends AuditDetail {
	
	///THE FOLLOWING ARE THE NEW FIELDS
	private String[] requiredChangeMessages;
	private String[] optionalChangeMessages;
	
	@Relationship(type = "BELONGS_TO")
	private ElementState element;

	/**
	 * Constructs an {@link AccessibilityDetailNode}
	 *
	 * @param requiredMessages the required change messages
	 * @param optionalMessages the optional change messages
	 * @param element the element that the accessibility detail node belongs to
	 *
	 * precondition: requiredMessages != null
	 * precondition: optionalMessages != null
	 * precondition: element != null
	 */
	public AccessibilityDetailNode(
			String[] requiredMessages,
			String[] optionalMessages,
			ElementState element
	) {
		assert requiredMessages != null : "requiredMessages must not be null";
		assert optionalMessages != null : "optionalMessages must not be null";
		assert element != null : "element must not be null";

		setRequiredChangeMessages(requiredMessages);
		setOptionalChangeMessages(optionalMessages);
		setElement(element);
	}

	@Override
	public String generateKey() {
		return "accessibilitydetailnode"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(element.getKey());
	}
}
