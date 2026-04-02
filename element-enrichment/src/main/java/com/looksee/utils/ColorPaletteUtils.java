package com.looksee.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.looksee.pageBuilder.models.CIEColorSpace;
import com.looksee.pageBuilder.models.ColorData;
import com.looksee.pageBuilder.models.ColorPaletteIssueMessage;
import com.looksee.pageBuilder.models.PaletteColor;
import com.looksee.pageBuilder.models.Score;
import com.looksee.pageBuilder.models.UXIssueMessage;
import com.looksee.pageBuilder.models.enums.AuditCategory;
import com.looksee.pageBuilder.models.enums.ColorScheme;
import com.looksee.pageBuilder.models.enums.Priority;


/**
 * 
 */
public class ColorPaletteUtils {
	private static Logger log = LoggerFactory.getLogger(ColorPaletteUtils.class);

	/**
	 * Scores site color palette based on the color scheme it most resembles
	 * @param palette
	 * @param scheme
	 * @return
	 * 
	 * @pre palette != null
	 * @pre scheme != null
	 */
	public static Score getPaletteScore(List<PaletteColor> palette, ColorScheme scheme) {
		assert palette != null;
		assert scheme != null;
		
		//if palette has exactly 1 color set and that color set has more than 1 color, then monochromatic
		int score = 0;
		int max_points = 3;
				
		if(ColorScheme.GRAYSCALE.equals(scheme)) {
			score = 3;
		}
		//check if monochromatic
		else if(ColorScheme.MONOCHROMATIC.equals(scheme)) {
			score = getMonochromaticScore(palette);
		}
		//check if complimentary
		else if( ColorScheme.COMPLEMENTARY.equals(scheme)) {
			score = getComplementaryScore(palette);
		}
		//analogous and triadic both have 3 colors
		else if(ColorScheme.ANALOGOUS.equals(scheme)) {
			score = 3;
		}
		else if(ColorScheme.SPLIT_COMPLIMENTARY.equals(scheme)) {
			log.debug("Color scheme is SPLIT COMPLIMENTARY!!!");
			score = 3;
		}
		else if(ColorScheme.TRIADIC.equals(scheme)) {
			//check if triadic
			//if hues are nearly equal in differences then return triadic
			log.debug("Color scheme is TRIADIC!!!");
			score = 3;
		}
		else if(ColorScheme.TETRADIC.equals(scheme)) {
			log.debug("Color scheme is Tetradic!!!");

			//check if outer points are equal in distances
			score = 2;
		}
		else {
			//unknown color scheme
			score = 0;
		}
		
		return new Score(score, max_points, new HashSet<>());
	}
	
