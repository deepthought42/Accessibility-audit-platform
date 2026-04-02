package com.looksee.models.audit.messages;

import com.looksee.models.audit.recommend.Recommendation;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import java.util.Set;
import org.springframework.data.neo4j.core.schema.Node;


/**
 * Details issues for when a page is devoid of a certain styling such as padding,
 * that should be used, because it adds extra white-space to the content.
 *
 * <p>invariant: type is always {@link ObservationType#STYLE_MISSING}</p>
 * <p>invariant: when constructed with parameters, inherits all invariants from {@link UXIssueMessage}</p>
 */
@Node
public class StylingMissingIssueMessage extends UXIssueMessage {
	
	/**
	 * Constructs a new {@link StylingMissingIssueMessage}
	 *
	 * @param description the description of the styling missing issue message
	 * @param recommendation the recommendation of the styling missing issue message
	 * @param priority the priority of the styling missing issue message
	 *
	 * precondition: description != null
	 * precondition: recommendation != null
	 * precondition: priority != null
	 */
	public StylingMissingIssueMessage(
			String description,
			Set<Recommendation> recommendation,
			Priority priority) {
		super();

		assert description != null;
		assert recommendation != null;
		assert priority != null;
		setDescription(description);
		setType(ObservationType.STYLE_MISSING);
		setKey(generateKey());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ObservationType getType() {
		return ObservationType.STYLE_MISSING;
	}

}
