package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.IdentifyPurposeAudit;
import com.looksee.models.audit.GenericIssue;

public class IdentifyPurposeTest {
    @Test
    public void testCheckCompliance_withValidDocument_noIssuesFound() {
        String validHtml = "<html><body>"
                + "<img src='image.jpg' alt='Description of image'>"
                + "<button aria-label='Click me'></button>"
                + "<div role='banner' aria-label='Site banner'></div>"
                + "</body></html>";

        Document doc = Jsoup.parse(validHtml);
        List<GenericIssue> issues = IdentifyPurposeAudit.checkCompliance(doc);

        // Assert that no issues were found
        assertEquals(0, issues.size(), "Expected no compliance issues, but some were found.");
    }

    @Test
    public void testCheckCompliance_withMissingAltAttribute_issueFound() {
        String invalidHtml = "<html><body>"
                + "<img src='image.jpg'>"
                + "</body></html>";

        Document doc = Jsoup.parse(invalidHtml);
        List<GenericIssue> issues = IdentifyPurposeAudit.checkCompliance(doc);

        // Assert that one issue was found related to the missing alt attribute
        assertEquals(1, issues.size(), "Expected one compliance issue, but found " + issues.size());
        assertEquals("Missing alt attribute", issues.get(0).getTitle());
    }

    @Test
    public void testCheckCompliance_withMissingAriaLabelOnButton_issueFound() {
        String invalidHtml = "<html><body>"
                + "<button>Click me</button>"
                + "</body></html>";

        Document doc = Jsoup.parse(invalidHtml);
        List<GenericIssue> issues = IdentifyPurposeAudit.checkCompliance(doc);

        // Assert that one issue was found related to the missing aria-label/aria-labelledby on the button
        assertEquals(1, issues.size(), "Expected one compliance issue, but found " + issues.size());
        assertEquals("Missing aria-label/aria-labelledby", issues.get(0).getTitle());
    }

    @Test
    public void testCheckCompliance_withMissingAriaLabelOnRegion_issueFound() {
        String invalidHtml = "<html><body>"
                + "<div role='banner'></div>"
                + "</body></html>";

        Document doc = Jsoup.parse(invalidHtml);
        List<GenericIssue> issues = IdentifyPurposeAudit.checkCompliance(doc);

        // Assert that one issue was found related to the missing aria-label/aria-labelledby on the region
        assertEquals(1, issues.size(), "Expected one compliance issue, but found " + issues.size());
        assertEquals("Missing aria-label/aria-labelledby for region", issues.get(0).getTitle());
    }

    @Test
    public void testCheckCompliance_withMultipleIssues_foundAllIssues() {
        String invalidHtml = "<html><body>"
                + "<img src='image.jpg'>"
                + "<button>Click me</button>"
                + "<div role='banner'></div>"
                + "</body></html>";

        Document doc = Jsoup.parse(invalidHtml);
        List<GenericIssue> issues = IdentifyPurposeAudit.checkCompliance(doc);

        // Assert that three issues were found
        assertEquals(3, issues.size(), "Expected three compliance issues, but found " + issues.size());

        // Check that all issues are identified correctly
        assertEquals("Missing alt attribute", issues.get(0).getTitle());
        assertEquals("Missing aria-label/aria-labelledby", issues.get(1).getTitle());
        assertEquals("Missing aria-label/aria-labelledby for region", issues.get(2).getTitle());
    }

    @Test
    public void testCheckCompliance_withNullDocument_throwsException() {
        try {
            IdentifyPurposeAudit.checkCompliance(null);
        } catch (IllegalArgumentException e) {
            assertEquals("Document cannot be null", e.getMessage());
        }
    }
}
