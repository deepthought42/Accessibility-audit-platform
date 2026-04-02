package com.looksee.pageBuilder.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.Node;

import com.looksee.pageBuilder.models.enums.AuditCategory;
import com.looksee.pageBuilder.models.enums.ColorScheme;
import com.looksee.pageBuilder.models.enums.ObservationType;
import com.looksee.pageBuilder.models.enums.Priority;

/**
 * A observation of potential error for a given color palette 
 */
@Node
public class ColorPaletteIssueMessage extends UXIssueMessage{
	
	private List<String> palette_colors = new ArrayList<>();
	
	private Set<String> colors = new HashSet<>();
	private String color_scheme;
	
	public ColorPaletteIssueMessage() {
		setPaletteColors(new ArrayList<>());
		setColors(new HashSet<>());
		setColorScheme(ColorScheme.UNKNOWN);
	}
	
	/**
	 * Constructs new object
	 * 
	 * @param priority
	 * @param description TODO
	 * @param recommendation
	 * @param colors
	 * @param palette_colors
	 * @param category TODO
	 * @param labels TODO
	 * @param wcag_compliance TODO
	 * @param title TODO
	 * @param points_earned TODO
	 * @param max_points TODO
	 * 
	 * @pre colors != null;
	 * @pre palette_colors != null;
	 */
	public ColorPaletteIssueMessage(
			Priority priority, 
			String description, 
			String recommendation, 
			Set<String> colors, 
			List<String> palette_colors, 
			AuditCategory category, 
			Set<String> labels, 
			String wcag_compliance, 
			String title, 
			int points_earned, 
			int max_points
	) {		
		super(	priority, 
				description, 
				ObservationType.COLOR_PALETTE,
				category,
				wcag_compliance,
				labels,
				"",
				title,
				points_earned,
				max_points,
				recommendation);
		
		assert colors != null;
		assert palette_colors != null;
		
		setColorScheme(ColorScheme.UNKNOWN);
		setColors(colors);
		setPaletteColors(palette_colors);
	}

	public Set<String> getColors() {
		return colors;
	}

	public void setColors(Set<String> colors) {
		for(String color : colors) {
			this.colors.add(color);
		}
	}

	public ColorScheme getColorScheme() {
		return ColorScheme.create(color_scheme);
	}

	public void setColorScheme(ColorScheme color_scheme) {
		this.color_scheme = color_scheme.getShortName();
	}

	public List<String> getPaletteColors() {
		return palette_colors;
	}

	public void setPaletteColors(List<String> palette) {
		this.palette_colors.addAll(palette);
	}
}
