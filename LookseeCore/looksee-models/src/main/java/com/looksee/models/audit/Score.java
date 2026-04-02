package com.looksee.models.audit;

import java.util.Set;

import com.looksee.models.audit.messages.UXIssueMessage;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents score as a combination of a score achieved and max possible score. This object also contains a set of
 * {@link UXIssueMessage issues} that were experienced while generating score.
 *
 * <p><b>Invariants:</b>
 * <ul>
 *   <li>pointsAchieved is non-negative and does not exceed maxPossiblePoints.</li>
 *   <li>maxPossiblePoints is non-negative.</li>
 *   <li>issueMessages is never null when constructed via the parameterized constructor.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class Score {

	private int pointsAchieved;
	private int maxPossiblePoints;
	private Set<UXIssueMessage> issueMessages;
	
	/**
	 * Constructor for {@link Score}
	 * @param pointsAchieved the points achieved
	 * @param maxPossiblePoints the max possible points
	 * @param issueMessages the issues that were experienced while generating score
	 *
	 * precondition: issueMessages != null
	 */
	public Score(int pointsAchieved, int maxPossiblePoints, Set<UXIssueMessage> issueMessages) {
		assert issueMessages != null;
		setPointsAchieved(pointsAchieved);
		setMaxPossiblePoints(maxPossiblePoints);
		setIssueMessages(issueMessages);
	}
}
