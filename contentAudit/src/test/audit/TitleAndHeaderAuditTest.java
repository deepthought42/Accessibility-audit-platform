package audit;

import org.junit.Assert;
import org.junit.Test;

import com.looksee.models.audit.informationarchitecture.TitleAndHeaderAudit;

public class TitleAndHeaderAuditTest {

	@Test
	public void hasFaviconTest() {
		String src_with_favicon = "<html>"
				+ "<head>"
				+ "<link rel='icon shortcut-icon' href='http://example.com/img.png' />"
				+ "</head>"
				+ "</html>";
		Assert.assertTrue(TitleAndHeaderAudit.hasFavicon(src_with_favicon));
		
		String src_with_invalid_favicon = "<html>"
				+ "<head>"
				+ "<link rel='icon shortcut-icon' />"
				+ "</head>"
				+ "</html>";
		Assert.assertTrue(TitleAndHeaderAudit.hasFavicon(src_with_invalid_favicon));
		
		String src_without_favicon = "<html>"
				+ "<head>"
				+ "</head>"
				+ "</html>";
		Assert.assertFalse(TitleAndHeaderAudit.hasFavicon(src_without_favicon));
	}
}
