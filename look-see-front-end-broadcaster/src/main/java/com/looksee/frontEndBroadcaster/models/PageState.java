package com.looksee.frontEndBroadcaster.models;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.looksee.frontEndBroadcaster.models.enums.BrowserType;
import com.looksee.utils.BrowserUtils;

import lombok.Getter;
import lombok.Setter;

/**
 * A reference to a web page
 *
 */
@Node
public class PageState extends LookseeObject {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PageState.class);

	@Getter
	@Setter
	private long auditRecordId;

	@Getter
	@Setter
	private String src;

	@Getter
	@Setter
	private String generalizedSrc;

	@Getter
	@Setter
	private String url;

	@Getter
	@Setter
	private String urlAfterLoading;

	@Getter
	@Setter
	private String viewportScreenshotUrl;

	@Getter
	@Setter
	private String fullPageScreenshotUrl;

	@Getter
	@Setter
	private String pageName;

	@Getter
	@Setter
	private BrowserType browser;

	@Getter
	@Setter
	private String title;

	@Getter
	@Setter
	private boolean loginRequired;

	@Getter
	@Setter
	private boolean secured;

	@Getter
	@Setter
	private boolean elementExtractionComplete;
	
	@Getter
	@Setter
	private long scrollXOffset;

	@Getter
	@Setter
	private long scrollYOffset;
	
	@Getter
	@Setter
	private int viewportWidth;

	@Getter
	@Setter
	private int viewportHeight;

	@Getter
	@Setter
	private int fullPageWidth;

	@Getter
	@Setter
	private int fullPageHeight;

	@Getter
	@Setter
	private int httpStatus;

	@Getter
	@Setter
	private Set<String> scriptUrls;

	@Getter
	@Setter
	private Set<String> stylesheetUrls;

	@Getter
	@Setter
	private Set<String> metadata;

	@Getter
	@Setter
	private Set<String> faviconUrl;

	@Getter
	@Setter
	private Set<String> keywords;
	
	@Getter
	@Setter
	@JsonIgnore
	@Relationship(type = "HAS", direction = Direction.OUTGOING)
	private List<ElementState> elements;


	public PageState() {
		super();
		setKeywords(new HashSet<>());
		setScriptUrls(new HashSet<>());
		setStylesheetUrls(new HashSet<>());
		setMetadata(new HashSet<>());
		setFaviconUrl(new HashSet<>());
		setBrowser(BrowserType.CHROME);
		setElementExtractionComplete(false);
		setAuditRecordId(-1L);
	}
	
	/**
	 * 	 Constructor
	 * 
	 * @param screenshot_url
	 * @param src
	 * @param isLandable
	 * @param scroll_x_offset
	 * @param scroll_y_offset
	 * @param viewport_width
	 * @param viewport_height
	 * @param browser_type
	 * @param full_page_screenshot_url
	 * @param full_page_width TODO
	 * @param full_page_height TODO
	 * @param url
	 * @param title TODO
	 * @param is_secure TODO
	 * @param http_status_code TODO
	 * @param url_after_page_load TODO
	 * @throws MalformedURLException 
	 */
	public PageState(String screenshot_url, 
					String src,
					long scroll_x_offset, 
					long scroll_y_offset, 
					int viewport_width,
					int viewport_height, 
					BrowserType browser_type, 
					String full_page_screenshot_url, 
					int full_page_width,
					int full_page_height, 
					String url, 
					String title, 
					boolean is_secure, 
					int http_status_code, 
					String url_after_page_load,
					long audit_record_id,
					Set<String> metadata,
					Set<String> stylesheets,
					Set<String> script_urls,
					Set<String> icon_links
	) {
		assert screenshot_url != null;
		assert src != null;
		assert !src.isEmpty();
		assert browser_type != null;
		assert full_page_screenshot_url != null;
		assert url != null;
		assert !url.isEmpty();

		setViewportScreenshotUrl(screenshot_url);
		setViewportWidth(viewport_width);
		setViewportHeight(viewport_height);
		setBrowser(browser_type);
		setSrc(src);
		setScrollXOffset(scroll_x_offset);
		setScrollYOffset(scroll_y_offset);
	    setLoginRequired(false);
		setFullPageScreenshotUrl(full_page_screenshot_url);
		setFullPageWidth(full_page_width);
		setFullPageHeight(full_page_height);
		setUrl(url);
		setUrlAfterLoading(url_after_page_load);
		setTitle(title);
		setSecured(is_secure);
		setHttpStatus(http_status_code);
		setPageName( generatePageName(getUrl()) );
		setMetadata( metadata );
		setStylesheetUrls( stylesheets);
		setScriptUrls( script_urls);
		setFaviconUrl(icon_links);
		setKeywords(new HashSet<>());
		setElementExtractionComplete(false);
		setAuditRecordId(audit_record_id);
		setGeneralizedSrc(BrowserUtils.generalizeSrc(src));
		setKey(generateKey());
	}

	/**
	 * Gets counts for all tags based on {@link Element}s passed
	 *
	 * @param page_elements
	 *            list of {@link Element}s
	 *
	 * @return Hash of counts for all tag names in list of {@ElementState}s
	 *         passed
	 */
	public Map<String, Integer> countTags(Set<Element> tags) {
		Map<String, Integer> elem_cnts = new HashMap<String, Integer>();
		for (Element tag : tags) {
			if (elem_cnts.containsKey(tag.getName())) {
				int cnt = elem_cnts.get(tag.getName());
				cnt += 1;
				elem_cnts.put(tag.getName(), cnt);
			} else {
				elem_cnts.put(tag.getName(), 1);
			}
		}
		return elem_cnts;
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PageState clone() {
		List<ElementState> elements = new ArrayList<ElementState>(getElements());
		PageState page = new PageState(getViewportScreenshotUrl(), 
							 getSrc(), 
							 getScrollXOffset(), 
							 getScrollYOffset(), 
							 getViewportWidth(), 
							 getViewportHeight(), 
							 getBrowser(), 
							 getFullPageScreenshotUrl(), 
							 getFullPageWidth(), 
							 getFullPageHeight(), 
							 getUrl(), 
							 getTitle(),
							 isSecured(),
							 getHttpStatus(),
							 getUrlAfterLoading(),
							 getAuditRecordId(),
							 getMetadata(),
							 getStylesheetUrls(),
							 getScriptUrls(),
							 getFaviconUrl());
		page.setElements(elements);
		return page;
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
		return "pagestate" + getAuditRecordId()+ org.apache.commons.codec.digest.DigestUtils.sha256Hex( getUrl() + getGeneralizedSrc() +getBrowser());
	}

	public void addElements(List<ElementState> elements) {
		//check for duplicates before adding
		for(ElementState element : elements) {
			if(element != null && !this.elements.contains(element)) {
				this.elements.add(element);
			}
		}
	}

	@Override
	public String toString() {
		return "(page => { key = "+getKey()+"; url = "+getUrl();
	}
}
