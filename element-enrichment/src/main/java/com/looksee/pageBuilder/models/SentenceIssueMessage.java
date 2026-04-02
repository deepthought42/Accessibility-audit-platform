package com.looksee.pageBuilder.models;

import java.util.Set;

import com.looksee.pageBuilder.models.enums.AuditCategory;
import com.looksee.pageBuilder.models.enums.Priority;


/**
 * A observation of potential error for a given {@link Element element} 
 */
public class SentenceIssueMessage extends ElementStateIssueMessage {	
	
	private int wordCount;
	
	public SentenceIssueMessage() {}
	
	public SentenceIssueMessage(
			Priority priority,
			String description,
			String recommendation, 
			ElementState element, 
			AuditCategory category, 
			Set<String> labels, 
			String wcag_compliance,
			String title, 
			int points_awarded,
			int max_points,
			int word_count
	) {
		super(	priority, 
				description,
				recommendation,
				element,
				category,
				labels,
				wcag_compliance,
				title,
				points_awarded,
				max_points);
		
		setWordCount(word_count);
	}

	public int getWordCount() {
		return wordCount;
	}

	public void setWordCount(int word_count) {
		this.wordCount = word_count;
	}

}
