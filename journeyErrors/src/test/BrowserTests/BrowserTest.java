package BrowserTests;

import static org.junit.Assert.assertTrue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import com.looksee.journeyExecutor.services.BrowserService;

public class BrowserTest {

	@Test
	public void testGeneralizeSrc() {
		String expected_result = "<html><head></head><body><app-footer></app-footer></body></html>";
		String src = "<app-footer _ngcontent-wiq-c74=\"\" class=\"footer\" _nghost-wiq-c23=\"\"></app-footer>  ";
		String generalized_src = BrowserService.generalizeSrc(src);
		System.out.println(generalized_src);
		assertTrue(expected_result.equals(generalized_src));
	}
	
	@Test
	public void testCommentRemoval() {
		String expected_result = "<html>\n"
				+ " <head></head>\n"
				+ " <body>\n"
				+ "  <app-footer _ngcontent-wiq-c74=\"\" class=\"footer\" _nghost-wiq-c23=\"\">\n"
				+ "   <div></div>\n"
				+ "  </app-footer> \n"
				+ " </body>\n"
				+ "</html>";
		
		String src = "<app-footer _ngcontent-wiq-c74=\"\" class=\"footer\" _nghost-wiq-c23=\"\"><div></div><!-- this is a comment --></app-footer>  ";
		Document html_doc = Jsoup.parse(src);

		BrowserService.removeComments(html_doc);
		
		assert(expected_result.equals(html_doc.html()));
	}
}
