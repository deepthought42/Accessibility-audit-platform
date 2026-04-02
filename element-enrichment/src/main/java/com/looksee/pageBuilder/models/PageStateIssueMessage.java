package com.looksee.pageBuilder.models;

import java.util.Set;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.looksee.pageBuilder.models.enums.AuditCategory;
import com.looksee.pageBuilder.models.enums.ObservationType;
import com.looksee.pageBuilder.models.enums.Priority;

/**
 * A observation of potential error for a given {@link Element element} 
 */
@Node
public class PageStateIssueMessage extends UXIssueMessage {

	@Relationship(type = "FOR")
	private PageState page_state;
	
	public PageStateIssueMessage() {}
	
	public PageStateIssueMessage(
				PageState page, 
				String description,
				String recommendation, 
				Priority priority, 
				AuditCategory category, 
				Set<String> labels,
				String wcag_compliance, 
				String title, 
				int points_awarded, 
				int max_points
	) {
		super(	priority, 
				description, 
				ObservationType.PAGE_STATE,
				category,
				wcag_compliance,
				labels,
				"",
				title,
				points_awarded,
				max_points,
				recommendation);
		
		setPage(page);
	}

	public PageState getElements() {
		return page_state;
	}


	public void setPage(PageState page_state) {
		this.page_state = page_state;
	}
}
