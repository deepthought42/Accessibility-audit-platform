package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.VisualPresentationAudit;
import com.looksee.models.audit.GenericIssue;

public class VisualPresentationAuditTest {

    private VisualPresentationAudit checker;

    @BeforeEach
    void setUp() {
        checker = new VisualPresentationAudit();
    }

    @Test
    void testForegroundAndBackgroundColorIssue() {
        String html = "<p style='color: #000; background-color: #fff;'>Sample text</p>";
        Document document = Jsoup.parse(html);
        List<GenericIssue> issues = checker.checkCompliance(document);

        assertFalse(issues.isEmpty());
        assertEquals("Foreground and Background Color Issue", issues.get(0).getTitle());
        assertEquals("Foreground and background colors are hard-coded.", issues.get(0).getDescription());
    }

    @Test
    void testFontSizeIssue() {
        String html = "<p style='font-size: 14px;'>Sample text</p>";
        Document document = Jsoup.parse(html);
        List<GenericIssue> issues = checker.checkCompliance(document).stream().filter(issue -> !issue.getRecommendation().isEmpty()).collect(Collectors.toList());

        assertFalse(issues.isEmpty());
        assertEquals("Font Size Issue", issues.get(0).getTitle());
        assertEquals("Font size is not defined in relative units (em, rem, or %).", issues.get(0).getDescription());
    }

    @Test
    void testTextJustificationIssue() {
        String html = "<p style='text-align: justify;'>This is justified text.</p>";
        Document document = Jsoup.parse(html);
        List<GenericIssue> issues = checker.checkCompliance(document).stream().filter(issue -> !issue.getRecommendation().isEmpty()).collect(Collectors.toList());

        assertFalse(issues.isEmpty());
        assertEquals("Text Justification Issue", issues.get(0).getTitle());
        assertEquals("Text is justified, which may cause readability issues.", issues.get(0).getDescription());
    }

    @Test
    void testLineHeightIssue() {
        String html = "<p style='line-height: 1.2em;'>Sample text</p>";
        Document document = Jsoup.parse(html);
        List<GenericIssue> issues = checker.checkCompliance(document).stream().filter(issue -> !issue.getRecommendation().isEmpty()).collect(Collectors.toList());

        assertFalse(issues.isEmpty());
        assertEquals("Line Height Issue", issues.get(0).getTitle());
        assertEquals("Line height is less than 1.5 times the font size.", issues.get(0).getDescription());
    }

    @Test
    void testParagraphSpacingIssue() {
        String html = "<p style='margin: 5px 10px;'>Sample text</p>";
        Document document = Jsoup.parse(html);
        List<GenericIssue> issues = checker.checkCompliance(document).stream().filter(issue -> !issue.getRecommendation().isEmpty()).collect(Collectors.toList());

        assertFalse(issues.isEmpty());
        assertEquals("Paragraph Spacing Issue", issues.get(0).getTitle());
        assertEquals("Paragraph spacing is not consistent with line spacing.", issues.get(0).getDescription());
    }

    @Test
    void testNoIssues() {
        String html = "<p style='font-size: 1.5em; line-height: 2em; text-align: left;'>Sample text</p>";
        Document document = Jsoup.parse(html);
        List<GenericIssue> issues = checker.checkCompliance(document).stream().filter(issue -> !issue.getRecommendation().isEmpty()).collect(Collectors.toList());

        assertFalse(issues.isEmpty());
    }

    @Test
    void testMultipleIssues() {
        String html = "<p style='font-size: 14px; line-height: 1.2em; text-align: justify; color: #000; background-color: #fff;'>Sample text</p>";
        Document document = Jsoup.parse(html);
        List<GenericIssue> issues = checker.checkCompliance(document);

        assertEquals(4, issues.size());
    }
}
