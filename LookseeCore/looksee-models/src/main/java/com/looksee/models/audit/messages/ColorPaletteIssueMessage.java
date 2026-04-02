package com.looksee.models.audit.messages;

import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ColorScheme;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * A observation of potential error for a given color palette.
 *
 * <p>invariant: paletteColors is non-null</p>
 * <p>invariant: colors is non-null</p>
 * <p>invariant: colorScheme is a valid {@link ColorScheme} value</p>
 */
@Node
public class ColorPaletteIssueMessage extends UXIssueMessage{
	
	/**
	 * The palette colors of the color palette issue message
	 */
	@Getter
	private List<String> paletteColors = new ArrayList<>();
	
	/**
	 * The colors of the color palette issue message
	 */
	@Getter
	private Set<String> colors = new HashSet<>();

	/**
	 * The color scheme of the color palette issue message
	 */
	private String colorScheme;
	
	/**
	 * Constructs a new {@link ColorPaletteIssueMessage}
	 */
	public ColorPaletteIssueMessage() {
		setPaletteColors(new ArrayList<>());
		setColors(new HashSet<>());
		setColorScheme(ColorScheme.UNKNOWN);
	}
	
	/**
	 * Constructs new object
	 * 
	 * @param priority the priority of the color palette issue message
	 * @param description the description of the color palette issue message
	 * @param recommendation the recommendation of the color palette issue message
	 * @param colors the colors of the color palette issue message
	 * @param paletteColors the palette colors of the color palette issue message
	 * @param category the category of the color palette issue message
	 * @param labels the labels of the color palette issue message
	 * @param wcagCompliance the wcag compliance of the color palette issue message
	 * @param title the title of the color palette issue message
	 * @param pointsEarned the points earned of the color palette issue message
	 * @param maxPoints the max points of the color palette issue message
	 *
	 * precondition: colors != null;
	 * precondition: paletteColors != null;
	 */
	public ColorPaletteIssueMessage(
			Priority priority,
			String description,
			String recommendation,
			Set<String> colors,
			List<String> paletteColors,
			AuditCategory category,
			Set<String> labels,
			String wcagCompliance,
			String title,
			int pointsEarned,
			int maxPoints
	) {
		super(	priority,
				description,
				ObservationType.COLOR_PALETTE,
				category,
				wcagCompliance,
				labels,
				"",
				title,
				pointsEarned,
				maxPoints,
				recommendation);
		
		assert colors != null;
		assert paletteColors != null;
		
		setColorScheme(ColorScheme.UNKNOWN);
		setColors(colors);
		setPaletteColors(paletteColors);
	}

	/**
	 * Sets the colors of the color palette issue message.
	 *
	 * @param colors the colors of the color palette issue message
	 *
	 * precondition: colors != null
	 */
	public void setColors(Set<String> colors) {
		assert colors != null;
		for(String color : colors) {
			this.colors.add(color);
		}
	}

	/**
	 * Returns the color scheme of the color palette issue message
	 *
	 * @return the color scheme of the color palette issue message
	 */
	public ColorScheme getColorScheme() {
		return ColorScheme.create(colorScheme);
	}

	/**
	 * Sets the color scheme of the color palette issue message.
	 *
	 * @param colorScheme the color scheme of the color palette issue message
	 *
	 * precondition: colorScheme != null
	 */
	public void setColorScheme(ColorScheme colorScheme) {
		assert colorScheme != null;
		this.colorScheme = colorScheme.getShortName();
	}

	/**
	 * Sets the palette colors of the color palette issue message.
	 *
	 * @param palette the palette colors of the color palette issue message
	 *
	 * precondition: palette != null
	 */
	public void setPaletteColors(List<String> palette) {
		assert palette != null;
		this.paletteColors.addAll(palette);
	}
}
