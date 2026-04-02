package com.looksee.pageBuilder.models;

import java.util.Set;

/**
 * Represents score as a combination of a score achieved and max possible score. This object also contains a set of
 * {@link UXIssueMessage issues} that were experienced while generating score
 */
public class Score {

	private int pointsAchieved;
	private int maxPossiblePoints;
	private Set<UXIssueMessage> messages;
	
	public Score(int points, int max_points, Set<UXIssueMessage> issue_messages) {
		setPointsAchieved(points);
		setMaxPossiblePoints(max_points);
		setIssueMessages(issue_messages);
	}
	
	public int getPointsAchieved() {
		return pointsAchieved;
	}
	public void setPointsAchieved(int points_achieved) {
		this.pointsAchieved = points_achieved;
	}
	public int getMaxPossiblePoints() {
		return maxPossiblePoints;
	}
	public void setMaxPossiblePoints(int max_possible_points) {
		this.maxPossiblePoints = max_possible_points;
	}

	public Set<UXIssueMessage> getIssueMessages() {
		return messages;
	}

	public void setIssueMessages(Set<UXIssueMessage> messages) {
		this.messages = messages;
	}
}
