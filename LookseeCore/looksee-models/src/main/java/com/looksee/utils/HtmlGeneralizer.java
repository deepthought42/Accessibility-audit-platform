package com.looksee.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pure utility methods for generalizing and normalizing HTML content.
 *
 * These methods were extracted from BrowserService to break the circular
 * dependency between model classes (PageState, ElementState, Page) and
 * the service layer.
 */
public final class HtmlGeneralizer {
	private static final Logger log = LoggerFactory.getLogger(HtmlGeneralizer.class);

	private HtmlGeneralizer() {
		// Utility class - prevent instantiation
	}

	/**
	 * Generalizes HTML source by removing scripts, links, styles, iframes,
	 * GDPR elements, all attributes, and comments. Collapses whitespace.
	 *
	 * @param src the HTML source to generalize (must not be null)
	 * @return the generalized HTML string
	 */
	public static String generalizeSrc(String src) {
		assert src != null;

		if (src.isEmpty()) {
			return "";
		}

		Document html_doc = Jsoup.parse(src);
		html_doc.select("script").remove();
		html_doc.select("link").remove();
		html_doc.select("style").remove();
		html_doc.select("iframe").remove();
		html_doc.select("#gdpr").remove();
		html_doc.select("#gdprModal").remove();

		for (Element element : html_doc.getAllElements()) {
			List<String> attToRemove = new ArrayList<>();
			for (Attribute a : element.attributes()) {
				attToRemove.add(a.getKey());
			}

			for (String att : attToRemove) {
				element.removeAttr(att);
			}
		}

		removeComments(html_doc);

		return html_doc.html().replace("\n", "")
							.replace("\t", "")
							.replace("  ", "")
							.replace(" ", "")
							.replace("> <", "><");
	}

	/**
	 * Removes HTML comments from an HTML string.
	 *
	 * @param html the HTML string to remove comments from (must not be null)
	 * @return HTML string without comments
	 */
	public static String removeComments(String html) {
		assert html != null;

		return Pattern.compile("<!--.*?-->").matcher(html).replaceAll("");
	}

	/**
	 * Removes HTML comments from an {@link Element} and its children.
	 *
	 * @param e the element (must not be null)
	 */
	public static void removeComments(Element e) {
		assert e != null;

		e.childNodes().stream()
			.filter(n -> n.nodeName().equals("#comment"))
			.collect(Collectors.toList())
			.forEach(n -> n.remove());
		e.children().forEach(elem -> removeComments(elem));
	}

	/**
	 * Extracts a template from HTML by cleaning it and removing
	 * id, name, and style attributes.
	 *
	 * @param outerHtml the outer HTML to extract a template from (must not be null or empty)
	 * @return templated version of element HTML
	 */
	public static String extractTemplate(String outerHtml) {
		assert outerHtml != null;
		assert !outerHtml.isEmpty();

		Document html_doc = Jsoup.parseBodyFragment(outerHtml);

		Cleaner cleaner = new Cleaner(Safelist.relaxed());
		html_doc = cleaner.clean(html_doc);

		html_doc.select("script").remove()
				.select("link").remove()
				.select("style").remove();

		for (Element element : html_doc.getAllElements()) {
			element.removeAttr("id");
			element.removeAttr("name");
			element.removeAttr("style");
		}

		return html_doc.html();
	}

	/**
	 * Removes scripts, styles, links, and meta tags from HTML source
	 * and normalizes whitespace.
	 *
	 * @param src the source code to clean (must not be null)
	 * @return the cleaned source code
	 */
	public static String cleanSrc(String src) {
		assert src != null;

		Document html_doc = Jsoup.parse(src);
		html_doc.select("script").remove();
		html_doc.select("style").remove();
		html_doc.select("link").remove();
		html_doc.select("meta").remove();

		String html = html_doc.html();
		html = html.replace("\r", "");
		html = html.replace("\n", "");
		html = html.replace("\t", " ");
		html = html.replace("  ", " ");
		html = html.replace("  ", " ");
		html = html.replace("  ", " ");

		return html.replace(" style=\"\"", "");
	}

	/**
	 * Extracts the host from a URL string.
	 *
	 * @param urlString the URL string (must not be null or empty)
	 * @return the host portion of the URL, or empty string if malformed
	 */
	public static String extractHost(String urlString) {
		assert urlString != null;
		assert !urlString.isEmpty();

		try {
			URL url = new URL(urlString);
			return url.getHost();
		} catch (MalformedURLException e) {
			log.warn("Failed to extract host from URL: " + urlString);
			return "";
		}
	}
}