	/**
	 * Scores site color palette based on the design system color palette and the colors found on the page
	 * 
	 * @param palette {@link List} of colors that define the accepted {@link DesignSystem} palette
	 * @param colors {@link List} of colors that were found on the page
	 * 
	 * @return {@link Score}
	 * 
	 * @pre palette != null
	 * @pre colors != null
	 */
	public static Score getPaletteScore(List<String> palette, List<ColorData> colors) {
		assert palette != null;
		assert colors != null;
		
		//if palette has exactly 1 color set and that color set has more than 1 color, then monochromatic
		int score = 0;
		int max_points = 0;
		
		Map<String, Boolean> non_compliant_colors = ColorPaletteUtils.retrieveNonCompliantColors(palette, colors);
		
		//if all colors match up with a palette color in hue then score is 1/1
		//otherwise score is based on the number of colors that deviate from the design system
		Set<String> labels = new HashSet<>();
		labels.add("brand");
		labels.add("color");
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.AESTHETICS.toString());
		
		
		Set<UXIssueMessage> messages = new HashSet<>();
		if(!non_compliant_colors.isEmpty()) {
			String title = "Page colors don't conform to the brand";
			String description = "Colors were found that aren't in the design system";
			String recommendation = "You shouldn't use colors that aren't part of brand's design system";
			String ada_compliance = "There are no ADA compliance guidelines regarding the website color" + 
									" palette. However, keeping a cohesive color palette allows you to create" + 
									" a webpage that is easy for everyone to read and interact with ";
			
			//for(String color : non_compliant_colors.keySet()) {
			UXIssueMessage palette_issue_message = new ColorPaletteIssueMessage(
																	Priority.HIGH,
																	description,
																	recommendation,
																	non_compliant_colors.keySet(),
																	palette,
																	AuditCategory.AESTHETICS,
																	labels,
																	ada_compliance, 
																	title,
																	0,
																	1);
			
			messages.add(palette_issue_message);
		}
		else {
			String title = "Page colors are on brand!";
			String description = "All colors on the page are part of the design system";
			String recommendation = "";
			String ada_compliance = "";
			
			//for(String color : non_compliant_colors.keySet()) {
			UXIssueMessage palette_success_message = new ColorPaletteIssueMessage(
																	Priority.HIGH,
																	description,
																	recommendation,
																	non_compliant_colors.keySet(),
																	palette,
																	AuditCategory.AESTHETICS,
																	labels,
																	ada_compliance, 
																	title,
																	1,
																	1);
			
			messages.add(palette_success_message);
		}
		return new Score(score, max_points, messages);
	}

	/**
	 * Compares colors to palette and if any colors are within 5 arc degrees of a palette color, then it is considered to
	 * 	conform to the palette.
	 * 
	 * @param palette
	 * @param colors
	 * @return
	 */
	private static Map<String, Boolean> retrieveNonCompliantColors(List<String> palette, List<ColorData> colors) {
		Map<String, Boolean> non_compliant_colors = new HashMap<>();

		for(ColorData color: colors) {
			boolean conforms_to_palette = false;
			for(String palette_color : palette) {
				boolean is_similar_hue = new ColorData(palette_color).isSimilarHue(color);
				//if color is a hue within 5 arc degrees of palette color then it matches
				//otherwise it is not a match within the palette and we should
				if(is_similar_hue) {
					conforms_to_palette = true;
					break;
				}
			}
			if(!conforms_to_palette) {
				//    add it to a list of non matching colors
				non_compliant_colors.put(color.rgb(), Boolean.TRUE);
			}
		}
		return non_compliant_colors;
	}

	/**
	 * // NOTE:: we consider black and white as one color and the shades of gray as shades of 1 extreme meaning that grayscale is 1 color(gray) with many shades.
	 * @param palette
	 * @return
	 * 
	 * @pre palette != null
	 */
	public static ColorScheme getColorScheme(Collection<PaletteColor> palette) {
		assert palette != null;
		
		//if palette has exactly 1 color set and that color set has more than 1 color, then monochromatic
		if(palette.isEmpty()) {
			return ColorScheme.GRAYSCALE;
		}
		//check if monochromatic
		else if(palette.size() == 1 && palette.iterator().next().getTintsShadesTones().size() > 1) {
			log.debug("COLOR IS MONOCHROMATIC!!!!!!!");
			return ColorScheme.MONOCHROMATIC;
		}
		
		//check if complimentary
		else if( palette.size() == 2 ) {
			return ColorScheme.COMPLEMENTARY;
		}
		//analogous and triadic both have 3 colors
		else if(palette.size() == 3) {
			//check if analogous
			//if difference in hue is less than 0.40 for min and max hues then return analogous
			double min_hue = 1.0;
			double max_hue = 0.0;
			for(PaletteColor palette_color : palette) {
				ColorData color = new ColorData(palette_color.getPrimaryColor());
				if(color.getHue() > max_hue) {
					max_hue = color.getHue();
				}
				if(color.getHue() < min_hue) {
					min_hue = color.getHue();
				}
			}
			
			if((max_hue-min_hue) < 0.16) {
				log.debug("Color scheme is ANALOGOUS");
				return ColorScheme.ANALOGOUS;
			}
			else {
				//if all hues are roughly the same distance apart, then TRIADIC
				if(areEquidistantColors(palette)) {
					return ColorScheme.TRIADIC;
				}
				else {
					return ColorScheme.SPLIT_COMPLIMENTARY;
				}
			}
		}
		else if(palette.size() == 4) {
			log.debug("Color scheme is Tetradic!!!");
			//check if hues are equal in differences
			return ColorScheme.TETRADIC;
		}
		else {
			return ColorScheme.UNKNOWN;
		}
	}
	
	/**
	 * TODO Needs testing
	 * Checks if all colors are equidistant on the color wheel
	 * 
	 * @param colors
	 * @return
	 * 
	 * @pre colors != null;
	 */
	private static boolean areEquidistantColors(Collection<PaletteColor> colors) {
		assert colors != null;
		
		List<PaletteColor> color_list = new ArrayList<>(colors);
		List<Double> distances = new ArrayList<>();
		for(int a=0; a < color_list.size()-1; a++) {
			ColorData color_a = new ColorData(color_list.get(a).getPrimaryColor());
			for(int b=a+1; b < color_list.size(); b++) {
				ColorData color_b = new ColorData(color_list.get(b).getPrimaryColor());

				distances.add(
						Math.sqrt( Math.pow((color_b.getHue() - color_a.getHue()), 2) 
								+ Math.pow((color_b.getSaturation() - color_a.getSaturation()), 2) 
								+ Math.pow((color_b.getBrightness() - color_a.getBrightness()), 2)));
			}
		}
		
		for(int a=0; a < distances.size()-1; a++) {
			for(int b=a+1; b < distances.size(); b++) {
				if( Math.abs(distances.get(a) - distances.get(b)) > .05 ){
					return false;
				}
			}	
		}
		
		return true;
	}

	/**
	 * Calculates a score for how well a palette adheres to a complimentary color palette
	 * @param palette
	 * @return
	 * 
	 * @pre palette != null
	 */
	private static int getComplementaryScore(List<PaletteColor> palette) {
		assert palette != null;
		
		//complimentary colors should add up to 255, 255, 255 with a margin of error of 2%
		double total_red = 0;
		double total_green = 0;
		double total_blue = 0;
		
		//if both color sets have only 1
		for(PaletteColor color : palette) {
			ColorData color_data = new ColorData(color.getPrimaryColor());
			total_red+= color_data.getRed();
			total_green += color_data.getGreen();
			total_blue += color_data.getBlue();
		}
		
		int red_score = getComplimentaryColorScore(total_red);
		int blue_score = getComplimentaryColorScore(total_blue);
		int green_score = getComplimentaryColorScore(total_green);

		return (red_score + blue_score + green_score)/ 3;
	}

	private static int getComplimentaryColorScore(double color_val) {
		//test if each color is within a margin of error that is acceptable for complimentary colors
		int score = 0;		
		if(color_val > 250 && color_val < 260) {
			score = 3;
		}
		else if(color_val > 245 && color_val < 265) {
			score = 2;
		}
		else if(color_val > 230 && color_val < 280) {
			score = 1;
		}
		return score;
	}

	/**
	 * Scores palette based on how well it adheres to a monochromatic color set
	 * @param palette
	 * @return
	 * 
	 * @pre palette != null
	 */
	private static int getMonochromaticScore(List<PaletteColor> palette) {
		assert palette != null;
		
		int tint_shade_tone_size = palette.get(0).getTintsShadesTones().size();
		int score = 0;
		if(tint_shade_tone_size == 2) {
			score = 3;
		}
		else if(tint_shade_tone_size <= 1) {
			score = 1;
		}
		else if(tint_shade_tone_size >= 3) {
			score = 2;
		}
		return score;
	}


	/**
	 * Extracts set of {@link PaletteColor colors} that define a palette based on a set of rgb strings
	 * 
	 * @param colors
	 * @return
	 */
	public static List<PaletteColor> extractPalette(List<ColorData> colors) {
		assert colors != null;
		
		//Group colors
		Set<Set<ColorData>> color_sets = groupColors(colors);
		Set<ColorData> primary_colors = identifyPrimaryColors(color_sets);
		List<PaletteColor> palette_colors = new ArrayList<>();
		
		for(ColorData color : primary_colors) {
			PaletteColor palette_color = new PaletteColor(color.rgb(), 
														  color.getUsagePercent(), 
														  new HashMap<>());
			palette_colors.add(palette_color);
		}
		
		return palette_colors;
	}
	
	/**
	 * Extracts set of {@link PaletteColor colors} that define a palette based on a set of rgb strings
	 * 
	 * @param colors
	 * @return
	 */
	public static List<PaletteColor> extractColors(List<ColorData> colors) {
		assert colors != null;
		
		List<PaletteColor> palette_colors = new ArrayList<>();		
		for(ColorData color : colors) {
			PaletteColor palette_color = new PaletteColor(color.rgb(), 
														  color.getUsagePercent(), 
														  new HashMap<>());
			palette_colors.add(palette_color);
		}
		
		return palette_colors;
	}
	/**
	 * Evaluates each color set to identify the primary color. The primary color is defined as the 
	 * second most used color in the set. The most used color in the set is defined as the background color
	 * 
	 * @param color_sets
	 * @return
	 */
	private static Set<ColorData> identifyPrimaryColors(Set<Set<ColorData>> color_sets) {
		assert color_sets != null;
		Set<ColorData> primary_colors = new HashSet<>();
		for(Set<ColorData> color_set : color_sets) {
			List<ColorData> color_list = new ArrayList<>(color_set);
			color_list.sort((o1, o2) -> Double.compare(o2.getUsagePercent(), o1.getUsagePercent()));

			primary_colors.add(color_list.get(0));
		}
		// TODO Auto-generated method stub
		return primary_colors;
	}

	/**
	 * 
	 * 
	 * @param colors
	 * @return
	 */
	@Deprecated
	public static Set<ColorData> identifyColorSet(List<ColorData> colors) {
		log.warn("identifying primary colors ....  "+colors.size());
		ColorData largest_color = null;
		Set<ColorData> primary_colors = new HashSet<>();
		while(!colors.isEmpty()) {
			log.warn("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
			log.warn("colors size before removal :: "+colors.size());
			
			double percent = -5.0;

			for(ColorData color : colors) {
				if(percent < color.getUsagePercent()) {
					percent = color.getUsagePercent();
					largest_color = color;
				}
			}
			if(largest_color == null) {
				continue;
			}
			log.warn("identified largest color  ::   "+largest_color);

			primary_colors.add(largest_color);
			
			Set<ColorData> similar_colors = new HashSet<>();
			//remove any similar colors to primary color
			for(ColorData color : colors) {
				if(!color.equals(largest_color) && isSimilar(color, largest_color)) {
					//log.warn("Similar Color found :: "+color);
					similar_colors.add(color);
				}
			}
			
			log.warn("similar colors found ::    "+similar_colors);
			colors.remove(largest_color);

			//remove similar colors from color set
			for(ColorData color : similar_colors) {
				colors.remove(color);
				//log.warn("removing color :: "+color.rgb());
			}
			log.warn("colors size after removal :: "+colors.size());

			log.warn("primary colors size :: "+primary_colors.size());
		}
		return primary_colors;
	}

	/**
	 * Converts a map representing primary and secondary colors within a palette from using {@link ColorData} to {@link String}
	 * 
	 * @param palette
	 * @return
	 */
	public static Map<String, Set<String>> convertPaletteToStringRepresentation(Map<ColorData, Set<ColorData>> palette) {
		assert palette != null;
		
		Map<String, Set<String>> stringified_map = new HashMap<>();
		for(ColorData primary : palette.keySet()) {
			Set<String> secondary_colors = new HashSet<>();
			for(ColorData secondary : palette.get(primary)) {
				if(secondary == null) {
					continue;
				}
				secondary_colors.add(secondary.rgb());
			}
			stringified_map.put(primary.rgb(), secondary_colors);
		}
		return stringified_map;
	}
	
	/**
	 * 
	 * @param colors
	 * @return
	 */
	public static Set<Set<ColorData>> groupColors(List<ColorData> colors) {
		assert colors != null;
		
		Set<Set<ColorData>> color_sets = new HashSet<>();
		while(!colors.isEmpty()) {
			//initialize set for all similar colors
			Set<ColorData> similar_colors = new HashSet<>();
			
			//identify most frequent color
			ColorData most_frequent_color = colors.parallelStream().max(Comparator.comparing( ColorData::getUsagePercent)).get();
			similar_colors.add(most_frequent_color);
			//identify all similar colors and remove them from the colors set
			for(int idx=0; idx < colors.size(); idx++) {
				ColorData color = colors.get(idx);
				if(color.equals(most_frequent_color)) {
					continue;
				}
				//add similar colors to similar colors set
				if(isSimilarHue(most_frequent_color, color)) {	
					similar_colors.add( color );
				}
			}
			
			//remove similar colors from colors list
			colors.removeAll(similar_colors);
			color_sets.add(similar_colors);
		}
		
		return color_sets;
	}
	
	public static boolean isSimilar(ColorData color1, ColorData color2) {
		assert color1 != null;
		assert color2 != null;
		
		CIEColorSpace cie_color1 = color1.RGBtoXYZ().XYZtoCIE();
		CIEColorSpace cie_color2 = color2.RGBtoXYZ().XYZtoCIE();

		double l_square = Math.pow(Math.abs(cie_color1.l-cie_color2.l), 2);
		double a_square = Math.pow(Math.abs(cie_color1.a-cie_color2.a), 2);
		double b_square = Math.pow(Math.abs(cie_color1.b-cie_color2.b), 2);

		double diff = Math.sqrt( l_square + a_square + b_square);
		return (1/diff) >= 0.1;
		
		/*
			if(isGrayScale(color1) && isGrayScale(color2)) {
				log.warn("both colors are grey  "+color1.rgb() + " : " + color2.rgb());
				return true;
			}
			else if((isGrayScale(color1) && !isGrayScale(color2))
				|| (!isGrayScale(color1) && isGrayScale(color2)))
			{
				log.warn("colors are not similar. one is gray scale and the other isn't");
				return false;
			}
	
			double hue_diff = Math.abs(color1.getHue() - color2.getHue());
			double brightness_diff = Math.abs(color1.getBrightness() - color2.getBrightness());
			double saturation = Math.abs(color1.getSaturation() - color2.getSaturation());
			double luminosity_diff = Math.abs(color1.getLuminosity() - color2.getLuminosity());
	
			double diff = Math.sqrt(hue_diff*hue_diff + luminosity_diff*luminosity_diff + saturation*saturation);
			log.warn("diff :: "+ diff);
			return diff <= 1.0;
		*/

	}

	/**
	 *	Checks if 2 colors are within 5 degrees
	 * 
	 * @param color1
	 * @param color2
	 * 
	 * @return true if the difference between the 2 hues is less 5 degrees, otherwise false
	 */
	public static boolean isSimilarHue(ColorData color1, ColorData color2) {
		assert color1 != null;
		assert color2 != null;
		
		if(isGrayScale(color1) && isGrayScale(color2)) {
			//log.warn("both colors are grey  "+color1.rgb() + " : " + color2.rgb());
			//log.warn("color luminosities ::   "+color1.getLuminosity() + "  :  "+color2.getLuminosity());
			return true;
		}
		else if((isGrayScale(color1) && !isGrayScale(color2))
			|| (!isGrayScale(color1) && isGrayScale(color2)))
		{
			//log.warn("colors are not similar. one is gray scale and the other isn't");
			return false;
		}

		double hue_diff = Math.abs(color1.getHue() - color2.getHue());

		return hue_diff <= 10;
	}

	
	public static boolean isGrayScale(ColorData color) {
		return ((color.getSaturation() < 15 && color.getBrightness() > 25)
				|| (color.getBrightness() < 25));
	}

	public static int getMax(ColorData color) {
		assert color != null;
		
		if(color.getRed() >= color.getBlue()
				&& color.getRed() >= color.getGreen()) {
			return color.getRed();
		}
		else if(color.getBlue() >= color.getRed()
				&& color.getBlue() >= color.getGreen()) {
			return color.getBlue();
		}
		
		return color.getGreen();
	}
	
	public static int getMin(ColorData color) {
		if(color.getRed() <= color.getBlue()
				&& color.getRed() <= color.getGreen()) {
			return color.getRed();
		}
		else if(color.getBlue() <= color.getRed()
				&& color.getBlue() <= color.getGreen()) {
			return color.getBlue();
		}
		
		return color.getGreen();
	}
}
