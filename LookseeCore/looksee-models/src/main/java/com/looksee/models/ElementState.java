package com.looksee.models;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.looksee.models.enums.ElementClassification;
import com.looksee.utils.HtmlGeneralizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Represents the state of an HTML element on a webpage, including its properties and attributes
 *
 * This class represents the state of an HTML element with the following invariants:
 * - outerHtml must not be null
 * - xpath must not be null
 * - key must not be null
 * - attributes must not be null
 * - renderedCssValues must not be null
 * - classification must not be null
 *
 * The class is responsible for:
 * - Storing element properties like text, location and dimensions
 * - Maintaining element styling and rendering information
 * - Providing element identification via key and xpath
 * - Supporting element classification and type information
 */
@JsonSubTypes({
	@Type(value = ImageElementState.class, name = "ImageElementState"),
})
@Node
@Getter
@Setter
@NoArgsConstructor
public class ElementState extends LookseeObject implements Comparable<ElementState> {
	private static Logger log = LoggerFactory.getLogger(ElementState.class);

	private String classification;
	private String outerHtml;
	private String name;
	private String ownedText;
	private String allText;
	private String cssSelector;
	private String xpath;
	private String screenshotUrl;
	private String backgroundColor;
	private String foregroundColor;
	private int xLocation;
	private int yLocation;
	private int width;
	private int height;
	private double textContrast;
	private double nonTextContrast;
	private boolean imageFlagged;
	private boolean visible;
	
	/**
	 * The rendered css values of the element
	 */
	@CompositeProperty
	private Map<String, String> renderedCssValues = new HashMap<>();
	
	/**
	 * The attributes of the element
	 */
	@CompositeProperty
	private Map<String, String> attributes = new HashMap<>();
	
	/**
	 * Constructs an {@link ElementState} object with the given parameters
	 *
	 * @param owned_text the text owned by the element
	 * @param all_text the text of the element
	 * @param xpath the xpath of the element
	 * @param name the name of the element
	 * @param attributes the attributes of the element
	 * @param css_map the css map of the element
	 * @param screenshot_url the screenshot url of the element
	 * @param x_location the x location of the element
	 * @param y_location the y location of the element
	 * @param width the width of the element
	 * @param height the height of the element
	 * @param classification the classification of the element
	 * @param outer_html the outer html of the element
	 * @param css_selector the css selector of the element
	 * @param font_color the font color of the element
	 * @param background_color the background color of the element
	 * @param image_flagged whether the element is an image
	 *
	 * precondition: xpath != null
	 * precondition: name != null
	 * precondition: screenshot_url != null
	 * precondition: !screenshot_url.isEmpty()
	 * precondition: outer_html != null;
	 * precondition: !outer_html.isEmpty()
	 */
	public ElementState(String owned_text,
						String all_text,
						String xpath,
						String name,
						Map<String, String> attributes,
						Map<String, String> css_map,
						String screenshot_url,
						int x_location,
						int y_location,
						int width,
						int height,
						ElementClassification classification,
						String outer_html,
						String css_selector,
						String font_color,
						String background_color,
						boolean image_flagged){
		assert name != null;
		assert xpath != null;
		assert !xpath.isEmpty();
		assert css_selector != null;
		assert !css_selector.isEmpty();
		assert outer_html != null;
		assert !outer_html.isEmpty();
		
		setName(name);
		setAttributes(attributes);
		setScreenshotUrl(screenshot_url);
		setOwnedText(owned_text);
		setAllText(all_text);
		setRenderedCssValues(css_map);
		setXLocation(x_location);
		setYLocation(y_location);
		setWidth(width);
		setHeight(height);
		setOuterHtml(outer_html);
		setCssSelector(css_selector);
		setClassification(classification);
		setXpath(xpath);
		setForegroundColor(font_color);
		setBackgroundColor(background_color);
		setImageFlagged(image_flagged);
		setKey(generateKey());
	}
	
	/**
	 * Prints the attributes of the element in a prettyish format
	 */
	public void printAttributes(){
		System.out.print("+++++++++++++++++++++++++++++++++++++++");
		for(String attribute : this.attributes.keySet()){
			System.out.print(attribute + " : ");
			System.out.print( attributes.get(attribute) + " ");
		}
		System.out.print("\n+++++++++++++++++++++++++++++++++++++++");
	}
	
	/** GETTERS AND SETTERS  **/
	
