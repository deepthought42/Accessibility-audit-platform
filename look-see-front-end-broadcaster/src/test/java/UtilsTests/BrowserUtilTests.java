package UtilsTests;

import static org.junit.Assert.*;

import org.junit.Test;

import com.looksee.utils.BrowserUtils;

public class BrowserUtilTests {

	@Test
	public void testIsExternal() {
		String url = "bootcamp.uxdesign.cc";
		boolean is_external = BrowserUtils.isExternalLink("look-see.com", url);
		System.out.println("is external = "+is_external);
		assertTrue(is_external);
		
		String url2 = "en.wikipedia.org";
		boolean is_external2 = BrowserUtils.isExternalLink("look-see.com", url2);
		assertTrue(is_external2);

		String url3 = "shopify.dev/docs/api/customer/unstable/mutations/subscriptionContractPause";
		boolean is_external3 = BrowserUtils.isExternalLink("look-see.com", url3);
		System.out.println("is external3 = "+is_external3);
		assertTrue(is_external3);

		String url4 = "sciencedirect.com/topics/computer-science/cortical-area";
		boolean is_external4 = BrowserUtils.isExternalLink("look-see.com", url4);
		System.out.println("is external4 = "+is_external4);
		assertTrue(is_external4);
		
	}

	@Test
	public void testIsRelative() {
		String url = "bootcamp.uxdesign.cc";
		boolean is_relative = BrowserUtils.isRelativeLink("look-see.com", url);
		assertFalse(is_relative);
		
		String url2 = "en.wikipedia.org";
		boolean is_relative2 = BrowserUtils.isRelativeLink("look-see.com", url2);
		assertFalse(is_relative2);

		String url3 = "shopify.dev/docs/api/customer/unstable/mutations/subscriptionContractPause";
		boolean is_relative3 = BrowserUtils.isRelativeLink("look-see.com", url3);
		assertFalse(is_relative3);
		
	}

	@Test
	public void containsHostTest() {
		String url = "bootcamp.uxdesign.cc";
		boolean has_host = BrowserUtils.containsHost(url);
		assertTrue(has_host);
		
		String url2 = "en.wikipedia.org";
		boolean has_host2 = BrowserUtils.containsHost(url2);
		assertTrue(has_host2);

		String url3 = "shopify.dev/docs/api/customer/unstable/mutations/subscriptionContractPause";
		boolean has_host3 = BrowserUtils.containsHost( url3);
		assertTrue(has_host3);
		
	}
}
