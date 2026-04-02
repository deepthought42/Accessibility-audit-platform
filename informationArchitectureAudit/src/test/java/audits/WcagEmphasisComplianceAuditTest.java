package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.models.WcagEmphasisComplianceAudit;

public class WcagEmphasisComplianceAuditTest {

    // --- checkEmphasisCompliance tests ---

    @Test
    void testCheckEmphasisCompliance_noBoldOrItalic() {
        String html = "<html><body><p>This is plain text.</p><strong>Semantic bold</strong></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> result = WcagEmphasisComplianceAudit.checkEmphasisCompliance(doc);
        assertTrue(result.isEmpty(), "No <b> or <i> elements should mean no issues");
    }

    @Test
    void testCheckEmphasisCompliance_onlyBold() {
        String html = "<html><body><p>This is <b>bold</b> text.</p></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> result = WcagEmphasisComplianceAudit.checkEmphasisCompliance(doc);
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("b"));
    }

    @Test
    void testCheckEmphasisCompliance_onlyItalic() {
        String html = "<html><body><p>This is <i>italic</i> text.</p></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> result = WcagEmphasisComplianceAudit.checkEmphasisCompliance(doc);
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("i"));
    }

    @Test
    void testCheckEmphasisCompliance_emptyDocument() {
        String html = "<html><body></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> result = WcagEmphasisComplianceAudit.checkEmphasisCompliance(doc);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckEmphasisCompliance_multipleBoldAndItalic() {
        String html = "<html><body>"
                + "<p><b>bold1</b></p>"
                + "<p><b>bold2</b></p>"
                + "<p><i>italic1</i></p>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<String> result = WcagEmphasisComplianceAudit.checkEmphasisCompliance(doc);
        assertEquals(3, result.size());
    }

    // --- checkSpecialTextCompliance tests ---

    @Test
    void testCheckSpecialTextCompliance_abbrWithTitle() {
        String html = "<html><body><p>The <abbr title=\"World Wide Web\">WWW</abbr> is great.</p></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> result = WcagEmphasisComplianceAudit.checkSpecialTextCompliance(doc);
        // abbr with title is compliant, so it shouldn't appear in non-compliant list
        // But strong, code, blockquote are always added if present. abbr without title is added.
        // Since no strong/code/blockquote present and abbr has title, expect empty
        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckSpecialTextCompliance_abbrWithoutTitle() {
        String html = "<html><body><p>The <abbr>WWW</abbr> is great.</p></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> result = WcagEmphasisComplianceAudit.checkSpecialTextCompliance(doc);
        assertEquals(1, result.size());
        assertTrue(result.get(0).contains("abbr"));
    }

    @Test
    void testCheckSpecialTextCompliance_noSpecialText() {
        String html = "<html><body><p>Just plain text without any special elements.</p></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> result = WcagEmphasisComplianceAudit.checkSpecialTextCompliance(doc);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCheckSpecialTextCompliance_strongElement() {
        String html = "<html><body><p>This is <strong>important</strong>.</p></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> result = WcagEmphasisComplianceAudit.checkSpecialTextCompliance(doc);
        // strong elements are always added as non-compliant selectors by getNonCompliantSelectors
        assertEquals(1, result.size());
    }

    @Test
    void testCheckSpecialTextCompliance_codeElement() {
        String html = "<html><body><p>Use <code>System.out.println()</code> to print.</p></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> result = WcagEmphasisComplianceAudit.checkSpecialTextCompliance(doc);
        assertEquals(1, result.size());
    }

    @Test
    void testCheckSpecialTextCompliance_blockquoteElement() {
        String html = "<html><body><blockquote>A famous quote.</blockquote></body></html>";
        Document doc = Jsoup.parse(html);

        List<String> result = WcagEmphasisComplianceAudit.checkSpecialTextCompliance(doc);
        assertEquals(1, result.size());
    }

    @Test
    void testCheckSpecialTextCompliance_mixedElements() {
        String html = "<html><body>"
                + "<strong>Bold text</strong>"
                + "<code>code snippet</code>"
                + "<abbr title=\"abbreviation\">abbr</abbr>"
                + "<abbr>missing title</abbr>"
                + "<blockquote>A quote</blockquote>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<String> result = WcagEmphasisComplianceAudit.checkSpecialTextCompliance(doc);
        // strong(1) + code(1) + blockquote(1) + abbr without title(1) = 4
        assertEquals(4, result.size());
    }

    @Test
    void testCheckSpecialTextCompliance_multipleAbbrsWithMixedTitles() {
        String html = "<html><body>"
                + "<abbr title=\"HyperText Markup Language\">HTML</abbr>"
                + "<abbr>CSS</abbr>"
                + "<abbr title=\"JavaScript\">JS</abbr>"
                + "<abbr>API</abbr>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<String> result = WcagEmphasisComplianceAudit.checkSpecialTextCompliance(doc);
        // Only abbrs without title are flagged: CSS and API = 2
        assertEquals(2, result.size());
    }
}
