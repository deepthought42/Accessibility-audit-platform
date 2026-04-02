package com.looksee.pageBuilder.models;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import com.assertthat.selenium_shutterbug.core.Capture;
import com.assertthat.selenium_shutterbug.core.Shutterbug;
import com.google.api.gax.rpc.ApiException;
import com.looksee.utils.ImageUtils;

import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.Declaration;
import cz.vutbr.web.css.NodeData;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.csskit.RuleFontFaceImpl;
import cz.vutbr.web.csskit.RuleKeyframesImpl;
import cz.vutbr.web.csskit.RuleMediaImpl;
import cz.vutbr.web.domassign.StyleMap;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

/**
 * Handles the management of selenium browser instances and provides various methods for interacting with the browser 
 */
@Component
public class Browser {
	
	private static Logger log = LoggerFactory.getLogger(Browser.class);
	private WebDriver driver = null;
	private String browserName; 
	private long yScrollOffset;
	private long xScrollOffset;
	private Dimension viewportSize;
	private static final String JS_GET_VIEWPORT_WIDTH = "var width = undefined; if (window.innerWidth) {width = window.innerWidth;} else if (document.documentElement && document.documentElement.clientWidth) {width = document.documentElement.clientWidth;} else { var b = document.getElementsByTagName('body')[0]; if (b.clientWidth) {width = b.clientWidth;}};return width;";
	private static final String JS_GET_VIEWPORT_HEIGHT = "var height = undefined;  if (window.innerHeight) {height = window.innerHeight;}  else if (document.documentElement && document.documentElement.clientHeight) {height = document.documentElement.clientHeight;}  else { var b = document.getElementsByTagName('body')[0]; if (b.clientHeight) {height = b.clientHeight;}};return height;";
	
    public Browser(){}

	/**
	 * 
	 * @param url
	 * @param browser  the name of the browser to use
	 * 			chrome = google chrome
	 * 			firefox = Firefox
	 * 			ie = internet explorer
	 * 			safari = safari
	 * 
	 * @throws MalformedURLException
	 * @throws ApiException 
	 * 
	 * @pre url != null
	 * @pre browser != null
	 */
	public Browser(String browser, URL hub_node_url) throws MalformedURLException {
		assert browser != null;
		
		//create proxy server connection for handling browserup proxy calls
		//BrowserUpProxyServer browserup_proxy = new BrowserUpProxyServer();
		
		/*
		DefaultApi browserup_proxy = new DefaultApi();
		
		List<HarEntry> har_entries = browserup_proxy.entries(port, urlPattern) //entries(8000, "\"^(http|https)://" + hub_node_url.getHost() + "\\\\.com/.*$\"");
		for(HarEntry entry: har_entries) {
			entry.
		}
		*/
		
		this.setBrowserName(browser);
		if("chrome".equals(browser)){
			this.driver = openWithChrome(hub_node_url);
		}
		else if("firefox".equals(browser)){
			this.driver = openWithFirefox(hub_node_url);
		}
		else if("internet_explorer".equals(browser)){
			this.driver = openWithInternetExplorer(hub_node_url);
		}
		//else if("safari".equals(browser)){
//			this.driver = openWithSafari(hub_node_url);
	//	}
		//else if("opera".equals(browser)){
		//	this.driver = openWithOpera(hub_node_url);
		//}
		setYScrollOffset(0);
		setXScrollOffset(0);
		setViewportSize(getViewportSize(driver));
	}
	
	/**
	 * @return current {@link WebDriver driver}
	 */
	public WebDriver getDriver(){
		return this.driver;
	}

	/**
	 * Navigates to a given url and waits for it the readyState to be complete
	 * 
	 * @param url
	 * @throws MalformedURLException 
	 */
	public void navigateTo(String url) {
		getDriver().get(url);
		
		try {
			waitForPageToLoad();
		}catch(Exception e) {
			/*
			e.printStackTrace();
			
			Alert alert = isAlertPresent();
			if(alert != null){
				log.debug("Alert was encountered during navigation page load!!!");
				PageAlert page_alert = new PageAlert(alert.getText());
				
				page_alert.performChoice(getDriver(), AlertChoice.DISMISS);
			}
			*/
		}
	}

	/**
	 * Removes canvas element added by Selenium when taking screenshots
	 * 
	 * @param src
	 * @return
	 * 
	 * @precondition src != null
	 * 
	 * @Version - 9/18/2023
	 */
	public static String cleanSrc(String src) {
		Document html_doc = Jsoup.parse(src);
		
		for(Element element : html_doc.select("script")) {
			element.remove();
		}
		
		for(Element element : html_doc.select("style")) {
			element.remove();
		}
		
		for(Element element : html_doc.select("link")) {
			if(element.attr("rel").contentEquals("text/css")) {
				element.remove();
			}
		}
		
		String html = html_doc.html();
		html = html.replace("  ", " ");
		html = html.replace("\n", "");
		html = html.replace("\r", "");
		html = html.replace("\t", "");

		return html.replace(" style=\"\"", "");
	}
	
	/**
	 * Closes the browser opened by the current driver.
	 */
	public void close(){
		try{
			//driver.close();
			driver.quit();
		}
		catch(Exception e){
			log.debug("Unknown exception occurred when closing browser" + e.getMessage());
		}
	}
	
