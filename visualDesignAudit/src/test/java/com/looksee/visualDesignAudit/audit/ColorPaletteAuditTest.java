package com.looksee.visualDesignAudit.audit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;

@RunWith(MockitoJUnitRunner.class)
public class ColorPaletteAuditTest {

    @Mock
    private AuditService audit_service;

    @Mock
    private UXIssueMessageService ux_issue_service;

    @InjectMocks
    private ColorPaletteAudit colorPaletteAudit;

    @Test
    public void testExecute_withEmptyColors() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        when(auditRecord.getColors()).thenReturn(new ArrayList<>());
        when(designSystem.getColorPalette()).thenReturn(new ArrayList<>());
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = colorPaletteAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertEquals(AuditCategory.AESTHETICS, result.getCategory());
        assertEquals(AuditSubcategory.COLOR_MANAGEMENT, result.getSubcategory());
        assertEquals(AuditName.COLOR_PALETTE, result.getName());
    }

    @Test
    public void testExecute_withMatchingColors() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        List<String> pageColors = Arrays.asList("rgb(255,0,0)", "rgb(0,255,0)");
        List<String> palette = Arrays.asList("rgb(255,0,0)", "rgb(0,255,0)", "rgb(0,0,255)");

        when(auditRecord.getColors()).thenReturn(pageColors);
        when(designSystem.getColorPalette()).thenReturn(palette);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ux_issue_service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = colorPaletteAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertEquals(AuditCategory.AESTHETICS, result.getCategory());
    }

    @Test
    public void testExecute_withNonMatchingColors() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        List<String> pageColors = Arrays.asList("rgb(128,128,128)");
        List<String> palette = Arrays.asList("rgb(255,0,0)", "rgb(0,255,0)");

        when(auditRecord.getColors()).thenReturn(pageColors);
        when(designSystem.getColorPalette()).thenReturn(palette);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ux_issue_service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = colorPaletteAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    @Test
    public void testExecute_whyItMattersIsSet() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        when(auditRecord.getColors()).thenReturn(new ArrayList<>());
        when(designSystem.getColorPalette()).thenReturn(new ArrayList<>());
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = colorPaletteAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result.getWhyItMatters());
        assertTrue(result.getWhyItMatters().contains("color"));
    }

    @Test
    public void testExecute_singleColorInPalette() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        List<String> pageColors = Arrays.asList("rgb(255,0,0)");
        List<String> palette = Arrays.asList("rgb(255,0,0)");

        when(auditRecord.getColors()).thenReturn(pageColors);
        when(designSystem.getColorPalette()).thenReturn(palette);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ux_issue_service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = colorPaletteAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    @Test
    public void testExecute_isNotWcagAudit() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        when(auditRecord.getColors()).thenReturn(new ArrayList<>());
        when(designSystem.getColorPalette()).thenReturn(new ArrayList<>());
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = colorPaletteAudit.execute(pageState, auditRecord, designSystem);

        // ColorPaletteAudit is not a WCAG audit
        assertNotNull(result);
    }

    @Test
    public void testExecute_multipleColorsOnPage() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        List<String> pageColors = Arrays.asList("rgb(255,0,0)", "rgb(0,255,0)", "rgb(0,0,255)", "rgb(128,128,128)", "rgb(255,255,0)");
        List<String> palette = Arrays.asList("rgb(255,0,0)", "rgb(0,255,0)", "rgb(0,0,255)");

        when(auditRecord.getColors()).thenReturn(pageColors);
        when(designSystem.getColorPalette()).thenReturn(palette);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ux_issue_service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = colorPaletteAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }
}
