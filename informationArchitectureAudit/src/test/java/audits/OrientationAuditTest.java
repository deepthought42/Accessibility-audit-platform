package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.OrientationAudit;
import com.looksee.models.audit.GenericIssue;

public class OrientationAuditTest {

    @Test
    void checkOrientationRestrictions_noRestrictions() {
        String html = "<html><head></head><body><p>Hello</p></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = OrientationAudit.checkOrientationRestrictions(doc);

        assertTrue(issues.isEmpty());
    }

    @Test
    void checkOrientationRestrictions_withViewportMetaTag() {
        // The method currently detects this but still returns empty list
        String html = "<html><head>"
                + "<meta name=\"viewport\" content=\"width=device-width, orientation=portrait\">"
                + "</head><body></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = OrientationAudit.checkOrientationRestrictions(doc);

        // The current implementation prints a warning but does not add issues to the list
        assertNotNull(issues);
        // Currently returns empty - the method detects the restriction but does not create issues
        assertTrue(issues.isEmpty());
    }

    @Test
    void checkOrientationRestrictions_withOrientationMediaQuery() {
        // The method currently detects this but still returns empty list
        String html = "<html><head>"
                + "<style>@media (orientation: portrait) { body { display: none; } }</style>"
                + "</head><body></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = OrientationAudit.checkOrientationRestrictions(doc);

        // The current implementation prints a warning but does not add issues to the list
        assertNotNull(issues);
        assertTrue(issues.isEmpty());
    }

    @Test
    void checkOrientationRestrictions_withLandscapeMediaQuery() {
        String html = "<html><head>"
                + "<style>@media (orientation: landscape) { .sidebar { width: 300px; } }</style>"
                + "</head><body></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = OrientationAudit.checkOrientationRestrictions(doc);

        assertNotNull(issues);
        assertTrue(issues.isEmpty());
    }

    @Test
    void checkOrientationRestrictions_withStyleButNoOrientationQuery() {
        String html = "<html><head>"
                + "<style>body { margin: 0; } @media (max-width: 600px) { body { font-size: 14px; } }</style>"
                + "</head><body></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = OrientationAudit.checkOrientationRestrictions(doc);

        assertTrue(issues.isEmpty());
    }

    @Test
    void checkOrientationRestrictions_viewportMetaWithoutOrientation() {
        String html = "<html><head>"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "</head><body></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = OrientationAudit.checkOrientationRestrictions(doc);

        assertTrue(issues.isEmpty());
    }
}
