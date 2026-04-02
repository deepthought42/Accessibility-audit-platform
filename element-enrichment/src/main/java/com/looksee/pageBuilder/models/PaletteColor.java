package com.looksee.pageBuilder.models;

import java.util.HashMap;
import java.util.Map;


/**
 * Contains data for individual palette primary colors and the shades, tints, and tones associated with them
 *
 */
public class PaletteColor {

	private String primaryColor;
	private double primaryColorPercent;
	
	private Map<String, String> tintsShadesTones = new HashMap<>();
	
	public PaletteColor() {}
	
	public PaletteColor(String primary_color, double primary_color_percent, Map<String, String> tints_shades_tones) {
		setPrimaryColor(primary_color.trim());
		setPrimaryColorPercent(primary_color_percent);
		addTintsShadesTones(tints_shades_tones);
	}

	public String getPrimaryColor() {
		return primaryColor;
	}

	private void setPrimaryColor(String primary_color) {
		this.primaryColor = primary_color;
	}

	public double getPrimaryColorPercent() {
		return primaryColorPercent;
	}

	private void setPrimaryColorPercent(double primary_color_percent) {
		this.primaryColorPercent = primary_color_percent;
	}

	public Map<String, String> getTintsShadesTones() {
		return tintsShadesTones;
	}

	public void addTintsShadesTones(Map<String, String> tints_shades_tones) {
		this.tintsShadesTones.putAll(tints_shades_tones);
	}
}
