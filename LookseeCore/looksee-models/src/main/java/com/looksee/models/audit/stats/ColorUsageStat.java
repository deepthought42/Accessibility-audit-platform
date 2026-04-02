package com.looksee.models.audit.stats;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a color usage stat
 */
@Getter
@Setter
@NoArgsConstructor
public class ColorUsageStat {

	private float red;
	private float green;
	private float blue;
	private double pixelPercent;
	private float score;
	
	/**
	 * Constructs a {@link ColorUsageStat}
	 * 
	 * @param red the red value
	 * @param green the green value
	 * @param blue the blue value
	 * @param pixelPercent the pixel percent
	 * @param score the score
	 */
	public ColorUsageStat(float red, float green, float blue, double pixelPercent, float score) {
		setRed(red);
		setGreen(green);
		setBlue(blue);
		setPixelPercent(pixelPercent);
		setScore(score);
	}

	/**
	 * Gets the RGB value of the color usage stat
	 * 
	 * @return the RGB value of the color usage stat
	 */
	public String getRGB() {
		return ((int)red)+","+((int)green)+","+((int)blue);
	}
}
