package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.UseOfColorAudit;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.GenericIssue;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;

public class UseOfColorAuditExecuteTest {

    private UseOfColorAudit useOfColorAudit;
    private AuditService mockAuditService;
    private ElementStateService mockElementStateService;

    @BeforeEach
    void setUp() throws Exception {
        useOfColorAudit = new UseOfColorAudit();
        mockAuditService = mock(AuditService.class);
        mockElementStateService = mock(ElementStateService.class);

        Field f1 = UseOfColorAudit.class.getDeclaredField("auditService");
        f1.setAccessible(true);
        f1.set(useOfColorAudit, mockAuditService);

        Field f2 = UseOfColorAudit.class.getDeclaredField("elementStateService");
        f2.setAccessible(true);
        f2.set(useOfColorAudit, mockElementStateService);

        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mockElementStateService.findByPageAndCssSelector(anyLong(), anyString()))
                .thenReturn(mock(ElementState.class));
    }

    // --- checkCompliance() static method tests ---

    @Test
    void testCheckCompliance_noInlineStyles() {
        String html = "<html><body>"
                + "<div>Plain text</div>"
                + "<p>No inline styles here</p>"
                + "<span>Clean content</span>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = UseOfColorAudit.checkCompliance(doc);

        assertTrue(issues.isEmpty(), "Should have no issues when there are no inline styles");
    }

    @Test
    void testCheckCompliance_elementWithInlineColor() {
        String html = "<html><body>"
                + "<span style=\"color: red\"></span>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = UseOfColorAudit.checkCompliance(doc);

        assertFalse(issues.isEmpty(), "Should detect issue when element uses color without textual indicator");
        assertEquals("Use of Color Violation", issues.get(0).getTitle());
        assertNotNull(issues.get(0).getDescription());
        assertNotNull(issues.get(0).getRecommendation());
    }

    @Test
    void testCheckCompliance_elementWithBackgroundColor() {
        String html = "<html><body>"
                + "<div style=\"background-color: #00FF00\"></div>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = UseOfColorAudit.checkCompliance(doc);

        assertFalse(issues.isEmpty(), "Should detect issue when element uses background-color without textual indicator");
        assertEquals("Use of Color Violation", issues.get(0).getTitle());
        assertNotNull(issues.get(0).getCssSelector(), "Issue should have a CSS selector");
    }

    @Test
    void testCheckCompliance_multipleColoredElements() {
        String html = "<html><body>"
                + "<div style=\"color: red\"></div>"
                + "<span style=\"background-color: blue\"></span>"
                + "<p style=\"color: green\"></p>"
                + "</body></html>";

        Document doc = Jsoup.parse(html);
        List<GenericIssue> issues = UseOfColorAudit.checkCompliance(doc);

        assertTrue(issues.size() >= 2, "Should detect multiple color-only issues across elements");
        for (GenericIssue issue : issues) {
            assertEquals("Use of Color Violation", issue.getTitle());
        }
    }

    // --- execute() tests ---

    @Test
    void testExecute_noIssues() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn("<html><body>"
                + "<div>Plain text content</div>"
                + "<p>No color issues here</p>"
                + "</body></html>");

        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        Audit result = useOfColorAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result, "Audit result should not be null");
        assertTrue(result.getMessages().isEmpty(), "Should have no issues for clean page");
        verify(mockAuditService).save(any(Audit.class));
    }

    @Test
    void testExecute_withColorIssues() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn("<html><body>"
                + "<div style=\"color: red\"></div>"
                + "<span style=\"background-color: blue\"></span>"
                + "</body></html>");

        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        Audit result = useOfColorAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result, "Audit result should not be null");
        assertFalse(result.getMessages().isEmpty(), "Should have issues for elements using color alone");
        verify(mockAuditService).save(any(Audit.class));
        verify(mockElementStateService, atLeastOnce()).findByPageAndCssSelector(anyLong(), anyString());
    }
}
