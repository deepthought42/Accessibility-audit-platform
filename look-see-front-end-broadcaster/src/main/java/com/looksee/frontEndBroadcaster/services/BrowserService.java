package com.looksee.frontEndBroadcaster.services;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.google.cloud.storage.StorageException;
import com.looksee.frontEndBroadcaster.gcp.CloudVisionUtils;
import com.looksee.frontEndBroadcaster.gcp.GoogleCloudStorage;
import com.looksee.frontEndBroadcaster.gcp.ImageSafeSearchAnnotation;
import com.looksee.frontEndBroadcaster.models.Browser;
import com.looksee.frontEndBroadcaster.models.BrowserConnectionHelper;
import com.looksee.frontEndBroadcaster.models.ElementState;
import com.looksee.frontEndBroadcaster.models.ImageElementState;
import com.looksee.frontEndBroadcaster.models.ImageFaceAnnotation;
import com.looksee.frontEndBroadcaster.models.ImageLandmarkInfo;
import com.looksee.frontEndBroadcaster.models.ImageSearchAnnotation;
import com.looksee.frontEndBroadcaster.models.Label;
import com.looksee.frontEndBroadcaster.models.Logo;
import com.looksee.frontEndBroadcaster.models.PageState;
import com.looksee.frontEndBroadcaster.models.Template;
import com.looksee.frontEndBroadcaster.models.enums.BrowserEnvironment;
import com.looksee.frontEndBroadcaster.models.enums.BrowserType;
import com.looksee.frontEndBroadcaster.models.enums.ElementClassification;
import com.looksee.frontEndBroadcaster.models.enums.TemplateType;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ElementStateUtils;
import com.looksee.utils.ImageUtils;

import io.github.resilience4j.retry.annotation.Retry;
import us.codecraft.xsoup.Xsoup;

/**
 * A collection of methods for interacting with the {@link Browser} session object
 *
 */
@Service
@Retry(name="webdriver")
public class BrowserService {
	private static Logger log = LoggerFactory.getLogger(BrowserService.class);
	
	private static String[] valid_xpath_attributes = {"class", "id", "name", "title"};
	
	@Autowired
	private ElementStateService element_state_service;
	
	/**
	 * retrieves a new browser connection
	 *
	 * @param browser_name name of the browser (ie. firefox, chrome)
	 *
	 * @return new {@link Browser} instance
	 * @throws MalformedURLException
	 *
	 * @pre browser_name != null;
	 * @pre !browser_name.isEmpty();
	 */
	public Browser getConnection(BrowserType browser, BrowserEnvironment browser_env) throws Exception {
		assert browser != null;

		return BrowserConnectionHelper.getConnection(browser, browser_env);
	}

