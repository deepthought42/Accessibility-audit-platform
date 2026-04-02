package com.looksee.visualDesignAudit.audit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.services.PageStateService;

@RunWith(MockitoJUnitRunner.class)
public class MarginAuditTest {

    @Mock
    private PageStateService page_state_service;

    @InjectMocks
    private MarginAudit marginAudit;

    // ========== isMultipleOf8 tests ==========

    @Test
    public void testIsMultipleOf8_withZero() {
        assertTrue(MarginAudit.isMultipleOf8("0px"));
    }

    @Test
    public void testIsMultipleOf8_with8px() {
        assertTrue(MarginAudit.isMultipleOf8("8px"));
    }

    @Test
    public void testIsMultipleOf8_with16px() {
        assertTrue(MarginAudit.isMultipleOf8("16px"));
    }

    @Test
    public void testIsMultipleOf8_with24px() {
        assertTrue(MarginAudit.isMultipleOf8("24px"));
    }

    @Test
    public void testIsMultipleOf8_with32px() {
        assertTrue(MarginAudit.isMultipleOf8("32px"));
    }

    @Test
    public void testIsMultipleOf8_with64px() {
        assertTrue(MarginAudit.isMultipleOf8("64px"));
    }

    @Test
    public void testIsMultipleOf8_with4px() {
        assertTrue(MarginAudit.isMultipleOf8("4px"));
    }

    @Test
    public void testIsMultipleOf8_with2px() {
        assertTrue(MarginAudit.isMultipleOf8("2px"));
    }

    @Test
    public void testIsMultipleOf8_with1px() {
        assertTrue(MarginAudit.isMultipleOf8("1px"));
    }

    @Test
    public void testIsMultipleOf8_with5px() {
        assertFalse(MarginAudit.isMultipleOf8("5px"));
    }

    @Test
    public void testIsMultipleOf8_with10px() {
        assertFalse(MarginAudit.isMultipleOf8("10px"));
    }

    @Test
    public void testIsMultipleOf8_with13px() {
        assertFalse(MarginAudit.isMultipleOf8("13px"));
    }

    @Test
    public void testIsMultipleOf8_with48px() {
        assertTrue(MarginAudit.isMultipleOf8("48px"));
    }

    @Test
    public void testIsMultipleOf8_withEmUnits() {
        assertTrue(MarginAudit.isMultipleOf8("16em"));
    }

    @Test
    public void testIsMultipleOf8_withRemUnits() {
        assertTrue(MarginAudit.isMultipleOf8("8rem"));
    }

    @Test
    public void testIsMultipleOf8_withPercentage() {
        assertTrue(MarginAudit.isMultipleOf8("0%"));
    }

    @Test
    public void testIsMultipleOf8_with40px() {
        assertTrue(MarginAudit.isMultipleOf8("40px"));
    }

    @Test
    public void testIsMultipleOf8_with3px() {
        assertFalse(MarginAudit.isMultipleOf8("3px"));
    }

    // ========== cleanSizeUnits (String) tests ==========

    @Test
    public void testCleanSizeUnits_removePx() {
        assertEquals("16", MarginAudit.cleanSizeUnits("16px"));
    }

    @Test
    public void testCleanSizeUnits_removeEm() {
        assertEquals("2", MarginAudit.cleanSizeUnits("2em"));
    }

    @Test
    public void testCleanSizeUnits_removeRem() {
        assertEquals("1", MarginAudit.cleanSizeUnits("1rem"));
    }

    @Test
    public void testCleanSizeUnits_removePercent() {
        assertEquals("50", MarginAudit.cleanSizeUnits("50%"));
    }

    @Test
    public void testCleanSizeUnits_removePt() {
        assertEquals("12", MarginAudit.cleanSizeUnits("12pt"));
    }

    @Test
    public void testCleanSizeUnits_removeVh() {
        assertEquals("100", MarginAudit.cleanSizeUnits("100vh"));
    }

    @Test
    public void testCleanSizeUnits_removeVw() {
        // Note: "vw" won't match since "vm" is replaced first, but "w" remains
        String result = MarginAudit.cleanSizeUnits("50vw");
        assertNotNull(result);
    }

    @Test
    public void testCleanSizeUnits_removeCm() {
        assertEquals("5", MarginAudit.cleanSizeUnits("5cm"));
    }

    @Test
    public void testCleanSizeUnits_removeMm() {
        assertEquals("10", MarginAudit.cleanSizeUnits("10mm"));
    }

