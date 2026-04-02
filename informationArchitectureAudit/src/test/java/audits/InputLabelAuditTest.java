package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.models.InputLabelAudit;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.services.ElementStateService;

public class InputLabelAuditTest {

    private InputLabelAudit audit;
    private ElementStateService mockElementStateService;
    private PageState mockPageState;

    @BeforeEach
    void setUp() throws Exception {
        audit = new InputLabelAudit();
        mockElementStateService = mock(ElementStateService.class);
        mockPageState = mock(PageState.class);
        when(mockPageState.getId()).thenReturn(1L);

        // Use reflection to inject mock ElementStateService
        Field field = InputLabelAudit.class.getDeclaredField("elementStateService");
        field.setAccessible(true);
        field.set(audit, mockElementStateService);

        ElementState mockElement = mock(ElementState.class);
        when(mockElementStateService.findByPageAndCssSelector(anyLong(), anyString())).thenReturn(mockElement);
    }

    @Test
    void testCheckWCAGCompliance_formWithLabels() {
        String html = "<html><body>"
                + "<form>"
                + "<label for=\"name\">Name:</label>"
                + "<input type=\"text\" id=\"name\" name=\"name\">"
                + "<label for=\"email\">Email:</label>"
                + "<input type=\"email\" id=\"email\" name=\"email\">"
                + "</form>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<UXIssueMessage> issues = audit.checkWCAGCompliance(doc, mockPageState);

        // All inputs have labels, so no missing label issues
        assertTrue(issues.stream().allMatch(i -> i.getTitle().contains("Label exists")),
                "All inputs should have associated labels");
    }

    @Test
    void testCheckWCAGCompliance_formWithoutLabels() {
        String html = "<html><body>"
                + "<form>"
                + "<input type=\"text\" id=\"name\" name=\"name\">"
                + "<input type=\"email\" id=\"email\" name=\"email\">"
                + "</form>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<UXIssueMessage> issues = audit.checkWCAGCompliance(doc, mockPageState);

        assertTrue(issues.stream().anyMatch(i -> i.getTitle().contains("Missing label")),
                "Inputs without labels should produce missing label issues");
    }

    @Test
    void testCheckWCAGCompliance_noForms() {
        String html = "<html><body><p>No forms here.</p></body></html>";
        Document doc = Jsoup.parse(html);

        List<UXIssueMessage> issues = audit.checkWCAGCompliance(doc, mockPageState);

        assertTrue(issues.isEmpty(), "No forms means no issues");
    }

    @Test
    void testCheckWCAGCompliance_multipleInputTypes() {
        String html = "<html><body>"
                + "<form>"
                + "<input type=\"text\" id=\"t1\" name=\"text1\">"
                + "<textarea id=\"t2\" name=\"area1\"></textarea>"
                + "<select id=\"t3\" name=\"sel1\"><option>A</option></select>"
                + "</form>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<UXIssueMessage> issues = audit.checkWCAGCompliance(doc, mockPageState);

        assertEquals(3, issues.size(), "Should report issues for all 3 unlabeled inputs");
    }

    @Test
    void testCheckWCAGCompliance_emptyForm() {
        String html = "<html><body><form></form></body></html>";
        Document doc = Jsoup.parse(html);

        List<UXIssueMessage> issues = audit.checkWCAGCompliance(doc, mockPageState);

        assertTrue(issues.isEmpty(), "Empty form should produce no issues");
    }

    @Test
    void testCheckWCAGCompliance_mixedLabeledAndUnlabeled() {
        String html = "<html><body>"
                + "<form>"
                + "<label for=\"name\">Name:</label>"
                + "<input type=\"text\" id=\"name\" name=\"name\">"
                + "<input type=\"email\" id=\"email\" name=\"email\">"
                + "</form>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<UXIssueMessage> issues = audit.checkWCAGCompliance(doc, mockPageState);

        assertEquals(2, issues.size(), "Should have issues for both inputs");
        // One labeled, one unlabeled
        assertTrue(issues.stream().anyMatch(i -> i.getTitle().contains("Label exists")));
        assertTrue(issues.stream().anyMatch(i -> i.getTitle().contains("Missing label")));
    }

    @Test
    void testCheckWCAGCompliance_multipleForms() {
        String html = "<html><body>"
                + "<form>"
                + "<input type=\"text\" id=\"a\" name=\"a\">"
                + "</form>"
                + "<form>"
                + "<label for=\"b\">B:</label>"
                + "<input type=\"text\" id=\"b\" name=\"b\">"
                + "</form>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<UXIssueMessage> issues = audit.checkWCAGCompliance(doc, mockPageState);

        assertEquals(2, issues.size(), "Should report issues for inputs in both forms");
    }
}
