package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.models.KeyboardAccessibleAudit;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditService;
import com.looksee.services.PageStateService;

public class KeyboardAccessibleAuditExecuteTest {

    private KeyboardAccessibleAudit keyboardAccessibleAudit;
    private AuditService mockAuditService;
    private PageStateService mockPageStateService;

    @BeforeEach
    void setUp() throws Exception {
        keyboardAccessibleAudit = new KeyboardAccessibleAudit();
        mockAuditService = mock(AuditService.class);
        mockPageStateService = mock(PageStateService.class);

        Field f = KeyboardAccessibleAudit.class.getDeclaredField("auditService");
        f.setAccessible(true);
        f.set(keyboardAccessibleAudit, mockAuditService);

        Field f2 = KeyboardAccessibleAudit.class.getDeclaredField("pageStateService");
        f2.setAccessible(true);
        f2.set(keyboardAccessibleAudit, mockPageStateService);

        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ========== Static method tests for checkKeyboardAccessibility ==========

    @Test
    void testCheckKeyboardAccessibility_allAccessible() {
        List<ElementState> elements = new ArrayList<>();

        ElementState el1 = mock(ElementState.class);
        when(el1.getAttribute("tabindex")).thenReturn("0");
        when(el1.getAttribute("role")).thenReturn(null);
        elements.add(el1);

        ElementState el2 = mock(ElementState.class);
        when(el2.getAttribute("tabindex")).thenReturn("1");
        when(el2.getAttribute("role")).thenReturn("");
        elements.add(el2);

        List<ElementState> result = KeyboardAccessibleAudit.checkKeyboardAccessibility(elements);
        assertTrue(result.isEmpty(), "All elements with tabindex should be accessible");
    }

    @Test
    void testCheckKeyboardAccessibility_noneAccessible() {
        List<ElementState> elements = new ArrayList<>();

        ElementState el1 = mock(ElementState.class);
        when(el1.getAttribute("tabindex")).thenReturn(null);
        when(el1.getAttribute("role")).thenReturn(null);
        elements.add(el1);

        ElementState el2 = mock(ElementState.class);
        when(el2.getAttribute("tabindex")).thenReturn("");
        when(el2.getAttribute("role")).thenReturn("");
        elements.add(el2);

        List<ElementState> result = KeyboardAccessibleAudit.checkKeyboardAccessibility(elements);
        assertEquals(2, result.size(), "Elements without tabindex or role should be non-accessible");
    }

    @Test
    void testCheckKeyboardAccessibility_mixedAccessibility() {
        List<ElementState> elements = new ArrayList<>();

        ElementState accessible = mock(ElementState.class);
        when(accessible.getAttribute("tabindex")).thenReturn("0");
        when(accessible.getAttribute("role")).thenReturn(null);
        elements.add(accessible);

        ElementState nonAccessible = mock(ElementState.class);
        when(nonAccessible.getAttribute("tabindex")).thenReturn(null);
        when(nonAccessible.getAttribute("role")).thenReturn(null);
        elements.add(nonAccessible);

        ElementState alsoAccessible = mock(ElementState.class);
        when(alsoAccessible.getAttribute("tabindex")).thenReturn("");
        when(alsoAccessible.getAttribute("role")).thenReturn("button");
        elements.add(alsoAccessible);

        List<ElementState> result = KeyboardAccessibleAudit.checkKeyboardAccessibility(elements);
        assertEquals(1, result.size(), "Only one element should be non-accessible");
        assertSame(nonAccessible, result.get(0));
    }

    @Test
    void testCheckKeyboardAccessibility_emptyList() {
        List<ElementState> elements = new ArrayList<>();
        List<ElementState> result = KeyboardAccessibleAudit.checkKeyboardAccessibility(elements);
        assertTrue(result.isEmpty(), "Empty input should return empty list");
    }

    @Test
    void testCheckKeyboardAccessibility_elementWithRole() {
        List<ElementState> elements = new ArrayList<>();

        ElementState el = mock(ElementState.class);
        when(el.getAttribute("tabindex")).thenReturn(null);
        when(el.getAttribute("role")).thenReturn("button");
        elements.add(el);

        List<ElementState> result = KeyboardAccessibleAudit.checkKeyboardAccessibility(elements);
        assertTrue(result.isEmpty(), "Element with role should be considered accessible");
    }

    // ========== Execute method tests ==========

    @Test
    void testExecute_noElements() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        when(mockPageStateService.getElementStates(1L)).thenReturn(Collections.emptyList());

        Audit result = keyboardAccessibleAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "No elements means no issues");
        assertEquals(0, result.getPoints());
        assertEquals(0, result.getTotalPossiblePoints());
        verify(mockAuditService, times(1)).save(any(Audit.class));
    }

    @Test
    void testExecute_withNonAccessibleElements() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(2L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        List<ElementState> elements = new ArrayList<>();

        ElementState accessible = mock(ElementState.class);
        when(accessible.getAttribute("tabindex")).thenReturn("0");
        when(accessible.getAttribute("role")).thenReturn(null);
        elements.add(accessible);

        ElementState nonAccessible1 = mock(ElementState.class);
        when(nonAccessible1.getAttribute("tabindex")).thenReturn(null);
        when(nonAccessible1.getAttribute("role")).thenReturn(null);
        elements.add(nonAccessible1);

        ElementState nonAccessible2 = mock(ElementState.class);
        when(nonAccessible2.getAttribute("tabindex")).thenReturn("");
        when(nonAccessible2.getAttribute("role")).thenReturn("");
        elements.add(nonAccessible2);

        when(mockPageStateService.getElementStates(2L)).thenReturn(elements);

        Audit result = keyboardAccessibleAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty(), "Should have issue messages for non-accessible elements");
        assertEquals(2, result.getMessages().size(), "Should have 2 issue messages");
        assertEquals(0, result.getPoints(), "Points earned should be 0 for non-accessible elements");
        assertEquals(2, result.getTotalPossiblePoints(), "Max points should be 2 (1 per issue)");
        verify(mockAuditService, times(1)).save(any(Audit.class));
    }

    @Test
    void testExecute_allAccessible() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(3L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        List<ElementState> elements = new ArrayList<>();

        ElementState el1 = mock(ElementState.class);
        when(el1.getAttribute("tabindex")).thenReturn("0");
        when(el1.getAttribute("role")).thenReturn(null);
        elements.add(el1);

        ElementState el2 = mock(ElementState.class);
        when(el2.getAttribute("tabindex")).thenReturn(null);
        when(el2.getAttribute("role")).thenReturn("link");
        elements.add(el2);

        when(mockPageStateService.getElementStates(3L)).thenReturn(elements);

        Audit result = keyboardAccessibleAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty(), "All accessible elements should produce no issues");
        assertEquals(0, result.getPoints());
        assertEquals(0, result.getTotalPossiblePoints());
        verify(mockAuditService, times(1)).save(any(Audit.class));
    }
}
