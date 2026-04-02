package com.looksee.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.looksee.utils.HtmlGeneralizer;
import com.looksee.utils.HtmlUtils;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * A reference to a web page that contains information about the page's state,
 * including its URL, title, source code, and associated elements.
 *
 * invariant: elements != null
 * invariant: pageStates != null
 */
@Getter
@Setter
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

	/**
	 * Default constructor that initializes empty lists for elements and page states.
	 */
	public Page() {
		super();
		setElements(new ArrayList<>());
		setPageStates(new ArrayList<>());
	}
	

	/**
	 * Creates a page instance that contains information about a state of a webpage.
	 *
	 * @param elements the list of elements on the page
	 * @param src the source code of the page
	 * @param title the title of the page
	 * @param url the URL of the page
	 * @param path the file path of the page
	 * @throws IllegalArgumentException if any parameter is null
	 *
	 * precondition: elements != null
	 * precondition: url != null
	 * precondition: src != null
	 * precondition: title != null
	 * precondition: path != null
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
		setSrc( HtmlGeneralizer.extractTemplate(HtmlUtils.cleanSrc(src)));
		setTitle(title);
		setPath(path);
		setKey(generateKey());
	}
	

	/**
	 * Gets counts for all tags based on the provided elements.
	 *
	 * @param tags the set of elements to count
	 * @return a map containing counts for all tag names in the provided elements
	 *
	 * precondition: tags != null
	 */
	public Map<String, Integer> countTags(Set<Element> tags) {
		assert tags != null;
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
	 * @param imgA the first image
	 * @param imgB the second image
	 * @return true if the images are identical, false otherwise
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
	 * Checks if this page is equal to another object.
	 *
	 * @param o the object to compare with
	 * @return true if the objects are equal, false otherwise
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
	 * Creates a deep copy of this page.
	 *
	 * @return a new Page instance with the same data
	 */
	@Override
	public Page clone() {
		List<Element> elements = new ArrayList<Element>(getElements());

		Page page = new Page(elements, getSrc(), getTitle(), getUrl(), getPath());
		return page;
	}

	/**
	 * Gets the list of elements on this page.
	 *
	 * @return the list of elements
	 */
	@JsonIgnore
	public List<Element> getElements() {
		return this.elements;
	}

	/**
	 * Sets the list of elements for this page.
	 *
	 * @param elements the list of elements to set
	 */
	@JsonIgnore
	public void setElements(List<Element> elements) {
		this.elements = elements;
	}

	/**
	 * Adds a single element to this page.
	 *
	 * @param element the element to add
	 *
	 * precondition: element != null
	 */
	public void addElement(Element element) {
		assert element != null;
		this.elements.add(element);
	}

	/**
	 * Generates a checksum for an image from a URL.
	 *
	 * @param digest the message digest algorithm to use
	 * @param url the URL of the image
	 * @return the hexadecimal string representation of the checksum
	 * @throws IOException if there is an error reading the image
	 *
	 * precondition: digest != null
	 * precondition: url != null
	 */
	public String getFileChecksum(MessageDigest digest, String url) throws IOException {
		assert digest != null;
		assert url != null;
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
	 * Generates a SHA-256 checksum for a buffered image.
	 *
	 * @param buff_img the buffered image to generate checksum for
	 * @return the hexadecimal string representation of the SHA-256 checksum
	 * @throws IOException if there is an error processing the image
	 *
	 * precondition: buff_img != null
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
	 * Generates a unique key for this page based on its source template.
	 *
	 * @return a unique key string for this page
	 */
	public String generateKey() {
		String src_template = HtmlGeneralizer.extractTemplate(getSrc());
		return "pagestate::" + org.apache.commons.codec.digest.DigestUtils.sha256Hex(src_template);
	}

	/**
	 * Adds multiple elements to this page, avoiding duplicates.
	 *
	 * @param elements the list of elements to add
	 *
	 * precondition: elements != null
	 */
	public void addElements(List<Element> elements) {
		assert elements != null;
		//check for duplicates before adding
		for(Element element : elements) {
			if(!this.elements.contains(element)) {
				this.elements.add(element);
			}
		}
	}
	
	/**
	 * Adds a page state to this page.
	 *
	 * @param page_state_record the page state to add
	 * @return true if the page state was added successfully
	 *
	 * precondition: page_state_record != null
	 */
	public boolean addPageState(PageState page_state_record) {
		assert page_state_record != null;
		return this.pageStates.add(page_state_record);
	}
}

