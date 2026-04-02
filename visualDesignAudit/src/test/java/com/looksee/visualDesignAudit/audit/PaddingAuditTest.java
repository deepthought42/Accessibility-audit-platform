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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.Score;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.services.PageStateService;

@RunWith(MockitoJUnitRunner.class)
public class PaddingAuditTest {

    @Mock
    private PageStateService page_state_service;

    @InjectMocks
    private PaddingAudit paddingAudit;

    // ========== isMultipleOf8 tests ==========

    @Test
    public void testIsMultipleOf8_withZero() {
        assertTrue(PaddingAudit.isMultipleOf8("0px"));
    }

    @Test
    public void testIsMultipleOf8_with8px() {
        assertTrue(PaddingAudit.isMultipleOf8("8px"));
    }

    @Test
    public void testIsMultipleOf8_with16px() {
        assertTrue(PaddingAudit.isMultipleOf8("16px"));
    }

    @Test
    public void testIsMultipleOf8_with24px() {
        assertTrue(PaddingAudit.isMultipleOf8("24px"));
    }

    @Test
    public void testIsMultipleOf8_with32px() {
        assertTrue(PaddingAudit.isMultipleOf8("32px"));
    }

    @Test
    public void testIsMultipleOf8_with64px() {
        assertTrue(PaddingAudit.isMultipleOf8("64px"));
    }

    @Test
    public void testIsMultipleOf8_with4px() {
        assertTrue(PaddingAudit.isMultipleOf8("4px"));
    }

    @Test
    public void testIsMultipleOf8_with2px() {
        assertTrue(PaddingAudit.isMultipleOf8("2px"));
    }

    @Test
    public void testIsMultipleOf8_with1px() {
        assertTrue(PaddingAudit.isMultipleOf8("1px"));
    }

    @Test
    public void testIsMultipleOf8_with5px() {
        assertFalse(PaddingAudit.isMultipleOf8("5px"));
    }

    @Test
    public void testIsMultipleOf8_with10px() {
        assertFalse(PaddingAudit.isMultipleOf8("10px"));
    }

    @Test
    public void testIsMultipleOf8_with13px() {
        assertFalse(PaddingAudit.isMultipleOf8("13px"));
    }

    @Test
    public void testIsMultipleOf8_with48px() {
        assertTrue(PaddingAudit.isMultipleOf8("48px"));
    }

    @Test
    public void testIsMultipleOf8_withEmUnits() {
        assertTrue(PaddingAudit.isMultipleOf8("16em"));
    }

    @Test
    public void testIsMultipleOf8_withRemUnits() {
        assertTrue(PaddingAudit.isMultipleOf8("8rem"));
    }

    @Test
    public void testIsMultipleOf8_with3px() {
        assertFalse(PaddingAudit.isMultipleOf8("3px"));
    }

    @Test
    public void testIsMultipleOf8_with6px() {
        assertFalse(PaddingAudit.isMultipleOf8("6px"));
    }

    @Test
    public void testIsMultipleOf8_with7px() {
        assertTrue(PaddingAudit.isMultipleOf8("7px"));
    }

    // ========== execute tests ==========

    @Test
    public void testExecute_withEmptyElements() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        when(pageState.getKey()).thenReturn("test-key");
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(page_state_service.getElementStates("test-key")).thenReturn(new ArrayList<>());

