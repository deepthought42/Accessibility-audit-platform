package UtilsTests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.looksee.utils.BrowserUtils;

public class BrowserUtilTests {

	@Test
	public void testIsRelative() {
		String url = "bootcamp.uxdesign.cc";
		boolean is_relative = BrowserUtils.isRelativeLink("look-see.com", url);
		System.out.println("is relative = "+is_relative);
		assertFalse(is_relative);
		
		String url2 = "en.wikipedia.org";
		boolean is_relative2 = BrowserUtils.isExternalLink("look-see.com", url2);
		assertTrue(is_relative2);
		
	}
}
