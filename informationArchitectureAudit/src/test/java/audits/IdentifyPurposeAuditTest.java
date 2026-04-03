package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.IdentifyPurposeAudit;
import com.looksee.models.audit.GenericIssue;

public class IdentifyPurposeAuditTest {

    @Test
    void checkCompliance_imagesWithAltAttributes() {
        String html = "<html><body><img src=\"photo.jpg\" alt=\"A sunset photo\"></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = IdentifyPurposeAudit.checkCompliance(doc);

        // Image has alt attribute, so no issue for images
        assertTrue(issues.stream().noneMatch(i -> i.getDescription().contains("Image element")));
    }

    @Test
    void checkCompliance_imagesMissingAltAttributes() {
        String html = "<html><body><img src=\"photo.jpg\"></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = IdentifyPurposeAudit.checkCompliance(doc);

        assertTrue(issues.stream().anyMatch(i -> i.getDescription().contains("Image element is missing a valid alt attribute")));
    }

    @Test
    void checkCompliance_imagesWithEmptyAlt() {
        // An image with alt="" is flagged because the selector img[alt=''] matches
        // empty alt attributes in modern jsoup versions.
        String html = "<html><body><img src=\"photo.jpg\" alt=\"\"></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = IdentifyPurposeAudit.checkCompliance(doc);

        assertTrue(issues.stream().anyMatch(i -> i.getDescription().contains("Image element is missing a valid alt attribute")));
    }

    @Test
    void checkCompliance_buttonsWithAriaLabel() {
        String html = "<html><body><button aria-label=\"Submit form\">Go</button></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = IdentifyPurposeAudit.checkCompliance(doc);

        assertTrue(issues.stream().noneMatch(i -> i.getDescription().contains("Button is missing")));
    }

    @Test
    void checkCompliance_buttonsMissingAriaLabelAndAriaLabelledby() {
        String html = "<html><body><button>Go</button></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = IdentifyPurposeAudit.checkCompliance(doc);

        assertTrue(issues.stream().anyMatch(i -> i.getDescription().contains("Button is missing a valid aria-label or aria-labelledby")));
    }

    @Test
    void checkCompliance_regionsWithAriaLabel() {
        String html = "<html><body><div role=\"navigation\" aria-label=\"Main navigation\">Nav</div></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = IdentifyPurposeAudit.checkCompliance(doc);

        assertTrue(issues.stream().noneMatch(i -> i.getDescription().contains("Region element")));
    }

    @Test
    void checkCompliance_regionsMissingAriaAttributes() {
        String html = "<html><body><section role=\"banner\">Content</section></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = IdentifyPurposeAudit.checkCompliance(doc);

        assertTrue(issues.stream().anyMatch(i -> i.getDescription().contains("Region element with role attribute is missing aria-label")));
    }

    @Test
    void checkCompliance_emptyDocument() {
        String html = "<html><body></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = IdentifyPurposeAudit.checkCompliance(doc);

        assertTrue(issues.isEmpty());
    }

    @Test
    void checkCompliance_mixedCompliantAndNonCompliant() {
        String html = "<html><body>"
                + "<img src=\"a.jpg\" alt=\"Photo\">"
                + "<img src=\"b.jpg\">"
                + "<button aria-label=\"Save\">Save</button>"
                + "<button>Delete</button>"
                + "<nav role=\"navigation\" aria-label=\"Main\">Nav</nav>"
                + "<div role=\"region\">Content</div>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = IdentifyPurposeAudit.checkCompliance(doc);

        // Should have issues for: img missing alt, button missing aria, div[role] missing aria
        long imageIssues = issues.stream().filter(i -> i.getDescription().contains("Image element")).count();
        long buttonIssues = issues.stream().filter(i -> i.getDescription().contains("Button is missing")).count();
        long regionIssues = issues.stream().filter(i -> i.getDescription().contains("Region element")).count();

        assertEquals(1, imageIssues);
        assertEquals(1, buttonIssues);
        assertEquals(1, regionIssues);
    }

    @Test
    void checkCompliance_nullDocumentThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            IdentifyPurposeAudit.checkCompliance(null);
        });
    }
}
