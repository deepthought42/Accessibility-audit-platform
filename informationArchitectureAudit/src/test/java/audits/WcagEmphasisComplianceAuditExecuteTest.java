package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.models.WcagEmphasisComplianceAudit;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;

public class WcagEmphasisComplianceAuditExecuteTest {

    private WcagEmphasisComplianceAudit wcagEmphasisComplianceAudit;
    private AuditService mockAuditService;
    private ElementStateService mockElementStateService;

    @BeforeEach
    void setUp() throws Exception {
        wcagEmphasisComplianceAudit = new WcagEmphasisComplianceAudit();
        mockAuditService = mock(AuditService.class);
        mockElementStateService = mock(ElementStateService.class);

        Field f1 = WcagEmphasisComplianceAudit.class.getDeclaredField("auditService");
        f1.setAccessible(true);
        f1.set(wcagEmphasisComplianceAudit, mockAuditService);

        Field f2 = WcagEmphasisComplianceAudit.class.getDeclaredField("elementStateService");
        f2.setAccessible(true);
        f2.set(wcagEmphasisComplianceAudit, mockElementStateService);

        when(mockElementStateService.findByPageAndCssSelector(anyLong(), anyString()))
                .thenReturn(mock(ElementState.class));
        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Audit executeWithHtml(String html) {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(html);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        return wcagEmphasisComplianceAudit.execute(pageState, auditRecord, designSystem);
    }

    @Test
    void testExecute_noIssues() {
        String html = "<html><head><title>Clean Page</title></head>"
                + "<body><h1>Hello World</h1><p>This is a clean page.</p></body></html>";

        Audit result = executeWithHtml(html);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "Clean HTML should produce no issues");
        assertEquals(0, result.getPoints());
        assertEquals(0, result.getTotalPossiblePoints());
        verify(mockAuditService, times(1)).save(any(Audit.class));
        verify(mockElementStateService, never()).findByPageAndCssSelector(anyLong(), anyString());
    }

    @Test
    void testExecute_withBoldElements() {
        String html = "<html><head><title>Test</title></head>"
                + "<body><p>This has <b>bold text</b> and <b>more bold</b>.</p></body></html>";

        Audit result = executeWithHtml(html);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty(), "Should have issues for <b> tags");
        assertEquals(2, result.getMessages().size(), "Should have 2 issues for 2 <b> tags");
        assertEquals(0, result.getPoints());
        assertEquals(2, result.getTotalPossiblePoints());
        verify(mockElementStateService, times(2)).findByPageAndCssSelector(anyLong(), anyString());
    }

    @Test
    void testExecute_withItalicElements() {
        String html = "<html><head><title>Test</title></head>"
                + "<body><p>This has <i>italic text</i>.</p></body></html>";

        Audit result = executeWithHtml(html);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty(), "Should have issues for <i> tags");
        assertEquals(1, result.getMessages().size(), "Should have 1 issue for 1 <i> tag");
        assertEquals(0, result.getPoints());
        assertEquals(1, result.getTotalPossiblePoints());
        verify(mockElementStateService, times(1)).findByPageAndCssSelector(anyLong(), anyString());
    }

    @Test
    void testExecute_withAbbrWithoutTitle() {
        String html = "<html><head><title>Test</title></head>"
                + "<body><p>The <abbr>HTML</abbr> specification.</p></body></html>";

        Audit result = executeWithHtml(html);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty(), "Should have issue for <abbr> without title");
        assertEquals(1, result.getMessages().size(), "Should have 1 issue for <abbr> missing title");
        assertEquals(0, result.getPoints());
        assertEquals(1, result.getTotalPossiblePoints());
    }

    @Test
    void testExecute_withAbbrWithTitle() {
        String html = "<html><head><title>Test</title></head>"
                + "<body><p>The <abbr title=\"HyperText Markup Language\">HTML</abbr> specification.</p></body></html>";

        Audit result = executeWithHtml(html);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "<abbr> with title should be compliant");
        assertEquals(0, result.getPoints());
        assertEquals(0, result.getTotalPossiblePoints());
        verify(mockElementStateService, never()).findByPageAndCssSelector(anyLong(), anyString());
    }

    @Test
    void testExecute_withStrongAndCode() {
        String html = "<html><head><title>Test</title></head>"
                + "<body><p>This is <strong>important</strong> and <code>System.out</code>.</p></body></html>";

        Audit result = executeWithHtml(html);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty(), "Should have issues for <strong> and <code>");
        assertEquals(2, result.getMessages().size(), "Should have 2 issues for <strong> and <code>");
        assertEquals(0, result.getPoints());
        assertEquals(2, result.getTotalPossiblePoints());
        verify(mockElementStateService, times(2)).findByPageAndCssSelector(anyLong(), anyString());
    }
}
