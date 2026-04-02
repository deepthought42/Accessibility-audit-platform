package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.SecurityAudit;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;

public class SecurityAuditTest {

    // --- makeDistinct tests ---

    @Test
    void testMakeDistinct_withDuplicates() {
        List<String> input = Arrays.asList("banana", "apple", "banana", "cherry", "apple");
        List<String> result = SecurityAudit.makeDistinct(input);

        assertEquals(3, result.size());
        assertEquals(Arrays.asList("apple", "banana", "cherry"), result);
    }

    @Test
    void testMakeDistinct_emptyList() {
        List<String> input = new ArrayList<>();
        List<String> result = SecurityAudit.makeDistinct(input);

        assertTrue(result.isEmpty());
    }

    @Test
    void testMakeDistinct_noDuplicates() {
        List<String> input = Arrays.asList("cherry", "banana", "apple");
        List<String> result = SecurityAudit.makeDistinct(input);

        assertEquals(3, result.size());
        assertEquals(Arrays.asList("apple", "banana", "cherry"), result);
    }

    @Test
    void testMakeDistinct_allSame() {
        List<String> input = Arrays.asList("test", "test", "test", "test");
        List<String> result = SecurityAudit.makeDistinct(input);

        assertEquals(1, result.size());
        assertEquals("test", result.get(0));
    }

    @Test
    void testMakeDistinct_singleElement() {
        List<String> input = Arrays.asList("only");
        List<String> result = SecurityAudit.makeDistinct(input);

        assertEquals(1, result.size());
        assertEquals("only", result.get(0));
    }

    @Test
    void testMakeDistinct_resultIsSorted() {
        List<String> input = Arrays.asList("zebra", "alpha", "mango", "alpha", "zebra");
        List<String> result = SecurityAudit.makeDistinct(input);

        assertEquals(3, result.size());
        // Verify sorted order
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).compareTo(result.get(i + 1)) < 0);
        }
    }

    // --- execute() tests using reflection ---

    private SecurityAudit createAuditWithMocks(AuditService mockAuditService, UXIssueMessageService mockIssueService) throws Exception {
        SecurityAudit audit = new SecurityAudit();

        Field f1 = SecurityAudit.class.getDeclaredField("audit_service");
        f1.setAccessible(true);
        f1.set(audit, mockAuditService);

        Field f2 = SecurityAudit.class.getDeclaredField("issue_message_service");
        f2.setAccessible(true);
        f2.set(audit, mockIssueService);

        return audit;
    }

    @Test
    void execute_withSecuredPageState() throws Exception {
        AuditService mockAuditService = mock(AuditService.class);
        UXIssueMessageService mockIssueService = mock(UXIssueMessageService.class);

        when(mockIssueService.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mockAuditService.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SecurityAudit audit = createAuditWithMocks(mockAuditService, mockIssueService);

        PageState pageState = mock(PageState.class);
        when(pageState.isSecured()).thenReturn(true);
        when(pageState.getUrl()).thenReturn("https://example.com");

        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        Audit result = audit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        // Secured page should earn 1 point out of 1
        assertEquals(1, result.getPoints());
        assertEquals(1, result.getTotalPossiblePoints());
        verify(mockIssueService, times(1)).save(any());
        verify(mockAuditService, times(1)).save(any());
    }

    @Test
    void execute_withUnsecuredPageState() throws Exception {
        AuditService mockAuditService = mock(AuditService.class);
        UXIssueMessageService mockIssueService = mock(UXIssueMessageService.class);

        when(mockIssueService.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mockAuditService.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SecurityAudit audit = createAuditWithMocks(mockAuditService, mockIssueService);

        PageState pageState = mock(PageState.class);
        when(pageState.isSecured()).thenReturn(false);
        when(pageState.getUrl()).thenReturn("http://example.com");

        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        Audit result = audit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        // Unsecured page should earn 0 points out of 1
        assertEquals(0, result.getPoints());
        assertEquals(1, result.getTotalPossiblePoints());
        verify(mockIssueService, times(1)).save(any());
        verify(mockAuditService, times(1)).save(any());
    }
}
