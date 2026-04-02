package com.looksee.pageBuilder.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.Node;

import com.looksee.pageBuilder.models.enums.AuditCategory;
import com.looksee.pageBuilder.models.enums.ObservationType;
import com.looksee.pageBuilder.models.enums.Priority;

/**
 * A observation of potential error for a given {@link Element element} 
 */
@Node
public class TypefacesIssue extends UXIssueMessage {	
	private List<String> typefaces = new ArrayList<>();
	
	public TypefacesIssue() {}
	
	public TypefacesIssue(
			List<String> typefaces, 
			String description, 
			String recommendation, 
			Priority priority, 
			AuditCategory category, 
			Set<String> labels, 
			String wcag_compliance, 
			int points_awarded, 
			int max_points,
			String title) {
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
		
		assert typefaces != null;
		assert !typefaces.isEmpty();
		
		setTypefaces(typefaces);
	}

	public List<String> getTypefaces() {
		return typefaces;
	}


	public void setTypefaces(List<String> typefaces) {
		this.typefaces = typefaces;
	}
	
	public boolean addTypefaces(List<String> typefaces) {
		return this.typefaces.addAll(typefaces);
	}
}