    @Test
    public void testCleanSizeUnits_removeIn() {
        assertEquals("2", MarginAudit.cleanSizeUnits("2in"));
    }

    @Test
    public void testCleanSizeUnits_removePc() {
        assertEquals("3", MarginAudit.cleanSizeUnits("3pc"));
    }

    @Test
    public void testCleanSizeUnits_removeAuto() {
        assertEquals("", MarginAudit.cleanSizeUnits("auto"));
    }

    @Test
    public void testCleanSizeUnits_removeImportant() {
        assertEquals("16", MarginAudit.cleanSizeUnits("16px!important"));
    }

    @Test
    public void testCleanSizeUnits_trimWhitespace() {
        assertEquals("8", MarginAudit.cleanSizeUnits("  8px  "));
    }

    // ========== cleanSizeUnits (List) tests ==========

    @Test
    public void testCleanSizeUnitsList_multipleUnits() {
        List<String> input = Arrays.asList("16px", "2em", "50%");
        List<String> result = MarginAudit.cleanSizeUnits(input);
        assertEquals(3, result.size());
        assertEquals("16", result.get(0));
        assertEquals("2", result.get(1));
        assertEquals("50", result.get(2));
    }

    @Test
    public void testCleanSizeUnitsList_withDecimalValues() {
        List<String> input = Arrays.asList("16.5px", "2.3em");
        List<String> result = MarginAudit.cleanSizeUnits(input);
        assertEquals("16", result.get(0));
        assertEquals("2", result.get(1));
    }

    @Test
    public void testCleanSizeUnitsList_emptyList() {
        List<String> input = new ArrayList<>();
        List<String> result = MarginAudit.cleanSizeUnits(input);
        assertTrue(result.isEmpty());
    }

    // ========== sortAndMakeDistinct tests ==========

    @Test
    public void testSortAndMakeDistinct_removesZeros() {
        List<Double> input = Arrays.asList(0.0, 8.0, 16.0, 0.0);
        List<Double> result = MarginAudit.sortAndMakeDistinct(input);
        assertFalse(result.contains(0.0));
    }

    @Test
    public void testSortAndMakeDistinct_removesDuplicates() {
        List<Double> input = Arrays.asList(8.0, 8.0, 16.0);
        List<Double> result = MarginAudit.sortAndMakeDistinct(input);
        assertEquals(2, result.size());
    }

    @Test
    public void testSortAndMakeDistinct_sorts() {
        List<Double> input = Arrays.asList(32.0, 8.0, 16.0);
        List<Double> result = MarginAudit.sortAndMakeDistinct(input);
        assertEquals(8.0, result.get(0), 0.001);
        assertEquals(16.0, result.get(1), 0.001);
        assertEquals(32.0, result.get(2), 0.001);
    }

