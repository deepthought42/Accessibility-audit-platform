package com.looksee.services.browser;

import com.looksee.gcp.ImageSafeSearchAnnotation;
import com.looksee.models.ElementState;
import com.looksee.models.ImageElementState;
import com.looksee.models.ImageFaceAnnotation;
import com.looksee.models.ImageLandmarkInfo;
import com.looksee.models.ImageSearchAnnotation;
import com.looksee.models.Label;
import com.looksee.models.Logo;
import com.looksee.models.enums.ElementClassification;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.jsoup.nodes.Element;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

/**
 * Adapter that converts raw browser output into an
 * {@link ImageElementState}. The image-specific variant of
 * {@link ElementStateAdapter}; pure functions with no dependencies on
 * repositories or Spring services.
 */
public final class ImageElementStateAdapter {

	private ImageElementStateAdapter() {
		// Static utility - do not instantiate
	}

	/**
	 * Constructs an {@link ImageElementState} from a Jsoup {@link Element},
	 * explicit size/location coordinates, and a set of image-annotation
	 * data.
	 *
	 * @param xpath the xpath of the element
	 * @param attributes the attributes of the element
	 * @param element the element
	 * @param classification the classification of the element
	 * @param rendered_css_values the rendered css values of the element
	 * @param screenshot_url the screenshot url of the element
	 * @param css_selector the css selector of the element
	 * @param landmark_info_set the landmark info set
	 * @param faces the faces
	 * @param image_search_set the image search set
	 * @param logos the logos
	 * @param labels the labels
	 * @param element_size the element size
	 * @param element_location the element location
	 *
	 * @return {@link ElementState} (actually an {@link ImageElementState})
	 * @throws IOException if an error occurs while building the element state
	 */
	public static ElementState toImageElementState(
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

		String background_color = rendered_css_values.get("background-color");
		if(background_color == null) {
			background_color = "rgb(255,255,255)";
		}

		String own_text = "";
		if(element != null && element.ownText() != null){
			own_text = element.ownText().trim();
		}
		ElementState element_state = new ImageElementState(
													own_text,
													element.text(),
													xpath,
													element.tagName(),
													attributes,
													rendered_css_values,
													screenshot_url,
													element_location.getX(),
													element_location.getY(),
													element_size.getWidth(),
													element_size.getHeight(),
													classification,
													element.outerHtml(),
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
	 * Constructs an {@link ImageElementState} from a Jsoup {@link Element},
	 * a Selenium {@link WebElement} (for location/size), and image-safety
	 * annotations.
	 *
	 * @param xpath the xpath of the element
	 * @param attributes the attributes of the element
	 * @param element the element
	 * @param web_elem the web element
	 * @param classification the classification of the element
	 * @param rendered_css_values the rendered css values of the element
	 * @param screenshot_url the screenshot url of the element
	 * @param css_selector the css selector of the element
	 * @param landmark_info_set the landmark info set
	 * @param faces the faces
	 * @param image_search_set the image search set
	 * @param logos the logos
	 * @param labels the labels
	 * @param safe_search_annotation the safe search annotation
	 *
	 * @return {@link ElementState} (actually an {@link ImageElementState})
	 * @throws IOException if an error occurs while building the element state
	 */
	public static ElementState toImageElementState(
			String xpath,
			Map<String, String> attributes,
			Element element,
			WebElement web_elem,
			ElementClassification classification,
			Map<String, String> rendered_css_values,
			String screenshot_url,
			String css_selector,
			Set<ImageLandmarkInfo> landmark_info_set,
			Set<ImageFaceAnnotation> faces,
			ImageSearchAnnotation image_search_set,
			Set<Logo> logos,
			Set<Label> labels,
			ImageSafeSearchAnnotation safe_search_annotation
	) throws IOException {
		assert xpath != null && !xpath.isEmpty();
		assert attributes != null;
		assert element != null;
		assert classification != null;
		assert rendered_css_values != null;
		assert screenshot_url != null;

		Point location = web_elem.getLocation();
		Dimension dimension = web_elem.getSize();

		String foreground_color = rendered_css_values.get("color");
		if(foreground_color == null || foreground_color.trim().isEmpty()) {
			foreground_color = "rgb(0,0,0)";
		}

		String background_color = rendered_css_values.get("background-color");
		if(background_color == null) {
			background_color = "rgb(255,255,255)";
		}

		// Convert gcp annotation to models annotation for ImageElementState constructor
		com.looksee.models.ImageSafeSearchAnnotation models_ssa = new com.looksee.models.ImageSafeSearchAnnotation(
			safe_search_annotation.getSpoof(),
			safe_search_annotation.getMedical(),
			safe_search_annotation.getAdult(),
			safe_search_annotation.getViolence(),
			safe_search_annotation.getRacy()
		);

		ElementState element_state = new ImageElementState(
													element.ownText().trim(),
													element.text(),
													xpath,
													element.tagName(),
													attributes,
													rendered_css_values,
													screenshot_url,
													location.getX(),
													location.getY(),
													dimension.getWidth(),
													dimension.getHeight(),
													classification,
													element.outerHtml(),
													css_selector,
													foreground_color,
													background_color,
													landmark_info_set,
													faces,
													image_search_set,
													logos,
													labels,
													models_ssa);

		return element_state;
	}
}
