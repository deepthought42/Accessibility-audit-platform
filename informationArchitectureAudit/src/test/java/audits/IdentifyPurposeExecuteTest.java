package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.IdentifyPurposeAudit;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;

public class IdentifyPurposeExecuteTest {

    private IdentifyPurposeAudit audit;
    private AuditService mockAuditService;
    private ElementStateService mockElementStateService;

    @BeforeEach
    void setUp() throws Exception {
        audit = new IdentifyPurposeAudit();
        mockAuditService = mock(AuditService.class);
        mockElementStateService = mock(ElementStateService.class);

        Field f1 = IdentifyPurposeAudit.class.getDeclaredField("auditService");
        f1.setAccessible(true);
        f1.set(audit, mockAuditService);

        Field f2 = IdentifyPurposeAudit.class.getDeclaredField("elementStateService");
        f2.setAccessible(true);
        f2.set(audit, mockElementStateService);

        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mockElementStateService.findByPageAndCssSelector(anyLong(), anyString()))
                .thenReturn(mock(ElementState.class));
    }

    @Test
    void testExecute_compliantPage() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><body>"
                + "<img alt=\"Logo\" src=\"logo.png\">"
                + "<button aria-label=\"Submit\">Submit</button>"
                + "<nav role=\"navigation\" aria-label=\"Main\">Nav</nav>"
                + "</body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        // Compliant page should have no issues from checkCompliance
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_nonCompliantImages() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><body><img src=\"photo.jpg\"><img src=\"banner.png\"></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_nonCompliantButtons() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><body><button>Click</button><input type=\"button\" value=\"Go\"></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_nonCompliantRegions() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><body><div role=\"banner\">Header</div><section role=\"main\">Content</section></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_emptyPage() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn("<html><body></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }
}
