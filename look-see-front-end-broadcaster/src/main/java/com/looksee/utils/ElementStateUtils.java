package com.looksee.utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;

import com.looksee.frontEndBroadcaster.models.ColorData;
import com.looksee.frontEndBroadcaster.models.ElementState;


public class ElementStateUtils {
	
	public static List<ElementState> filterElementsWithNegativePositions(List<ElementState> elements) {
		List<ElementState> filtered_elements = new ArrayList<>();

		for(ElementState element : elements){
			if(element.getXLocation() >= 0 && element.getYLocation() >= 0){
				filtered_elements.add(element);
			}
		}

		return filtered_elements;
	}

	/**
	 * Enriches background colors for a list of {@link ElementState elements}
	 * 
	 * @param element_states
	 * @return
	 */
	public static ElementState enrichBackgroundColor(ElementState element) {
		//ENRICHMENT : BACKGROUND COLORS
		try {
			String color_css = element.getRenderedCssValues().get("color");
			if(color_css == null) {
				color_css = "#000000";
			}
			
			ColorData font_color = new ColorData(color_css);
			
			//extract opacity color
			ColorData bkg_color = null;
			if(element.getScreenshotUrl().trim().isEmpty()) {
				bkg_color = new ColorData(element.getRenderedCssValues().get("background-color"));
			}
			else if(element.getScreenshotUrl() != null && !element.getScreenshotUrl().isEmpty()){
				bkg_color = ImageUtils.extractBackgroundColor( new URL(element.getScreenshotUrl()),
																font_color);
			}
			else {
				return element;
			}
			
			
			//Identify background color by getting largest color used in picture
			//ColorData background_color_data = ImageUtils.extractBackgroundColor(new URL(element.getScreenshotUrl()));
			String bg_color = bkg_color.rgb();	
			ColorData background_color = new ColorData(bg_color);
			element.setBackgroundColor(background_color.rgb());
			element.setForegroundColor(font_color.rgb());
			
			double contrast = ColorData.computeContrast(background_color, font_color);
			element.setTextContrast(contrast);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return element;
	}

	/**
	 * Checks if {@link ElementState} is an interactive element. The methods 
	 * defines element interactivity as an element that is either natively interactive in 
	 * HTML such as the tags a, button, select, and input. This method also
	 * checks that the outer html has reference to "click" or onClick.
	 * 
	 * @param element
	 * @return true if the element is interactive, otherwise returns false
	 * 
	 * @pre element != null
	 */
    public static boolean isInteractiveElement(Element element) {
		assert element != null;
		
		String tagName = element.tagName();

		if("a".equals(tagName) 
			|| "button".equals(tagName) 
			|| "select".equals(tagName) 
			|| "input".equals(tagName) )
		{
			return true;
		}

		String outerHtml = element.outerHtml().toLowerCase();
		if(outerHtml.contains("click")
			&& (element.html() != null && !element.html().contains("click"))){
			return true;
		}

		return false;
    }
}
