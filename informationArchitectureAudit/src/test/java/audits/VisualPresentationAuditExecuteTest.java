package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.VisualPresentationAudit;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.GenericIssue;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;

public class VisualPresentationAuditExecuteTest {

    private VisualPresentationAudit visualPresentationAudit;
    private AuditService mockAuditService;
    private ElementStateService mockElementStateService;

    @BeforeEach
    void setUp() throws Exception {
        visualPresentationAudit = new VisualPresentationAudit();
        mockAuditService = mock(AuditService.class);
        mockElementStateService = mock(ElementStateService.class);

        Field f1 = VisualPresentationAudit.class.getDeclaredField("auditService");
        f1.setAccessible(true);
        f1.set(visualPresentationAudit, mockAuditService);

        Field f2 = VisualPresentationAudit.class.getDeclaredField("elementStateService");
        f2.setAccessible(true);
        f2.set(visualPresentationAudit, mockElementStateService);

        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mockElementStateService.findByPageAndCssSelector(anyLong(), anyString()))
                .thenReturn(mock(ElementState.class));
    }

    // ---- checkCompliance tests ----

    @Test
    void testCheckCompliance_noIssues() {
        String html = "<html><body><p>Simple clean text with no inline styles.</p></body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = visualPresentationAudit.checkCompliance(doc);

        assertTrue(issues.isEmpty(), "Should find no issues when there are no inline styles");
    }

    @Test
    void testCheckCompliance_inlineColorStyle() {
        String html = "<html><body>"
                + "<p style='color: #000; background-color: #fff;'>Styled text</p>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = visualPresentationAudit.checkCompliance(doc);

        assertFalse(issues.isEmpty(), "Should find issues when inline color and background-color are set");
        boolean hasColorIssue = issues.stream()
                .anyMatch(issue -> "Foreground and Background Color Issue".equals(issue.getTitle()));
        assertTrue(hasColorIssue, "Should detect hard-coded foreground and background colors");
    }

    @Test
    void testCheckCompliance_absoluteFontSize() {
        String html = "<html><body>"
                + "<p style='font-size: 16px;'>Text with absolute font size</p>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = visualPresentationAudit.checkCompliance(doc)
                .stream()
                .filter(issue -> !issue.getRecommendation().isEmpty())
                .collect(Collectors.toList());

        assertFalse(issues.isEmpty(), "Should find issues when font-size uses absolute units");
        boolean hasFontSizeIssue = issues.stream()
                .anyMatch(issue -> "Font Size Issue".equals(issue.getTitle()));
        assertTrue(hasFontSizeIssue, "Should detect font-size not defined in relative units");
    }

    @Test
    void testCheckCompliance_relativeFontSize() {
        String html = "<html><body>"
                + "<p style='font-size: 1.2em;'>Text with relative font size</p>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = visualPresentationAudit.checkCompliance(doc)
                .stream()
                .filter(issue -> !issue.getRecommendation().isEmpty())
                .collect(Collectors.toList());

        boolean hasFontSizeIssue = issues.stream()
                .anyMatch(issue -> "Font Size Issue".equals(issue.getTitle()));
        assertFalse(hasFontSizeIssue, "Should not flag font-size when relative units (em) are used");
    }

    @Test
    void testCheckCompliance_textJustify() {
        String html = "<html><body>"
                + "<p style='text-align: justify;'>Justified text content here.</p>"
                + "</body></html>";
        Document doc = Jsoup.parse(html);

        List<GenericIssue> issues = visualPresentationAudit.checkCompliance(doc)
                .stream()
                .filter(issue -> !issue.getRecommendation().isEmpty())
                .collect(Collectors.toList());

        assertFalse(issues.isEmpty(), "Should find issues when text-align is justify");
        boolean hasJustifyIssue = issues.stream()
                .anyMatch(issue -> "Text Justification Issue".equals(issue.getTitle()));
        assertTrue(hasJustifyIssue, "Should detect justified text alignment");
    }

    // ---- Execute tests ----

    @Test
    void testExecute_noIssues() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><body><p>Clean page with no inline styles.</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        Audit result = visualPresentationAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "Should have no issue messages for a clean page");
        assertEquals(0, result.getPoints());
        verify(mockAuditService, times(1)).save(any(Audit.class));
    }

    @Test
    void testExecute_withInlineColorAndAbsoluteFontSize() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><body>"
                + "<p style='color: #000; background-color: #fff; font-size: 14px;'>Styled text</p>"
                + "</body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        Audit result = visualPresentationAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty(), "Should have issue messages for styled elements");
        verify(mockElementStateService, atLeastOnce()).findByPageAndCssSelector(anyLong(), anyString());
        verify(mockAuditService, times(1)).save(any(Audit.class));
    }
}
