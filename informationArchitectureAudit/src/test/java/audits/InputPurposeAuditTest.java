package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.InputPurposeAudit;
import com.looksee.models.audit.GenericIssue;

public class InputPurposeAuditTest {

    @Test
    void checkCompliance_inputWithValidAutocomplete() {
        String html = "<html><body><input name=\"email\" autocomplete=\"email\" aria-label=\"email address\"></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = InputPurposeAudit.checkCompliance(doc);

        assertTrue(issues.stream().anyMatch(i -> i.getTitle().equals("Compliant autocomplete attribute")));
    }

    @Test
    void checkCompliance_inputWithInvalidAutocomplete() {
        String html = "<html><body><input name=\"email\" autocomplete=\"invalid-value\" aria-label=\"email\"></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = InputPurposeAudit.checkCompliance(doc);

        assertTrue(issues.stream().anyMatch(i -> i.getTitle().equals("Non-compliant Autocomplete Attribute")));
    }

    @Test
    void checkCompliance_inputWithNoAutocomplete() {
        String html = "<html><body><input name=\"email\" aria-label=\"email\"></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = InputPurposeAudit.checkCompliance(doc);

        assertTrue(issues.stream().anyMatch(i -> i.getTitle().equals("Non-compliant Autocomplete Attribute")));
    }

    @Test
    void checkCompliance_inputWithAriaLabel() {
        String html = "<html><body><input name=\"username\" autocomplete=\"username\" aria-label=\"username\"></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = InputPurposeAudit.checkCompliance(doc);

        assertTrue(issues.stream().anyMatch(i -> i.getTitle().equals("Compliant ARIA label")));
    }

    @Test
    void checkCompliance_inputWithNoAriaLabel() {
        String html = "<html><body><input name=\"foo\" autocomplete=\"email\"></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = InputPurposeAudit.checkCompliance(doc);

        assertTrue(issues.stream().anyMatch(i -> i.getTitle().equals("Non-compliant ARIA Label")));
    }

    @Test
    void checkCompliance_multipleInputsMixedCompliance() {
        String html = "<html><body>"
                + "<input name=\"email\" autocomplete=\"email\" aria-label=\"email address\">"
                + "<input name=\"foo\" autocomplete=\"bogus\">"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = InputPurposeAudit.checkCompliance(doc);

        // Each input produces two issues (one for autocomplete, one for aria-label) = 4 total
        assertTrue(issues.size() >= 4);
        assertTrue(issues.stream().anyMatch(i -> i.getTitle().equals("Compliant autocomplete attribute")));
        assertTrue(issues.stream().anyMatch(i -> i.getTitle().equals("Non-compliant Autocomplete Attribute")));
    }

    @Test
    void checkCompliance_inputTypeHiddenIsNotSkipped() {
        // The current implementation does not skip hidden inputs; it checks all input elements
        String html = "<html><body><input type=\"hidden\" name=\"csrf\" value=\"abc123\"></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = InputPurposeAudit.checkCompliance(doc);

        // The method processes all input elements regardless of type
        assertFalse(issues.isEmpty());
    }

    @Test
    void checkCompliance_inputTypeSubmitIsNotSkipped() {
        // The current implementation does not skip submit inputs; it checks all input elements
        String html = "<html><body><input type=\"submit\" value=\"Submit\"></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = InputPurposeAudit.checkCompliance(doc);

        // The method processes all input elements regardless of type
        assertFalse(issues.isEmpty());
    }

    @Test
    void checkCompliance_nullDocumentThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            InputPurposeAudit.checkCompliance(null);
        });
    }
}
