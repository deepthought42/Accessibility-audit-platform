package com.looksee.visualDesignAudit.audit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.WCAGComplianceLevel;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;

@RunWith(MockitoJUnitRunner.class)
public class TextColorContrastAuditTest {

    @Mock
    private AuditService audit_service;

    @Mock
    private UXIssueMessageService issue_message_service;

    @InjectMocks
    private TextColorContrastAudit textColorContrastAudit;

    // ========== execute - null design system, defaults to AAA ==========

    @Test
    public void testExecute_withNoElements_nullDesignSystem() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);

        when(pageState.getElements()).thenReturn(new ArrayList<>());
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertEquals(AuditCategory.AESTHETICS, result.getCategory());
        assertEquals(AuditSubcategory.COLOR_MANAGEMENT, result.getSubcategory());
        assertEquals(AuditName.TEXT_BACKGROUND_CONTRAST, result.getName());
    }

    @Test
    public void testExecute_withDesignSystemLevelA_returnsNull() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        when(designSystem.getWcagComplianceLevel()).thenReturn(WCAGComplianceLevel.A);

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, designSystem);

        assertNull(result);
    }

    @Test
    public void testExecute_withDesignSystemLevelAA() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        when(designSystem.getWcagComplianceLevel()).thenReturn(WCAGComplianceLevel.AA);
        when(pageState.getElements()).thenReturn(new ArrayList<>());
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    @Test
    public void testExecute_withDesignSystemLevelAAA() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        when(designSystem.getWcagComplianceLevel()).thenReturn(WCAGComplianceLevel.AAA);
        when(pageState.getElements()).thenReturn(new ArrayList<>());
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    // ========== execute - text elements with various contrast levels ==========

    @Test
    public void testExecute_largeTextLowContrast() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);

        ElementState element = createTextElement("h1", "24px", "400", "rgb(200,200,200)", "rgb(210,210,210)", 2.0);
        List<ElementState> elements = new ArrayList<>();
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.hasAuditBeenExecuted(any(), anyLong())).thenReturn(false);
        when(issue_message_service.saveColorContrast(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertEquals(AuditCategory.AESTHETICS, result.getCategory());
    }

    @Test
    public void testExecute_largeTextMediumContrast() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);

        ElementState element = createTextElement("h1", "24px", "400", "rgb(0,0,0)", "rgb(150,150,150)", 3.5);
        List<ElementState> elements = new ArrayList<>();
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.hasAuditBeenExecuted(any(), anyLong())).thenReturn(false);
        when(issue_message_service.saveColorContrast(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, null);

        assertNotNull(result);
    }

    @Test
    public void testExecute_largeTextHighContrast() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);

        ElementState element = createTextElement("h1", "24px", "400", "rgb(0,0,0)", "rgb(255,255,255)", 5.0);
        List<ElementState> elements = new ArrayList<>();
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.hasAuditBeenExecuted(any(), anyLong())).thenReturn(false);
        when(issue_message_service.saveColorContrast(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, null);

        assertNotNull(result);
    }

    @Test
    public void testExecute_smallTextLowContrast() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);

        ElementState element = createTextElement("p", "14px", "400", "rgb(200,200,200)", "rgb(210,210,210)", 2.0);
        List<ElementState> elements = new ArrayList<>();
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.hasAuditBeenExecuted(any(), anyLong())).thenReturn(false);
        when(issue_message_service.saveColorContrast(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, null);

        assertNotNull(result);
    }

    @Test
    public void testExecute_smallTextMediumContrast() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);

        ElementState element = createTextElement("p", "14px", "400", "rgb(0,0,0)", "rgb(150,150,150)", 5.5);
        List<ElementState> elements = new ArrayList<>();
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.hasAuditBeenExecuted(any(), anyLong())).thenReturn(false);
        when(issue_message_service.saveColorContrast(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, null);

        assertNotNull(result);
    }

    @Test
    public void testExecute_smallTextHighContrast() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);

        ElementState element = createTextElement("p", "14px", "400", "rgb(0,0,0)", "rgb(255,255,255)", 8.0);
        List<ElementState> elements = new ArrayList<>();
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.hasAuditBeenExecuted(any(), anyLong())).thenReturn(false);
        when(issue_message_service.saveColorContrast(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, null);

        assertNotNull(result);
    }

    @Test
    public void testExecute_boldTextAbove14pt() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);

        // Bold text at 14pt (18.5px) is considered large text
        ElementState element = createTextElement("p", "19px", "700", "rgb(0,0,0)", "rgb(255,255,255)", 3.5);
        List<ElementState> elements = new ArrayList<>();
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.hasAuditBeenExecuted(any(), anyLong())).thenReturn(false);
        when(issue_message_service.saveColorContrast(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, null);

        assertNotNull(result);
    }

    @Test
    public void testExecute_alreadyExecutedAudit() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);

        ElementState element = createTextElement("p", "14px", "400", "rgb(200,200,200)", "rgb(210,210,210)", 2.0);
        List<ElementState> elements = new ArrayList<>();
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.hasAuditBeenExecuted(any(), anyLong())).thenReturn(true);

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        // When audit already executed, no new issues are created
    }

    @Test
    public void testExecute_whyItMattersIsSet() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);

        when(pageState.getElements()).thenReturn(new ArrayList<>());
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, null);

        assertNotNull(result.getWhyItMatters());
        assertFalse(result.getWhyItMatters().isEmpty());
    }

    @Test
    public void testExecute_withAACompliance_largeTextMediumContrast() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);
        when(designSystem.getWcagComplianceLevel()).thenReturn(WCAGComplianceLevel.AA);

        ElementState element = createTextElement("h1", "24px", "400", "rgb(0,0,0)", "rgb(150,150,150)", 3.5);
        List<ElementState> elements = new ArrayList<>();
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.hasAuditBeenExecuted(any(), anyLong())).thenReturn(false);
        when(issue_message_service.saveColorContrast(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    @Test
    public void testExecute_withAACompliance_smallTextMediumContrast() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);
        when(designSystem.getWcagComplianceLevel()).thenReturn(WCAGComplianceLevel.AA);

        ElementState element = createTextElement("p", "14px", "400", "rgb(0,0,0)", "rgb(120,120,120)", 5.5);
        List<ElementState> elements = new ArrayList<>();
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.hasAuditBeenExecuted(any(), anyLong())).thenReturn(false);
        when(issue_message_service.saveColorContrast(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = textColorContrastAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    @Test
    public void testConstructor_default() {
        TextColorContrastAudit audit = new TextColorContrastAudit();
        assertNotNull(audit);
    }

    private ElementState createTextElement(String name, String fontSize, String fontWeight,
                                           String foregroundColor, String backgroundColor, double textContrast) {
        ElementState element = mock(ElementState.class);
        when(element.getName()).thenReturn(name);
        when(element.getOwnedText()).thenReturn("Sample text content");
        when(element.getForegroundColor()).thenReturn(foregroundColor);
        when(element.getBackgroundColor()).thenReturn(backgroundColor);
        when(element.getTextContrast()).thenReturn(textContrast);
        when(element.getId()).thenReturn(1L);

        Map<String, String> cssValues = new HashMap<>();
        cssValues.put("font-size", fontSize);
        cssValues.put("font-weight", fontWeight);
        when(element.getRenderedCssValues()).thenReturn(cssValues);

        return element;
    }
}