	/**
 	 * Constructs an {@link Element} from a JSOUP {@link Element element}
 	 * 
	 * @param xpath
	 * @param attributes
	 * @param element
	 * @param web_elem
	 * @param classification
	 * @param rendered_css_values
	 * @param screenshot_url TODO
	 * @param css_selector TODO
	 * @pre xpath != null && !xpath.isEmpty()
	 * @pre attributes != null
	 * @pre element != null
	 * @pre classification != null
	 * @pre rendered_css_values != null
	 * @pre css_values != null
	 * @pre screenshot != null
	 * 
	 * @return {@link ElementState} based on {@link WebElement} and other params
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	public static ElementState buildElementState(
			String xpath, 
			Map<String, String> attributes, 
			Element element,
			ElementClassification classification, 
			Map<String, String> rendered_css_values, 
			String screenshot_url,
			String css_selector,
			Dimension element_size,
			Point element_location
	) throws IOException {
		assert xpath != null && !xpath.isEmpty();
		assert attributes != null;
		assert element != null;
		assert classification != null;
		assert rendered_css_values != null;
		
		String foreground_color = rendered_css_values.get("color");
		if(foreground_color == null || foreground_color.trim().isEmpty()) {
			foreground_color = "rgb(0,0,0)";
		}
		
		String ownText = "";
		if(element != null && element.ownText() != null){
			ownText = element.ownText();
		}

		String element_text = "";
		if(element != null && element.text() != null){
			element_text = element.text();
		}

		String tag_name = "";
		if(element != null && element.tagName() != null){
			tag_name = element.tagName();
		}

		String outer_html = "";
		if(element != null && element.outerHtml() != null){
			outer_html = element.outerHtml();
		}

		ElementState element_state = new ElementState(
											ownText,
											element_text,
											xpath, 
											tag_name, 
											attributes, 
											rendered_css_values, 
											screenshot_url, 
											element_location.getX(), 
											element_location.getY(), 
											element_size.getWidth(),
											element_size.getHeight(),
											classification,
											outer_html,
											css_selector, 
											foreground_color,
											rendered_css_values.get("background-color"),
											false);
		
		return element_state;
	}
	
	public static ElementState buildImageElementState(
			String xpath, 
			Map<String, String> attributes, 
			Element element,
			ElementClassification classification, 
			Map<String, String> rendered_css_values, 
			String screenshot_url,
			String css_selector,
			Set<ImageLandmarkInfo> landmark_info_set,
			Set<ImageFaceAnnotation> faces,
			ImageSearchAnnotation image_search_set,
			Set<Logo> logos,
			Set<Label> labels,
			Dimension element_size,
			Point element_location
	) throws IOException{
		assert xpath != null && !xpath.isEmpty();
		assert attributes != null;
		assert element != null;
		assert classification != null;
		assert rendered_css_values != null;
		
		String foreground_color = rendered_css_values.get("color");
		if(foreground_color == null || foreground_color.trim().isEmpty()) {
			foreground_color = "rgb(0,0,0)";
		}
		
		String background_color = rendered_css_values.get("background-color");
		if(background_color == null) {
			background_color = "rgb(255,255,255)";
		}
		
		String ownText = "";
		if(element != null && element.ownText() != null){
			ownText = element.ownText().trim();
		}

		String text = "";
		if(element != null && element.text() != null){
			text = element.text();
		}

		String tag_name = "";
		if(element != null && element.tagName() != null){
			tag_name = element.tagName();
		}

		String outer_html = "";
		if(element != null && element.outerHtml() != null){
			outer_html = element.outerHtml();
		}

		ElementState element_state = new ImageElementState(
													ownText,
													text,
													xpath,
													tag_name,
													attributes,
													rendered_css_values,
													screenshot_url,
													element_location.getX(),
													element_location.getY(),
													element_size.getWidth(),
													element_size.getHeight(),
													classification,
													outer_html,
													css_selector,
													foreground_color,
													background_color,
													landmark_info_set,
													faces,
													image_search_set,
													logos,
													labels);
		
		return element_state;
	}

	/**
	 *
	 * Navigates to a url, checks that the service is available, then removes drift 
	 * 	chat client from page if it exists. Finally it builds a {@link PageState}
	 *
	 *Constructs a page object that contains all child elements that are considered to be potentially expandable.
	 * @param url_after_loading TODO
	 * @param title TODO
	 * @return page {@linkplain PageState}
	 * @throws StorageException 
	 * @throws IOException 
	 * @throws XPathExpressionException 
	 * @throws Exception 
	 * 
	 * @pre browser != null
	 * 
	 * @Version - 9/18/2023
	 */
	public PageState buildPageState( Browser browser, long audit_record_id, String browser_url) throws WebDriverException, IOException {
		assert browser != null;
		
		//remove 3rd party chat apps such as drift, and ...(NB: fill in as more identified)
		
		URL current_url = new URL(browser_url);
		int status_code = BrowserUtils.getHttpStatus(current_url);
		String url_without_protocol = BrowserUtils.getPageUrl(current_url.toString());
		browser.removeDriftChat();
		browser.removeGDPRmodals();
		boolean is_secure = BrowserUtils.checkIfSecure(current_url);

		String source = BrowserUtils.cleanSrc(browser.getDriver().getPageSource());
		
		if(Browser.is503Error(source)) {
			throw new ServiceUnavailableException("503(Service Unavailable) Error encountered. Starting over..");
		}
		
		Document html_doc = Jsoup.parse(source);
		Set<String> metadata = BrowserService.extractMetadata(html_doc);
		Set<String> stylesheets = BrowserService.extractStylesheets(html_doc);
		Set<String> script_urls =  BrowserService.extractScriptUrls(html_doc);
		Set<String> fav_icon_links = BrowserService.extractIconLinks(html_doc);
		//PageState page_record = retrievePageFromDB(audit_record_id, url_without_protocol, source, BrowserType.CHROME);
		//if(page_record != null){
		//	return page_record;
		//}
		
        //scroll to bottom then back to top to make sure all elements that may be hidden until the page is scrolled
		String title = browser.getDriver().getTitle();

		BufferedImage viewport_screenshot = browser.getViewportScreenshot();
		String screenshot_checksum = ImageUtils.getChecksum(viewport_screenshot);

		BufferedImage full_page_screenshot = browser.getFullPageScreenshotShutterbug();
		String full_page_screenshot_checksum = ImageUtils.getChecksum(full_page_screenshot);
		
		String viewport_screenshot_url = GoogleCloudStorage.saveImage(viewport_screenshot, 
																	  current_url.getHost(), 
																	  screenshot_checksum, 
																	  BrowserType.create(browser.getBrowserName()));
		viewport_screenshot.flush();

		String full_page_screenshot_url = GoogleCloudStorage.saveImage(full_page_screenshot, 
																		current_url.getHost(), 
																		full_page_screenshot_checksum, 
																		BrowserType.create(browser.getBrowserName()));
		full_page_screenshot.flush();
		
		long x_offset = browser.getXScrollOffset();
		long y_offset = browser.getYScrollOffset();
		Dimension size = browser.getDriver().manage().window().getSize();
		
		return new PageState(
							viewport_screenshot_url,
							source,
							x_offset,
							y_offset,
							size.getWidth(),
							size.getHeight(),
							BrowserType.CHROME,
							full_page_screenshot_url,
							full_page_screenshot.getWidth(),
							full_page_screenshot.getHeight(), 
							url_without_protocol, 
							title,
							is_secure,
							status_code,
							current_url.toString(),
							audit_record_id,
							metadata,
							stylesheets,
							script_urls,
							fav_icon_links);
	}
	

	/**
	 * identify and collect data for elements within the Document Object Model 
	 * 
	 * @param domain_map_id TODO
	 * @param full_page_screenshot TODO
	 * @param page_source
	 * @param rule_sets TODO
	 * @param reviewed_xpaths
	 * 
	 * @return List of ElementStates
	 * 
	 * @throws Exception 
	 * @throws XPathExpressionException 
	 * 
	 * @pre xpaths != null
	 * @pre browser != null
	 * @pre page_state != null
	 */
	public List<ElementState> getDomElementStates(
			PageState page_state,
			List<String> xpaths,
			Browser browser,
			long domain_map_id,
			BufferedImage full_page_screenshot,
			String browser_url
	) throws Exception {
		assert xpaths != null;
		assert browser != null;
		assert page_state != null;
		
		List<ElementState> visited_elements = new ArrayList<>();
		List<ElementState> image_elements = new ArrayList<>();
		String body_src = extractBody(page_state.getSrc());
		
		Document html_doc = Jsoup.parse(body_src);
		String host = (new URL(browser_url)).getHost();
		
		xpaths = xpaths.parallelStream().map(xpath -> xpath).filter(Objects::nonNull).collect(Collectors.toList());
		
		if(page_state.getViewportWidth() != 1920){
			log.warn("XPATH COUNT = "+xpaths.size()+";;;    page id = "+page_state.getKey()+"  :   "+page_state.getUrl());
			log.warn("viewport doesn't match expections");
		}

		//iterate over xpaths to build ElementStates without screenshots
		for(String xpath : xpaths) {
			//load JSOUP element
			Elements elements = Xsoup.compile(xpath).evaluate(html_doc).getElements();
			Element element = elements.first();
			
			String tag_name = element.tagName();
			//check if element is visible in pane and if not then continue to next element xpath
			if( isStructureTag(tag_name) || !ElementStateUtils.isInteractiveElement(element)){
				continue;
			}
			
			WebElement web_element = browser.findElement(xpath);
			if(web_element == null) {
				log.warn("web element is null : "+xpath+"  ;;   for page = "+page_state.getKey());
				continue;
			}

			Rectangle rect = web_element.getRect();
			Dimension element_size = new Dimension(rect.getWidth(), rect.getHeight());
			Point element_location = new Point(rect.getX(), rect.getY());
			if( doesElementHaveNegativePosition(element_location)
				|| BrowserUtils.isHidden(element_location, element_size)
				|| rect.getHeight() <= 0
				|| rect.getWidth() <= 0
				|| !web_element.isDisplayed()
			){
				continue;
			}

			String css_selector = generateCssSelectorFromXpath(xpath);
			ElementClassification classification = ElementClassification.UNKNOWN;
			
			if(isImageElement(tag_name)) {
				ElementState element_state = buildImageElementState(xpath,
																   new HashMap<>(),
																   element,
																   classification,
																   new HashMap<>(),
																   null,
																   css_selector,
																   null,
																   null,
																   null,
																   null,
																   null,
																   element_size,
																   element_location);
				
				ElementState element_record = element_state_service.findByDomainMapAndKey(domain_map_id, element_state);
				if(element_record == null) {
					element_state = enrichElementState(browser, web_element, element_state, full_page_screenshot, host);
					//element_record = element_state_service.save(domain_map_id, page_state.getId(), element_state);
				}
				
				image_elements.add(element_record);
			}
			else {
				ElementState element_state = buildElementState(xpath,
															   new HashMap<>(),
															   element,
															   classification,
															   new HashMap<>(),
															   null,
															   css_selector,
															   element_size,
															   element_location);
				
				ElementState element_record = element_state_service.findByDomainMapAndKey(domain_map_id, element_state);
				if(element_record == null) {
					element_state = enrichElementState(browser, web_element, element_state, full_page_screenshot, host);
					element_state = ElementStateUtils.enrichBackgroundColor(element_state);
					//element_record = element_state_service.save(domain_map_id, page_state.getId(), element_state);
				}

				visited_elements.add(element_record);
			}
		}

		image_elements = image_elements.parallelStream()
											.map(element -> element)
											.filter(Objects::nonNull)
											.map(element -> enrichImageElement(element))
											.collect(Collectors.toList());

		visited_elements.addAll(image_elements);

		return visited_elements;
	}

