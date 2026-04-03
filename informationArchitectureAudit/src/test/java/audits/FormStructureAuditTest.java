package audits;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.FormStructureAudit;
import com.looksee.models.audit.GenericIssue;

public class FormStructureAuditTest {

    // --- validateForm tests ---

    @Test
    void validateForm_allInputsProperlyLabeled() {
        String html = "<form>"
                + "<label for=\"name\">Name</label><input id=\"name\" type=\"text\">"
                + "<label for=\"email\">Email</label><input id=\"email\" type=\"email\">"
                + "</form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        List<GenericIssue> issues = FormStructureAudit.validateForm(form);

        assertEquals(2, issues.size());
        for (GenericIssue issue : issues) {
            assertTrue(issue.getDescription().contains("has an associated label with text:"));
        }
    }

    @Test
    void validateForm_unlabeledInputs() {
        String html = "<form>"
                + "<input id=\"name\" type=\"text\">"
                + "<input id=\"email\" type=\"email\">"
                + "</form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        List<GenericIssue> issues = FormStructureAudit.validateForm(form);

        assertEquals(2, issues.size());
        for (GenericIssue issue : issues) {
            assertTrue(issue.getDescription().contains("is missing an associated label"));
        }
    }

    @Test
    void validateForm_inputsWithAriaLabel() {
        String html = "<form>"
                + "<input id=\"phone\" type=\"tel\" aria-label=\"Phone number\">"
                + "</form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        List<GenericIssue> issues = FormStructureAudit.validateForm(form);

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).getDescription().contains("has an associated label via aria attributes"));
    }

    @Test
    void validateForm_inputsWithAriaLabelledby() {
        String html = "<form>"
                + "<span id=\"lbl\">Username</span>"
                + "<input id=\"user\" type=\"text\" aria-labelledby=\"lbl\">"
                + "</form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        List<GenericIssue> issues = FormStructureAudit.validateForm(form);

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).getDescription().contains("has an associated label via aria attributes"));
    }

    @Test
    void validateForm_selectAndTextareaElements() {
        String html = "<form>"
                + "<label for=\"color\">Color</label><select id=\"color\"><option>Red</option></select>"
                + "<label for=\"bio\">Bio</label><textarea id=\"bio\"></textarea>"
                + "</form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        List<GenericIssue> issues = FormStructureAudit.validateForm(form);

        assertEquals(2, issues.size());
        for (GenericIssue issue : issues) {
            assertTrue(issue.getDescription().contains("has an associated label with text:"));
        }
    }

    @Test
    void validateForm_emptyForm() {
        String html = "<form></form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        List<GenericIssue> issues = FormStructureAudit.validateForm(form);

        assertTrue(issues.isEmpty());
    }

    // --- validateFieldsetGrouping tests ---

    @Test
    void validateFieldsetGrouping_properFieldsetAndLegend() {
        String html = "<form>"
                + "<fieldset><legend>Personal Info</legend>"
                + "<input id=\"name\" type=\"text\">"
                + "</fieldset>"
                + "</form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        List<String> messages = FormStructureAudit.validateFieldsetGrouping(form);

        assertFalse(messages.isEmpty());
        assertTrue(messages.stream().anyMatch(m -> m.contains("Fieldset with <legend>: 'Personal Info' found.")));
    }

    @Test
    void validateFieldsetGrouping_noFieldset() {
        String html = "<form><input id=\"name\" type=\"text\"></form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        List<String> messages = FormStructureAudit.validateFieldsetGrouping(form);

        assertFalse(messages.isEmpty());
        assertTrue(messages.stream().anyMatch(m -> m.contains("No <fieldset> elements found")));
        assertTrue(messages.stream().anyMatch(m -> m.contains("Form controls found outside of fieldsets")));
    }

    @Test
    void validateFieldsetGrouping_fieldsetWithoutLegend() {
        String html = "<form>"
                + "<fieldset><input id=\"name\" type=\"text\"></fieldset>"
                + "</form>";
        Document doc = Jsoup.parse(html);
        Element form = doc.select("form").first();

        List<String> messages = FormStructureAudit.validateFieldsetGrouping(form);

        assertFalse(messages.isEmpty());
        assertTrue(messages.stream().anyMatch(m -> m.contains("Fieldset without a <legend> found")));
    }
}
