package com.looksee.browser.helpers;

import com.looksee.browser.BrowserFactory;
import com.looksee.browser.MobileFactory;
import com.looksee.browser.config.BrowserStackProperties;
import com.looksee.browser.Browser;
import com.looksee.browser.MobileDevice;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import java.net.MalformedURLException;
import java.net.URL;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class for creating a {@link Browser} connection.
 *
 * <p>Retries are intentionally <em>not</em> wired into this helper: this
 * class is framework-agnostic plain Java, and the retry annotation that
 * used to live here depended on Spring AOP. Callers that need retries
 * should wrap the static methods in their own retry logic (resilience4j,
 * Failsafe, a hand-rolled loop, etc.).
 */
@NoArgsConstructor
public class BrowserConnectionHelper {
	/**
	 * The logger for the {@link BrowserConnectionHelper} class
	 */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(BrowserConnectionHelper.class);
	
	/**
	 * The index of the selenium hub
	 */
	private static int SELENIUM_HUB_IDX = 0;

	private static String[] HUB_URLS;

	/**
	 * The index of the Appium server for round-robin selection
	 */
	private static int APPIUM_SERVER_IDX = 0;

	private static String[] APPIUM_URLS;

	/**
	 * Whether BrowserStack is enabled as the connection provider
	 */
	private static boolean browserStackEnabled = false;

	/**
	 * The BrowserStack hub URL
	 */
	private static String browserStackHubUrl = null;

	/**
	 * The BrowserStack configuration properties
	 */
	private static BrowserStackProperties browserStackProperties = null;

	/**
	 * Gets the selenium hub URLs, either from environment variable SELENIUM_URLS or fallback to hardcoded list
	 * @param urls the selenium hub URLs
	 *
	 * precondition: urls != null
	 */
	public static void setConfiguredSeleniumUrls(String[] urls) {
		assert urls != null;
		HUB_URLS=urls;
	}

	/**
	 * Sets the Appium server URLs for mobile driver connections
	 * @param urls the Appium server URLs
	 *
	 * precondition: urls != null
	 */
	public static void setConfiguredAppiumUrls(String[] urls) {
		assert urls != null;
		APPIUM_URLS = urls;
	}

	/**
	 * Configures BrowserStack as the connection provider.
	 * When set, {@link #getConnection} will use BrowserStack instead of the
	 * default Selenium URL-based round-robin.
	 *
	 * @param hubUrl the BrowserStack hub URL
	 * @param properties the BrowserStack configuration properties
	 *
	 * precondition: hubUrl != null
	 * precondition: properties != null
	 */
	public static void setBrowserStackConfig(String hubUrl, BrowserStackProperties properties) {
		assert hubUrl != null;
		assert properties != null;

		browserStackHubUrl = hubUrl;
		browserStackProperties = properties;
		browserStackEnabled = true;
	}

	/**
	 * Clears the BrowserStack configuration, reverting to the default
	 * Selenium URL-based connection.
	 */
	public static void clearBrowserStackConfig() {
		browserStackEnabled = false;
		browserStackHubUrl = null;
		browserStackProperties = null;
	}

	/**
	 * Creates a {@link Browser} connection
	 *
	 * @param browser the browser to connect to
	 * @param environment the environment to connect to
	 *
	 * @return the browser connection
	 *
	 * precondition: browser != null
	 * precondition: environment != null
	 *
	 * @throws MalformedURLException if the url is malformed
	 */
	public static Browser getConnection(BrowserType browser, BrowserEnvironment environment)
			throws MalformedURLException
    {
		assert browser != null;
		assert environment != null;

		if (browserStackEnabled) {
			URL server_url = new URL(browserStackHubUrl);
			return BrowserFactory.createBrowserStackBrowser(browser.toString(), server_url, browserStackProperties);
		}

		URL server_url = null;

		if (environment.equals(BrowserEnvironment.DISCOVERY)
				&& ("chrome".equalsIgnoreCase(browser.toString())
					|| "firefox".equalsIgnoreCase(browser.toString()))) {
			String entry = HUB_URLS[SELENIUM_HUB_IDX % HUB_URLS.length];
			// Accept either a bare `host:port` (production form, prefixed with
			// https:// and /wd/hub) or a fully qualified URL such as
			// `http://selenium-chrome:4444/wd/hub` (used by the local docker
			// compose stack, where the standalone-chrome image serves plain
			// HTTP). Existing prod configs that pass `host:port` are unaffected.
			if (entry.startsWith("http://") || entry.startsWith("https://")) {
				server_url = new URL(entry.endsWith("/wd/hub") ? entry : entry + "/wd/hub");
			} else {
				server_url = new URL("https://" + entry + "/wd/hub");
			}
			SELENIUM_HUB_IDX++;
		}

		return BrowserFactory.createBrowser(browser.toString(), server_url);
	}

	/**
	 * Creates a {@link MobileDevice} connection via Appium
	 *
	 * @param browser the mobile browser type (ANDROID, IOS)
	 * @param environment the environment to connect to
	 *
	 * @return the mobile device connection
	 *
	 * precondition: browser != null
	 * precondition: browser.isMobile()
	 * precondition: environment != null
	 *
	 * @throws MalformedURLException if the url is malformed
	 * @throws IllegalStateException if Appium URLs are not configured
	 */
	public static MobileDevice getMobileConnection(BrowserType browser, BrowserEnvironment environment)
			throws MalformedURLException
    {
		assert browser != null;
		assert browser.isMobile();
		assert environment != null;

		if (browserStackEnabled) {
			URL server_url = new URL(browserStackHubUrl);
			return MobileFactory.createBrowserStackMobileDevice(browser.toString(), server_url, browserStackProperties);
		}

		if (APPIUM_URLS == null || APPIUM_URLS.length == 0) {
			throw new IllegalStateException(
				"Appium URLs not configured. Set appium.urls property.");
		}

		String appiumEntry = APPIUM_URLS[APPIUM_SERVER_IDX % APPIUM_URLS.length];
		// Mirror the desktop-browser path: accept either a bare `host:port`
		// (production form, prefixed with http:// and /wd/hub) or a fully
		// qualified URL such as `http://appium:4723/wd/hub` (used by the local
		// docker compose stack). Existing prod configs that pass `host:port`
		// are unaffected.
		URL server_url;
		if (appiumEntry.startsWith("http://") || appiumEntry.startsWith("https://")) {
			server_url = new URL(appiumEntry.endsWith("/wd/hub") ? appiumEntry : appiumEntry + "/wd/hub");
		} else {
			server_url = new URL("http://" + appiumEntry + "/wd/hub");
		}
		APPIUM_SERVER_IDX++;

		return MobileFactory.createMobileDevice(browser.toString(), server_url);
	}
}
