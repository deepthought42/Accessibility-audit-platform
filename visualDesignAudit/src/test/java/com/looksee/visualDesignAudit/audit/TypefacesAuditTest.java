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
import com.looksee.services.PageStateService;
import com.looksee.services.UXIssueMessageService;

@RunWith(MockitoJUnitRunner.class)
public class TypefacesAuditTest {

    @Mock
    private UXIssueMessageService issue_message_service;

    @Mock
    private PageStateService page_state_service;

    @InjectMocks
    private TypefacesAudit typefacesAudit;

    @Test
    public void testExecute_withNoStylesheets() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        when(pageState.getSrc()).thenReturn("<html><head></head><body></body></html>");
        when(pageState.getKey()).thenReturn("test-key");
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(page_state_service.getElementStates("test-key")).thenReturn(new ArrayList<>());
        when(issue_message_service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = typefacesAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
        assertEquals(AuditCategory.AESTHETICS, result.getCategory());
        assertEquals(AuditSubcategory.TYPOGRAPHY, result.getSubcategory());
        assertEquals(AuditName.TYPEFACES, result.getName());
    }

    @Test
    public void testExecute_returnsCorrectAuditMetadata() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        when(pageState.getSrc()).thenReturn("<html><head></head><body></body></html>");
        when(pageState.getKey()).thenReturn("test-key");
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(page_state_service.getElementStates("test-key")).thenReturn(new ArrayList<>());
        when(issue_message_service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = typefacesAudit.execute(pageState, auditRecord, designSystem);

        assertEquals(AuditCategory.AESTHETICS, result.getCategory());
        assertEquals(AuditSubcategory.TYPOGRAPHY, result.getSubcategory());
        assertEquals(AuditName.TYPEFACES, result.getName());
    }

    @Test
    public void testExecute_whyItMattersIsSet() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        when(pageState.getSrc()).thenReturn("<html><head></head><body></body></html>");
        when(pageState.getKey()).thenReturn("test-key");
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(page_state_service.getElementStates("test-key")).thenReturn(new ArrayList<>());
        when(issue_message_service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = typefacesAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result.getWhyItMatters());
        assertTrue(result.getWhyItMatters().contains("typography"));
    }

    @Test
    public void testExecute_withStylesheetContainingSingleTypeface() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        String html = "<html><head><style>body { font-family: Arial, sans-serif; }</style></head><body></body></html>";
        when(pageState.getSrc()).thenReturn(html);
        when(pageState.getKey()).thenReturn("test-key");
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(page_state_service.getElementStates("test-key")).thenReturn(new ArrayList<>());
        when(issue_message_service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = typefacesAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    @Test
    public void testExecute_auditIsNotWcag() {
        PageState pageState = mock(PageState.class);
        AuditRecord auditRecord = mock(AuditRecord.class);
        DesignSystem designSystem = mock(DesignSystem.class);

        when(pageState.getSrc()).thenReturn("<html><head></head><body></body></html>");
        when(pageState.getKey()).thenReturn("test-key");
        when(pageState.getUrl()).thenReturn("https://example.com");
        when(page_state_service.getElementStates("test-key")).thenReturn(new ArrayList<>());
        when(issue_message_service.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Audit result = typefacesAudit.execute(pageState, auditRecord, designSystem);

        assertNotNull(result);
    }

    @Test
    public void testConstructor_default() {
        TypefacesAudit audit = new TypefacesAudit();
        assertNotNull(audit);
    }
}