        Audit result = paddingAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertEquals(AuditCategory.AESTHETICS, result.getCategory());
        assertEquals(AuditSubcategory.WHITESPACE, result.getSubcategory());
        assertEquals(AuditName.PADDING, result.getName());
    }

    @Test
    public void testExecute_withElementsHavingPadding() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        ElementState element = mock(ElementState.class);
        Map<String, String> cssValues = new HashMap<>();
        cssValues.put("padding-top", "16px");
        cssValues.put("padding-bottom", "8px");
        cssValues.put("padding-left", "0px");
        cssValues.put("padding-right", "0px");
        when(element.getRenderedCssValues()).thenReturn(cssValues);

        when(pageState.getKey()).thenReturn("test-key");
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(page_state_service.getElementStates("test-key")).thenReturn(Arrays.asList(element));

        Audit result = paddingAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertEquals(AuditCategory.AESTHETICS, result.getCategory());
    }

    @Test
    public void testExecute_withNonMultipleOf8Padding() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        ElementState element = mock(ElementState.class);
        Map<String, String> cssValues = new HashMap<>();
        cssValues.put("padding-top", "13px");
        cssValues.put("padding-bottom", "0px");
        cssValues.put("padding-left", "0px");
        cssValues.put("padding-right", "0px");
        when(element.getRenderedCssValues()).thenReturn(cssValues);

        when(pageState.getKey()).thenReturn("test-key");
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(page_state_service.getElementStates("test-key")).thenReturn(Arrays.asList(element));

        Audit result = paddingAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    @Test
    public void testExecute_withMultipleElements() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        ElementState element1 = mock(ElementState.class);
        Map<String, String> cssValues1 = new HashMap<>();
        cssValues1.put("padding-top", "8px");
        cssValues1.put("padding-bottom", "8px");
        cssValues1.put("padding-left", "16px");
        cssValues1.put("padding-right", "16px");
        when(element1.getRenderedCssValues()).thenReturn(cssValues1);

        ElementState element2 = mock(ElementState.class);
        Map<String, String> cssValues2 = new HashMap<>();
        cssValues2.put("padding-top", "5px");
        cssValues2.put("padding-bottom", "0px");
        when(element2.getRenderedCssValues()).thenReturn(cssValues2);

        when(pageState.getKey()).thenReturn("test-key");
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(page_state_service.getElementStates("test-key")).thenReturn(Arrays.asList(element1, element2));

        Audit result = paddingAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertTrue(result.getPoints() >= 0);
    }

    // ========== evaluateSpacingConsistency tests ==========

    @Test
    public void testEvaluateSpacingConsistency_emptyMap() {
        Map<ElementState, List<String>> emptyMap = new HashMap<>();
        Score result = paddingAudit.evaluateSpacingConsistency(emptyMap);

        assertNotNull(result);
        assertEquals(0, result.getPointsAchieved());
        assertEquals(0, result.getMaxPossiblePoints());
    }

    @Test
    public void testEvaluateSpacingConsistency_singleElement() {
        Map<ElementState, List<String>> map = new HashMap<>();
        ElementState element = mock(ElementState.class);
        map.put(element, Arrays.asList("16px"));

        Score result = paddingAudit.evaluateSpacingConsistency(map);
        assertNotNull(result);
    }

    @Test
    public void testEvaluateSpacingConsistency_consistentPadding() {
        Map<ElementState, List<String>> map = new HashMap<>();
        ElementState element1 = mock(ElementState.class);
        ElementState element2 = mock(ElementState.class);
        map.put(element1, Arrays.asList("8px", "16px"));
        map.put(element2, Arrays.asList("8px", "24px"));

        Score result = paddingAudit.evaluateSpacingConsistency(map);
        assertNotNull(result);
        assertTrue(result.getMaxPossiblePoints() > 0);
    }

    @Test
    public void testEvaluateSpacingConsistency_withAutoValues() {
        Map<ElementState, List<String>> map = new HashMap<>();
        ElementState element = mock(ElementState.class);
        map.put(element, Arrays.asList("auto", "0", "16px"));

        Score result = paddingAudit.evaluateSpacingConsistency(map);
        assertNotNull(result);
    }

    @Test
    public void testEvaluateSpacingConsistency_withMixedUnits() {
        Map<ElementState, List<String>> map = new HashMap<>();
        ElementState element = mock(ElementState.class);
        map.put(element, Arrays.asList("16px", "2em", "50%"));

        Score result = paddingAudit.evaluateSpacingConsistency(map);
        assertNotNull(result);
    }

    @Test
    public void testExecute_noPaddingKeys() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        ElementState element = mock(ElementState.class);
        Map<String, String> cssValues = new HashMap<>();
        cssValues.put("margin-top", "16px");
        when(element.getRenderedCssValues()).thenReturn(cssValues);

        when(pageState.getKey()).thenReturn("test-key");
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(page_state_service.getElementStates("test-key")).thenReturn(Arrays.asList(element));

        Audit result = paddingAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertEquals(0, result.getPoints());
    }
}
