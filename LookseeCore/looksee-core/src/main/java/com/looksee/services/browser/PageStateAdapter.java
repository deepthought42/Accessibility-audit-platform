package com.looksee.services.browser;

import com.looksee.browser.Browser;
import com.looksee.browser.enums.BrowserType;
import com.looksee.exceptions.ServiceUnavailableException;
import com.looksee.gcp.GoogleCloudStorage;
import com.looksee.models.PageState;
import com.looksee.services.BrowserService;
import com.looksee.utils.BrowserUtils;
import com.looksee.browser.utils.HtmlUtils;
import com.looksee.utils.ImageUtils;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

/**
 * Adapter that converts a live {@link Browser} session into a
 * {@link PageState} domain object.
 *
 * <p>Unlike {@link ElementStateAdapter} and
 * {@link ImageElementStateAdapter}, this one is a Spring {@code @Service}
 * because it requires {@link GoogleCloudStorage} to upload the viewport
 * and full-page screenshots that {@link PageState} references.
 *
 * <p>It intentionally delegates HTML metadata extraction back to the
 * existing static helpers on {@link BrowserService}
 * ({@link BrowserService#extractMetadata(Document)} etc.), which are pure
 * parsing utilities kept on {@code BrowserService} for scope reasons.
 */
@Service
@ConditionalOnBean(GoogleCloudStorage.class)
public class PageStateAdapter {

	private static final Logger log = LoggerFactory.getLogger(PageStateAdapter.class);

	private final GoogleCloudStorage googleCloudStorage;

	@Autowired
	public PageStateAdapter(GoogleCloudStorage googleCloudStorage) {
		this.googleCloudStorage = googleCloudStorage;
	}

	/**
	 * Navigates the browser to {@code url}, captures metadata + screenshots,
	 * and builds a {@link PageState}. Screenshots are uploaded via
	 * {@link GoogleCloudStorage}.
	 *
	 * @param url the {@link URL} to navigate to (must not be null)
	 * @param browser the {@link Browser} session (must not be null)
	 * @param isSecure unused; {@link BrowserUtils#checkIfSecure(URL)} is
	 *                 used instead — kept for signature compatibility
	 * @param httpStatus unused; {@link BrowserUtils#getHttpStatus(URL)} is
	 *                   used instead — kept for signature compatibility
	 * @param audit_record_id the audit record id for the resulting
	 *                        {@link PageState}
	 *
	 * @return a populated {@link PageState}
	 * @throws WebDriverException if the underlying driver fails
	 * @throws IOException if screenshot upload fails
	 * @throws NullPointerException if any required value is null
	 */
	public PageState toPageState(
			URL url,
			Browser browser,
			boolean isSecure,
			int httpStatus,
			long audit_record_id
	) throws WebDriverException, IOException, NullPointerException {
		assert browser != null;
		assert url != null;

		browser.navigateTo(url.toString());
		browser.removeDriftChat();

		URL current_url = new URL(browser.getDriver().getCurrentUrl());
		String url_without_protocol = BrowserUtils.getPageUrl(current_url.toString());
		log.warn("building page state for URL :: "+current_url);

		boolean is_secure = BrowserUtils.checkIfSecure(current_url);
        int status_code = BrowserUtils.getHttpStatus(current_url);

        //scroll to bottom then back to top to make sure all elements that may be hidden until the page is scrolled
		String source = HtmlUtils.cleanSrc(browser.getDriver().getPageSource());

		if(HtmlUtils.is503Error(source)) {
			browser.close();
			throw new ServiceUnavailableException("503(Service Unavailable) Error encountered.");
		}
		Document html_doc = Jsoup.parse(source);
		Set<String> metadata = BrowserService.extractMetadata(html_doc);
		Set<String> stylesheets = BrowserService.extractStylesheets(html_doc);
		Set<String> script_urls =  BrowserService.extractScriptUrls(html_doc);
		Set<String> fav_icon_links = BrowserService.extractIconLinks(html_doc);

		String title = browser.getDriver().getTitle();

		BufferedImage viewport_screenshot = browser.getViewportScreenshot();
		String screenshot_checksum = ImageUtils.getChecksum(viewport_screenshot);
		String viewport_screenshot_url = googleCloudStorage.saveImage(viewport_screenshot,
																		current_url.getHost(),
																		screenshot_checksum,
																		BrowserType.create(browser.getBrowserName()));
		viewport_screenshot.flush();

		BufferedImage full_page_screenshot = browser.getFullPageScreenshotShutterbug();
		String full_page_screenshot_checksum = ImageUtils.getChecksum(full_page_screenshot);
		String full_page_screenshot_url = googleCloudStorage.saveImage(full_page_screenshot,
																		current_url.getHost(),
																		full_page_screenshot_checksum,
																		BrowserType.create(browser.getBrowserName()));
		full_page_screenshot.flush();

		long x_offset = browser.getXScrollOffset();
		long y_offset = browser.getYScrollOffset();
		Dimension size = browser.getDriver().manage().window().getSize();

		return new PageState(viewport_screenshot_url,
							source,
							x_offset,
							y_offset,
							size.getWidth(),
							size.getHeight(),
							BrowserType.CHROME,
							full_page_screenshot_url,
							full_page_screenshot.getWidth(),
							full_page_screenshot.getHeight(),
							url_without_protocol,
							title,
							is_secure,
							status_code,
							current_url.toString(),
							audit_record_id,
							metadata,
							stylesheets,
							script_urls,
							fav_icon_links);
	}

