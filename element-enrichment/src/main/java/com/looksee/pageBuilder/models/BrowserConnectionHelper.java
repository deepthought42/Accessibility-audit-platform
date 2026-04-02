package com.looksee.pageBuilder.models;

import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.looksee.pageBuilder.models.enums.BrowserEnvironment;
import com.looksee.pageBuilder.models.enums.BrowserType;

import io.github.resilience4j.retry.annotation.Retry;

@Retry(name="webdriver")
public class BrowserConnectionHelper {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(BrowserConnectionHelper.class);

	private static int SELENIUM_HUB_IDX = 0;
	//private static final String[] RESOURCE_HEAVY_REQUEST_HUB_IP_ADDRESS = {"selenium-standalone-pb1-uydih6tjpa-uc.a.run.app"};

	//GOOGLE CLOUD CLUSTER
	private static final String[] RESOURCE_HEAVY_REQUEST_HUB_IP_ADDRESS = {	"selenium-standalone-1-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-2-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-3-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-4-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-5-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-6-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-7-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-8-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-9-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-10-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-11-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-12-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-13-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-14-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-15-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-16-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-17-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-18-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-19-uydih6tjpa-uc.a.run.app",
																			"selenium-standalone-20-uydih6tjpa-uc.a.run.app"};
																			
	/**
	 * Creates a {@linkplain WebDriver} connection
	 * 
	 * @param browser
	 * @param environment
	 * 
	 * @return
	 * 
	 * @pre browser != null
	 * @pre environment != null
	 * 
	 * @throws MalformedURLException
	 */
    @Retry(name="webdriver")
	public static Browser getConnection(BrowserType browser, BrowserEnvironment environment)
			throws MalformedURLException
    {
		assert browser != null;
		assert environment != null;

		URL hub_url = null;
		if(environment.equals(BrowserEnvironment.DISCOVERY) && "chrome".equalsIgnoreCase(browser.toString())){
			hub_url = new URL( "https://"+RESOURCE_HEAVY_REQUEST_HUB_IP_ADDRESS[SELENIUM_HUB_IDX%RESOURCE_HEAVY_REQUEST_HUB_IP_ADDRESS.length]+"/wd/hub");
		}
		else if(environment.equals(BrowserEnvironment.DISCOVERY) && "firefox".equalsIgnoreCase(browser.toString())){
			hub_url = new URL( "https://"+RESOURCE_HEAVY_REQUEST_HUB_IP_ADDRESS[SELENIUM_HUB_IDX%RESOURCE_HEAVY_REQUEST_HUB_IP_ADDRESS.length]+"/wd/hub");
		}
		SELENIUM_HUB_IDX++;

		return new Browser(browser.toString(), hub_url);
	}
}
