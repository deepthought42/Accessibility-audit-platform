package com.looksee.visualDesignAudit.audit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;

@RunWith(MockitoJUnitRunner.class)
public class FontAuditTest {

    private FontAudit fontAudit;

    @Before
    public void setUp() {
        fontAudit = new FontAudit();
    }

    // ========== makeDistinct tests ==========

    @Test
    public void testMakeDistinct_removeDuplicates() {
        List<String> input = Arrays.asList("Arial", "Arial", "Helvetica");
        List<String> result = FontAudit.makeDistinct(input);
        assertEquals(2, result.size());
    }

    @Test
    public void testMakeDistinct_alreadyDistinct() {
        List<String> input = Arrays.asList("Arial", "Helvetica", "Georgia");
        List<String> result = FontAudit.makeDistinct(input);
        assertEquals(3, result.size());
    }

    @Test
    public void testMakeDistinct_emptyList() {
        List<String> input = new ArrayList<>();
        List<String> result = FontAudit.makeDistinct(input);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMakeDistinct_singleElement() {
        List<String> input = Arrays.asList("Arial");
        List<String> result = FontAudit.makeDistinct(input);
        assertEquals(1, result.size());
        assertEquals("Arial", result.get(0));
    }

    @Test
    public void testMakeDistinct_sorts() {
        List<String> input = Arrays.asList("Zilla", "Arial", "Helvetica");
        List<String> result = FontAudit.makeDistinct(input);
        assertEquals("Arial", result.get(0));
        assertEquals("Helvetica", result.get(1));
        assertEquals("Zilla", result.get(2));
    }

    @Test
    public void testMakeDistinct_allDuplicates() {
        List<String> input = Arrays.asList("16px", "16px", "16px");
        List<String> result = FontAudit.makeDistinct(input);
        assertEquals(1, result.size());
    }

    @Test
    public void testMakeDistinct_numbericStrings() {
        List<String> input = Arrays.asList("16px", "14px", "16px", "18px");
        List<String> result = FontAudit.makeDistinct(input);
        assertEquals(3, result.size());
    }

    // ========== execute tests ==========

    @Test
    public void testExecute_withNoElements() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        when(pageState.getElements()).thenReturn(new ArrayList<>());
        when(pageState.getUrl()).thenReturn("https://example.com");

        Audit result = fontAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertEquals(AuditCategory.AESTHETICS, result.getCategory());
        assertEquals(AuditSubcategory.TYPOGRAPHY, result.getSubcategory());
        assertEquals(AuditName.FONT, result.getName());
    }

    @Test
    public void testExecute_withConsistentHeaders() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        List<ElementState> elements = new ArrayList<>();
        ElementState h1Element1 = createMockElement("h1", "24px", "normal", "400", "normal", "");
        ElementState h1Element2 = createMockElement("h1", "24px", "normal", "400", "normal", "");
        elements.add(h1Element1);
        elements.add(h1Element2);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");

        Audit result = fontAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertTrue(result.getPoints() > 0);
    }

    @Test
    public void testExecute_withInconsistentHeaders() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        List<ElementState> elements = new ArrayList<>();
        ElementState h1Element1 = createMockElement("h1", "24px", "normal", "400", "normal", "");
        ElementState h1Element2 = createMockElement("h1", "32px", "normal", "700", "normal", "");
        elements.add(h1Element1);
        elements.add(h1Element2);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");

        Audit result = fontAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    @Test
    public void testExecute_withSmallFontSize() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        List<ElementState> elements = new ArrayList<>();
        ElementState element = createMockElement("p", "10px", "normal", "400", "normal", "Some text");
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");

        Audit result = fontAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        // Small font should earn 0 points for that element
    }

    @Test
    public void testExecute_withProperFontSize() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        List<ElementState> elements = new ArrayList<>();
        ElementState element = createMockElement("p", "16px", "normal", "400", "normal", "Some text");
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");

        Audit result = fontAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertTrue(result.getPoints() > 0);
    }

    @Test
    public void testExecute_withMixedHeaderTypes() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        List<ElementState> elements = new ArrayList<>();
        elements.add(createMockElement("h1", "32px", "normal", "700", "normal", ""));
        elements.add(createMockElement("h2", "24px", "normal", "600", "normal", ""));
        elements.add(createMockElement("h3", "18px", "normal", "500", "normal", ""));
        elements.add(createMockElement("p", "16px", "normal", "400", "normal", "Body text"));

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");

        Audit result = fontAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertTrue(result.getPoints() >= 0);
    }

    @Test
    public void testExecute_withNoTextElements() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        List<ElementState> elements = new ArrayList<>();
        ElementState element = createMockElement("div", "16px", "normal", "400", "normal", "");
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");

        Audit result = fontAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    @Test
    public void testExecute_exactlyAt12px() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        List<ElementState> elements = new ArrayList<>();
        ElementState element = createMockElement("span", "12px", "normal", "400", "normal", "Border text");
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");

        Audit result = fontAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        // 12px is >= 12 so should pass
        assertTrue(result.getPoints() > 0);
    }

    private ElementState createMockElement(String name, String fontSize, String lineHeight,
                                           String fontWeight, String fontVariant, String ownedText) {
        ElementState element = mock(ElementState.class);
        when(element.getName()).thenReturn(name);
        when(element.getOwnedText()).thenReturn(ownedText);

        Map<String, String> cssValues = new HashMap<>();
        cssValues.put("font-size", fontSize);
        cssValues.put("line-height", lineHeight);
        cssValues.put("font-weight", fontWeight);
        cssValues.put("font-variant", fontVariant);
        when(element.getRenderedCssValues()).thenReturn(cssValues);

        return element;
    }
}