	/**
	 * Builds a {@link PageState} from an already-navigated {@link Browser}
	 * session whose current URL is {@code browser_url}.
	 *
	 * @param browser the {@link Browser} session (must not be null)
	 * @param audit_record_id the audit record id for the resulting
	 *                        {@link PageState}
	 * @param browser_url the current browser url (must not be null or empty)
	 *
	 * @return a populated {@link PageState}
	 * @throws WebDriverException if the underlying driver fails
	 * @throws IOException if screenshot upload fails
	 */
	public PageState toPageState(Browser browser, long audit_record_id, String browser_url) throws WebDriverException, IOException {
		assert browser != null;
		assert browser_url != null;
		assert !browser_url.isEmpty();

		//remove 3rd party chat apps such as drift, and ...(NB: fill in as more identified)

		URL current_url = new URL(browser_url);
		int status_code = BrowserUtils.getHttpStatus(current_url);
		String url_without_protocol = BrowserUtils.getPageUrl(current_url.toString());
		browser.removeDriftChat();
		browser.removeGDPRmodals();
		boolean is_secure = BrowserUtils.checkIfSecure(current_url);

		String source = HtmlUtils.cleanSrc(browser.getDriver().getPageSource());

		if(HtmlUtils.is503Error(source)) {
			throw new ServiceUnavailableException("503(Service Unavailable) Error encountered. Starting over..");
		}

		Document html_doc = Jsoup.parse(source);
		Set<String> metadata = BrowserService.extractMetadata(html_doc);
		Set<String> stylesheets = BrowserService.extractStylesheets(html_doc);
		Set<String> script_urls =  BrowserService.extractScriptUrls(html_doc);
		Set<String> fav_icon_links = BrowserService.extractIconLinks(html_doc);
		//PageState page_record = retrievePageFromDB(audit_record_id, url_without_protocol, source, BrowserType.CHROME);
		//if(page_record != null){
		//	return page_record;
		//}

        //scroll to bottom then back to top to make sure all elements that may be hidden until the page is scrolled
		String title = browser.getDriver().getTitle();

		BufferedImage viewport_screenshot = browser.getViewportScreenshot();
		String screenshot_checksum = ImageUtils.getChecksum(viewport_screenshot);

		BufferedImage full_page_screenshot = browser.getFullPageScreenshotShutterbug();
		String full_page_screenshot_checksum = ImageUtils.getChecksum(full_page_screenshot);

		String viewport_screenshot_url = googleCloudStorage.saveImage(viewport_screenshot,
																	current_url.getHost(),
																	screenshot_checksum,
																	BrowserType.create(browser.getBrowserName()));
		viewport_screenshot.flush();

		String full_page_screenshot_url = googleCloudStorage.saveImage(full_page_screenshot,
																	current_url.getHost(),
																	full_page_screenshot_checksum,
																	BrowserType.create(browser.getBrowserName()));
		full_page_screenshot.flush();

		long x_offset = browser.getXScrollOffset();
		long y_offset = browser.getYScrollOffset();
		Dimension size = browser.getDriver().manage().window().getSize();

		return new PageState(
							viewport_screenshot_url,
							source,
							x_offset,
							y_offset,
							size.getWidth(),
							size.getHeight(),
							BrowserType.CHROME,
							full_page_screenshot_url,
							full_page_screenshot.getWidth(),
							full_page_screenshot.getHeight(),
							url_without_protocol,
							title,
							is_secure,
							status_code,
							current_url.toString(),
							audit_record_id,
							metadata,
							stylesheets,
							script_urls,
							fav_icon_links);
	}