	/**
	 * Checks if element tag is 'img'
	 * @param web_element
	 * @return
	 */
	private boolean isImageElement(String tag_name) {
		return "img".equalsIgnoreCase(tag_name);
	}

	/** MESSAGE GENERATION METHODS **/
	static String[] data_extraction_messages = {
			"Locating elements",
			"Create an account to get results faster",
			"Looking for content",
			"Having a look-see",
			"Extracting colors",
			"Checking fonts",
			"Pssst. Get results faster by logging in",
			"Mapping page structure",
			"Locating links",
			"Extracting navigation",
			"Pssst. Get results faster by logging in",
			"Create an account to get results faster",
			"Mapping CSS styles",
			"Generating unique CSS selector",
			"Mapping forms",
			"Measuring whitespace",
			"Pssst. Get results faster by logging in",
			"Create an account to get results faster",
			"Mapping attributes",
			"Mapping attributes",
			"Mapping attributes",
			"Mapping attributes",
			"Mapping attributes",
			"Mapping attributes",
			"Extracting color palette",
			"Looking for headers",
			"Mapping content structure",
			"Create an account to get results faster",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Wow! There's a lot of elements here",
			"Crunching the numbers",
			"Pssst. Get results faster by logging in",
			"Create an account to get results faster",
			"Searching for areas of interest",
			"Evaluating purpose of webpage",
			"Just a single page audit? Login to audit a domain",
			"Labeling icons",
			"Labeling images",
			"Labeling logos",
			"Applying customizations",
			"Checking for overfancification",
			"Grouping by proximity",
			"Almost there!",
			"Create an account to get results faster",
			"Labeling text elements",
			"Labeling links",
			"Pssst. Get results faster by logging in",
			"Labeling images",
			"Mapping form fields",
			"Extracting templates",
			"Contemplating the meaning of the universe",
			"Checking template structure"
			};


	/** MESSAGE GENERATION METHODS **/
	
	/**
	 * Retrieves transparency value from rgba string
	 * @param css_value
	 * @return
	 */
	private boolean hasTransparency(String css_value) {
		assert css_value != null;
		assert !css_value.isEmpty();
		
		assert css_value.startsWith("rgba(");
		if(css_value.startsWith("rgb(")) {
			return false;
		}
		
		css_value = css_value.replace("rgba(", "");
		css_value = css_value.replace(")", "");
		String[] rgba = css_value.split(",");
		double transparency_value = Double.parseDouble(rgba[3].trim());

		return transparency_value < 1.0;
	}

	/**
	 * Checks if {@link Element element} is a part of a slideshow container
	 * 
	 * @param element
	 * @return
	 */
	private static boolean isSliderElement(Element element) {
		for(org.jsoup.nodes.Attribute attr : element.attributes()) {
			if(attr.getValue().toLowerCase().contains("slider") || attr.getKey().toLowerCase().contains("slider")) {
				return true;
			}
		}
		
		return false;
	}

	
	public static boolean doesElementHaveNegativePosition(Point location) {
		return location.getX() < 0 || location.getY() < 0;
	}

	public static boolean hasWidthAndHeight(Dimension dimension) {
		return dimension.getHeight() > 1 && dimension.getWidth() > 1;
	}

	/**
	 * Filters out html, body, link, title, script, meta, head, iframe, or noscript tags
	 *
	 * @param tag_name
	 *
	 * @pre tag_name != null
	 *
	 * @return true if tag name is html, body, link, title, script, meta, head, iframe, or noscript
	 */
	public static boolean isStructureTag(String tag_name) {
		assert tag_name != null;

		return "head".contentEquals(tag_name) || "link".contentEquals(tag_name) 
				|| "script".contentEquals(tag_name) || "g".contentEquals(tag_name) 
				|| "path".contentEquals(tag_name) || "svg".contentEquals(tag_name) 
				|| "polygon".contentEquals(tag_name) || "br".contentEquals(tag_name) 
				|| "style".contentEquals(tag_name) || "polyline".contentEquals(tag_name) 
				|| "use".contentEquals(tag_name) || "template".contentEquals(tag_name) 
				|| "audio".contentEquals(tag_name)  || "iframe".contentEquals(tag_name)
				|| "noscript".contentEquals(tag_name) || "meta".contentEquals(tag_name) 
				|| "base".contentEquals(tag_name) || "em".contentEquals(tag_name)
				|| "body".contentEquals(tag_name);
	}

