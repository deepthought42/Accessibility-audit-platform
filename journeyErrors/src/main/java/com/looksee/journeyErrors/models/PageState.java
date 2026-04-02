package com.looksee.journeyErrors.models;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.looksee.journeyErrors.models.enums.BrowserType;

/**
 * A reference to a web page
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Node
public class PageState extends LookseeObject {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PageState.class);

	//@JsonIgnore
	private String src;
	private String url;
	private String urlAfterLoading;
	private String viewportScreenshotUrl;
	private String fullPageScreenshotUrlOnload;
	private String fullPageScreenshotUrlComposite;
	private String pageName;
	private String browser;
	private String title;

	private boolean loginRequired;
	private boolean secured;
	private boolean landable;
	
	private long scrollXOffset;
	private long scrollYOffset;
	
	private int viewportWidth;
	private int viewportHeight;
	private int fullPageWidth;
	private int fullPageHeight;
	private int httpStatus;

	private Set<String> scriptUrls;
	private Set<String> stylesheetUrls;
	private Set<String> metadata;
	private Set<String> faviconUrl;
	private Set<String> keywords;
	
	@Relationship(type = "HAS", direction = Direction.OUTGOING)
	private List<ElementState> elements;


	public PageState() {
		super();
		setElements(new ArrayList<>());
		setKeywords(new HashSet<>());
		setScriptUrls(new HashSet<>());
		setStylesheetUrls(new HashSet<>());
		setMetadata(new HashSet<>());
		setFaviconUrl(new HashSet<>());
		setSrc("");
		setBrowser(BrowserType.CHROME);
		
	}

	/**
	 * Compares two images pixel by pixel.
	 *
	 * @param imgA
	 *            the first image.
	 * @param imgB
	 *            the second image.
	 * @return whether the images are both the same or not.
	 */
	public static boolean compareImages(BufferedImage imgA, BufferedImage imgB) {
		// The images must be the same size.
		if (imgA.getWidth() == imgB.getWidth() && imgA.getHeight() == imgB.getHeight()) {
			int width = imgA.getWidth();
			int height = imgA.getHeight();

			// Loop over every pixel.
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					// Compare the pixels for equality.
					if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
						return false;
					}
				}
			}
		} else {
			return false;
		}

		return true;
	}

	/**
	 * Checks if Pages are equal
	 *
	 * @param page
	 *            the {@link PageVersion} object to compare current page to
	 *
	 * @pre page != null
	 * @return boolean value
	 *
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof PageState))
			return false;

		PageState that = (PageState) o;
		
		return this.getKey().equals(that.getKey());
	}

	@JsonIgnore
	public List<ElementState> getElements() {
		return this.elements;
	}

	@JsonIgnore
	public void setElements(List<ElementState> elements) {
		this.elements = elements;
	}

	public void setLandable(boolean isLandable) {
		this.landable = isLandable;
	}

	public boolean isLandable() {
		return this.landable;
	}

	public void addElement(ElementState element) {
		this.elements.add(element);
	}	

	/**
	 * Generates page name using path
	 * 
	 * @return
	 * @throws MalformedURLException
	 */
	public String generatePageName(String url) {
		String name = "";

		try {
			String path = new URL(url).getPath().trim();
			path = path.replace("/", " ");
			path = path.trim();
			if("/".equals(path) || path.isEmpty()){
				path = "home";
			}
			name += path;
			
			return name.trim();
		} catch(MalformedURLException e){}
		
		return url;
	}
	
	/**
	 * 
	 * @param buff_img
	 * @return
	 * @throws IOException
	 */
	public static String getFileChecksum(BufferedImage buff_img) throws IOException {
		assert buff_img != null;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		boolean foundWriter = ImageIO.write(buff_img, "png", baos);
		assert foundWriter; // Not sure about this... with jpg it may work but
							// other formats ?
		// Get file input stream for reading the file content
		byte[] data = baos.toByteArray();
		try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			sha.update(data);
			byte[] thedigest = sha.digest(data);
			return Hex.encodeHexString(thedigest);
		} catch (NoSuchAlgorithmException e) {
			log.error("Error generating checksum of buffered image");
		}
		return "";

	}
	
	/**
	 * {@inheritDoc}
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 *
	 * @pre page != null
	 */
	public String generateKey() {
		
		return "pagestate";
	}

	public String getSrc() {
		return new String(Base64.getDecoder().decode(src));
	}

	public void setSrc(String src) {
		this.src = Base64.getEncoder().encodeToString(src.getBytes());
	}

	public long getScrollXOffset() {
		return scrollXOffset;
	}

	public void setScrollXOffset(long scrollXOffset) {
		this.scrollXOffset = scrollXOffset;
	}

	public long getScrollYOffset() {
		return scrollYOffset;
	}

	public void setScrollYOffset(long scrollYOffset) {
		this.scrollYOffset = scrollYOffset;
	}

	public String getViewportScreenshotUrl() {
		return viewportScreenshotUrl;
	}

	public void setViewportScreenshotUrl(String viewport_screenshot_url) {
		this.viewportScreenshotUrl = viewport_screenshot_url;
	}

	public BrowserType getBrowser() {
		return BrowserType.create(browser);
	}

	public void setBrowser(BrowserType browser) {
		this.browser = browser.toString();
	}

	public int getViewportWidth() {
		return viewportWidth;
	}

	public void setViewportWidth(int viewport_width) {
		this.viewportWidth = viewport_width;
	}

	public int getViewportHeight() {
		return viewportHeight;
	}

	public void setViewportHeight(int viewport_height) {
		this.viewportHeight = viewport_height;
	}

	public boolean isLoginRequired() {
		return loginRequired;
	}

	public void setLoginRequired(boolean login_required) {
		this.loginRequired = login_required;
	}
	
	public String getFullPageScreenshotUrlOnload() {
		return fullPageScreenshotUrlOnload;
	}

	public void setFullPageScreenshotUrlOnload(String full_page_screenshot_url) {
		this.fullPageScreenshotUrlOnload = full_page_screenshot_url;
	}

	public int getFullPageWidth() {
		return fullPageWidth;
	}
	
	public void setFullPageWidth(int full_page_width) {
		this.fullPageWidth = full_page_width;
	}
	
	public int getFullPageHeight() {
		return fullPageHeight;
	}

	public void setFullPageHeight(int full_page_height) {
		this.fullPageHeight = full_page_height;
	}

	public void addElements(List<ElementState> elements) {
		//check for duplicates before adding
		for(ElementState element : elements) {
			if(element != null && !this.elements.contains(element)) {				
				this.elements.add(element);
			}
		}
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPageName() {
		return pageName;
	}

	public void setPageName(String page_name) {
		this.pageName = page_name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Set<String> getScriptUrls() {
		return scriptUrls;
	}

	public void setScriptUrls(Set<String> script_urls) {
		this.scriptUrls = script_urls;
	}

	public Set<String> getStylesheetUrls() {
		return stylesheetUrls;
	}

	public void setStylesheetUrls(Set<String> stylesheet_urls) {
		this.stylesheetUrls = stylesheet_urls;
	}

	public Set<String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Set<String> metadata) {
		this.metadata = metadata;
	}

	public Set<String> getFaviconUrl() {
		return faviconUrl;
	}

	public void setFaviconUrl(Set<String> favicon_url) {
		this.faviconUrl = favicon_url;
	}

	public boolean isSecured() {
		return secured;
	}

	public void setSecured(boolean is_secure) {
		this.secured = is_secure;
	}

	public Set<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(Set<String> keywords) {
		this.keywords = keywords;
	}

	public int getHttpStatus() {
		return httpStatus;
	}

	public String getFullPageScreenshotUrlComposite() {
		return fullPageScreenshotUrlComposite;
	}

	public void setFullPageScreenshotUrlComposite(String full_page_screenshot_url_composite) {
		this.fullPageScreenshotUrlComposite = full_page_screenshot_url_composite;
	}

	public void setHttpStatus(int http_status) {
		this.httpStatus = http_status;
	}

	public String getUrlAfterLoading() {
		return urlAfterLoading;
	}

	public void setUrlAfterLoading(String url_after_loading) {
		this.urlAfterLoading = url_after_loading;
	}
	
	@Override
	public String toString() {
		return "(page => { key = "+getKey()+"; url = "+getUrl();
	}
}
