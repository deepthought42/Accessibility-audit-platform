package com.looksee.utils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.looksee.pageBuilder.models.recommend.ColorContrastRecommendation;
import com.looksee.pageBuilder.models.ColorData;
import com.looksee.pageBuilder.models.ColorUsageStat;
import com.looksee.pageBuilder.models.ElementState;

public class ColorUtils {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ColorUtils.class);
	
	/**
	 * Reviews Text color contrast with relation to text size and weight to determine if it meets WCAG 2.1 color contrast guidelines 
	 * @param contrast
	 * @param font_size
	 * @param is_bold
	 * @return
	 */
	public static boolean textContrastMeetsWcag21AAA(double contrast, double font_size, boolean is_bold) {		
		return (font_size >= 18 || (font_size >= 14 && is_bold) && contrast >= 4.5) 
				|| ((font_size < 18 && (font_size >= 14 && !is_bold) || font_size < 14)  && contrast >= 7.0) ;
	}

	/**
	 * Reviews Text color contrast with relation to text size and weight to determine if it meets WCAG 2.1 color contrast guidelines 
	 * @param contrast
	 * @param font_size
	 * @param is_bold
	 * @return
	 */
	public static boolean nonTextContrastMeetsWcag21AAA(double contrast) {		
		return contrast >= 3.0;
	}
	
	public static ColorContrastRecommendation findCompliantFontColor(ColorData font_color, 
			ColorData background_color,
			boolean is_dark_theme, 
			double font_size, 
			boolean is_bold) 
	{
		//if text isn't black then see if we can make it lighter
		double contrast = ColorData.computeContrast(background_color, font_color);
		
		boolean contrast_compliant = ColorUtils.textContrastMeetsWcag21AAA(contrast, font_size, is_bold);
		ColorData color_text = font_color.clone();
		
		while(!contrast_compliant 
				&& (color_text.getRed() > 0 || color_text.getGreen() > 0 || color_text.getBlue() > 0)) 
		{
			int new_red = color_text.getRed() - 1;
			if(new_red < 0) {
				new_red = 0;
			}
			
			int new_green = color_text.getGreen() - 1;
			if(new_green < 0) {
				new_green = 0;
			}
			
			int new_blue = color_text.getBlue() - 1;
			if(new_blue < 0) {
				new_blue = 0;
			}
			color_text = new ColorData(new_red, new_green, new_blue);
			
			contrast = ColorData.computeContrast(background_color, color_text);
			contrast_compliant = ColorUtils.textContrastMeetsWcag21AAA(contrast, font_size, is_bold);
		}
		if(!ColorUtils.textContrastMeetsWcag21AAA(contrast, font_size, is_bold)) {
			return null;
		}
		return new ColorContrastRecommendation(color_text.rgb(), background_color.rgb());
	}
	
	/**
	 * Shifts the shade of the background toward white to find a potential color pair
	 * 
	 * @param font_color
	 * @param background_color
	 * @param is_dark_theme
	 * @param font_size
	 * @param is_bold
	 * 
	 * @pre font_color != null
	 * @pre background_color != null
	 * 
	 * @return {@link ColorContrastRecommendation recommendation}
	 */
	public static ColorContrastRecommendation findCompliantBackgroundColor(ColorData font_color, 
																			ColorData background_color, 
																			boolean is_dark_theme, 
																			double font_size, 
																			boolean is_bold) 
	{
		assert font_color != null;
		assert background_color != null;
		
		double contrast = ColorData.computeContrast(background_color, font_color);
		boolean contrast_compliant = ColorUtils.textContrastMeetsWcag21AAA(contrast, font_size, is_bold);
		
		//if background isn't white then see if we can make it lighter
		ColorData color = background_color.clone();
		while(!contrast_compliant && (color.getRed() < 255 || color.getGreen() < 255 || color.getBlue() < 255)) {
			int new_red = color.getRed() + 1;
			if(new_red > 255) {
				new_red = 255;
			}
			
			int new_green = color.getGreen() + 1;
			if(new_green > 255) {
				new_green = 255;
			}
			
			int new_blue = color.getBlue() + 1;
			if(new_blue > 255) {
				new_blue = 255;
			}
			
			color = new ColorData(new_red, new_green, new_blue);
			contrast = ColorData.computeContrast(color, font_color);
			contrast_compliant = ColorUtils.textContrastMeetsWcag21AAA(contrast, font_size, is_bold);
		}
		
		if(!ColorUtils.textContrastMeetsWcag21AAA(contrast, font_size, is_bold)) {
			return null;
		}
		return new ColorContrastRecommendation(font_color.rgb(), color.rgb());
	}

	/**
	 * Identifies a color for the parent background that meets WCAG 2.1 compliance for color contrast
	 * 
	 * @param element_color
	 * @param background_color
	 * @param is_dark_theme
	 * 
	 * @pre element_color != null
	 * @pre background_color != null
	 * 
	 * @return
	 */
	public static ColorContrastRecommendation findCompliantNonTextBackgroundColor(ColorData element_color,
																				  ColorData background_color, 
																				  boolean is_dark_theme) {
		assert element_color != null;
		assert background_color != null;
		
		double contrast = ColorData.computeContrast(background_color, element_color);
		boolean contrast_compliant = ColorUtils.nonTextContrastMeetsWcag21AAA(contrast);
		
		//if background isn't white then see if we can make it lighter
		ColorData bg_color = background_color.clone();
		
		if(is_dark_theme) {
			while(!contrast_compliant && (bg_color.getRed() > 0 || bg_color.getGreen() > 0 || bg_color.getBlue() > 0)) {
				int new_red = bg_color.getRed() - 1;
				if(new_red < 0) {
					new_red = 0;
				}
				
				int new_green = bg_color.getGreen() - 1;
				if(new_green < 0) {
					new_green = 0;
				}
				
				int new_blue = bg_color.getBlue() - 1;
				if(new_blue < 0) {
					new_blue = 0;
				}
				
				bg_color = new ColorData(new_red, new_green, new_blue);
				contrast = ColorData.computeContrast(bg_color, element_color);
				contrast_compliant = ColorUtils.nonTextContrastMeetsWcag21AAA(contrast);
			}
		}
		else {
			while(!contrast_compliant && (bg_color.getRed() < 255 || bg_color.getGreen() < 255 || bg_color.getBlue() < 255)) {
				int new_red = bg_color.getRed() + 1;
				if(new_red > 255) {
					new_red = 255;
				}
				
				int new_green = bg_color.getGreen() + 1;
				if(new_green > 255) {
					new_green = 255;
				}
				
				int new_blue = bg_color.getBlue() + 1;
				if(new_blue > 255) {
					new_blue = 255;
				}
				
				bg_color = new ColorData(new_red, new_green, new_blue);
				contrast = ColorData.computeContrast(bg_color, element_color);
				contrast_compliant = ColorUtils.nonTextContrastMeetsWcag21AAA(contrast);
			}
		}
		if(!ColorUtils.nonTextContrastMeetsWcag21AAA(contrast)) {
			return null;
		}
		return new ColorContrastRecommendation(element_color.rgb(), bg_color.rgb());
	}

	/**
	 * Generates color recommendations based on contrast using either the background or the border color(if border is present)
	 *   that would make the contrast compliant with WCAG 2.1 AAA standards
	 *   
	 * @param element
	 * @param background_color
	 * @param is_dark_theme
	 * @return
	 */
	public static Set<ColorContrastRecommendation> findCompliantElementColors(ElementState element,
																				ColorData background_color,
																				boolean is_dark_theme) {
		assert element != null;
		assert background_color != null;
		
		Set<ColorContrastRecommendation> recommendations = new HashSet<>();
		ColorData element_color = new ColorData(element.getBackgroundColor());
		double contrast = ColorData.computeContrast(background_color, element_color);
		boolean contrast_compliant = ColorUtils.nonTextContrastMeetsWcag21AAA(contrast);
		
		//calculate for element background
		if(is_dark_theme) {
			//if background isn't white then see if we can make it lighter
			while(!contrast_compliant && (element_color.getRed() < 255 || element_color.getGreen() < 255 || element_color.getBlue() < 255)) {
				int new_red = element_color.getRed() + 1;
				if(new_red > 255) {
					new_red = 255;
				}
				
				int new_green = element_color.getGreen() + 1;
				if(new_green > 255) {
					new_green = 255;
				}
				
				int new_blue = element_color.getBlue() + 1;
				if(new_blue > 255) {
					new_blue = 255;
				}
				
				element_color = new ColorData(new_red, new_green, new_blue);
				contrast = ColorData.computeContrast(element_color, background_color);
				contrast_compliant = ColorUtils.nonTextContrastMeetsWcag21AAA(contrast);
			}
		}
		else {
			//if background isn't white then see if we can make it lighter
			while(!contrast_compliant && (element_color.getRed() > 0 || element_color.getGreen() > 0 || element_color.getBlue() > 0)) {
				int new_red = element_color.getRed() - 1;
				if(new_red < 0) {
					new_red = 0;
				}
				
				int new_green = element_color.getGreen() - 1;
				if(new_green < 0) {
					new_green = 0;
				}
				
				int new_blue = element_color.getBlue() - 1;
				if(new_blue < 0) {
					new_blue = 0;
				}
				
				element_color = new ColorData(new_red, new_green, new_blue);
				contrast = ColorData.computeContrast(element_color, background_color);
				contrast_compliant = ColorUtils.nonTextContrastMeetsWcag21AAA(contrast);
			}
		}
		
		if(ColorUtils.nonTextContrastMeetsWcag21AAA(contrast)) {
			recommendations.add( new ColorContrastRecommendation(element_color.rgb(), background_color.rgb()) );
		}
		
		
		String border_rgb = element.getRenderedCssValues().get("border-color");
		if(border_rgb == null) {
			return recommendations;
		}
		ColorData border_color = new ColorData(border_rgb);
		
		
		//calculate for element border color
		if(is_dark_theme) {
			//if background isn't white then see if we can make it lighter
			while(!contrast_compliant 
					&& (border_color.getRed() < 255 || border_color.getGreen() < 255 || border_color.getBlue() < 255)) {
				int new_red = border_color.getRed() + 1;
				if(new_red > 255) {
					new_red = 255;
				}
				
				int new_green = border_color.getGreen() + 1;
				if(new_green > 255) {
					new_green = 255;
				}
				
				int new_blue = border_color.getBlue() + 1;
				if(new_blue > 255) {
					new_blue = 255;
				}
				
				border_color = new ColorData(new_red, new_green, new_blue);
				contrast = ColorData.computeContrast(border_color, background_color);
				contrast_compliant = ColorUtils.nonTextContrastMeetsWcag21AAA(contrast);
			}
		}
		else {
			//if background isn't white then see if we can make it lighter
			while(!contrast_compliant 
					&& (border_color.getRed() > 0 || border_color.getGreen() > 0 || border_color.getBlue() > 0)) {
				int new_red = border_color.getRed() - 1;
				if(new_red < 0) {
					new_red = 0;
				}
				
				int new_green = border_color.getGreen() - 1;
				if(new_green < 0) {
					new_green = 0;
				}
				
				int new_blue = border_color.getBlue() - 1;
				if(new_blue < 0) {
					new_blue = 0;
				}
				
				border_color = new ColorData(new_red, new_green, new_blue);
				contrast = ColorData.computeContrast(border_color, background_color);
				contrast_compliant = ColorUtils.nonTextContrastMeetsWcag21AAA(contrast);
			}
		}
		
		
		if(ColorUtils.nonTextContrastMeetsWcag21AAA(contrast)) {
			recommendations.add( new ColorContrastRecommendation(border_color.rgb(), background_color.rgb()) );
		}
		
		
		//generate color recommendation for border color
		return recommendations;
	}
	
	/**
	 * 
	 * @param screenshot_url
	 * @param elements
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static List<ColorUsageStat> extractColorsFromScreenshot(URL screenshot_url,
															 		List<ElementState> elements
	) throws MalformedURLException, IOException {		
		//copy page state full page screenshot
		BufferedImage screenshot = ImageIO.read(screenshot_url);
		
		removeImageElements(screenshot, elements);
		
		//return CloudVisionUtils.extractImageProperties(screenshot);
		return ImageUtils.extractImageProperties(screenshot);
	}

	/**
	 * Removes image elements from a screenshot
	 * 
	 * @param screenshot
	 * @param elements
	 */
	public static void removeImageElements(BufferedImage screenshot, List<ElementState> elements) {
		for(ElementState element : elements) {
			if(!element.getName().contentEquals("img")) {
				continue;
			}
			
			for(int x_pixel = element.getXLocation(); x_pixel < (element.getXLocation()+element.getWidth()); x_pixel++) {
				if(x_pixel >= screenshot.getWidth()) {
					break;
				}
				
				if(x_pixel < 0) {
					continue;
				}
				for(int y_pixel = element.getYLocation(); y_pixel < (element.getYLocation()+element.getHeight()); y_pixel++) {
					if(y_pixel >= screenshot.getHeight()) {
						break;
					}
					
					if(y_pixel < 0) {
						continue;
					}
					screenshot.setRGB(x_pixel, y_pixel, new Color(0,0,0).getRGB());
				}	
			}
		}
	}

	/** COLOR CHECKS **/
	
	/**
	 * Checks if the given {@link ColorData} hue is a variant of the color Red.
	 * 
	 * @param color_data
	 * 
	 * @return true if it's red, otherwise false
	 */
	public static boolean isRed(ColorData color_data) {
		double red_low = 321.0;
		double red_hi = 10.0;
		
		return color_data.getHue() >= red_low || color_data.getHue() <= red_hi;
	}

	/**
	 * Checks if the given {@link ColorData} hue is a variant of the color Orange.
	 * 
	 * @param color_data
	 * 
	 * @return true if it's orange, otherwise false
	 */
	public static boolean isOrange(ColorData color_data) {
		double orange_low = 11.0;
		double orange_hi = 40.0;
		
		return color_data.getHue() >= orange_low && color_data.getHue() <= orange_hi;
	}

	/**
	 * Checks if the given {@link ColorData} hue is a variant of the color Gold.
	 * 
	 * @param color_data
	 * 
	 * @return true if it's gold, otherwise false
	 */
	public static boolean isGold(ColorData color_data) {
		double gold_low = 41.0;
		double gold_high = 50.0;
		
		return color_data.getHue() >= gold_low && color_data.getHue() <= gold_high;
	}

	/**
	 * Checks if the given {@link ColorData} hue is a variant of the color Yellow.
	 * 
	 * @param color_data
	 * 
	 * @return true if it's yellow, otherwise false
	 */
	public static boolean isYellow(ColorData color_data) {
		double yellow_low = 51.0;
		double yellow_high = 72.0;
		
		return color_data.getHue() >= yellow_low && color_data.getHue() <= yellow_high;
	}

	/**
	 * Checks if the given {@link ColorData} hue is a variant of the color Green.
	 * 
	 * @param color_data
	 * 
	 * @return true if it's green, otherwise false
	 */
	public static boolean isGreen(ColorData color_data) {
		double green_low = 73.0;
		double green_high = 165.0;
		
		return color_data.getHue() >= green_low && color_data.getHue() <= green_high;
	}

	/**
	 * Checks if the given {@link ColorData} hue is a variant of the color Cyan.
	 * 
	 * @param color_data
	 * 
	 * @return true if it's cyan, otherwise false
	 */
	public static boolean isCyan(ColorData color_data) {
		double cyan_low = 166.0;
		double cyan_high = 195.0;
		
		return color_data.getHue() >= cyan_low && color_data.getHue() <= cyan_high;
	}

	/**
	 * Checks if the given {@link ColorData} hue is a variant of the color Blue.
	 * 
	 * @param color_data
	 * 
	 * @return true if it's blue, otherwise false
	 */
	public static boolean isBlue(ColorData color_data) {
		double blue_low = 196.0;
		double blue_high = 225.0;
		
		return color_data.getHue() >= blue_low && color_data.getHue() <= blue_high;
	}

	/**
	 * Checks if the given {@link ColorData} hue is a variant of the color Violet.
	 * 
	 * @param color_data
	 * 
	 * @return true if it's violet, otherwise false
	 */
	public static boolean isViolet(ColorData color_data) {
		double violet_low = 226.0;
		double violet_high = 250.0;
		
		return color_data.getHue() >= violet_low && color_data.getHue() <= violet_high;
	}

	/**
	 * Checks if the given {@link ColorData} hue is a variant of the color Purple.
	 * 
	 * @param color_data
	 * 
	 * @return true if it's purple, otherwise false
	 */
	public static boolean isPurple(ColorData color_data) {
		double purple_low = 251.0;
		double purple_high = 280.0;
		
		return color_data.getHue() >= purple_low && color_data.getHue() <= purple_high;
	}

	/**
	 * Checks if the given {@link ColorData} hue is a variant of the color Magenta.
	 * 
	 * @param color_data
	 * 
	 * @return true if it's magenta, otherwise false
	 */
	public static boolean isMagenta(ColorData color_data) {
		double magenta_low = 281.0;
		double magenta_high = 320.0;	
		
		return color_data.getHue() >= magenta_low && color_data.getHue() <= magenta_high;
	}

	public static boolean isBlack(ColorData color_data) {
		return color_data.getBrightness() < 15;
	}
	
	public static boolean isWhite(ColorData color_data) {
		return color_data.getSaturation() < 5 && color_data.getBrightness() > 90;
	}
	
	
	
}
