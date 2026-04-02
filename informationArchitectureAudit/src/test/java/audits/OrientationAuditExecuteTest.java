package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.OrientationAudit;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;

public class OrientationAuditExecuteTest {

    private OrientationAudit audit;
    private AuditService mockAuditService;
    private ElementStateService mockElementStateService;

    @BeforeEach
    void setUp() throws Exception {
        audit = new OrientationAudit();
        mockAuditService = mock(AuditService.class);
        mockElementStateService = mock(ElementStateService.class);

        Field f1 = OrientationAudit.class.getDeclaredField("auditService");
        f1.setAccessible(true);
        f1.set(audit, mockAuditService);

        Field f2 = OrientationAudit.class.getDeclaredField("elementStateService");
        f2.setAccessible(true);
        f2.set(audit, mockElementStateService);

        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mockElementStateService.findByPageAndCssSelector(anyLong(), anyString()))
                .thenReturn(mock(ElementState.class));
    }

    @Test
    void testExecute_noOrientationRestrictions() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn("<html><body><p>Content</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        // checkOrientationRestrictions returns empty list, so no issues
        assertTrue(result.getMessages().isEmpty());
        verify(mockAuditService).save(any(Audit.class));
    }

    @Test
    void testExecute_withOrientationInStyle() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><head><style>@media (orientation: portrait) { body { display: none; } }</style></head>"
                + "<body><p>Content</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        // Even with orientation in style, the current implementation returns empty list
        verify(mockAuditService).save(any(Audit.class));
    }

    @Test
    void testExecute_withViewportMeta() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><head><meta name=\"viewport\" content=\"width=device-width, orientation=portrait\"></head>"
                + "<body><p>Content</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        verify(mockAuditService).save(any(Audit.class));
    }

    @Test
    void testExecute_landscapeOrientationStyle() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><head><style>@media (orientation: landscape) { .sidebar { width: 300px; } }</style></head>"
                + "<body><p>Content</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        verify(mockAuditService).save(any(Audit.class));
    }

    @Test
    void testExecute_multipleStyleTags() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><head>"
                + "<style>body { color: black; }</style>"
                + "<style>@media (orientation: portrait) { body { padding: 10px; } }</style>"
                + "</head><body><p>Content</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
    }

    @Test
    void testExecute_viewportWithoutOrientation() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"></head>"
                + "<body><p>Content</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_bothStyleAndViewportOrientation() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><head>"
                + "<meta name=\"viewport\" content=\"width=device-width, orientation=portrait\">"
                + "<style>@media (orientation: portrait) { body { display: none; } }</style>"
                + "</head><body><p>Content</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
    }
}