	/**
	 * Gets the attribute of the element with the given name
	 *
	 * @param attr_name the name of the attribute
	 * @return the attribute value
	 *
	 * precondition: attr_name != null
	 */
	public String getAttribute(String attr_name){
		assert attr_name != null;
		//get id for element
		for(String tag_attr : this.attributes.keySet()){
			if(tag_attr.equalsIgnoreCase(attr_name)){
				return this.attributes.get(tag_attr);
			}
		}
		
		return null;
	}
	
	/**
	 * Adds an attribute to the element
	 *
	 * @param attribute the name of the attribute
	 * @param values the value of the attribute
	 *
	 * precondition: attribute != null
	 * precondition: values != null
	 */
	public void addAttribute(String attribute, String values) {
		assert attribute != null;
		assert values != null;

		this.attributes.put(attribute, values);
	}

	/**
	 * Generates a key using both path and result in order to guarantee uniqueness
	 *
	 * @return the key
	 */
	public String generateKey() {
		String generalized_html = HtmlGeneralizer.generalizeSrc(getOuterHtml());
		
		return "elementstate"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(generalized_html);
	}

	/**
	 * Checks if {@link ElementState elements} are equal
	 *
	 * @param o the object to compare to
	 * @return true if the elements are equal, false otherwise
	 */
	@Override
	public boolean equals(Object o){
		if(o == null) return false;
		
		if (this == o) return true;
        if (!(o instanceof ElementState)) return false;
        
        ElementState that = (ElementState)o;
		return this.getKey().equals(that.getKey());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(outerHtml, xpath);
	}

	/**
	 * Prints the attributes of the element in a prettyish format
	 */
	public void print() {
		log.warn("element key :: "+getKey());
		log.warn("element desc :: "+getId());
		log.warn("element points :: "+getAllText());
		log.warn("element max point :: "+getBackgroundColor());
		log.warn("element reco :: "+getCssSelector());
		log.warn("element score :: "+getForegroundColor());
		log.warn("element title ::"+ getHeight());
		log.warn("element wcag :: "+getName());
		log.warn("element why it matters :: "+getNonTextContrast());
		log.warn("element category :: "+getOuterHtml());
		log.warn("element labels:: "+getOwnedText());
		log.warn("element priority :: "+getScreenshotUrl());
		log.warn("element recommendations list :: "+getTextContrast());
		log.warn("element type :: "+getWidth());
		log.warn("element :: "+getWidth());
		log.warn("element x_loc :: "+getXLocation());
		log.warn("element y_loc :: "+getYLocation());
		log.warn("element attr :: "+getAttributes());
		//log.warn("element children :: "+getChildElements());
		log.warn("element classification :: "+getClassification());
		log.warn("element created_at :: "+getCreatedAt());


		log.warn("------------------------------------------------------------------------------");
		
	}
	
	/**
	 * Clones the element
	 *
	 * @return the cloned element
	 */
	public ElementState clone() {
		ElementState page_elem = new ElementState();
		page_elem.setAttributes(this.getAttributes());
		page_elem.setRenderedCssValues(this.getRenderedCssValues());
		page_elem.setKey(this.getKey());
		page_elem.setName(this.getName());
		page_elem.setScreenshotUrl(this.getScreenshotUrl());
		page_elem.setOwnedText(this.getOwnedText());
		page_elem.setAllText(this.getAllText());
		page_elem.setYLocation(this.getYLocation());
		page_elem.setXLocation(this.getXLocation());
		page_elem.setWidth(this.getWidth());
		page_elem.setHeight(this.getHeight());
		page_elem.setOuterHtml(this.getOuterHtml());
		
		return page_elem;
	}

	/**
	 * Compares two elements
	 *
	 * @param o the element to compare to
	 * @return the comparison result
	 */
	@Override
	public int compareTo(ElementState o) {
        return this.getKey().compareTo(o.getKey());
	}

	/**
	 * Gets the classification of the element
	 *
	 * @return the classification
	 */
	public ElementClassification getClassification() {
		return ElementClassification.create(classification);
	}

	/**
	 * Sets the classification of the element
	 *
	 * @param classification the classification
	 *
	 * precondition: classification != null
	 */
	public void setClassification(ElementClassification classification) {
		assert classification != null;
		
		this.classification = classification.toString();
	}

	/**
	 * Sets the rendered css values of the element
	 *
	 * @param rendered_css_values the rendered css values
	 *
	 * precondition: rendered_css_values != null
	 */
	public void setRenderedCssValues(Map<String, String> rendered_css_values) {
		assert rendered_css_values != null;
		
		this.renderedCssValues.putAll(rendered_css_values);
	}
}