	public static List<WebElement> filterNoWidthOrHeight(List<WebElement> web_elements) {
		List<WebElement> elements = new ArrayList<WebElement>(web_elements.size());
		for(WebElement element : web_elements){
			Dimension dimension = element.getSize();
			if(dimension.getHeight() > 1 && dimension.getWidth() > 1){
				elements.add(element);
			}
		}

		return elements;
	}

	public static List<ElementState> filterNoWidthOrHeight(List<ElementState> web_elements, boolean is_element_state) {
		List<ElementState> elements = new ArrayList<>(web_elements.size());
		for(ElementState element : web_elements){
			if(element.getHeight() > 1 && element.getWidth() > 1){
				elements.add(element);
			}
		}

		return elements;
	}

	/**
	 * Checks if {@link WebElement element} is visible in the current viewport window or not
	 * 
	 * @param browser {@link Browser browser} connection to use 
	 * @param location {@link Point point} where the element top left corner is located
	 * @param size {@link Dimension size} of the element
	 * 
	 * @return true if element is rendered within viewport, otherwise false
	 */
	public static boolean isElementVisibleInPane(Browser browser, Point location, Dimension size){
		assert browser != null;
		assert location != null;
		assert size != null;
		
		long y_offset = browser.getYScrollOffset();
		long x_offset = browser.getXScrollOffset();

		int x = location.getX();
		int y = location.getY();

		int height = size.getHeight();
		int width = size.getWidth();

		return x < browser.getViewportSize().getWidth()
				&& y < browser.getViewportSize().getHeight()
				&& x >= x_offset 
				&& y >= y_offset 
				&& ((x-x_offset)+width) < (browser.getViewportSize().getWidth())
				&& ((y-y_offset)+height) < (browser.getViewportSize().getHeight());
	}

	/**
	 * Checks if {@link ElementState element} is visible in the current viewport window or not
	 * 
	 * @param browser {@link Browser browser} connection to use 
	 * @param location {@link ElementState element} to be be evaluated
	 * 
	 * @return true if element is rendered within viewport, otherwise false
	 */
	public static boolean isElementVisibleInPane(Browser browser, ElementState element){
		assert browser != null;

		browser.getViewportScrollOffset();
		//browser.setXScrollOffset(offsets.getX());
		//browser.setYScrollOffset(offsets.getY());
		
		long y_offset = browser.getYScrollOffset();
		long x_offset = browser.getXScrollOffset();

		int x = element.getXLocation();
		int y = element.getYLocation();

		int height = element.getHeight();
		int width = element.getWidth();

		return x < browser.getViewportSize().getWidth()
				&& y < browser.getViewportSize().getHeight()
				&& x >= x_offset 
				&& y >= y_offset 
				&& ((x-x_offset)+width) <= (browser.getViewportSize().getWidth())
				&& ((y-y_offset)+height) <= (browser.getViewportSize().getHeight());
	}
	
	/**
	 * Checks if {@link WebElement element} is visible in the current viewport window or not
	 * 
	 * @param viewport_size {@link Browser browser} connection to use 
	 * @param size {@link Dimension size} of the element
	 * 
	 * @return true if element is rendered within viewport, otherwise false
	 */
	public static boolean doesElementFitInViewport(Dimension viewport_size, Point position, Dimension size){
		assert viewport_size != null;
		assert size != null;

		int height = size.getHeight();
		int width = size.getWidth();

		return width < (viewport_size.getWidth())
				&& height < (viewport_size.getHeight());
	}
	
	/**generateXpath
	 * Get immediate child elements for a given element
	 *
	 * @param elem	WebElement to get children for
	 * @return list of WebElements
	 */
	public static List<WebElement> getChildElements(WebElement elem) throws WebDriverException{
		return elem.findElements(By.xpath("./*"));
	}

	private List<String> getChildElements(String xpath, List<String> xpaths) {
		List<String> child_xpaths = xpaths.parallelStream()
											.filter( path -> !xpath.equals(path) && xpath.contains(path) )
											.collect(Collectors.toList());
		
		return child_xpaths;
	}

	/**
	 * Get immediate child elements for a given element
	 *
	 * @param elem	WebElement to get children for
	 * @return list of WebElements
	 */
	public static List<WebElement> getNestedElements(WebElement elem) throws WebDriverException{
		return elem.findElements(By.xpath(".//*"));
	}

	/**
	 * Get immediate parent elements for a given element
	 *
	 * @param elem	{@linkplain WebElement) to get parent of
	 * @return parent {@linkplain WebElement)
	 */
	public WebElement getParentElement(WebElement elem) throws WebDriverException{
		return elem.findElement(By.xpath(".."));
	}

	public static String cleanAttributeValues(String attribute_values_string) {
		String escaped = attribute_values_string.replaceAll("[\\t\\n\\r]+"," ");
		escaped = escaped.trim().replaceAll("\\s+", " ");
		escaped = escaped.replace("\"", "\\\"");
		return escaped.replace("\'", "'");
	}

	/**
	 * generates a unique xpath for this element.
	 *
	 * @return an xpath that identifies this element uniquely
	 */
	public static String generateXpathUsingJsoup(Element element, Document doc, Attributes attributes, Map<String, Integer> xpath_cnt){
		List<String> attributeChecks = new ArrayList<>();
		List<String> valid_attributes = Arrays.asList(valid_xpath_attributes);
		Element element_copy = element.clone();
		String xpath = "/"+element.tagName();
		for(org.jsoup.nodes.Attribute attr : attributes.asList()){
			if(valid_attributes.contains(attr.getKey())){
				String attribute_values = attr.getValue();
				String trimmed_values = cleanAttributeValues(attribute_values.trim());
				//check if attribute is auto generated
				String reduced_values = removeAutoGeneratedValues(trimmed_values);
				if(reduced_values.length() > 0 && !reduced_values.contains("javascript") && !reduced_values.contains("void()")){
					attributeChecks.add("contains(@" + attr.getKey() + ",\"" + reduced_values.split(" ")[0] + "\")");
				}
			}
		}

		if(attributeChecks.size()>0){
			xpath += "["+ attributeChecks.get(0).toString()+"]";
		}

		Element last_element = element;
		Element parent = null;
		String last_element_tagname = last_element.tagName();
	    while(!"html".equals(last_element_tagname) && !"body".equals(last_element_tagname)){
	    	try{
	    		parent = last_element.parent();

	    		if(!isStructureTag(parent.tagName())){
	    			Elements elements = Xsoup.compile("//"+parent.tagName() + xpath).evaluate(doc).getElements();
		    		if( elements.isEmpty()){
		    			break;
		    		}
	    			else if( elements.size() == 1){
		    			return "//"+parent.tagName() + xpath;
		    		}
		    		else{
			    		xpath = "/" + parent.tagName() + xpath;
		    		}
		    		last_element = parent;
		    		last_element_tagname = last_element.tagName();
	    		}
	    		else{
	    			log.warn("Encountered structure tag. Aborting element xpath extraction..");
	    			break;
	    		}
	    	}catch(InvalidSelectorException e){
	    		parent = null;
	    		log.warn("Invalid selector exception occurred while generating xpath through parent nodes");
	    		break;
	    	}
	    }
	    if(!xpath.startsWith("//")){
			xpath = "/"+xpath;
		}

		return uniqifyXpath(element_copy, xpath, doc, xpath_cnt);
	}
	
