package com.looksee.models.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Defines all {@link ColorScheme color schemes} that exist in the system
 * Complementary - Two colors that are on opposite sides of the color wheel.
 * This combination provides a high contrast and high impact color combination.
 * Together, these colors will appear brighter and more prominent.
*
* Monochromatic - Three shades, tones and tints of one base color. Provides a
* subtle and conservative color combination. This is a versatile color
* combination that is easy to apply to design projects for a harmonious look.
*
* Analogous - Three colors that are side by side on the color wheel. This color
* combination is versatile, but can be overwhelming. To balance an analogous
* color scheme, choose one dominant color, and use the others as accents.
*
* SPLIT_COMPLIMENTARY - Three shades with uneven spacing between them
*
* Triadic - Three colors that are evenly spaced on the color wheel. This provides
* a high contrast color scheme, but less so than the complementary color
* combination — making it more versatile. This combination creates bold,
* vibrant color palettes.

 * Tetradic
Four colors that are evenly spaced on the color wheel. Tetradic color schemes are bold and work best if you let one color be dominant, and use the others as accents. The more colors you have in your palette, the more difficult it is to balance,
 */
public enum ColorScheme {
    /**
     * Represents a complementary color scheme.
     */
	COMPLEMENTARY("complementary"),

    /**
     * Represents a monochromatic color scheme.
     */
	MONOCHROMATIC("monochromatic"),

    /**
     * Represents an analogous color scheme.
     */
	ANALOGOUS("analogous"),

    /**
     * Represents a triadic color scheme.
     */
	TRIADIC("triadic"),

    /**
     * Represents a tetradic color scheme.
     */
	TETRADIC("tetradic"),

    /**
     * Represents a grayscale color scheme.
     */
	UNKNOWN("unknown"),

    /**
     * Represents a grayscale color scheme.
     */
	GRAYSCALE("grayscale"),

    /**
     * Represents a split complementary color scheme.
     */
	SPLIT_COMPLIMENTARY("split_complimentary");
	
	private String shortName;

	ColorScheme (String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the short name of the color scheme.
     * @return the short name of the color scheme
     */
    @Override
    public String toString() {
        return shortName;
    }

    /**
     * Creates a ColorScheme from a string.
     * @param value the string to create the ColorScheme from
     * @return the ColorScheme
     */
    @JsonCreator
    public static ColorScheme create (String value) {
        if(value == null) {
            return UNKNOWN;
        }
        for(ColorScheme v : values()) {
            if(value.equalsIgnoreCase(v.getShortName())) {
                return v;
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns the short name of the color scheme.
     * @return the short name of the color scheme
     */
    public String getShortName() {
        return shortName;
    }
}
