package audits;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.TitleAndHeaderAudit;

public class TitleAndHeaderAuditTest {

    @Test
    void testHasFavicon_withRelIcon() {
        String html = "<html><head><link rel=\"icon\" href=\"/favicon.ico\"></head><body></body></html>";
        assertTrue(TitleAndHeaderAudit.hasFavicon(html));
    }

    @Test
    void testHasFavicon_withShortcutIcon() {
        String html = "<html><head><link rel=\"shortcut icon\" href=\"/favicon.ico\" type=\"image/x-icon\"></head><body></body></html>";
        assertTrue(TitleAndHeaderAudit.hasFavicon(html));
    }

    @Test
    void testHasFavicon_noFavicon() {
        String html = "<html><head><link rel=\"stylesheet\" href=\"/style.css\"></head><body></body></html>";
        assertFalse(TitleAndHeaderAudit.hasFavicon(html));
    }

    @Test
    void testHasFavicon_emptyHtml() {
        String html = "<html><head></head><body></body></html>";
        assertFalse(TitleAndHeaderAudit.hasFavicon(html));
    }

    @Test
    void testHasFavicon_appleTouchIcon() {
        String html = "<html><head><link rel=\"apple-touch-icon\" href=\"/apple-icon.png\"></head><body></body></html>";
        // "apple-touch-icon" contains "icon", so hasFavicon checks rel.contains("icon")
        assertTrue(TitleAndHeaderAudit.hasFavicon(html));
    }

    @Test
    void testHasFavicon_multipleLinkElements() {
        String html = "<html><head>"
                + "<link rel=\"stylesheet\" href=\"/style.css\">"
                + "<link rel=\"canonical\" href=\"https://example.com\">"
                + "<link rel=\"icon\" href=\"/favicon.png\">"
                + "</head><body></body></html>";
        assertTrue(TitleAndHeaderAudit.hasFavicon(html));
    }

    @Test
    void testHasFavicon_noLinkElements() {
        String html = "<html><head><title>Test</title></head><body><p>Hello</p></body></html>";
        assertFalse(TitleAndHeaderAudit.hasFavicon(html));
    }

    @Test
    void testHasFavicon_relWithoutIcon() {
        String html = "<html><head><link rel=\"preload\" href=\"/font.woff2\"></head><body></body></html>";
        assertFalse(TitleAndHeaderAudit.hasFavicon(html));
    }
}
