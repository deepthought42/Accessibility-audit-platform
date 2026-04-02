package com.looksee.journeyErrors.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Node;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.looksee.journeyErrors.models.enums.ElementClassification;


/**
 * Contains all the pertinent information for an element on a page. A ElementState
 *  may be a Parent and/or child of another ElementState. This heirarchy is not
 *  maintained by ElementState though. 
 */
@JsonTypeInfo(
	  use = JsonTypeInfo.Id.NAME, 
	  include = JsonTypeInfo.As.PROPERTY, 
	  property = "type")
@JsonSubTypes({ 
  @Type(value = ImageElementState.class, name = "ImageElementState"), 
})
@Node
public class ElementState extends LookseeObject implements Comparable<ElementState> {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ElementState.class);

	private String name;
	private String ownedText;
	private String allText;
	private String cssSelector;
	private String outerHtml;
	private String xpath;
	private String classification;
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
	
	@CompositeProperty
	private Map<String, String> renderedCssValues = new HashMap<>();
	
	@CompositeProperty
	private Map<String, String> attributes = new HashMap<>();
	
	/*
	@Relationship(type = "HAS_CHILD", direction = Direction.OUTGOING)
	private List<ElementState> childElements = new ArrayList<>();
*/
	public ElementState(){
		super();
	}
	
	/**
	 * 
	 * @param all_text TODO
	 * @param xpath
	 * @param name
	 * @param attributes
	 * @param css_map
	 * @param outer_html TODO
	 * @param css_selector TODO
	 * @param font_color TODO
	 * @param background_color TODO
	 * @param image_flagged TODO
	 * @param text
	 * @pre xpath != null
	 * @pre name != null
	 * @pre screenshot_url != null
	 * @pre !screenshot_url.isEmpty()
	 * @pre outer_html != null;
	 * @pre !outer_html.isEmpty()
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
						boolean is_visible, 
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
		setVisible(is_visible);
		setForegroundColor(font_color);
		setBackgroundColor(background_color);
		setImageFlagged(image_flagged);
		setKey(generateKey());
	}
	
	/**
	 * Print Attributes for this element in a prettyish format
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
		
	public String getName() {
		return name;
	}
	
	public void setName(String tagName) {
		this.name = tagName;
	}
	
	public String getOwnedText() {
		return ownedText;
	}
	
	public void setOwnedText(String text) {
		this.ownedText = text;
	}
	
	public String getAllText() {
		return allText;
	}
	
	public void setAllText(String text) {
		this.allText = text;
	}

	public void setAttributes(Map<String, String> attribute_persist_list) {
		this.attributes = attribute_persist_list;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	public String getAttribute(String attr_name){
		//get id for element
		for(String tag_attr : this.attributes.keySet()){
			if(tag_attr.equalsIgnoreCase(attr_name)){
				return this.attributes.get(tag_attr);
			}
		}
		
		return null;
	}
	

	public void addAttribute(String attribute, String values) {
		this.attributes.put(attribute, values);
	}
	
	public String getScreenshotUrl() {
		return this.screenshotUrl;
	}

	public void setScreenshotUrl(String screenshot_url) {
		this.screenshotUrl = screenshot_url;
	}

	/**
	 * Generates a key using the outer html. Keys are not unique across entire dataset. Instead, ElementState keys 
	 * are only unique per domain.
	 * 
	 * @return
	 */
	public String generateKey() {
		return "elementstate"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(getOuterHtml());

		/*
		String key = "";
		List<String> properties = new ArrayList<>(getRenderedCssValues().keySet());
		Collections.sort(properties);
		for(String style : properties) {
			key += getRenderedCssValues().get(style);
		}
		
		return "elementstate"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(key)+org.apache.commons.codec.digest.DigestUtils.sha256Hex(getOuterHtml());
		*/
	}

	/**
	 * Checks if {@link ElementState elements} are equal
	 * 
	 * @param elem
	 * @return whether or not elements are equal
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

	public int getXLocation() {
		return xLocation;
	}

	public void setXLocation(int x_location) {
		this.xLocation = x_location;
	}

	public int getYLocation() {
		return yLocation;
	}

	public void setYLocation(int y_location) {
		this.yLocation = y_location;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public int compareTo(ElementState o) {
        return this.getKey().compareTo(o.getKey());
	}

	public String getCssSelector() {
		return cssSelector;
	}

	public void setCssSelector(String css_selector) {
		this.cssSelector = css_selector;
	}

	public void setOuterHtml(String outer_html) {
		this.outerHtml = outer_html;
	}

	public String getOuterHtml() {
		return outerHtml;
	}

	public ElementClassification getClassification() {
		return ElementClassification.create(classification);
	}

	public void setClassification(ElementClassification classification) {
		this.classification = classification.toString();
	}
	
	/*
	public List<ElementState> getChildElements() {
		return childElements;
	}

	public void setChildElements(List<ElementState> child_elements) {
		this.childElements = child_elements;
	}
	
	public void addChildElement(ElementState child_element) {
		this.childElements.add(child_element);
	}
*/
	public Map<String, String> getRenderedCssValues() {
		return renderedCssValues;
	}

	public void setRenderedCssValues(Map<String, String> rendered_css_values) {
		this.renderedCssValues.putAll(rendered_css_values);
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public String getXpath() {
		return xpath;
	}

	public void setXpath(String xpath) {
		this.xpath = xpath;
	}

	public double getTextContrast() {
		return textContrast;
	}

	public void setTextContrast(double text_contrast) {
		this.textContrast = text_contrast;
	}

	public double getNonTextContrast() {
		return nonTextContrast;
	}

	public void setNonTextContrast(double non_text_contrast) {
		this.nonTextContrast = non_text_contrast;
	}

	public String getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(String background_color) {
		this.backgroundColor = background_color;
	}
	
	public String getForegroundColor() {
		return foregroundColor;
	}

	public void setForegroundColor(String foreground_color) {
		this.foregroundColor = foreground_color;
	}

	public boolean isImageFlagged() {
		return imageFlagged;
	}

	public void setImageFlagged(boolean image_flagged) {
		this.imageFlagged = image_flagged;
	}
}
