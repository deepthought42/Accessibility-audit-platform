package com.looksee.services.browser;

import com.looksee.models.ElementState;
import com.looksee.models.enums.ElementClassification;
import java.io.IOException;
import java.util.Map;
import org.jsoup.nodes.Element;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

/**
 * Adapter that converts raw browser output (Jsoup {@link Element} plus
 * Selenium {@link Dimension}/{@link Point}/{@link WebElement}) into a
 * domain {@link ElementState}. This is the harness that sits between the
 * standalone {@code looksee-browser} module and the {@code looksee-models}
 * domain types; the browser module itself has no knowledge of
 * {@link ElementState}.
 *
 * <p>Both overloads here are pure functions with no dependencies on
 * repositories, Spring services, or external resources.
 */
public final class ElementStateAdapter {

	private ElementStateAdapter() {
		// Static utility - do not instantiate
	}

	/**
	 * Constructs an {@link ElementState} from a Jsoup {@link Element} and
	 * explicit size/location coordinates.
	 *
	 * @param xpath the xpath (must not be null or empty)
	 * @param attributes the attributes (must not be null)
	 * @param element the element (must not be null)
	 * @param classification the classification (must not be null)
	 * @param rendered_css_values the rendered css values (must not be null)
	 * @param screenshot_url the screenshot url
	 * @param css_selector the css selector
	 * @param element_size the element size
	 * @param element_location the element location
	 *
	 * @return {@link ElementState} based on the supplied data
	 * @throws IOException if an error occurs while reading the screenshot
	 */
	public static ElementState toElementState(
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

		ElementState element_state = new ElementState(
											element.ownText().trim(),
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
											rendered_css_values.get("background-color"),
											false);

		return element_state;
	}

	/**
	 * Constructs an {@link ElementState} from a Jsoup {@link Element} and a
	 * Selenium {@link WebElement} (from which size and location are
	 * derived).
	 *
	 * @param xpath the xpath (must not be null or empty)
	 * @param attributes the attributes (must not be null)
	 * @param element the element (must not be null)
	 * @param web_elem the web element to pull location/size from
	 * @param classification the classification (must not be null)
	 * @param rendered_css_values the rendered css values (must not be null)
	 * @param screenshot_url the screenshot url (must not be null)
	 * @param css_selector the css selector
	 *
	 * @return {@link ElementState} based on the supplied data
	 * @throws IOException if an error occurs while building the element state
	 */
	public static ElementState toElementState(
			String xpath,
			Map<String, String> attributes,
			Element element,
			WebElement web_elem,
			ElementClassification classification,
			Map<String, String> rendered_css_values,
			String screenshot_url,
			String css_selector
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

		ElementState element_state = new ElementState(
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
											rendered_css_values.get("background-color"),
											false);

		return element_state;
	}
}