	/**
	 * open new firefox browser
	 * 
	 * @param url
	 * @return firefox web driver
	 * @throws MalformedURLException 
	 */
	public static WebDriver openWithFirefox(URL hub_node_url) 
			throws MalformedURLException, UnreachableBrowserException 
	{
		/*
		FirefoxOptions options = new FirefoxOptions();
		options.addArguments("user-agent=QanairyBot");
*/
		//DesiredCapabilities capabilities = DesiredCapabilities.firefox();
		ImmutableCapabilities capabilities = new ImmutableCapabilities("browserName", "firefox");

		//options.setHeadless(true);
	    RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, capabilities);
		driver.manage().window().maximize();

	    //driver.manage().window().setSize(new Dimension(1024, 768));
	    // Puts an Implicit wait, Will wait for 10 seconds before throwing exception
	    //driver.manage().timeouts().implicitlyWait(300, TimeUnit.SECONDS);
	    
		return driver;
	}

	/**
	 * open new opera browser
	 * 
	 * @param url
	 * @return Opera web driver
	 * @throws MalformedURLException 
	 */
	/*
	public static WebDriver openWithOpera(URL hub_node_url) 
			throws MalformedURLException, UnreachableBrowserException{
	    DesiredCapabilities cap = DesiredCapabilities.opera();
	    cap.setBrowserName("opera");
		cap.setJavascriptEnabled(true);

	    RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, cap);
	    // Puts an Implicit wait, Will wait for 10 seconds before throwing exception
	    driver.manage().timeouts().implicitlyWait(300, TimeUnit.SECONDS);
	    
		return driver;
	}
	*/
	
	/**
	 * open new Safari browser window
	 * 
	 * @param url
	 * @return safari web driver
	 */
	/*
	public static WebDriver openWithSafari(URL hub_node_url) 
			throws MalformedURLException, UnreachableBrowserException{
	    DesiredCapabilities cap = DesiredCapabilities.safari();

		RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, cap);
	    driver.manage().timeouts().implicitlyWait(300, TimeUnit.SECONDS);

		return driver;
	}
	*/
	
	/**
	 * Opens internet explorer browser window
	 * 
	 * @param url
	 * @return internet explorer web driver
	 */
	public static WebDriver openWithInternetExplorer(URL hub_node_url) 
								throws MalformedURLException, UnreachableBrowserException {
	    //DesiredCapabilities capabilities = DesiredCapabilities.internetExplorer();
		ImmutableCapabilities capabilities = new ImmutableCapabilities("browserName", "ie");

		RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, capabilities);
		
		return driver;
	}
	
	/**
	 * open new Chrome browser window
	 * 
	 * @param url
	 * @return Chrome web driver
	 * @throws MalformedURLException 
	 */
	public static WebDriver openWithChrome(URL hub_node_url) 
			throws MalformedURLException, 
					UnreachableBrowserException, 
					WebDriverException
	{
		ChromeOptions chrome_options = new ChromeOptions();
		chrome_options.addArguments("user-agent=LookseeBot");
		chrome_options.addArguments("window-size=1920,1080");
		chrome_options.addArguments("--remote-allow-origins=*");
		chrome_options.addArguments("--headless=new");

		//options.setHeadless(true);

		//ImmutableCapabilities capabilities = new ImmutableCapabilities("browserName", "chrome");

		//cap.setCapability("video", "True"); // NOTE: "True" is a case sensitive string, not boolean.

		//cap.setCapability("screenshot", true);
		//cap.setPlatform(Platform.LINUX);
		//cap.setCapability("maxInstances", 5);
		// optional video recording
		/*String record_video = "True";
		// video record
		if (record_video.equalsIgnoreCase("True")) {
			cap.setCapability("video", "True"); // NOTE: "True" is a case sensitive string, not boolean.
		} else {
			cap.setCapability("video", "False"); // NOTE: "False" is a case sensitive string, not boolean.
		}*/
		log.debug("Requesting chrome remote driver from hub");
		RemoteWebDriver driver = new RemoteWebDriver(hub_node_url, chrome_options);
		//driver.manage().window().maximize();

		//driver.manage().window().setSize(new Dimension(1024, 768));
	    //driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5L));
	    //driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(2L));
	    //driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30L));
	    		
	    //driver.manage().timeouts().pageLoadTimeout(30L, TimeUnit.SECONDS);
		return driver;
	}
	
	/**
	 * Accepts alert
	 * 
	 * @param driver
	 * @param wait
	 */
	public static void AcceptAlert(WebDriver driver, WebDriverWait wait) {
	    if (wait == null) {
	        wait = new WebDriverWait(driver, 5);
	    }
	    try{
	        Alert alert = wait.until(new ExpectedCondition<Alert>(){
				public Alert apply(WebDriver driver) {
	                try {
	                  return driver.switchTo().alert();
	                } catch (NoAlertPresentException e) {
	                  return null;
	                }
	              }
	           }
	        );
	        alert.accept();
	    }
	    catch(TimeoutException e){}
	}
	
	/**
	 * Gets image as a base 64 string
	 * 
	 * @return File png file of image
	 * @throws IOException
	 */
	public BufferedImage getViewportScreenshot() throws IOException {
		return ImageIO.read(((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE));
	}
	
	/**
	 * Gets image as a base 64 string
	 * 
	 * @return File png file of image
	 * @throws IOException
	 * 
	 * NOTE: The out put of this method is a screenshot that doesn't make any effort at removing duplicated sections
	 * caused by things like floating elements sticky navigation bars
	 */
	public BufferedImage getFullPageScreenshot() throws IOException {
		return Shutterbug.shootPage(driver, Capture.FULL_SCROLL).getImage();
	}
	
	public BufferedImage getFullPageScreenshotAshot() throws IOException {
		ru.yandex.qatools.ashot.Screenshot screenshot = new AShot().shootingStrategy(ShootingStrategies.viewportPasting(1000)).takeScreenshot(driver);
        return screenshot.getImage();
	}
	
	@Retryable
	public BufferedImage getFullPageScreenshotShutterbug() throws IOException {
		//NOTE: best for CHROME
	    return Shutterbug.shootPage(driver, Capture.FULL, 1000, true).getImage();
	}
	

	/**
	 * Gets image as a base 64 string
	 * 
	 * @return File png file of image
	 * @throws IOException
	 */
	@Deprecated
	public BufferedImage getFullPageScreenshotStitched() throws IOException {
		double percentage = 0.10;
		
		//scroll to top of page
		scrollToTopOfPage();

		//capture viewport screenshot
		int viewport_height = extractViewportHeight(driver);
		long last_y_offset = 0;
		List<BufferedImage> screenshots = new ArrayList<>();
		//String page_url = driver.getCurrentUrl();
		//while scroll position isn't at end of page
		do {
			//scroll 75% of the height of the viewport
			//capture screenshot
			BufferedImage current_screenshot = getViewportScreenshot();
			current_screenshot = current_screenshot.getSubimage(0, 
																0, 
																current_screenshot.getWidth()-20, 
																current_screenshot.getHeight());
			last_y_offset = extractYOffset(driver);
			scrollDownPercent(percentage);

			screenshots.add(current_screenshot);
		}while(extractYOffset(driver) > last_y_offset);
		
		BufferedImage original_image = null;
		
		if(screenshots.size() > 0) {
			original_image = screenshots.remove(0);
		}

		//identify stitching points by using a sliding window with random sampling to determine
		// if both images match. If a sliding window is found that matches for both images, then stitch images
		int window_size = (int)Math.ceil(viewport_height/4.0);

		//stitch images together using following steps
		//    1. retrieve row that is 25% from top of last screenshot
		int current_screenshot_row = viewport_height-1 - window_size;
		int original_screenshot_row = 0;
		
		//    3. compare rows from step 1 and 2. if they are equal, then append all rows after the row in the current image for step 1 to the original screenshot
		//                 else decrement row for original screenshot and repeat steps 1-3
		boolean doWindowsMatch = false;
		for(BufferedImage current_screenshot : screenshots){
			//	  2. retrieve row that is 25% of the way down the visible area
			original_screenshot_row =  (original_image.getHeight()-1-window_size);

			do {			
				doWindowsMatch = ImageUtils.areWindowsMatching(current_screenshot, 
																current_screenshot_row, 
																original_image, 
																original_screenshot_row, 
																window_size);

				//doRowsMatch = areRowsMatching(current_screenshot, current_screenshot_row, original_image, original_screenshot_row);
				if(doWindowsMatch) {
					BufferedImage cropped_og_img = original_image.getSubimage(0, 
																			  0, 
																			  original_image.getWidth(),
																			  original_screenshot_row);
					
					
					current_screenshot = current_screenshot.getSubimage(0, 
																		current_screenshot_row, 
																		current_screenshot.getWidth(), 
																		current_screenshot.getHeight()-current_screenshot_row);
							
					//append all rows after the row in the current image for step 1 to the original screenshot
					int height_total = cropped_og_img.getHeight() + current_screenshot.getHeight();
					BufferedImage concat_image = new BufferedImage(cropped_og_img.getWidth(), height_total, BufferedImage.TYPE_INT_RGB);
					Graphics2D g2d = concat_image.createGraphics();
		            g2d.drawImage(cropped_og_img, 0, 0, null);
		            g2d.drawImage(current_screenshot, 0, original_screenshot_row, null);
			        g2d.dispose();
			        
			        original_image = concat_image;
			        break;
				}
				else {
					//decrement row for original screenshot
					current_screenshot_row--;
					if(current_screenshot_row <= 0 && original_screenshot_row > (original_image.getHeight() - current_screenshot.getHeight()) ) {
						current_screenshot_row = viewport_height - (int)(viewport_height*percentage) - window_size;
						original_screenshot_row--;
					}
				}
			}while(!doWindowsMatch && (original_screenshot_row >= (int)(original_image.getHeight() - current_screenshot.getHeight()) && current_screenshot_row >= 0));
		}
		
		for(BufferedImage current_screenshot : screenshots) {
			current_screenshot.flush();
		}
		screenshots.clear();
		return original_image;
	}

	/**
	 * Retrieves screenshot for an {@link WebElement element}
	 * 
	 * @param screenshot
	 * @param elem
	 * @return
	 * @throws IOException
	 */
	public BufferedImage getElementScreenshot(WebElement element) throws Exception{
		//log.warn("Fullpage width and height :: " + this.getFullPageScreenshot().getWidth() + " , " + this.getFullPageScreenshot().getHeight());

		//calculate element position within screen
		return Shutterbug.shootElementVerticallyCentered(driver, element).getImage();
		//return Shutterbug.shootElement(driver, element).getImage();
	}
	

	/**
	 * 
	 * @param screenshot
	 * @param elem
	 * @return
	 * @throws IOException
	 */
	public static BufferedImage getElementScreenshot(ElementState element_state, 
													BufferedImage page_screenshot) throws IOException{
		int width = element_state.getWidth();
		int height = element_state.getHeight();
		
		if( (element_state.getXLocation() + element_state.getWidth()) > page_screenshot.getWidth() ) {
			width = page_screenshot.getWidth() - element_state.getXLocation()-1;
		}
		
		if( (element_state.getYLocation() + element_state.getHeight()) > page_screenshot.getHeight() ) {
			height = page_screenshot.getHeight() - element_state.getYLocation()-1;
		}
		
		return page_screenshot.getSubimage(element_state.getXLocation(), element_state.getYLocation(), width, height);
		
/*ORIGINAL CODE		
		//calculate element position within screen
		int point_x = element_state.getXLocation()+5;
		int point_y = element_state.getYLocation();
		int width = element_state.getWidth()+5;
		int height = element_state.getHeight();
		if((point_x+width) >= page_screenshot.getWidth()) {
			width = page_screenshot.getWidth()-point_x-1;
		}
		
		return page_screenshot.getSubimage(point_x, point_y, width, height);
		*/
	}
	
	/**
	 * 
	 * @param screenshot
	 * @param elem
	 * @return
	 * @throws IOException
	 */
	public static BufferedImage getElementScreenshot(Point element_location,
													 Dimension element_size,
													 BufferedImage page_screenshot) throws IOException{
		int width = element_size.getWidth();
		int height = element_size.getHeight();
		
		if( (element_location.getX() + element_size.getWidth()) > page_screenshot.getWidth() ) {
			width = page_screenshot.getWidth() - element_location.getX()-1;
		}
		
		if( (element_location.getY() + element_size.getHeight()) > page_screenshot.getHeight() ) {
			height = page_screenshot.getHeight() - element_location.getY()-1;
		}
		
		return page_screenshot.getSubimage(element_location.getX(), element_location.getY(), width, height);
	}

	/**
	 * 
	 * @param elements
	 * @param for_id
	 * @return
	 */
	public static ElementState findLabelFor(Set<ElementState> elements, String for_id){
		for(ElementState elem : elements){
			//ElementState tag = (ElementState)elem;
			if(elem.getName().equals("label") ){
				if(elem.getAttribute("for").contains(for_id)){
					return elem;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Finds labels that match ids passed
	 * @param elements
	 * @param for_ids
	 * @return
	 */
	public static Set<ElementState> findLabelsFor(Set<ElementState> elements, String[] for_ids){
		Set<ElementState> labels = new HashSet<ElementState>();
		for(ElementState elem : elements){
			//ElementState tag = (ElementState)elem;
			if(elem.getName().equals("label") ){
				for(String id : for_ids){
					if(elem.getAttributes().get("for").contains(id)){
						labels.add(elem);
					}
				}
			}
		}
		
		return labels;
	}


	/**
	 * 
	 * @param page_element
	 * @param driver
	 */
	public static void outlineElement(ElementState page_element, WebDriver driver) {
		WebElement element = driver.findElement(By.xpath(page_element.getXpath()));
		((JavascriptExecutor)driver).executeScript("arguments[0].style.border='2px solid yellow'", element);
	}
	
	/**
	 * Finds page element by xpath
	 * 
	 * @param xpath
	 * 
	 * @return {@link WebElement} located at the provided xpath
	 * 
	 * @pre xpath != null
	 * @pre !xpath.isEmpty()
	 */
	public WebElement findWebElementByXpath(String xpath){
		assert xpath != null;
		assert !xpath.isEmpty();
		
		return driver.findElement(By.xpath(xpath));
	}
	
	/**
	 * Reads all css styles and loads them into a hash for a given {@link WebElement element}
	 * 
	 * NOTE: THIS METHOD IS VERY SLOW DUE TO SLOW NATURE OF getCssValue() METHOD. AS cssList GROWS
	 * SO WILL THE TIME IN AT LEAST A LINEAR FASHION. THIS LIST CURRENTLY TAKES ABOUT .4 SECONDS TO CHECK ENTIRE LIST OF 13 CSS ATTRIBUTE TYPES
	 * @param element the element to for which css styles should be loaded.
	 * @throws XPathExpressionException 
	 * @throws IOException 
	 */
	public static Map<String, String> loadPreRenderCssProperties(Document jsoup_doc, 
																 org.w3c.dom.Document w3c_document, 
																 Map<String, Map<String, String>> rule_set_list, 
																 URL url, 
																 String xpath, 
																 Element element
	 ) throws XPathExpressionException, IOException {
		
		assert w3c_document != null;
		assert url != null;
		assert xpath != null;
		
		Map<String, String> css_map = new HashMap<>();

		//THE FOLLOWING WORKS TO GET RENDERED CSS VALUES FOR EACH ELEMENT THAT ACTUALLY HAS CSS


		//count all elements with non 0 px values that aren't decimals
		//extract all rules
		
		for(String css_selector : rule_set_list.keySet()) {
			if(css_selector.startsWith("@")){
				continue;
			}
			String suffixless_selector = css_selector;
			if(css_selector.contains(":")) {
				suffixless_selector = css_selector.substring(0, css_selector.indexOf(":"));
			}
			Elements selected_elements = jsoup_doc.select(suffixless_selector);
			for(Element selected_elem : selected_elements) {
				if(selected_elem.html().equals(element.html())) {
					//apply rule styling to element css_map
					css_map.putAll(rule_set_list.get(css_selector));
				}
			}
		}
		
		return css_map;
	}
	
	/**
	 * Reads all css styles and loads them into a hash for a given {@link WebElement element}
	 * 
	 *
	 * @param element the element to for which css styles should be loaded.
	 * @throws XPathExpressionException 
	 */
	public static Map<String, String> loadCssPropertiesUsingParser(Document w3c_document, 
																   StyleMap map, 
																   URL url, 
																   String xpath
    ) throws XPathExpressionException{
		assert w3c_document != null;
		assert url != null;
		assert xpath != null;
		
		Map<String, String> css_map = new HashMap<>();

		//THE FOLLOWING WORKS TO GET RENDERED CSS VALUES FOR EACH ELEMENT THAT ACTUALLY HAS CSS


		//count all elements with non 0 px values that aren't decimals
		//log.warn("w3c_document :: "+w3c_document);
		//log.warn("URL :: "+url);
		
		//create the style map

		XPath xPath = XPathFactory.newInstance().newXPath();
		Node node = (Node)xPath.compile(xpath).evaluate(w3c_document, XPathConstants.NODE);
		NodeData style = map.get((org.w3c.dom.Element)node); //get the style map for the element
		//log.warn("Element styling  ::  "+style);
		if(style != null) {
			for(String property : style.getPropertyNames()) {
				
				//log.warn("property value 2 :: "+style.getAsString(property, false));
				if(style.getValue(property, false) == null) {
					continue;
				}
				String property_value = style.getValue(property, true).toString();
				//String property_value = CssPropertyFactory.construct(style.getProperty(property));
				//log.warn("Property : value    ->    "+property+  "   :    "+property_value);
				if("background-color".contentEquals(property)) {
					log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
					log.warn("background color property found " + property_value);
					log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
					
				}
				if(property_value == null || property_value.isEmpty() || "none".equalsIgnoreCase(property_value)) {
					continue;
				}
				css_map.put(property, property_value);
			}
		}
		//}
		return css_map;
	}
	
	/**
	 * Reads all css styles and loads them into a hash for a given {@link WebElement element}
	 * 
	 * NOTE: THIS METHOD IS VERY SLOW DUE TO SLOW NATURE OF getCssValue() METHOD. AS cssList GROWS
	 * SO WILL THE TIME IN AT LEAST A LINEAR FASHION. THIS LIST CURRENTLY TAKES ABOUT .4 SECONDS TO CHECK ENTIRE LIST OF 13 CSS ATTRIBUTE TYPES
	 * @param root the element to for which css styles should be loaded.
	 * @throws XPathExpressionException 
	 */
	public static Map<String, String> loadCssPrerenderedPropertiesUsingParser(List<RuleSet> rule_sets, org.jsoup.nodes.Node element){
		assert rule_sets != null;
		assert element != null;
		
		Map<String, String> css_map = new HashMap<>();
		//map rule set declarations with elements and save element
		for(RuleSet rule_set : rule_sets) {
			for(CombinedSelector selector : rule_set.getSelectors()) {
				
				String selector_str = selector.toString();
				if(selector_str.startsWith(".")
					|| selector_str.startsWith("#")) 
				{
					selector_str = selector_str.substring(1);
				}

				if(element.attr("class").contains(selector_str) || element.attr("id").contains(selector_str) || element.nodeName().equals(selector_str)) {
					
					//TODO look for padding and add it to the document
					for(Declaration declaration : rule_set) {
						String raw_property_value = declaration.toString();
						raw_property_value = raw_property_value.replace(";", "");
						String[] property_val = raw_property_value.split(":");
						
						css_map.put(property_val[0], property_val[1]);
					}
				}
			}
		}
		
		return css_map;
	}
	
	/**
	 * Reads all css styles and loads them into a hash for a given {@link WebElement element}
	 * 
	 * @param element the element to for which css styles should be loaded.
	 */
	public static Map<String, String> loadCssProperties(WebElement element, WebDriver driver){
		JavascriptExecutor executor = (JavascriptExecutor)driver;
		String script = "var s = '';" +
		                "var o = getComputedStyle(arguments[0]);" +
		                "for(var i = 0; i < o.length; i++){" +
		                "s+=o[i] + ':' + o.getPropertyValue(o[i])+';';}" + 
		                "return s;";

		String response = executor.executeScript(script, element).toString();
		
		Map<String, String> css_map = new HashMap<String, String>();

		String[] css_prop_vals = response.split(";");
		for(String prop_val_pair : css_prop_vals) {
			String[] prop_val = prop_val_pair.split(":");
			
			if(prop_val.length == 1) {
				continue;
			}
			if(prop_val.length > 0) {
				String prop1 = prop_val[0];
				String prop2 = prop_val[1];
				css_map.put(prop1, prop2);
			}
		}
		
		return css_map;
	}
	
	/**
	 * Reads all css styles and loads them into a hash for a given {@link WebElement element}
	 * 
	 * NOTE: THIS METHOD IS VERY SLOW DUE TO SLOW NATURE OF getCssValue() METHOD. AS cssList GROWS
	 * SO WILL THE TIME IN AT LEAST A LINEAR FASHION. THIS LIST CURRENTLY TAKES ABOUT .4 SECONDS TO CHECK ENTIRE LIST OF 13 CSS ATTRIBUTE TYPES
	 * @param element the element to for which css styles should be loaded.
	 */
	@Deprecated
	public static Map<String, String> loadCssProperties(WebElement element){
		Map<String, String> css_map = new HashMap<String, String>();
		css_map.put("color", element.getCssValue("color"));
		
		//background color and image
		css_map.put("background-color", element.getCssValue("background-color"));
		css_map.put("background-image", element.getCssValue("background-image"));
		
		//font/text
		css_map.put("font-size", element.getCssValue("font-size"));
		css_map.put("font-weight", element.getCssValue("font-weight"));
		css_map.put("font-variant", element.getCssValue("font-variant"));
		css_map.put("font-family", element.getCssValue("font-family"));
		css_map.put("line-height", element.getCssValue("line-height"));
		
		
		//border colors
		css_map.put("border-top-color", element.getCssValue("border-top-color"));
		css_map.put("border-right-color", element.getCssValue("border-right-color"));
		css_map.put("border-bottom-color", element.getCssValue("border-bottom-color"));
		css_map.put("border-left-color", element.getCssValue("border-left-color"));

		//border inline/block colors
		css_map.put("border-inline-start-color", element.getCssValue("border-inline-start-color"));
		css_map.put("border-inline-end-color", element.getCssValue("border-inline-end-color"));
		css_map.put("border-block-start-color", element.getCssValue("border-block-sta)rt-color"));
		css_map.put("border-block-end-color", element.getCssValue("border-block-end-color"));

		//border dimensions
		css_map.put("border-inline-start-width", element.getCssValue("border-inline-start-width"));
		css_map.put("border-inline-end-width", element.getCssValue("border-inline-end-width"));
		css_map.put("border-block-start-width", element.getCssValue("border-block-start-width"));
		css_map.put("border-block-end-width", element.getCssValue("border-block-end-width"));

		//margins
		css_map.put("margin-top", element.getCssValue("margin-top"));
		css_map.put("margin-right", element.getCssValue("margin-right"));
		css_map.put("margin-bottom", element.getCssValue("margin-bottom"));
		css_map.put("margin-left", element.getCssValue("margin-left"));

		//padding
		css_map.put("padding-top", element.getCssValue("padding-top"));
		css_map.put("padding-right", element.getCssValue("padding-right"));
		css_map.put("padding-bottom", element.getCssValue("padding-bottom"));
		css_map.put("padding-left", element.getCssValue("padding-left"));

		
		return css_map;
	}
	
	/**
	 * Reads all css styles and loads them into a hash for a given {@link WebElement element}
	 * 
	 * NOTE: THIS METHOD IS VERY SLOW DUE TO SLOW NATURE OF getCssValue() METHOD. AS cssList GROWS
	 * SO WILL THE TIME IN AT LEAST A LINEAR FASHION. THIS LIST CURRENTLY TAKES ABOUT .4 SECONDS TO CHECK ENTIRE LIST OF 13 CSS ATTRIBUTE TYPES
	 * @param element the element to for which css styles should be loaded.
	 */
	public static Map<String, String> loadTextCssProperties(WebElement element){
		String[] cssList = {"font-family", "font-size", "text-decoration-color", "text-emphasis-color"};
		Map<String, String> css_map = new HashMap<String, String>();
		
		for(String propertyName : cssList){
			String element_value = element.getCssValue(propertyName);
			if(element_value != null && !element_value.isEmpty()){
				css_map.put(propertyName, element_value);
			}
		}
		
		return css_map;
	}
	
	public String getBrowserName() {
		return browserName;
	}

	public void setBrowserName(String browser_name) {
		this.browserName = browser_name;
	}
	

	public long getYScrollOffset() {
		return yScrollOffset;
	}

	public void setYScrollOffset(long y_scroll_offset) {
		this.yScrollOffset = y_scroll_offset;
	}

	public long getXScrollOffset() {
		return xScrollOffset;
	}

	public void setXScrollOffset(long x_scroll_offset) {
		this.xScrollOffset = x_scroll_offset;
	}
	
	/**
	 * Scroll to element by scrolling down 1 screen length at a time until the element is visible
	 *  within the viewport\
	 *  
	 * @param xpath
	 * @param elem
	 */
	public void scrollToElement(String xpath, WebElement elem) 
    {
		if(xpath.contains("nav") || xpath.startsWith("//body/header")) {
			scrollToTopOfPage();
			return;
		}
		
		Point element_offset = elem.getLocation();
		while(this.getYScrollOffset() != element_offset.getY()) {
			scrollDownFull();
		}

		getViewportScrollOffset();
    }
	
	/**
	 * 
	 * @param element
	 */
	public void scrollToElement(WebElement element) 
    {
		((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
		//long pause_time = Math.abs(this.getYScrollOffset() - element.getLocation().getY())/10;
		//TimingUtils.pauseThread(pause_time);
		getViewportScrollOffset();
    }
	
	/**
	 * Removes element from browser DOM
	 * 
	 * @param class_name
	 */
	public void removeElement(String class_name) {
		JavascriptExecutor js;
		if (this.getDriver() instanceof JavascriptExecutor) {
		    js = (JavascriptExecutor) driver;
		    js.executeScript("return document.getElementsByClassName('" + class_name + "')[0].remove();");
		}
	}
	
	/**
	 * Extract all attributes from a given {@link WebElement}
	 * 
	 * @param element {@link WebElement} to have attributes loaded for
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Map<String, String> extractAttributes(WebElement element) {
		List<String> attribute_strings = (ArrayList<String>)((JavascriptExecutor)driver).executeScript("var items = []; for (index = 0; index < arguments[0].attributes.length; ++index) { items.push(arguments[0].attributes[index].name + '::' + arguments[0].attributes[index].value) }; return items;", element);
		return loadAttributes(attribute_strings);
	}
	
	/**
	 * Remove Drift.com chat app
	 */
	public void removeDriftChat() {
		((JavascriptExecutor)driver).executeScript("var element=document.getElementById(\"drift-frame-chat\");if(typeof(element)!='undefined' && element != null){document.getElementById(\"drift-frame-chat\").remove();document.getElementById(\"drift-frame-controller\").remove();}");
	}
	
	/**
	 * Loads attributes for this element into a list of attributes
	 * 
	 * @param attributeList
	 */
	private Map<String, String> loadAttributes( List<String> attributeList){
		Map<String, String> attributes_seen = new HashMap<String, String>();
		for(int i = 0; i < attributeList.size(); i++){
			String[] attributes = attributeList.get(i).split("::");
			
			if(attributes.length > 1){
				String attribute_name = attributes[0].trim().replace("\'", "'");
				String[] attributeVals = attributes[1].split(" ");

				if(!attributes_seen.containsKey(attribute_name)){
					attributes_seen.put(attribute_name, Arrays.asList(attributeVals).toString());	
				}
			}
		}

		return attributes_seen;
	}

	
	/**
	 * Retrieves the x and y scroll offset of the viewport as a {@link Point}
	 * 
	 * @param browser
	 * 
	 * @return {@link Point} containing offsets
	 */
	public Point getViewportScrollOffset(){	
		int x_offset = 0;
		int y_offset = 0;
		
		Object offset_obj= ((JavascriptExecutor)driver).executeScript("return window.pageXOffset+','+window.pageYOffset;");
		if(offset_obj instanceof String) {
			String offset_str = (String)offset_obj;
			String[] coord = offset_str.split(",");
			x_offset = Integer.parseInt(coord[0]);
			y_offset = Integer.parseInt(coord[1]);
		}

		/*
		Object objy = ((JavascriptExecutor)driver).executeScript("return window.pageYOffset;");
		Object objx = ((JavascriptExecutor)driver).executeScript("return window.pageXOffset;");

		
		if(objy instanceof Double){
			y_offset = ((Double)objy).intValue(); 
		}
		else if(objy instanceof Long){
			y_offset = ((Long)objy).intValue(); 
		}
		else if(objy instanceof Integer) {
			y_offset = ((Integer)objy).intValue(); 
		}
		else {
			y_offset = Integer.parseInt(objy.toString());
		}
		
		if(objx instanceof Double){
			x_offset = ((Double)objx).intValue(); 
		}
		else if(objx instanceof Long){
			x_offset = ((Long)objx).intValue(); 
		}
		else if(objx instanceof Integer) {
			x_offset = ((Integer)objx).intValue(); 
		}
		else {
			x_offset = Integer.parseInt(objx.toString());
		}
		*/
		
		this.setXScrollOffset(x_offset);
		this.setYScrollOffset(y_offset);
		
		return new Point(x_offset, y_offset);
	}
	
	/**
	 * Retrieve coordinates of {@link WebElement} in the current viewport
	 * 
	 * @param element {@link WebElement}
	 * @return {@link Point} coordinates
	 */
	public static Point getLocationInViewport(ElementState element, int x_offset, int y_offset) {
		int y_coord = element.getYLocation() - y_offset;
		int x_coord = element.getXLocation() - x_offset;

		return new Point(x_coord, y_coord);
	}
	
	public static int calculateYCoordinate(int y_offset, Point location){
		if((location.getY() - y_offset) >= 0){
			return location.getY() - y_offset;
		}
		return y_offset;
	}
	
	public static int calculateXCoordinate(int x_offset, Point location){
		if((location.getX() - x_offset) >= 0){
			return location.getX() - x_offset;
		}
		return x_offset;
	}

	/**
	 * Waits for the document ready state to be complete, then observes page transition if it exists
	 */
	public void waitForPageToLoad() {
		new WebDriverWait(driver, 30L).until(
				webDriver -> ((JavascriptExecutor) webDriver)
					.executeScript("return document.readyState")
					.equals("complete"));
	}
	
	private static Dimension getViewportSize(WebDriver driver) {
		int width = extractViewportWidth(driver);
		int height = extractViewportHeight(driver);
		return new Dimension(width, height);
	}

	private static long extractXOffset(WebDriver driver) {
		JavascriptExecutor executor = (JavascriptExecutor) driver;
		return (Long) executor.executeScript("return window.pageXOffset;");
	}
	
	private static long extractYOffset(WebDriver driver) {
		JavascriptExecutor executor = (JavascriptExecutor) driver;
		return (Long) executor.executeScript("return window.pageYOffset;");
	}
	
	private static int extractViewportWidth(WebDriver driver) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		int viewportWidth = Integer.parseInt(js.executeScript(JS_GET_VIEWPORT_WIDTH, new Object[0]).toString());
		return viewportWidth;
	}

	private static int extractViewportHeight(WebDriver driver) {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		int result = Integer.parseInt(js.executeScript(JS_GET_VIEWPORT_HEIGHT, new Object[0]).toString());
		return result;
	}

	public Dimension getViewportSize() {
		return viewportSize;
	}

	public void setViewportSize(Dimension viewport_size) {
		this.viewportSize = viewport_size;
	}

	public void moveMouseOutOfFrame() {
		try{
			Actions mouseMoveAction = new Actions(driver).moveByOffset(-(getViewportSize().getWidth()/3), -(getViewportSize().getHeight()/3) );
			mouseMoveAction.build().perform();
		}catch(Exception e){
			//log.warn("Exception occurred while moving mouse out of frame :: " + e.getMessage());
		}
	}

	public void moveMouseToNonInteractive(Point point) {
		try{
			Actions mouseMoveAction = new Actions(driver).moveByOffset(point.getX(), point.getY());
			mouseMoveAction.build().perform();
		}catch(Exception e){
			//log.warn("Exception occurred while moving mouse out of frame :: " + e.getMessage());
		}
	}
	
	/**
	 * 
	 * @param driver
	 * @return
	 */
	public Alert isAlertPresent(){
	    try { 
	        return driver.switchTo().alert(); 
	    }   // try 
	    catch (NoAlertPresentException Ex) { 
	        return null; 
	    }   // catch 
	}

	public boolean isDisplayed(ElementState element) {
		WebElement web_element = driver.findElement(By.xpath(element.getXpath()));
		return web_element.isDisplayed();
	}

	public static List<RuleSet> extractRuleSetsFromStylesheets(List<String> raw_stylesheets, URL page_state_url) {
		List<RuleSet> rule_sets = new ArrayList<>();
		for(String raw_stylesheet : raw_stylesheets) {
			//parse the style sheet
			try {
				StyleSheet sheet = CSSFactory.parseString(raw_stylesheet, page_state_url);
				for(int idx = 0; idx < sheet.size(); idx++) {
					if(sheet.get(idx) instanceof RuleFontFaceImpl
							|| sheet.get(idx) instanceof RuleMediaImpl
							|| sheet.get(idx) instanceof RuleKeyframesImpl) {
						continue;
					}
					
					//access the rules and declarations
					RuleSet rule = (RuleSet) sheet.get(idx);       //get the first rule
					rule_sets.add(rule);
				}
				//or even print the entire style sheet (formatted)
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return rule_sets;
	}

	/**
	 * 
	 * @param src
	 * @return
	 */
	public static List<String> extractStylesheets(String src) {
		List<String> raw_stylesheets = new ArrayList<>();
		Document doc = Jsoup.parse(src);	
		Elements stylesheets = doc.select("link");
		for(Element stylesheet : stylesheets) {
			String rel_value = stylesheet.attr("rel");
			if("stylesheet".equalsIgnoreCase(rel_value)) {
				String stylesheet_url = stylesheet.absUrl("href");
				//parse the style sheet
				if(stylesheet_url.trim().isEmpty()) {
					stylesheet_url = stylesheet.attr("href");
					if(stylesheet_url.startsWith("//")) {
						stylesheet_url = "https:"+stylesheet_url;
					}
				}
				try {
					log.warn("Adding stylesheet to raw stylesheets   ::   "+stylesheet_url);
					raw_stylesheets.add(URLReader(new URL(stylesheet_url)));
				} catch (KeyManagementException | NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					log.warn(e1.getMessage());
					//e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		
		return raw_stylesheets;
	}
	
	/**
	 * extract body html from source
	 * 
	 * @param src - entire html source for a web page
	 * 
	 * @return
	 * 
	 * @pre src != null;
	 */
	public static String extractBody(String src) {
		assert src != null;
		
		Document doc = Jsoup.parse(src);	
		Elements body_elements = doc.select("body");
		return body_elements.html();
	}
	
	public static String URLReader(URL url) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        SSLContext sc = SSLContext.getDefault();
        
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

        con.setSSLSocketFactory(sc.getSocketFactory());
        // get response code, 200 = Success
        int responseCode = con.getResponseCode();
        if(con.getContentEncoding() != null && con.getContentEncoding().equalsIgnoreCase("gzip")) {
        	return readStream(new GZIPInputStream( con.getInputStream()));
        }
        else {
        	return readStream(con.getInputStream());
        }
	}

	private static String readStream(InputStream in) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in));) {
            String nextLine = "";
            while ((nextLine = reader.readLine()) != null) {
                sb.append(nextLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

	/**
	 * Scroll to th bottom of the body element
	 */
	public void scrollToBottomOfPage() {
		((JavascriptExecutor) driver)
	     	.executeScript("window.scrollTo(0, document.body.scrollHeight)");
		getViewportScrollOffset();
	}
	
	/**
	 * Scroll to position (0,0)
	 */
	public void scrollToTopOfPage() {
		((JavascriptExecutor) driver)
	     	.executeScript("window.scrollTo(0, 0)");
		getViewportScrollOffset();
	}
	
	public void scrollDownPercent(double percent) {
		((JavascriptExecutor) driver)
	     	.executeScript("window.scrollBy(0, (window.innerHeight*"+percent+"))");
		getViewportScrollOffset();
	}
	
	public void scrollDownFull() {
		((JavascriptExecutor) driver)
	     	.executeScript("window.scrollBy(0, window.innerHeight)");
		getViewportScrollOffset();
	}

	/**
	 * Retrieve HTML source form webpage
	 * 
	 * @return HTML source
	 */
	public String getSource() {
		return this.getDriver().getPageSource();
	}

	public boolean is503Error() {
		return this.getSource().contains("503 Service Temporarily Unavailable");
	}

	public static boolean is503Error(String source) {
		return source.contains("503 Service Temporarily Unavailable");
	}

	public WebElement findElement(String xpath) throws WebDriverException{
		return getDriver().findElement(By.xpath(xpath));
	}

	/**
	 * 
	 * @param element
	 */
	public void scrollToElementCentered(WebElement element) 
	{ 
		((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
		
		getViewportScrollOffset();
	}
}
