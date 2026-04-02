package com.looksee.models.designsystem;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contains data for individual palette primary colors and the shades, tints, and tones associated with them
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class PaletteColor {

	private String primaryColor;
	private double primaryColorPercent;
	
	private Map<String, String> tintsShadesTones = new HashMap<>();
	
	/**
	 * Constructor for PaletteColor
	 * @param primary_color the primary color
	 * @param primary_color_percent the primary color percent
	 * @param tints_shades_tones the tints, shades, and tones
	 */
	public PaletteColor(String primary_color,
						double primary_color_percent,
						Map<String, String> tints_shades_tones) {
		setPrimaryColor(primary_color.trim());
		setPrimaryColorPercent(primary_color_percent);
		addTintsShadesTones(tints_shades_tones);
	}

	/**
	 * Gets the tints, shades, and tones
	 * @return the tints, shades, and tones
	 */
	public Map<String, String> getTintsShadesTones() {
		return tintsShadesTones;
	}

	/**
	 * Adds tints, shades, and tones
	 * @param tints_shades_tones the tints, shades, and tones
	 */
	public void addTintsShadesTones(Map<String, String> tints_shades_tones) {
		this.tintsShadesTones.putAll(tints_shades_tones);
	}
}