	/**
	 * generates a unique xpath for this element.
	 *
	 * @return an xpath that identifies this element uniquely
	 */
	public static String generateCssSelectorFromXpath(String xpath){
		List<String> selectors = new ArrayList<>();
		
		//split xpath on '/' character
		String[] xpath_selectors = xpath.split("/");
		for(String xpath_selector : xpath_selectors) {
			//transform selector to css selector
			String css_select = transformXpathSelectorToCss(xpath_selector);
			selectors.add(css_select);
		}
		
		return buildCssSelector(selectors);
	}

	/**
	 * combines list of sub selectors into cohesive css_selector
	 * @param selectors
	 * @return
	 */
	private static String buildCssSelector(List<String> selectors) {
		String css_selector = "";
		
		for(String selector : selectors) {
			if(css_selector.isEmpty() && !selector.isEmpty()) {
				css_selector = selector;
			}
			else if(!css_selector.isEmpty() && !selector.isEmpty()){
				css_selector += " " + selector;
			}
		}
		
		return css_selector;
	}

	public static String transformXpathSelectorToCss(String xpath_selector) {
		String selector = "";
		
		//convert index value with format '[integer]' to css format		
		String pattern_string = "(\\[([0-9]+)\\])";
        Pattern pattern_index = Pattern.compile(pattern_string);
        Matcher matcher = pattern_index.matcher(xpath_selector);
        if(matcher.find()) {
        	String match = matcher.group(1);
        	match = match.replace("[", "");
        	match = match.replace("]", "");
        	int element_index = Integer.parseInt(match);
        	selector = xpath_selector.replaceAll(pattern_string, "");

			selector += ":nth-child(" + element_index + ")";
        }
        else {
        	selector = xpath_selector;
        }
        
		return selector.trim();
	}

	private static String removeAutoGeneratedValues(String trimmed_values) {
		String[] values = trimmed_values.split(" ");
		List<String> reduced_vals = new ArrayList<>();
		for(String val : values){
			//check if value is auto-generated
			if(!isAutoGenerated(val)){
				reduced_vals.add(val);
			}
		}
		return String.join(" ", reduced_vals);
	}

	private static boolean isAutoGenerated(String val) {
		//check if value ends in a number
		return val.length() > 0 && Character.isDigit(val.charAt(val.length()-1));
	}

	/**
	 * generates a unique xpath for this element.
	 *
	 * @return an xpath that identifies this element uniquely
	 */
	public static Map<String, String> generateAttributesMapUsingJsoup(Element element){
		Map<String, String> attributes = new HashMap<>();
		for(Attribute attribute : element.attributes() ){
			attributes.put(attribute.getKey(), attribute.getValue());
		}

		return attributes;
	}

	/**
	 * creates a unique xpath based on a given hash of xpaths
	 *
	 * @param driver
	 * @param xpathHash
	 *
	 * @return
	 */
	public static String uniqifyXpath(Element elem, String xpath, Document doc, Map<String, Integer> xpath_cnt){
		try {
			List<Element> elements = Xsoup.compile(xpath).evaluate(doc).getElements();
			if(elements.size() > 1){
				int count = 0;
				if(xpath_cnt.containsKey(xpath)){
					count = xpath_cnt.get(xpath);
				}
				xpath_cnt.put(xpath, ++count);
				String unique_xpath = "("+xpath+")[" + count + "]";
				return unique_xpath;
			}

		}catch(InvalidSelectorException e){
			log.warn(e.getMessage());
		}

		return xpath;
	}

	/**
	 * creates a unique xpath by taking a given xpath and shortening it from the left until it finds
	 * the shortest unique xpath.
	 *
	 * @param xpath a valid xpath for a {@link WebElement}
	 * @param driver a {@link WebDriver} connection to a selenium instance
	 *
	 * @return a shortened unique xpath
	 */
	public static String uniqifyXpath(String xpath, WebDriver driver){
		try {
			//parse xpath into array
			String temp_xpath = xpath.substring(1);
			String[] xpath_arr = temp_xpath.split("/");
			String last_unique_xpath = xpath;
			int last_index = 1;
			do {
				//construct new xpath by joining current array with '/'
				String new_xpath = "/";
				for(int i=last_index; i<xpath_arr.length; i++) {
					new_xpath += "/"+xpath_arr[i];
				}
				//get WebElements By xpath
				List<WebElement> web_elements = driver.findElements(By.xpath(new_xpath));
				
				if(web_elements.size() > 1) {
					break;
				}
				else {
					last_unique_xpath = new_xpath;
				}
				
				last_unique_xpath = new_xpath;
				last_index++;
			}while(last_index >= xpath_arr.length);
			return last_unique_xpath;
		}catch(InvalidSelectorException e){
			log.error(e.getMessage());
		}

		return xpath;
	}
	
