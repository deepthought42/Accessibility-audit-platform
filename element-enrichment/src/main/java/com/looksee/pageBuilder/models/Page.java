package com.looksee.pageBuilder.models;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.Relationship;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.looksee.pageBuilder.services.BrowserService;

/**
 * A reference to a web page
 *
 */
public class Page extends LookseeObject {
	private static Logger log = LoggerFactory.getLogger(Page.class);

	private String url;
	private String path;
	private String src;
	private String title;
	
	@Relationship(type = "HAS")
	private List<Element> elements;
	
	@Relationship(type = "HAS")
	private List<PageState> pageStates;


	public Page() {
		super();
		setElements(new ArrayList<>());
		setPageStates(new ArrayList<>());
	}
	

	/**
	 * Creates a page instance that is meant to contain information about a
	 * state of a webpage
	 *
	 * @param url
	 * @param elements
	 * @param full_page_screenshot_url TODO
	 * @param full_page_checksum TODO
	 * @param title TODO
	 * @param screenshot
	 * @throws MalformedURLException
	 * @throws IOException
	 *
	 * @pre elements != null
	 * @pre screenshot_url != null;
	 */
	public Page(List<Element> elements, String src, String title, String url, String path)
	{
		super();
		assert elements != null;
		assert url != null;
		assert src != null;
		assert title != null;
		assert path != null;

		setElements(elements);
		setPageStates(new ArrayList<>());
		setUrl(url);
		setSrc( BrowserService.extractTemplate(Browser.cleanSrc(src)));
		setTitle(title);
		setPath(path);
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
	 *            the {@link Page} object to compare current page to
	 *
	 * @pre page != null
	 * @return boolean value
	 *
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Page))
			return false;

		Page that = (Page) o;
		
		return this.getKey().equals(that.getKey());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Page clone() {
		List<Element> elements = new ArrayList<Element>(getElements());

		Page page = new Page(elements, getSrc(), getTitle(), getUrl(), getPath());
		return page;
	}

	@JsonIgnore
	public List<Element> getElements() {
		return this.elements;
	}

	@JsonIgnore
	public void setElements(List<Element> elements) {
		this.elements = elements;
	}

	public void addElement(Element element) {
		this.elements.add(element);
	}

	public String getFileChecksum(MessageDigest digest, String url) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedImage buff_img = ImageIO.read(new URL(url));

		boolean foundWriter = ImageIO.write(buff_img, "png", baos);
		assert foundWriter; // Not sure about this... with jpg it may work but
							// other formats ?

		// Get file input stream for reading the file content
		byte[] data = baos.toByteArray();
		digest.update(data);
		byte[] thedigest = digest.digest(data);
		return Hex.encodeHexString(thedigest);
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
		String src_template = BrowserService.extractTemplate(getSrc());
		return "pagestate::" + org.apache.commons.codec.digest.DigestUtils.sha256Hex(src_template);
	}

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public void addElements(List<Element> elements) {
		//check for duplicates before adding
		for(Element element : elements) {
			if(!this.elements.contains(element)) {				
				this.elements.add(element);
			}
		}
	}

	public String getTitle() {
		return title;
	}

	public void setPageStates(List<PageState> page_states) {
		this.pageStates = page_states;
	}

	public List<PageState> getPageStates(){
		return this.pageStates;
	}
	
	public boolean addPageState(PageState page_state_record) {
		return this.pageStates.add(page_state_record);
	}
	
	public void setTitle(String title) {
		this.title = title;
	}


	public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
	}


	public String getPath() {
		return path;
	}


	public void setPath(String path) {
		this.path = path;
	}

}

