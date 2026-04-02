package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.SecurityAudit;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;

public class SecurityAuditExecuteTest {

    private SecurityAudit securityAudit;
    private AuditService mockAuditService;
    private UXIssueMessageService mockIssueService;

    @BeforeEach
    void setUp() throws Exception {
        securityAudit = new SecurityAudit();
        mockAuditService = mock(AuditService.class);
        mockIssueService = mock(UXIssueMessageService.class);

        Field f1 = SecurityAudit.class.getDeclaredField("audit_service");
        f1.setAccessible(true);
        f1.set(securityAudit, mockAuditService);

        Field f2 = SecurityAudit.class.getDeclaredField("issue_message_service");
        f2.setAccessible(true);
        f2.set(securityAudit, mockIssueService);

        when(mockIssueService.save(any(UXIssueMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void testExecute_securedPage() {
        PageState pageState = mock(PageState.class);
        when(pageState.isSecured()).thenReturn(true);
        when(pageState.getUrl()).thenReturn("https://example.com");
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        Audit result = securityAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("Page is secure")));
        assertEquals(1, result.getPoints());
    }

    @Test
    void testExecute_unsecuredPage() {
        PageState pageState = mock(PageState.class);
        when(pageState.isSecured()).thenReturn(false);
        when(pageState.getUrl()).thenReturn("http://example.com");
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        Audit result = securityAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
        assertTrue(result.getMessages().stream()
                .anyMatch(msg -> msg.getTitle().contains("Page isn't secure")));
        assertEquals(0, result.getPoints());
    }

    @Test
    void testExecute_verifySaveCalledOnce() {
        PageState pageState = mock(PageState.class);
        when(pageState.isSecured()).thenReturn(true);
        when(pageState.getUrl()).thenReturn("https://example.com");
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        securityAudit.execute(pageState, auditRecord, designSystem);

        verify(mockAuditService, times(1)).save(any(Audit.class));
        verify(mockIssueService, times(1)).save(any(UXIssueMessage.class));
    }
}