	/**
	 * creates a unique xpath by taking a given xpath and shortening it from the left until it finds
	 * the shortest unique xpath.
	 *
	 * @param xpath a valid xpath for a {@link WebElement}
	 * @param driver a {@link WebDriver} connection to a selenium instance
	 *
	 * @return a shortened unique xpath
	 */
	public static String uniqifyXpath(String xpath, Document html_doc){
		try {
			String temp_xpath = xpath.substring(1);
			String[] xpath_arr = temp_xpath.split("/");
			
			String last_unique_xpath = xpath;
			int last_index = 1;
			while(last_index <= xpath_arr.length) {
				//construct new xpath by joining current array with '/'
				String new_xpath = "/";
				for(int i=last_index; i<xpath_arr.length; i++) {
					new_xpath += "/"+xpath_arr[i];
				}

				Elements elements = Xsoup.compile(new_xpath).evaluate(html_doc).getElements();
				if(elements.size() > 1 || new_xpath.equals("/")) {
					break;
				}
				else {
					last_unique_xpath = new_xpath;
				}
				
				last_unique_xpath = new_xpath;
				last_index++;
			}
			
			return last_unique_xpath;
		}catch(InvalidSelectorException e){
			log.error(e.getMessage());
		}

		return xpath;
	}
	
	
	

	public Map<String, Template> findTemplates(List<com.looksee.frontEndBroadcaster.models.Element> element_list){
		//create a map for the various duplicate elements
		Map<String, Template> element_templates = new HashMap<>();
		List<com.looksee.frontEndBroadcaster.models.Element> parents_only_element_list = new ArrayList<>();
		for(com.looksee.frontEndBroadcaster.models.Element element : element_list) {
			if(!ElementClassification.LEAF.equals(element.getClassification())) {
				parents_only_element_list.add(element);
			}
		}

		//iterate over all elements in list
		
		Map<String, Boolean> identified_templates = new HashMap<String, Boolean>();
		for(int idx1 = 0; idx1 < parents_only_element_list.size()-1; idx1++){
			com.looksee.frontEndBroadcaster.models.Element element1 = parents_only_element_list.get(idx1);
			boolean at_least_one_match = false;
			if(identified_templates.containsKey(element1.getKey()) ) {
				continue;
			}
			//for each element iterate over all elements in list
			for(int idx2 = idx1+1; idx2 < parents_only_element_list.size(); idx2++){
				com.looksee.frontEndBroadcaster.models.Element element2 = parents_only_element_list.get(idx2);
				if(identified_templates.containsKey(element2.getKey()) || !element1.getName().equals(element2.getName())){
					continue;
				}
				//get largest string length
				int max_length = element1.getTemplate().length();
				if(element2.getTemplate().length() > max_length){
					max_length = element2.getTemplate().length();
				}
				
				if(max_length == 0) {
					log.warn("max length of 0 between both templates");
					continue;
				}
				
				if(element1.getTemplate().equals(element2.getTemplate())){
					String template_str = element2.getTemplate();
					if(!element_templates.containsKey(template_str)){
						element_templates.put(template_str, new Template(TemplateType.UNKNOWN, template_str));
					}
					element_templates.get(template_str).getElements().add(element2);
					identified_templates.put(element2.getKey(), Boolean.TRUE);
					at_least_one_match = true;
					continue;
				}

				log.warn("getting levenshtein distance...");
				//double distance = StringUtils.getJaroWinklerDistance(element_list.get(idx1).getTemplate(), element_list.get(idx2).getTemplate());
				//calculate distance between loop1 value and loop2 value
				double distance = StringUtils.getLevenshteinDistance(element1.getTemplate(), element2.getTemplate());
				//if value is within threshold then add loop2 value to map for loop1 value xpath
				double avg_string_size = ((element1.getTemplate().length() + element2.getTemplate().length())/2.0);
				double similarity = distance / avg_string_size;
				//double sigmoid = new Sigmoid(0,1).value(similarity);

				//calculate distance of children if within 20%
				if(distance == 0.0 || similarity < 0.025){
					log.warn("Distance ;  Similarity :: "+distance + "  ;  "+similarity);
					String template_str = element1.getTemplate();
					if(!element_templates.containsKey(template_str)){
						element_templates.put(template_str, new Template(TemplateType.UNKNOWN, template_str));
					}
					element_templates.get(template_str).getElements().add(element2);
					identified_templates.put(element2.getKey(), Boolean.TRUE);

					at_least_one_match = true;
				}
			}
			if(at_least_one_match){
				String template_str = element1.getTemplate();
				element_templates.get(template_str).getElements().add(element1);
				identified_templates.put(element1.getKey(), Boolean.TRUE);
			}
			log.warn("****************************************************************");

		}

		return element_templates;
	}

	/**
	 * Checks if Attributes contains keywords indicative of a slider 
	 * @param attributes
	 * 
	 * @return true if any of keywords present, otherwise false
	 * 
	 * @pre attributes != null
	 * @pre !attributes.isEmpty()
	 */
	public static boolean doesAttributesContainSliderKeywords(Map<String, List<String>> attributes) {
		assert attributes != null;
		assert !attributes.isEmpty();
		for(String attr : attributes.keySet()) {
			if(attributes.get(attr).contains("slide")) {
				return true;
			}
		}
		return false;
	}

	public Map<String, Template> reduceTemplatesToParents(Map<String, Template> list_elements_list) {
		Map<String, Template> element_map = new HashMap<>();
		List<Template> template_list = new ArrayList<>(list_elements_list.values());
		//check if element is a child of another element in the list. if yes then don't add it to the list
		for(int idx1=0; idx1 < template_list.size(); idx1++){
			boolean is_child = false;
			for(int idx2=0; idx2 < template_list.size(); idx2++){
				if(idx1 != idx2 && template_list.get(idx2).getTemplate().contains(template_list.get(idx1).getTemplate())){
					is_child = true;
					break;
				}
			}

			if(!is_child){
				element_map.put(template_list.get(idx1).getTemplate(), template_list.get(idx1));
			}
		}

		//remove duplicates
		log.warn("total elements left after reduction :: " + element_map.values().size());
		return element_map;
	}

	/**
	 *
	 * Atom - A leaf element or an element that contains only 1 leaf element regardless of depth
	 * Molecule - Contains at least 2 atoms and cannot contain any molecules
	 * Organism - Contains at least 2 molecules or at least 1 molecule and 1 atom or at least 1 organism, Must not be an immediate child of body
	 * Template - An Immediate child of the body tag or the descendant such that the element is the first to have sibling elements
	 *
	 * @param template
	 * @return
	 */
	public TemplateType classifyTemplate(String template){
		Document html_doc = Jsoup.parseBodyFragment(template);
		Element root_element = html_doc.body();

		return classifyUsingChildren(root_element);
	}

