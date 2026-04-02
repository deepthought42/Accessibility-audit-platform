package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.ReflowAudit;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;
import com.looksee.services.PageStateService;

public class ReflowAuditExecuteTest {

    private ReflowAudit reflowAudit;
    private AuditService mockAuditService;
    private ElementStateService mockElementStateService;
    private PageStateService mockPageStateService;

    @BeforeEach
    void setUp() throws Exception {
        reflowAudit = new ReflowAudit();
        mockAuditService = mock(AuditService.class);
        mockElementStateService = mock(ElementStateService.class);
        mockPageStateService = mock(PageStateService.class);

        Field f1 = ReflowAudit.class.getDeclaredField("auditService");
        f1.setAccessible(true);
        f1.set(reflowAudit, mockAuditService);

        Field f2 = ReflowAudit.class.getDeclaredField("elementStateService");
        f2.setAccessible(true);
        f2.set(reflowAudit, mockElementStateService);

        Field f3 = ReflowAudit.class.getDeclaredField("pageStateService");
        f3.setAccessible(true);
        f3.set(reflowAudit, mockPageStateService);

        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- isFixedWidth tests via reflection ---

    private boolean invokeIsFixedWidth(String width) throws Exception {
        Method method = ReflowAudit.class.getDeclaredMethod("isFixedWidth", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(reflowAudit, width);
    }

    @Test
    void testIsFixedWidth_px() throws Exception {
        assertTrue(invokeIsFixedWidth("500px"), "Pixel values should be considered fixed width");
    }

    @Test
    void testIsFixedWidth_percent() throws Exception {
        assertTrue(invokeIsFixedWidth("50%"), "Percentage values should be considered fixed width");
    }

    @Test
    void testIsFixedWidth_em() throws Exception {
        assertFalse(invokeIsFixedWidth("10em"), "Em values should not be considered fixed width");
    }

    @Test
    void testIsFixedWidth_auto() throws Exception {
        assertFalse(invokeIsFixedWidth("auto"), "Auto should not be considered fixed width");
    }

    @Test
    void testIsFixedWidth_null() throws Exception {
        Method method = ReflowAudit.class.getDeclaredMethod("isFixedWidth", String.class);
        method.setAccessible(true);
        try {
            method.invoke(reflowAudit, (String) null);
            // If no exception, the method handled null gracefully
        } catch (Exception e) {
            // Expected: null may cause assertion error or NPE
            assertTrue(e.getCause() instanceof AssertionError
                    || e.getCause() instanceof NullPointerException,
                    "Null input should cause AssertionError or NullPointerException");
        }
    }

    // --- checkElementForReflowCompliance tests via reflection ---

    private UXIssueMessage invokeCheckElementForReflowCompliance(ElementState element) throws Exception {
        Method method = ReflowAudit.class.getDeclaredMethod("checkElementForReflowCompliance", ElementState.class);
        method.setAccessible(true);
        return (UXIssueMessage) method.invoke(reflowAudit, element);
    }

    @Test
    void testCheckElementForReflowCompliance_fixedWidthHiddenOverflow() throws Exception {
        ElementState elem = mock(ElementState.class);
        Map<String, String> cssValues = new HashMap<>();
        cssValues.put("width", "500px");
        cssValues.put("overflow", "hidden");
        when(elem.getRenderedCssValues()).thenReturn(cssValues);
        when(elem.getName()).thenReturn("div");
        when(elem.getCssSelector()).thenReturn("body > div");

        UXIssueMessage issue = invokeCheckElementForReflowCompliance(elem);

        assertNotNull(issue, "Should return an issue for fixed width with hidden overflow");
        assertTrue(issue.getTitle().contains("doesn't adjust to fit the viewport"),
                "Issue title should indicate reflow problem");
    }

    @Test
    void testCheckElementForReflowCompliance_flexibleWidth() throws Exception {
        ElementState elem = mock(ElementState.class);
        Map<String, String> cssValues = new HashMap<>();
        cssValues.put("width", "auto");
        cssValues.put("overflow", "visible");
        when(elem.getRenderedCssValues()).thenReturn(cssValues);
        when(elem.getName()).thenReturn("section");

        UXIssueMessage issue = invokeCheckElementForReflowCompliance(elem);

        assertNotNull(issue, "Should still return a message (passing observation)");
        assertTrue(issue.getTitle().contains("adjusts to fit the viewport"),
                "Issue title should indicate content adjusts properly");
    }

    // --- execute() tests ---

    @Test
    void testExecute_noElements() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(mockPageStateService.getElementStates(1L)).thenReturn(new ArrayList<>());

        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        Audit result = reflowAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result, "Audit result should not be null");
        assertTrue(result.getMessages().isEmpty(), "Should have no issues for empty page");
        verify(mockAuditService).save(any(Audit.class));
    }

    @Test
    void testExecute_withFixedWidthElements() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");

        ElementState elem1 = mock(ElementState.class);
        Map<String, String> cssValues1 = new HashMap<>();
        cssValues1.put("width", "500px");
        cssValues1.put("overflow", "hidden");
        when(elem1.getRenderedCssValues()).thenReturn(cssValues1);
        when(elem1.getName()).thenReturn("div");
        when(elem1.getCssSelector()).thenReturn("body > div.container");

        ElementState elem2 = mock(ElementState.class);
        Map<String, String> cssValues2 = new HashMap<>();
        cssValues2.put("width", "300px");
        cssValues2.put("overflow", "scroll");
        when(elem2.getRenderedCssValues()).thenReturn(cssValues2);
        when(elem2.getName()).thenReturn("section");
        when(elem2.getCssSelector()).thenReturn("body > section");

        List<ElementState> elements = new ArrayList<>();
        elements.add(elem1);
        elements.add(elem2);
        when(mockPageStateService.getElementStates(1L)).thenReturn(elements);

        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        Audit result = reflowAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result, "Audit result should not be null");
        assertFalse(result.getMessages().isEmpty(), "Should have issues for fixed width elements");
        assertEquals(2, result.getMessages().size(),
                "Should have one issue per element");
        verify(mockAuditService).save(any(Audit.class));
    }
}
