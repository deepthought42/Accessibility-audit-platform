package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.models.ListStructureAudit;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;

public class ListStructureAuditExecuteTest {

    private ListStructureAudit audit;
    private AuditService mockAuditService;
    private ElementStateService mockElementStateService;

    @BeforeEach
    void setUp() throws Exception {
        audit = new ListStructureAudit();
        mockAuditService = mock(AuditService.class);
        mockElementStateService = mock(ElementStateService.class);

        Field f1 = ListStructureAudit.class.getDeclaredField("auditService");
        f1.setAccessible(true);
        f1.set(audit, mockAuditService);

        Field f2 = ListStructureAudit.class.getDeclaredField("elementStateService");
        f2.setAccessible(true);
        f2.set(audit, mockElementStateService);

        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mockElementStateService.findByPageAndCssSelector(anyLong(), anyString()))
                .thenReturn(mock(ElementState.class));
    }

    @Test
    void testExecute_compliantLists() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><body><ul><li>Item 1</li><li>Item 2</li></ul>"
                + "<ol><li>First</li><li>Second</li></ol></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_nonCompliantLists() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><body><ul><div>Not a list item</div></ul></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_noLists() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn("<html><body><p>No lists</p></body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_mixedCompliance() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(pageState.getSrc()).thenReturn(
                "<html><body>"
                + "<ul><li>Good</li></ul>"
                + "<ol><span>Bad</span></ol>"
                + "</body></html>");
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertEquals(1, result.getMessages().size());
    }
}
