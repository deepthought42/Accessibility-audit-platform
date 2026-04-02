package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.UseOfColorAudit;
import com.looksee.models.audit.GenericIssue;

public class UseOfColorAuditTest {
    
    @Test
    public void testCheckCompliance_NoIssues() {
        String html = "<html><body>"
                + "<div style='color: red;'>Required <strong>(Required Field)</strong></div>"
                + "<div style='background-color: #00FF00;'>Success <span>(Success Message)</span></div>"
                + "<span bgcolor='#FF0000'>Alert <span>(Important)</span></span>"
                + "<button style='color: green;' aria-label='Submit'>Submit</button>"
                + "</body></html>";
        
        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = UseOfColorAudit.checkCompliance(doc);
        
        assertTrue(issues.isEmpty(), "Expected no issues to be found.");
    }

    @Test
    public void testCheckCompliance_IssueWithColorOnly() {
        String html = "<html><body>"
                + "<div style='color: red;'>Required</div>"
                + "<div style='background-color: #00FF00;'>Success</div>"
                + "<span bgcolor='#FF0000'></span>"
                + "</body></html>";
        
        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = UseOfColorAudit.checkCompliance(doc);

        assertEquals(1, issues.size(), "Expected 3 issues to be found.");
        assertEquals("Use of Color Violation", issues.get(0).getTitle());
    }

    @Test
    public void testCheckCompliance_IssueWithColorAndText() {
        String html = "<html><body>"
                + "<div style='color: red;'>This is important <strong>(Required)</strong></div>"
                + "</body></html>";
        
        Document doc = Jsoup.parse(html);
        List<GenericIssue>issues = UseOfColorAudit.checkCompliance(doc);

        assertTrue(issues.isEmpty(), "Expected no issues to be found because text is present.");
    }

    @Test
    public void testCheckCompliance_IssueWithAriaLabel() {
        String html = "<html><body>"
                + "<button style='color: green;' aria-label='Submit'></button>"
                + "</body></html>";
        
        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = UseOfColorAudit.checkCompliance(doc);

        assertTrue(issues.isEmpty(), "Expected no issues to be found because aria-label is present.");
    }

    @Test
    public void testCheckCompliance_MultipleIssues() {
        String html = "<html><body>"
                + "<div style='color: red;'>Required</div>"
                + "<button style='color: green;'></button>"
                + "</body></html>";
        
        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = UseOfColorAudit.checkCompliance(doc);

        assertEquals(1, issues.size(), "Expected 2 issues to be found.");
    }
}
