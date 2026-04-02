package com.looksee.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.looksee.models.enums.ElementClassification;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Represents the state of an image element in a web page, including its visual properties,
 * content analysis results, and safety classifications.
 *
 * <p><b>Class Invariants:</b>
 * <ul>
 *   <li>logos, labels, landmarkInfoSet, and faces sets are never null</li>
 *   <li>adult, racy, and violence fields contain safety classification strings when set</li>
 *   <li>inherits all invariants from ElementState parent class</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <ul>
 *   <li>Stores image analysis results including logos, landmarks, faces, and labels detected</li>
 *   <li>Tracks safety classifications for adult, racy and violent content</li>
 *   <li>Maintains relationships to related image analysis entities in the graph database</li>
 *   <li>Used for content moderation and image understanding features</li>
 * </ul>
 */
@Getter
@Setter
@Node
public class ImageElementState extends ElementState {
	
	@Relationship(type="HAS")
	private Set<Logo> logos;
	
	@Relationship(type="HAS")
	private Set<Label> labels;
	
	@Relationship(type="HAS")
	private Set<ImageLandmarkInfo> landmarkInfoSet;
	
	@Relationship(type="HAS")
	private Set<ImageFaceAnnotation> faces;
	
	@Relationship(type="HAS")
	private ImageSearchAnnotation imageSearchSet;
	
	private String adult;
	private String racy;
	private String violence;
	
	/**
	 * Constructs a new {@link ImageElementState}
	 */
	public ImageElementState() {
		super();
		this.logos = new HashSet<>();
		this.labels = new HashSet<>();
		this.landmarkInfoSet = new HashSet<>();
		this.faces = new HashSet<>();
		setImageFlagged(false);
	}
	
	/**
	 * Constructor for an image element state.
	 *
	 * @param owned_text the owned text of the element
	 * @param all_text the all text of the element
	 * @param xpath the xpath of the element
	 * @param tagName the tag name of the element
	 * @param attributes the attributes of the element
	 * @param rendered_css_values the rendered css values of the element
	 * @param screenshot_url the screenshot url of the element
	 * @param x the x coordinate of the element
	 * @param y the y coordinate of the element
	 * @param width the width of the element
	 * @param height the height of the element
	 * @param classification the classification of the element
	 * @param outer_html the outer html of the element
	 * @param css_selector the css selector of the element
	 * @param foreground_color the foreground color of the element
	 * @param background_color the background color of the element
	 * @param landmark_info_set the landmark info set of the element
	 * @param faces the faces of the element
	 * @param image_search the image search of the element
	 * @param logos the logos of the element
	 * @param labels the labels of the element
	 * @param safe_search_annotation the safe search annotation of the element
	 *
	 * precondition: xpath != null
	 * precondition: tagName != null
	 * precondition: outer_html != null
	 * precondition: css_selector != null
	 * precondition: classification != null
	 * precondition: image_search != null
	 * precondition: safe_search_annotation != null
	 */
	public ImageElementState(String owned_text,
							String all_text,
							String xpath,
							String tagName,
							Map<String, String> attributes,
							Map<String, String> rendered_css_values,
							String screenshot_url,
							int x,
							int y,
							int width,
							int height,
							ElementClassification classification,
							String outer_html,
							String css_selector,
							String foreground_color,
							String background_color,
							Set<ImageLandmarkInfo> landmark_info_set,
							Set<ImageFaceAnnotation> faces,
							ImageSearchAnnotation image_search,
							Set<Logo> logos,
							Set<Label> labels,
							ImageSafeSearchAnnotation safe_search_annotation
	) {
		super(owned_text,
				all_text,
				xpath,
				tagName,
				attributes,
				rendered_css_values,
				screenshot_url,
				x,
				y,
				width,
				height,
				classification,
				outer_html,
				css_selector,
				foreground_color,
				background_color,
				!image_search.getFullMatchingImages().isEmpty());

		assert xpath != null;
		assert tagName != null;
		assert outer_html != null;
		assert css_selector != null;
		assert classification != null;
		assert image_search != null;
		assert safe_search_annotation != null;

		setLandmarkInfoSet(landmark_info_set);
		setFaces(faces);
		setImageSearchSet(image_search);
		setLogos(logos);
		setLabels(labels);
		setAdult(safe_search_annotation.getAdult());
		setRacy(safe_search_annotation.getRacy());
		setViolence(safe_search_annotation.getViolence());
	}
	
	/**
	 * Constructor for an image element state.
	 *
	 * @param owned_text the owned text of the element
	 * @param all_text the all text of the element
	 * @param xpath the xpath of the element
	 * @param tagName the tag name of the element
	 * @param attributes the attributes of the element
	 * @param rendered_css_values the rendered css values of the element
	 * @param screenshot_url the screenshot url of the element
	 * @param x the x coordinate of the element
	 * @param y the y coordinate of the element
	 * @param width the width of the element
	 * @param height the height of the element
	 * @param classification the classification of the element
	 * @param outer_html the outer html of the element
	 * @param css_selector the css selector of the element
	 * @param foreground_color the foreground color of the element
	 * @param background_color the background color of the element
	 * @param landmark_info_set the landmark info set of the element
	 * @param faces the faces of the element
	 * @param image_search the image search of the element
	 * @param logos the logos of the element
	 * @param labels the labels of the element
	 *
	 * precondition: xpath != null
	 * precondition: tagName != null
	 * precondition: outer_html != null
	 * precondition: css_selector != null
	 * precondition: classification != null
	 */
	public ImageElementState(String owned_text,
							String all_text,
							String xpath,
							String tagName,
							Map<String, String> attributes,
							Map<String, String> rendered_css_values,
							String screenshot_url,
							int x,
							int y,
							int width,
							int height,
							ElementClassification classification,
							String outer_html,
							String css_selector,
							String foreground_color,
							String background_color,
							Set<ImageLandmarkInfo> landmark_info_set,
							Set<ImageFaceAnnotation> faces,
							ImageSearchAnnotation image_search,
							Set<Logo> logos,
							Set<Label> labels)
	{
		super(owned_text,
				all_text,
				xpath,
				tagName,
				attributes,
				rendered_css_values,
				screenshot_url,
				x,
				y,
				width,
				height,
				classification,
				outer_html,
				css_selector,
				foreground_color,
				background_color,
				false);

			assert xpath != null;
			assert tagName != null;
			assert outer_html != null;
			assert css_selector != null;
			assert classification != null;

			setLandmarkInfoSet(landmark_info_set);
			setFaces(faces);
			setImageSearchSet(image_search);
			setLogos(logos);
			setLabels(labels);
	}

	/**
	 * Checks if the image element state contains adult content.
	 *
	 * @return true if the image element state contains adult content, false otherwise
	 */
	@JsonIgnore
	public boolean isAdultContent() {
		if(getAdult() == null || getRacy() == null) {
			return false;
		}
		return getAdult().contains("LIKELY")
				|| getRacy().contains("LIKELY");
	}

	/**
	 * Checks if the image element state contains violent content.
	 *
	 * @return true if the image element state contains violent content, false otherwise
	 */
	@JsonIgnore
	public boolean isViolentContent() {
		return getViolence().contains("LIKELY");
	}
}