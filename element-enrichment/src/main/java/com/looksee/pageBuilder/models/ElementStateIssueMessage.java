package com.looksee.pageBuilder.models;


import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.looksee.pageBuilder.models.enums.AuditCategory;
import com.looksee.pageBuilder.models.enums.ObservationType;
import com.looksee.pageBuilder.models.enums.Priority;


/**
 * A observation of potential error for a given {@link Element element} 
 */
@Node
public class ElementStateIssueMessage extends UXIssueMessage {	
	private static Logger log = LoggerFactory.getLogger(ElementStateIssueMessage.class);

	@Relationship(type = "FOR")
	private ElementState element;

	public ElementStateIssueMessage() {}
	
	public ElementStateIssueMessage(
			Priority priority,
			String description,
			String recommendation, 
			ElementState element, 
			AuditCategory category, 
			Set<String> labels, 
			String wcag_compliance,
			String title, 
			int points_awarded,
			int max_points
	) {
		super(	priority, 
				description, 
				ObservationType.ELEMENT,
				category,
				wcag_compliance,
				labels,
				"",
				title,
				points_awarded,
				max_points,
				recommendation);
		
		setElement(element);
	}

	public ElementState getElement() {
		return element;
	}

	public void setElement(ElementState element) {
		this.element = element;
	}
	
	@Override
	public void print() {
		log.warn("ux issue key :: "+getKey());
		log.warn("ux issue desc :: "+getDescription());
		log.warn("ux issue points :: "+getPoints());
		log.warn("ux issue max point :: "+getMaxPoints());
		log.warn("ux issue reco :: "+getRecommendation());
		log.warn("ux issue score :: "+getScore());
		log.warn("ux issue title ::"+ getTitle());
		log.warn("ux issue wcag :: "+getWcagCompliance());
		log.warn("ux issue why it matters :: "+getWhyItMatters());
		log.warn("ux issue category :: "+getCategory());
		log.warn("ux issue labels:: "+getLabels());
		log.warn("ux issue priority :: "+getPriority());
		log.warn("ux issue type :: "+getType());
		log.warn("------------------------------------------------------------------------------");
		
	}
}
