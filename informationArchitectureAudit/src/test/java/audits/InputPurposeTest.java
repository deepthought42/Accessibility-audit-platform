package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.InputPurposeAudit;
import com.looksee.models.audit.GenericIssue;

public class InputPurposeTest {
    
    /**
     * Test for valid HTML with correct autocomplete and aria-label attributes.
     */
    @Test
    public void testValidHtml() {
        String html = "<html><body>"
                + "<form>"
                + "<input type='text' name='fullname' autocomplete='name' aria-label='Full name'>"
                + "<input type='email' name='user-email' autocomplete='email' aria-label='Email address'>"
                + "<input type='text' name='street-address' autocomplete='street-address' aria-label='Street address'>"
                + "<input type='text' name='city' autocomplete='address-level2' aria-label='City'>"
                + "<input type='text' name='username' autocomplete='username' aria-label='Username'>"
                + "</form>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = InputPurposeAudit.checkCompliance(doc);
        boolean isCompliant = true;
        for(GenericIssue issue: issues){
            if(!issue.getRecommendation().isEmpty()){
                isCompliant = false;
            }
        }
        assertTrue(isCompliant, "There should be no compliance issues for valid HTML");
    }

    /**
     * Test for HTML with missing or incorrect autocomplete attributes.
     */
    @Test
    public void testInvalidAutocomplete() {
        String html = "<html><body>"
                + "<form>"
                + "<input type='text' name='fullname' autocomplete='wrong-value' aria-label='Full name'>"
                + "<input type='email' name='user-email' aria-label='Email address'>"
                + "<input type='text' name='street-address' autocomplete='street-address' aria-label='Street address'>"
                + "<input type='text' name='city' autocomplete='address-level2' aria-label='City'>"
                + "<input type='text' name='username' autocomplete='username' aria-label='Username'>"
                + "</form>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = InputPurposeAudit.checkCompliance(doc);

        assertFalse(issues.isEmpty(), "There should be issues with incorrect or missing autocomplete attributes");
        assertTrue(issues.stream().anyMatch(issue -> issue.getTitle().equals("Non-compliant Autocomplete Attribute")),
                   "The issue list should contain a non-compliant autocomplete attribute issue");
    }

    /**
     * Test for HTML with missing or incorrect ARIA labels.
     */
    @Test
    public void testInvalidAriaLabel() {
        String html = "<html><body>"
                + "<form>"
                + "<input type='text' name='fullname' autocomplete='name'>"
                + "<input type='email' name='user-email' aria-label='Email address'>"
                + "<input type='text' name='street-address' autocomplete='street-address' aria-label='Street address'>"
                + "<input type='text' name='city' aria-label='City'>"
                + "<input type='text' name='username' aria-label='Username'>"
                + "</form>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = InputPurposeAudit.checkCompliance(doc);

        assertFalse(issues.isEmpty(), "There should be issues with missing or incorrect ARIA labels");
        assertTrue(issues.stream().anyMatch(issue -> issue.getTitle().equals("Non-compliant ARIA Label")),
                   "The issue list should contain a non-compliant ARIA label issue");
    }

    /**
     * Test for null HTML document.
     */
    @Test
    public void testNullDocument() {
        assertThrows(IllegalArgumentException.class, () -> InputPurposeAudit.checkCompliance(null),
                     "Checking compliance with a null document should throw an IllegalArgumentException");
    }

    /**
     * Test for ARIA labels and autocomplete attributes being correctly identified in a mixed-validity HTML document.
     */
    @Test
    public void testMixedHtml() {
        String html = "<html><body>"
                + "<form>"
                + "<input type='text' name='fullname' autocomplete='name' aria-label='Full name'>"
                + "<input type='email' name='user-email' autocomplete='wrong-value' aria-label='Email address'>"
                + "<input type='text' name='street-address' autocomplete='street-address'>"
                + "<input type='text' name='city' aria-label='City'>"
                + "<input type='text' name='username' aria-label='Username'>"
                + "</form>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = InputPurposeAudit.checkCompliance(doc);

        assertFalse(issues.isEmpty(), "There should be issues found in the mixed-validity HTML document");
        assertTrue(issues.stream().anyMatch(issue -> issue.getTitle().equals("Non-compliant Autocomplete Attribute")),
                   "The issue list should contain a non-compliant autocomplete attribute issue");
        assertTrue(issues.stream().anyMatch(issue -> issue.getTitle().equals("Non-compliant ARIA Label")),
                   "The issue list should contain a non-compliant ARIA label issue");
    }
}
