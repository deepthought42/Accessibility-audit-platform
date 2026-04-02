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

import com.looksee.gcp.GoogleCloudStorage;
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
public class NonTextColorContrastAuditTest {

    @Mock
    private AuditService audit_service;

    @Mock
    private UXIssueMessageService issue_message_service;

    @Mock
    private GoogleCloudStorage google_cloud_storage;

    @InjectMocks
    private NonTextColorContrastAudit nonTextColorContrastAudit;

    // ========== execute - compliance level tests ==========

    @Test
    public void testExecute_withDesignSystemLevelA_returnsNull() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        when(designSystem.getWcagComplianceLevel()).thenReturn(WCAGComplianceLevel.A);

        Audit result = nonTextColorContrastAudit.execute(pageState, auditRecord, designSystem);

        assertNull(result);
    }

    @Test
    public void testExecute_withNoElements_nullDesignSystem() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);

        when(pageState.getElements()).thenReturn(new ArrayList<>());
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = nonTextColorContrastAudit.execute(pageState, auditRecord, null);

        assertNotNull(result);
        assertEquals(AuditCategory.AESTHETICS, result.getCategory());
        assertEquals(AuditSubcategory.COLOR_MANAGEMENT, result.getSubcategory());
        assertEquals(AuditName.NON_TEXT_BACKGROUND_CONTRAST, result.getName());
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

        Audit result = nonTextColorContrastAudit.execute(pageState, auditRecord, designSystem);

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

        Audit result = nonTextColorContrastAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    // ========== execute - button elements ==========

    @Test
    public void testExecute_withButtonElement_highContrast() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);
        when(designSystem.getWcagComplianceLevel()).thenReturn(WCAGComplianceLevel.AA);

        ElementState button = createButtonElement("button", "rgb(0,0,255)", "rgb(255,255,255)", "/btn1");
        List<ElementState> elements = new ArrayList<>();
        elements.add(button);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.saveColorContrast(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = nonTextColorContrastAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    @Test
    public void testExecute_withButtonElement_lowContrast() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);
        when(designSystem.getWcagComplianceLevel()).thenReturn(WCAGComplianceLevel.AA);

        ElementState button = createButtonElement("button", "rgb(200,200,200)", "rgb(210,210,210)", "/btn2");
        List<ElementState> elements = new ArrayList<>();
        elements.add(button);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.saveColorContrast(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = nonTextColorContrastAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    // ========== execute - input elements ==========

    @Test
    public void testExecute_withInputElement() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);
        when(designSystem.getWcagComplianceLevel()).thenReturn(WCAGComplianceLevel.AA);

        ElementState input = createInputElement("rgb(240,240,240)", "rgb(255,255,255)", "/input1");
        List<ElementState> elements = new ArrayList<>();
        elements.add(input);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.saveColorContrast(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = nonTextColorContrastAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    @Test
    public void testExecute_withNonButtonNonInputElements() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);
        when(designSystem.getWcagComplianceLevel()).thenReturn(WCAGComplianceLevel.AA);

        ElementState div = mock(ElementState.class);
        when(div.getName()).thenReturn("div");
        Map<String, String> attrs = new HashMap<>();
        when(div.getAttributes()).thenReturn(attrs);

        List<ElementState> elements = new ArrayList<>();
        elements.add(div);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = nonTextColorContrastAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertEquals(0, result.getPoints());
        assertEquals(0, result.getPoints());
    }

    @Test
    public void testExecute_withElementHavingButtonClass() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);
        when(designSystem.getWcagComplianceLevel()).thenReturn(WCAGComplianceLevel.AA);

        ElementState element = mock(ElementState.class);
        when(element.getName()).thenReturn("a");
        Map<String, String> attrs = new HashMap<>();
        attrs.put("class", "btn-primary button-lg");
        when(element.getAttributes()).thenReturn(attrs);
        when(element.getAttribute("class")).thenReturn("btn-primary button-lg");
        when(element.getBackgroundColor()).thenReturn("rgb(0,0,255)");
        when(element.getXpath()).thenReturn("/html/body/a");
        when(element.getKey()).thenReturn("elem-key-1");
        when(element.getId()).thenReturn(1L);

        Map<String, String> cssValues = new HashMap<>();
        cssValues.put("border-inline-start-width", "0px");
        cssValues.put("border-inline-end-width", "0px");
        cssValues.put("border-block-start-width", "0px");
        cssValues.put("border-block-end-width", "0px");
        cssValues.put("border-bottom-color", "rgb(0,0,255)");
        cssValues.put("border-top-color", "rgb(0,0,255)");
        cssValues.put("border-left-color", "rgb(0,0,255)");
        cssValues.put("border-right-color", "rgb(0,0,255)");
        cssValues.put("background-color", "rgb(0,0,255)");
        when(element.getRenderedCssValues()).thenReturn(cssValues);

        List<ElementState> elements = new ArrayList<>();
        elements.add(element);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.saveColorContrast(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = nonTextColorContrastAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    @Test
    public void testExecute_whyItMattersContainsExpectedContent() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);

        when(pageState.getElements()).thenReturn(new ArrayList<>());
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = nonTextColorContrastAudit.execute(pageState, auditRecord, null);

        assertNotNull(result.getWhyItMatters());
        assertTrue(result.getWhyItMatters().contains("Icons"));
    }

    @Test
    public void testExecute_withMixedElements() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);
        when(designSystem.getWcagComplianceLevel()).thenReturn(WCAGComplianceLevel.AA);

        ElementState button = createButtonElement("button", "rgb(0,0,200)", "rgb(255,255,255)", "/body/button");
        ElementState div = mock(ElementState.class);
        when(div.getName()).thenReturn("div");
        Map<String, String> divAttrs = new HashMap<>();
        when(div.getAttributes()).thenReturn(divAttrs);
        ElementState input = createInputElement("rgb(230,230,230)", "rgb(255,255,255)", "/body/input");

        List<ElementState> elements = new ArrayList<>();
        elements.add(button);
        elements.add(div);
        elements.add(input);

        when(pageState.getElements()).thenReturn(elements);
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(audit_service.save(any(Audit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issue_message_service.saveColorContrast(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = nonTextColorContrastAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    private ElementState createButtonElement(String name, String bgColor, String parentBgColor, String xpath) {
        ElementState element = mock(ElementState.class);
        when(element.getName()).thenReturn(name);
        when(element.getBackgroundColor()).thenReturn(bgColor);
        when(element.getXpath()).thenReturn(xpath);
        when(element.getKey()).thenReturn("key-" + xpath);
        when(element.getId()).thenReturn((long) xpath.hashCode());

        Map<String, String> attrs = new HashMap<>();
        when(element.getAttributes()).thenReturn(attrs);
        if (!"button".equalsIgnoreCase(name) && !"input".equalsIgnoreCase(name)) {
            when(element.getAttribute("class")).thenReturn("");
        }

        Map<String, String> cssValues = new HashMap<>();
        cssValues.put("border-inline-start-width", "0px");
        cssValues.put("border-inline-end-width", "0px");
        cssValues.put("border-block-start-width", "0px");
        cssValues.put("border-block-end-width", "0px");
        cssValues.put("border-bottom-color", bgColor);
        cssValues.put("border-top-color", bgColor);
        cssValues.put("border-left-color", bgColor);
        cssValues.put("border-right-color", bgColor);
        cssValues.put("background-color", bgColor);
        when(element.getRenderedCssValues()).thenReturn(cssValues);

        return element;
    }

    private ElementState createInputElement(String bgColor, String parentBgColor, String xpath) {
        ElementState element = mock(ElementState.class);
        when(element.getName()).thenReturn("input");
        when(element.getBackgroundColor()).thenReturn(bgColor);
        when(element.getXpath()).thenReturn(xpath);
        when(element.getKey()).thenReturn("key-" + xpath);
        when(element.getId()).thenReturn((long) xpath.hashCode());

        Map<String, String> attrs = new HashMap<>();
        when(element.getAttributes()).thenReturn(attrs);

        Map<String, String> cssValues = new HashMap<>();
        cssValues.put("border-inline-start-width", "0px");
        cssValues.put("border-inline-end-width", "0px");
        cssValues.put("border-block-start-width", "0px");
        cssValues.put("border-block-end-width", "0px");
        cssValues.put("border-bottom-color", bgColor);
        cssValues.put("border-top-color", bgColor);
        cssValues.put("border-left-color", bgColor);
        cssValues.put("border-right-color", bgColor);
        cssValues.put("background-color", bgColor);
        when(element.getRenderedCssValues()).thenReturn(cssValues);

        return element;
    }
}
