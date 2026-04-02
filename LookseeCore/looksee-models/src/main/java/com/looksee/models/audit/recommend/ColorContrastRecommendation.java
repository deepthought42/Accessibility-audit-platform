package com.looksee.models.audit.recommend;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A ColorContrastRecommendation is a recommendation to improve the color contrast of a page.
 *
 * <p>invariant: color1Rgb is non-null and non-empty when set via constructor</p>
 * <p>invariant: color2Rgb is non-null and non-empty when set via constructor</p>
 */
@NoArgsConstructor
@Getter
@Setter
public class ColorContrastRecommendation extends Recommendation {

	private String color1Rgb;
	private String color2Rgb;
	
	/**
	 * Creates a new ColorContrastRecommendation
	 * @param color1Rgb the first color in RGB format
	 * @param color2Rgb the second color in RGB format
	 *
	 * precondition: color1Rgb != null
	 * precondition: !color1Rgb.isEmpty()
	 * precondition: color2Rgb != null
	 * precondition: !color2Rgb.isEmpty()
	 */
	public ColorContrastRecommendation(String color1Rgb, String color2Rgb) {
		assert color1Rgb != null;
		assert !color1Rgb.isEmpty();
		assert color2Rgb != null;
		assert !color2Rgb.isEmpty();
		setColor1Rgb(color1Rgb);
		setColor2Rgb(color2Rgb);
	}
}
