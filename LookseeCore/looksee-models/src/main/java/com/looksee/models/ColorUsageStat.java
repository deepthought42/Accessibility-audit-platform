package com.looksee.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A color usage stat
 */
@NoArgsConstructor
@Getter
@Setter
public class ColorUsageStat {

	private float red;
	private float green;
	private float blue;
	private double pixelPercent;
	private float score;
	
	/**
	 * Constructs a new {@link ColorUsageStat}
	 *
	 * @param red the red value of the color usage stat
	 * @param green the green value of the color usage stat
	 * @param blue the blue value of the color usage stat
	 * @param pixel_percent the pixel percent of the color usage stat
	 * @param score the score of the color usage stat
	 */
	public ColorUsageStat(float red, float green, float blue, double pixel_percent, float score) {
		setRed(red);
		setGreen(green);
		setBlue(blue);
		setPixelPercent(pixel_percent);
		setScore(score);
	}

	/**
	 * Returns the RGB value of the color usage stat
	 *
	 * @return the RGB value of the color usage stat
	 */
	public String getRGB() {
		return ((int)red)+","+((int)green)+","+((int)blue);
	}
}