	private TemplateType classifyUsingChildren(Element root_element) {
		assert root_element != null;

		int atom_cnt = 0;
		int molecule_cnt = 0;
		int organism_cnt = 0;
		int template_cnt = 0;
		if(root_element.children() == null || root_element.children().isEmpty()){
			return TemplateType.ATOM;
		}

		//categorize each eleemnt
		for(Element element : root_element.children()){
			TemplateType type = classifyUsingChildren(element);
			if(type == TemplateType.ATOM){
				atom_cnt++;
			}
			else if(type == TemplateType.MOLECULE){
				molecule_cnt++;
			}
			else if(type == TemplateType.ORGANISM){
				organism_cnt++;
			}
			else if(type == TemplateType.TEMPLATE){
				template_cnt++;
			}
		}

		if(atom_cnt == 1){
			return TemplateType.ATOM;
		}
		else if(atom_cnt > 1 && molecule_cnt == 0 && organism_cnt == 0 && template_cnt == 0){
			return TemplateType.MOLECULE;
		}
		else if( (molecule_cnt == 1 && atom_cnt > 0 || molecule_cnt > 1 || organism_cnt > 0) && template_cnt == 0){
			return TemplateType.ORGANISM;
		}
		else if(isTopLevelElement()){
			return TemplateType.TEMPLATE;
		}
		return TemplateType.UNKNOWN;

	}

