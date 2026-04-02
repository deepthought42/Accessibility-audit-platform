package audits;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.looksee.audit.informationArchitecture.audits.TextSpacingAudit;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.services.AuditService;
import com.looksee.services.PageStateService;

public class TextSpacingAuditExecuteTest {

    private TextSpacingAudit audit;
    private AuditService mockAuditService;
    private PageStateService mockPageStateService;

    @BeforeEach
    void setUp() throws Exception {
        audit = new TextSpacingAudit();
        mockAuditService = mock(AuditService.class);
        mockPageStateService = mock(PageStateService.class);

        Field f1 = TextSpacingAudit.class.getDeclaredField("auditService");
        f1.setAccessible(true);
        f1.set(audit, mockAuditService);

        Field f2 = TextSpacingAudit.class.getDeclaredField("pageStateService");
        f2.setAccessible(true);
        f2.set(audit, mockPageStateService);

        when(mockAuditService.save(any(Audit.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ElementState createElementWithCss(String fontSize, String lineHeight, String letterSpacing, String wordSpacing, String marginBottom) {
        ElementState elem = mock(ElementState.class);
        Map<String, String> css = new HashMap<>();
        css.put("font-size", fontSize);
        css.put("line-height", lineHeight);
        css.put("letter-spacing", letterSpacing);
        css.put("word-spacing", wordSpacing);
        css.put("margin-bottom", marginBottom);
        when(elem.getRenderedCssValues()).thenReturn(css);
        return elem;
    }

    @Test
    void testExecute_noElements() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(mockPageStateService.getElementStates(1L)).thenReturn(new ArrayList<>());
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_compliantElements() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");

        // Compliant: line-height 24px >= 1.5 * 16px, letter-spacing 2px >= 0.12 * 16px,
        // word-spacing 3px >= 0.16 * 16px, margin-bottom 32px >= 2 * 16px
        ElementState elem = createElementWithCss("16px", "24px", "2px", "3px", "32px");
        List<ElementState> elements = new ArrayList<>();
        elements.add(elem);
        when(mockPageStateService.getElementStates(1L)).thenReturn(elements);
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_nonCompliantElements() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");

        // Non-compliant: small line-height, letter-spacing, word-spacing, margin-bottom
        ElementState elem = createElementWithCss("16px", "16px", "0px", "0px", "8px");
        List<ElementState> elements = new ArrayList<>();
        elements.add(elem);
        when(mockPageStateService.getElementStates(1L)).thenReturn(elements);
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
    }

    @Test
    void testExecute_multipleElements() {
        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(pageState.getUrl()).thenReturn("https://example.com");

        ElementState compliant = createElementWithCss("16px", "24px", "2px", "3px", "32px");
        ElementState nonCompliant = createElementWithCss("16px", "16px", "0px", "0px", "8px");
        List<ElementState> elements = new ArrayList<>();
        elements.add(compliant);
        elements.add(nonCompliant);
        when(mockPageStateService.getElementStates(1L)).thenReturn(elements);
        AuditRecord auditRecord = mock(AuditRecord.class);

        Audit result = audit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertFalse(result.getMessages().isEmpty());
    }

    // --- Static method tests ---

    @Test
    void testEvaluateTextSpacing_compliant() {
        ElementState elem = createElementWithCss("16px", "24px", "2px", "3px", "32px");
        List<ElementState> elements = new ArrayList<>();
        elements.add(elem);

        List<UXIssueMessage> issues = TextSpacingAudit.evaluateTextSpacing(elements);
        assertTrue(issues.isEmpty());
    }

    @Test
    void testEvaluateTextSpacing_insufficientLineHeight() {
        ElementState elem = createElementWithCss("16px", "16px", "2px", "3px", "32px");
        List<ElementState> elements = new ArrayList<>();
        elements.add(elem);

        List<UXIssueMessage> issues = TextSpacingAudit.evaluateTextSpacing(elements);
        assertTrue(issues.stream().anyMatch(i -> i.getTitle().contains("line height")));
    }

    @Test
    void testEvaluateTextSpacing_insufficientLetterSpacing() {
        ElementState elem = createElementWithCss("16px", "24px", "0px", "3px", "32px");
        List<ElementState> elements = new ArrayList<>();
        elements.add(elem);

        List<UXIssueMessage> issues = TextSpacingAudit.evaluateTextSpacing(elements);
        assertTrue(issues.stream().anyMatch(i -> i.getTitle().contains("letter spacing")));
    }

    @Test
    void testEvaluateTextSpacing_insufficientWordSpacing() {
        ElementState elem = createElementWithCss("16px", "24px", "2px", "0px", "32px");
        List<ElementState> elements = new ArrayList<>();
        elements.add(elem);

        List<UXIssueMessage> issues = TextSpacingAudit.evaluateTextSpacing(elements);
        assertTrue(issues.stream().anyMatch(i -> i.getTitle().contains("word spacing")));
    }

    @Test
    void testEvaluateTextSpacing_insufficientParagraphSpacing() {
        ElementState elem = createElementWithCss("16px", "24px", "2px", "3px", "8px");
        List<ElementState> elements = new ArrayList<>();
        elements.add(elem);

        List<UXIssueMessage> issues = TextSpacingAudit.evaluateTextSpacing(elements);
        assertTrue(issues.stream().anyMatch(i -> i.getTitle().contains("paragraph spacing")));
    }

    @Test
    void testEvaluateTextSpacing_emptyList() {
        List<UXIssueMessage> issues = TextSpacingAudit.evaluateTextSpacing(new ArrayList<>());
        assertTrue(issues.isEmpty());
    }

    @Test
    void testParseCssValue_px() {
        assertEquals(16.0, TextSpacingAudit.parseCssValue("16px"), 0.001);
    }

    @Test
    void testParseCssValue_em() {
        assertEquals(32.0, TextSpacingAudit.parseCssValue("2em"), 0.001);
    }

    @Test
    void testParseCssValue_rem() {
        assertEquals(24.0, TextSpacingAudit.parseCssValue("1.5rem"), 0.001);
    }

    @Test
    void testParseCssValue_pt() {
        assertEquals(13.33, TextSpacingAudit.parseCssValue("10pt"), 0.01);
    }

    @Test
    void testParseCssValue_percent() {
        assertEquals(16.0, TextSpacingAudit.parseCssValue("100%"), 0.001);
    }

    @Test
    void testParseCssValue_nullOrEmpty() {
        assertEquals(0.0, TextSpacingAudit.parseCssValue(null), 0.001);
        assertEquals(0.0, TextSpacingAudit.parseCssValue(""), 0.001);
    }

    @Test
    void testParseCssValue_invalidValue() {
        assertEquals(0.0, TextSpacingAudit.parseCssValue("auto"), 0.001);
    }

    @Test
    void testParseCssValue_cm() {
        assertEquals(37.795, TextSpacingAudit.parseCssValue("1cm"), 0.001);
    }

    @Test
    void testParseCssValue_in() {
        assertEquals(96.0, TextSpacingAudit.parseCssValue("1in"), 0.001);
    }
}