	/**
	 * Builds a {@link PageState} from a pre-captured screenshot and page source,
	 * without a live {@link Browser}. Used by the remote-mode one-shot capture
	 * flow in {@link BrowserService#capturePage(URL, BrowserType, long)}, where
	 * browser-service's {@code POST /v1/capture} returns both in a single
	 * round-trip.
	 *
	 * <p>Known simplification: only one screenshot is stored; it's uploaded as
	 * both the viewport and full-page screenshot of the resulting
	 * {@link PageState}. Splitting viewport from full-page in remote mode is a
	 * phase-3b enhancement.
	 */
	public PageState toPageState(byte[] screenshot,
								 String source,
								 long audit_record_id,
								 String browser_url,
								 BrowserType browser_type) throws IOException {
		assert screenshot != null && screenshot.length > 0;
		assert source != null;
		assert browser_url != null && !browser_url.isEmpty();
		assert browser_type != null;

		URL current_url = new URL(browser_url);
		int status_code = BrowserUtils.getHttpStatus(current_url);
		String url_without_protocol = BrowserUtils.getPageUrl(current_url.toString());
		boolean is_secure = BrowserUtils.checkIfSecure(current_url);

		String clean_source = HtmlUtils.cleanSrc(source);
		if (HtmlUtils.is503Error(clean_source)) {
			throw new ServiceUnavailableException("503(Service Unavailable) Error encountered.");
		}

		Document html_doc = Jsoup.parse(clean_source);
		Set<String> metadata = BrowserService.extractMetadata(html_doc);
		Set<String> stylesheets = BrowserService.extractStylesheets(html_doc);
		Set<String> script_urls = BrowserService.extractScriptUrls(html_doc);
		Set<String> fav_icon_links = BrowserService.extractIconLinks(html_doc);
		String title = html_doc.title();

		BufferedImage image;
		try (ByteArrayInputStream in = new ByteArrayInputStream(screenshot)) {
			image = javax.imageio.ImageIO.read(in);
		}
		if (image == null) {
			throw new IOException("remote capture returned bytes that are not a decodable image");
		}
		String checksum = ImageUtils.getChecksum(image);
		String screenshot_url = googleCloudStorage.saveImage(image,
				current_url.getHost(), checksum, browser_type);
		int width = image.getWidth();
		int height = image.getHeight();
		image.flush();

		return new PageState(
				screenshot_url,
				clean_source,
				0L,
				0L,
				width,
				height,
				browser_type,
				screenshot_url,
				width,
				height,
				url_without_protocol,
				title,
				is_secure,
				status_code,
				current_url.toString(),
				audit_record_id,
				metadata,
				stylesheets,
				script_urls,
				fav_icon_links);
	}
}
