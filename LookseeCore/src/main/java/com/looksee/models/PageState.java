package com.looksee.models;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.looksee.gcp.GoogleCloudStorage;
import com.looksee.models.enums.BrowserType;
import com.looksee.services.BrowserService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

/**
 * A reference to a web page that stores its state and content
 *
 * Invariants:
 * - url is not null
 * - auditRecordId >= 0
 * - browser is not null
 * - elements list is not null
 * - all elements in elements list are not null
 * - src is not null if page has content
 */
@Getter
@Setter
@Node
public class PageState extends LookseeObject {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(PageState.class);

	@Autowired
	private GoogleCloudStorage googleCloudStorage;

	private long auditRecordId;
	private String src;
	private String generalizedSrc;
	private String url;
	private String urlAfterLoading;
	private String viewportScreenshotUrl;
	private String fullPageScreenshotUrl;
	private String pageName;
	private BrowserType browser;
	private String title;
	private boolean loginRequired;
	private boolean secured;
	private boolean elementExtractionComplete;
	private boolean interactiveElementExtractionComplete;
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

	@JsonIgnore
	@Relationship(type = "HAS", direction = Direction.OUTGOING)
	private List<ElementState> elements;

	
	/**
	 * Default constructor for PageState
	 */
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
	 * Constructor for PageState
	 *
	 * @param screenshot_url the url of the screenshot
	 * @param src the source url
	 * @param scroll_x_offset the x offset of the scroll
	 * @param scroll_y_offset the y offset of the scroll
	 * @param viewport_width the width of the viewport
	 * @param viewport_height the height of the viewport
	 * @param browser_type the browser type
	 * @param full_page_screenshot_url the url of the full page screenshot
	 * @param full_page_width the width of the full page
	 * @param full_page_height the height of the full page
	 * @param url the url of the page
	 * @param title the title of the page
	 * @param is_secure whether the page is secure
	 * @param http_status_code the http status code
	 * @param url_after_page_load the url after page load
	 * @param audit_record_id the audit record id
	 * @param metadata the metadata
	 * @param stylesheets the stylesheets
	 * @param script_urls the script urls
	 * @param icon_links the icon links
	 *
	 * @throws MalformedURLException if the url is malformed
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
	) throws IOException {
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
		setInteractiveElementExtractionComplete(false);
		setElementExtractionComplete(false);
		setKeywords(new HashSet<>());
		setAuditRecordId(audit_record_id);
		setGeneralizedSrc(BrowserService.generalizeSrc(src));
		setKey(generateKey());
	}

	/**
	 * Compares two images pixel by pixel.
	 *
	 * @param imgA the first image
	 * @param imgB the second image
	 * @return whether the images are both the same or not.
	 *
	 * precondition: imgA != null
	 * precondition: imgB != null
	 */
	public static boolean compareImages(BufferedImage imgA, BufferedImage imgB) {
		assert imgA != null;
		assert imgB != null;
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
	 * @param o the object to compare to
	 * @return true if the pages are equal, false otherwise
	 *
	 * precondition: o != null
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
		try {
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
		} catch (IOException e) {
			throw new RuntimeException("Failed to clone PageState", e);
		}
	}

	/**
	 * Adds an element to the page state
	 *
	 * @param element the element to add
	 *
	 * precondition: element != null
	 */
	public void addElement(ElementState element) {
		assert element != null;

		this.elements.add(element);
	}

	/**
	 * Generates page name using path
	 *
	 * @param url the url of the page
	 * @return the page name
	 *
	 * precondition: url != null
	 */
	public String generatePageName(String url) {
		assert url != null;
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
	 * Generates a checksum for a buffered image
	 *
	 * @param buff_img buffered image
	 * @return checksum of the buffered image
	 *
	 * precondition: buff_img != null
	 * @throws IOException if the buffered image is not found
	 * @throws NoSuchAlgorithmException if the algorithm is not found
	 */
	public static String getFileChecksum(BufferedImage buff_img) throws IOException, NoSuchAlgorithmException {
		assert buff_img != null;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		boolean foundWriter = ImageIO.write(buff_img, "png", baos);
		assert foundWriter; // Not sure about this... with jpg it may work but
							// other formats ?
		// Get file input stream for reading the file content
		byte[] data = baos.toByteArray();
		MessageDigest sha = MessageDigest.getInstance("SHA-256");
		sha.update(data);
		byte[] thedigest = sha.digest(data);
		return Hex.encodeHexString(thedigest);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String generateKey() {
		return "pagestate" + getAuditRecordId()+ org.apache.commons.codec.digest.DigestUtils.sha256Hex( getUrl() + getGeneralizedSrc() +getBrowser());
	}

	/**
	 * Adds a list of {@link ElementState}s to the {@link PageState}
	 *
	 * @param elements list of {@link ElementState}s
	 *
	 * precondition: elements != null
	 */
	public void addElements(List<ElementState> elements) {
		assert elements != null;

		//check for duplicates before adding
		for(ElementState element : elements) {
			if(element != null && !this.elements.contains(element)) {
				this.elements.add(element);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * @return string representation of {@link PageState}
	 */
	public String toString() {
		return "(page => { key = "+getKey()+"; url = "+getUrl();
	}

	/**
	 * Custom getter and setter for src
	 *
	 * @return src
	 */
	public String getSrc() {
		return googleCloudStorage.getHtmlContent(src);
	}

	/**
	 * Custom setter for src
	 *
	 * @param src src
	 * @throws IOException if the src is not found
	 */
	public void setSrc(String src) throws IOException {
		String path_key = BrowserService.extractHost(this.getUrl()) + "/pages/" + getKey();
		this.src = googleCloudStorage.uploadHtmlContent(src, path_key);
	}
}