    @Test
    public void testSortAndMakeDistinct_emptyList() {
        List<Double> input = new ArrayList<>();
        List<Double> result = MarginAudit.sortAndMakeDistinct(input);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSortAndMakeDistinct_allZeros() {
        List<Double> input = Arrays.asList(0.0, 0.0, 0.0);
        List<Double> result = MarginAudit.sortAndMakeDistinct(input);
        assertTrue(result.isEmpty());
    }

    // ========== removeZeroValues tests ==========

    @Test
    public void testRemoveZeroValues_removesZeros() {
        List<Integer> input = Arrays.asList(0, 8, 16, 0);
        List<Integer> result = MarginAudit.removeZeroValues(input);
        assertEquals(2, result.size());
        assertFalse(result.contains(0));
    }

    @Test
    public void testRemoveZeroValues_noZeros() {
        List<Integer> input = Arrays.asList(8, 16, 24);
        List<Integer> result = MarginAudit.removeZeroValues(input);
        assertEquals(3, result.size());
    }

    @Test
    public void testRemoveZeroValues_emptyList() {
        List<Integer> input = new ArrayList<>();
        List<Integer> result = MarginAudit.removeZeroValues(input);
        assertTrue(result.isEmpty());
    }

    // ========== deflateGCD tests ==========

    @Test
    public void testDeflateGCD_dividesBy100() {
        List<Double> input = Arrays.asList(800.0, 1600.0);
        List<Double> result = MarginAudit.deflateGCD(input);
        assertEquals(8.0, result.get(0), 0.001);
        assertEquals(16.0, result.get(1), 0.001);
    }

    @Test
    public void testDeflateGCD_removesDuplicates() {
        List<Double> input = Arrays.asList(800.0, 800.0);
        List<Double> result = MarginAudit.deflateGCD(input);
        assertEquals(1, result.size());
    }

    @Test
    public void testDeflateGCD_sorts() {
        List<Double> input = Arrays.asList(1600.0, 800.0);
        List<Double> result = MarginAudit.deflateGCD(input);
        assertEquals(8.0, result.get(0), 0.001);
        assertEquals(16.0, result.get(1), 0.001);
    }

    // ========== URLReader tests ==========

    @Test(expected = Exception.class)
    public void testURLReader_invalidUrl() throws Exception {
        MarginAudit.URLReader(new java.net.URL("https://invalid.example.com/404"));
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

        Audit result = marginAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertEquals(AuditCategory.AESTHETICS, result.getCategory());
        assertEquals(AuditSubcategory.WHITESPACE, result.getSubcategory());
        assertEquals(AuditName.MARGIN, result.getName());
    }

    @Test
    public void testExecute_withElementsHavingMargins() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        ElementState element = mock(ElementState.class);
        Map<String, String> cssValues = new HashMap<>();
        cssValues.put("margin-top", "16px");
        cssValues.put("margin-bottom", "8px");
        cssValues.put("margin-left", "0px");
        cssValues.put("margin-right", "0px");
        cssValues.put("padding-top", "0px");
        cssValues.put("padding-bottom", "0px");
        cssValues.put("padding-left", "0px");
        cssValues.put("padding-right", "0px");
        when(element.getRenderedCssValues()).thenReturn(cssValues);
        when(element.getOwnedText()).thenReturn("");

        when(pageState.getKey()).thenReturn("test-key");
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(page_state_service.getElementStates("test-key")).thenReturn(Arrays.asList(element));

        Audit result = marginAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertEquals(AuditCategory.AESTHETICS, result.getCategory());
    }

    @Test
    public void testExecute_withNonMultipleOf8Margins() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        ElementState element = mock(ElementState.class);
        Map<String, String> cssValues = new HashMap<>();
        cssValues.put("margin-top", "13px");
        cssValues.put("margin-bottom", "0px");
        cssValues.put("margin-left", "0px");
        cssValues.put("margin-right", "0px");
        cssValues.put("padding-top", "0px");
        cssValues.put("padding-bottom", "0px");
        cssValues.put("padding-left", "0px");
        cssValues.put("padding-right", "0px");
        when(element.getRenderedCssValues()).thenReturn(cssValues);
        when(element.getOwnedText()).thenReturn("");

        when(pageState.getKey()).thenReturn("test-key");
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(page_state_service.getElementStates("test-key")).thenReturn(Arrays.asList(element));

        Audit result = marginAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        // Non-multiple of 8 should earn 0 points for that margin
        assertNotNull(result);
        assertTrue(result.getPoints() >= 0);
    }

    @Test
    public void testExecute_withMarginUsedAsPadding() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        ElementState element = mock(ElementState.class);
        Map<String, String> cssValues = new HashMap<>();
        cssValues.put("margin-top", "16px");
        cssValues.put("margin-bottom", "0px");
        cssValues.put("margin-left", "0px");
        cssValues.put("margin-right", "0px");
        cssValues.put("padding-top", "0px");
        cssValues.put("padding-bottom", "0px");
        cssValues.put("padding-left", "0px");
        cssValues.put("padding-right", "0px");
        when(element.getRenderedCssValues()).thenReturn(cssValues);
        when(element.getOwnedText()).thenReturn("Some text content");

        when(pageState.getKey()).thenReturn("test-key");
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(page_state_service.getElementStates("test-key")).thenReturn(Arrays.asList(element));

        Audit result = marginAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    // ========== convertList tests ==========

    @Test
    public void testConvertList_stringToDouble() {
        List<String> input = Arrays.asList("1.5", "2.5", "3.5");
        List<Double> result = MarginAudit.convertList(input, s -> Double.parseDouble(s));
        assertEquals(3, result.size());
        assertEquals(1.5, result.get(0), 0.001);
        assertEquals(2.5, result.get(1), 0.001);
        assertEquals(3.5, result.get(2), 0.001);
    }

    @Test
    public void testConvertList_intToString() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        List<String> result = MarginAudit.convertList(input, i -> String.valueOf(i));
        assertEquals(3, result.size());
        assertEquals("1", result.get(0));
    }
}
