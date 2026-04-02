package com.looksee.utils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;

import com.looksee.pageBuilder.models.ColorData;
import com.looksee.pageBuilder.models.ElementState;


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

	public static List<ElementState> filterNotVisibleInViewport(int x_offset, int y_offset, List<ElementState> elements, Dimension viewport_size) {
		List<ElementState> filtered_elements = new ArrayList<>();

		for(ElementState element : elements){
			if(isElementVisibleInPane( x_offset, y_offset, element, viewport_size)){
				filtered_elements.add(element);
			}
		}

		return filtered_elements;
	}

	/**
	 * Filters out html, body, script and link tags
	 *
	 * @param web_elements
	 * @return
	 */
	public static List<WebElement> filterStructureTags(List<WebElement> web_elements) {
		List<WebElement> elements = new ArrayList<>();
		for(WebElement element : web_elements){
			if(element.getTagName().equals("html") || element.getTagName().equals("body")
					|| element.getTagName().equals("link") || element.getTagName().equals("script")
					|| element.getTagName().equals("title") || element.getTagName().equals("meta")
					|| element.getTagName().equals("head")){
				continue;
			}
			elements.add(element);
		}
		return elements;
	}
	
	
	public static boolean isElementVisibleInPane(int x_offset, int y_offset, ElementState elem, Dimension viewport_size){
		int x = elem.getXLocation();
		int y = elem.getYLocation();

		int height = elem.getHeight();
		int width = elem.getWidth();

		return x >= x_offset && y >= y_offset && (x+width) <= (viewport_size.getWidth()+x_offset)
				&& (y+height) <= (viewport_size.getHeight()+y_offset);
	}

	
	public static boolean isHeader(String tag_name) {
		return "h1".equalsIgnoreCase(tag_name) 
				|| "h2".equalsIgnoreCase(tag_name)
				|| "h3".equalsIgnoreCase(tag_name)
				|| "h4".equalsIgnoreCase(tag_name)
				|| "h5".equalsIgnoreCase(tag_name)
				|| "h6".equalsIgnoreCase(tag_name);
	}


	/**
	 * Checks if outer html fragment owns text. An element is defined as owning text if 
	 *   if it contains text immediately within the element. If an element has only
	 *   child elements and no text then it does not own text
	 *   
	 * @param element_state {@link ElementState element} to be evaluated for text ownership
	 * 
	 * @return 1 if element is text owner, otherwise 0
	 * 
	 * @pre element_state != null;
	 */
	public static boolean isTextContainer(ElementState element_state) {
		assert element_state != null;
		
		Document doc = Jsoup.parseBodyFragment(element_state.getOuterHtml());
		Element body = doc.body();
		return !body.ownText().isEmpty();
	}
	
	/**
	 * Checks if outer html fragment owns text. An element is defined as owning text if 
	 *   if it contains text immediately within the element. If an element has only
	 *   child elements and no text then it does not own text
	 *   
	 * @param element_state {@link WebElement element} to be evaluated for text ownership
	 * 
	 * @return 1 if element is text owner, otherwise 0
	 * 
	 * @pre element_state != null;
	 */
	public static boolean isTextContainer(WebElement element) {
		assert element != null;
		
		Document doc = Jsoup.parseBodyFragment(element.getAttribute("outerHTML"));
		Element body = doc.body();
		return !body.ownText().isEmpty();
	}

	public static boolean isList(String tag_name) {
		return "ul".equalsIgnoreCase(tag_name) 
				|| "ol".equalsIgnoreCase(tag_name)
				|| "li".equalsIgnoreCase(tag_name);
	}

	public static Stream<ElementState> enrichBackgroundColor(List<ElementState> element_states) {
		//ENRICHMENT : BACKGROUND COLORS
		return element_states.parallelStream()
										.filter(element -> element != null)
										.map(element -> {
				try {
					ColorData font_color = new ColorData(element.getRenderedCssValues().get("color"));				
					//extract opacity color
					ColorData bkg_color = null;
					if(element.getScreenshotUrl().trim().isEmpty()) {
					bkg_color = new ColorData(element.getRenderedCssValues().get("background-color"));
					}
					else {
					//log.warn("extracting background color");
					bkg_color = ImageUtils.extractBackgroundColor( new URL(element.getScreenshotUrl()),
												   font_color);
					
					//log.warn("done extracting background color");
					}
					String bg_color = bkg_color.rgb();	
					
					//Identify background color by getting largest color used in picture
					//ColorData background_color_data = ImageUtils.extractBackgroundColor(new URL(element.getScreenshotUrl()));
					ColorData background_color = new ColorData(bg_color);
					element.setBackgroundColor(background_color.rgb());
					element.setForegroundColor(font_color.rgb());
					
					double contrast = ColorData.computeContrast(background_color, font_color);
					element.setTextContrast(contrast);
					return element;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			return element;
		});
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
			else {
				bkg_color = ImageUtils.extractBackgroundColor( new URL(element.getScreenshotUrl()),
																font_color);
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
}
