package com.looksee.audits.performance;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Describes the cumulative time used for different resource groups to load.
 */
@Getter
@Setter
@NoArgsConstructor
public class GroupWorkBreakdown extends AuditDetail {

	private String group;
	private String groupLabel;
	private Double duration;
	
	/**
	 * Constructor
	 *
	 * @param group the group
	 * @param duration the duration
	 * @param groupLabel the group label
	 *
	 * precondition: group != null
	 * precondition: groupLabel != null
	 */
	public GroupWorkBreakdown(String group, double duration, String groupLabel ) {
		assert group != null : "group must not be null";
		assert groupLabel != null : "groupLabel must not be null";

		setGroup(group);
		setGroupLabel(groupLabel);
		setDuration(duration);
	}

	@Override
	public String generateKey() {
		return "groupworkbreakdown"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(group + groupLabel + duration.toString());
	}
}