	private boolean isTopLevelElement() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public static boolean testContainsElement(List<String> keys) {
		for(String key : keys) {
			if(key.contains("elementstate")) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param src
	 * @return
	 */
	public List<String> extractAllUniqueElementXpaths(String src) {
		assert src != null;
		
		Map<String, String> frontier = new HashMap<>();
		List<String> xpaths = new ArrayList<>();
		String body_src = extractBody(src);
		
		Document html_doc = Jsoup.parse(body_src);
		frontier.put("//body","");
		
		while(!frontier.isEmpty()) {
			String next_xpath = frontier.keySet().iterator().next();
			frontier.remove(next_xpath);
			xpaths.add(next_xpath);
			
			Elements elements = Xsoup.compile(next_xpath).evaluate(html_doc).getElements();
			if(elements.size() == 0) {
				log.warn("NO ELEMENTS WITH XPATH FOUND :: "+next_xpath);
				continue;
			}
			Element element = elements.first();
			List<Element> children = new ArrayList<Element>(element.children());
			Map<String, Integer> xpath_cnt = new HashMap<>();
			
			for(Element child : children) {
				if(isStructureTag(child.tagName())) {
					continue;
				}
				String xpath = next_xpath + "/" + child.tagName();
				
				if(xpath_cnt.containsKey(child.tagName()) ) {
					xpath_cnt.put(child.tagName(), xpath_cnt.get(child.tagName())+1);
				}
				else {
					xpath_cnt.put(child.tagName(), 1);
				}
				
				xpath = xpath + "["+xpath_cnt.get(child.tagName())+"]";

				frontier.put(xpath, "");
			}
		}
		
		return xpaths.parallelStream().map(xpath -> {
			return uniqifyXpath(xpath, html_doc);
		}).collect(Collectors.toList());
	}

	public static String extractBody(String src) {
		String patternString = "<body[^\\>]*>([\\s\\S]*)<\\/body>";

        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(src);
        if(matcher.find()) {
        	return matcher.group();
        }
        return "";
	}

	public static Set<String> extractMetadata(Document html_doc) {
		Elements meta_tags = html_doc.getElementsByTag("meta");
		Set<String> meta_tag_html = new HashSet<String>();
		
		for(Element meta_tag : meta_tags) {
			meta_tag_html.add(meta_tag.outerHtml());
		}
		return meta_tag_html;
	}

	public static Set<String> extractStylesheets(Document html_doc) {
		Elements link_tags = html_doc.getElementsByTag("link");
		Set<String> stylesheet_urls = new HashSet<String>();
		
		for(Element link_tag : link_tags) {
			stylesheet_urls.add(link_tag.absUrl("href"));
		}
		return stylesheet_urls;
	}

	public static Set<String> extractScriptUrls(Document html_doc) {
		Elements script_tags = html_doc.getElementsByTag("script");
		Set<String> script_urls = new HashSet<String>();
		
		for(Element script_tag : script_tags) {
			String src_url = script_tag.absUrl("src");
			if(src_url != null && !src_url.isEmpty()) {
				script_urls.add(script_tag.absUrl("src"));
			}
		}
		return script_urls;
	}

	public static Set<String> extractIconLinks(Document html_doc) {
		Elements icon_tags = html_doc.getElementsByTag("link");
		Set<String> icon_urls = new HashSet<String>();
		
		for(Element icon_tag : icon_tags) {
			if(icon_tag.attr("rel").contains("icon")){
				icon_urls.add(icon_tag.absUrl("href"));
			}
		}
		return icon_urls;
	}
	
	private static String calculateSha256(String value) {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(value);
	}
	
	/**
	 * Enriches {@link ElementState element} with screenshots, 
	 * 
	 * @param browser
	 * @param web_element
	 * @param element_state
	 * @param page_screenshot
	 * @param host
	 * @return
	 * @throws IOException
	 */
	public ElementState enrichElementState(Browser browser,
											WebElement web_element,
											ElementState element_state,
											BufferedImage page_screenshot,
											String host) throws IOException
	{	
		if(element_state.getYLocation() < browser.getYScrollOffset()) {
			browser.scrollToElement(web_element);
		}
		else {
			browser.scrollToElementCentered(web_element);
		}
		
		WebDriverWait wait = new WebDriverWait(browser.getDriver(), 30);
		wait.until(ExpectedConditions.elementToBeClickable(web_element));
		
		String element_screenshot_url = "";
		BufferedImage element_screenshot = null;
		//TODO: REMOVE FOLLOWING COMMENT ONCE CONFIRMED WORKING AS EXPECTED IN PAGE AUDIT ENRICHMENT MICROSERVICE
		//Map<String, String> rendered_css_props = Browser.loadCssProperties(web_element, browser.getDriver());
		Map<String, String> attributes = browser.extractAttributes(web_element);
		
		if(BrowserUtils.isLargerThanViewport(element_state, browser.getViewportSize().getWidth(), browser.getViewportSize().getHeight())) {
			try {
				element_screenshot = Browser.getElementScreenshot(element_state, page_screenshot);
				String screenshot_checksum = ImageUtils.getChecksum(element_screenshot);
				element_screenshot_url = GoogleCloudStorage.saveImage(element_screenshot, 
																		host, 
																		screenshot_checksum, 
																		BrowserType.create(browser.getBrowserName()));
				element_screenshot.flush();
			}
			catch(Exception e1){
				log.warn("Exception occurred while extracting screenshot from full page screenshot");
			}
		}
		else {
			try {
				//extract element screenshot from full page screenshot
				element_screenshot = browser.getElementScreenshot(web_element);
				String screenshot_checksum = ImageUtils.getChecksum(element_screenshot);
				
				element_screenshot_url = GoogleCloudStorage.saveImage(element_screenshot, host, screenshot_checksum, BrowserType.create(browser.getBrowserName()));
				element_screenshot.flush();
			}
			catch(Exception e1){
				element_screenshot = Browser.getElementScreenshot(element_state, page_screenshot);
				String screenshot_checksum = ImageUtils.getChecksum(element_screenshot);
				element_screenshot_url = GoogleCloudStorage.saveImage(element_screenshot, host, screenshot_checksum, BrowserType.create(browser.getBrowserName()));
				element_screenshot.flush();
			}
		}
		
		element_state.setScreenshotUrl(element_screenshot_url);
		element_state.setAttributes(attributes);
		//element_state.setRenderedCssValues(rendered_css_props);

		return element_state;
	}

	/**
	 * Performs image enrichment in parallel for all elements in the given list
	 * @param element_states
	 * @return
	 */
	public List<ElementState> enrichImageElement(List<ElementState> element_states)
	{	
		return element_states.parallelStream().map(element_state -> {
			if(element_state instanceof ImageElementState && !element_state.getScreenshotUrl().isEmpty()) {
				BufferedImage element_screenshot;
				try {
					element_screenshot = ImageIO.read(new URL(element_state.getScreenshotUrl()));

					//retrieve image landmark properties from google cloud vision
					//Set<ImageLandmarkInfo> landmark_info_set = CloudVisionUtils.extractImageLandmarks(element_screenshot);
					Set<ImageLandmarkInfo> landmark_info_set = null;
					//retrieve image faces properties from google cloud vision
					//Set<ImageFaceAnnotation> faces = CloudVisionUtils.extractImageFaces(element_screenshot);
					Set<ImageFaceAnnotation> faces = null;
					//retrieve image reverse image search properties from google cloud vision
					ImageSearchAnnotation image_search_set = CloudVisionUtils.searchWebForImageUsage(element_screenshot);
					ImageSafeSearchAnnotation img_safe_search_annotation = CloudVisionUtils.detectSafeSearch(element_screenshot);
					
					//retrieve image logos from google cloud vision
					Set<Logo> logos = new HashSet<>();//CloudVisionUtils.extractImageLogos(element_screenshot);

					//retrieve image labels
					Set<Label> labels = CloudVisionUtils.extractImageLabels(element_screenshot);

					ImageElementState image_element = (ImageElementState)element_state;
					//image_element.setScreenshotUrl(element_screenshot_url);
					image_element.setFaces(faces);
					image_element.setLandmarkInfoSet(landmark_info_set);
					image_element.setImageSearchSet(image_search_set);

					image_element.setAdult(img_safe_search_annotation.getAdult());
					image_element.setRacy(img_safe_search_annotation.getRacy());
					image_element.setViolence(img_safe_search_annotation.getViolence());
					image_element.setLogos(logos);
					image_element.setLabels(labels);
					return image_element;
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;

		}).filter(e -> e==null)
		.collect(Collectors.toList());
	}

	/**
	 * Enriches elements using cloud vision utils
	 * 
	 * @param element_state
	 * @return
	 */
	public ElementState enrichImageElement(ElementState element_state) 
	{	
		if(element_state instanceof ImageElementState 
				&& element_state.getScreenshotUrl() != null 
				&& !element_state.getScreenshotUrl().isEmpty()) 
		{
			BufferedImage element_screenshot;
			try {
				element_screenshot = ImageIO.read(new URL(element_state.getScreenshotUrl()));

				//retrieve image landmark properties from google cloud vision
				//Set<ImageLandmarkInfo> landmark_info_set = CloudVisionUtils.extractImageLandmarks(element_screenshot);
				Set<ImageLandmarkInfo> landmark_info_set = null;
				//retrieve image faces properties from google cloud vision
				//Set<ImageFaceAnnotation> faces = CloudVisionUtils.extractImageFaces(element_screenshot);
				Set<ImageFaceAnnotation> faces = null;
				//retrieve image reverse image search properties from google cloud vision
				ImageSearchAnnotation image_search_set = CloudVisionUtils.searchWebForImageUsage(element_screenshot);
				ImageSafeSearchAnnotation img_safe_search_annotation = CloudVisionUtils.detectSafeSearch(element_screenshot);
				
				//retrieve image logos from google cloud vision
				Set<Logo> logos = new HashSet<>();//CloudVisionUtils.extractImageLogos(element_screenshot);

				//retrieve image labels
				Set<Label> labels = CloudVisionUtils.extractImageLabels(element_screenshot);

				ImageElementState image_element = (ImageElementState)element_state;
				//image_element.setScreenshotUrl(element_screenshot_url);
				image_element.setFaces(faces);
				image_element.setLandmarkInfoSet(landmark_info_set);
				image_element.setImageSearchSet(image_search_set);

				if(img_safe_search_annotation != null){
					image_element.setAdult(img_safe_search_annotation.getAdult());
					image_element.setRacy(img_safe_search_annotation.getRacy());
					image_element.setViolence(img_safe_search_annotation.getViolence());
				}
				image_element.setLogos(logos);
				image_element.setLabels(labels);
				return image_element;
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return element_state;
	}
}


@ResponseStatus(HttpStatus.SEE_OTHER)
class ServiceUnavailableException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 794045239226319408L;

	public ServiceUnavailableException(String msg) {
		super(msg);
	}
}

@ResponseStatus(HttpStatus.SEE_OTHER)
class FiveZeroThreeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 452417401491490882L;

	public FiveZeroThreeException(String msg) {
		super(msg);
	}
}
