package com.looksee.visualDesignAudit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.gcp.PubSubAuditUpdatePublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.models.Domain;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditName;
import com.looksee.models.message.PageAuditMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.services.PageStateService;
import com.looksee.visualDesignAudit.audit.ImageAudit;
import com.looksee.visualDesignAudit.audit.ImagePolicyAudit;
import com.looksee.visualDesignAudit.audit.NonTextColorContrastAudit;
import com.looksee.visualDesignAudit.audit.TextColorContrastAudit;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AuditControllerTest {

    @Mock
    private AuditRecordService audit_record_service;

    @Mock
    private DomainService domain_service;

    @Mock
    private PageStateService page_state_service;

    @Mock
    private TextColorContrastAudit text_contrast_audit_impl;

    @Mock
    private NonTextColorContrastAudit non_text_contrast_audit_impl;

    @Mock
    private ImageAudit image_audit;

    @Mock
    private ImagePolicyAudit image_policy_audit;

    @Mock
    private PubSubAuditUpdatePublisherImpl audit_update_topic;

    @InjectMocks
    private AuditController auditController;

    private ObjectMapper mapper = new ObjectMapper();

    // ========== receiveMessage tests ==========

    @Test
    public void testReceiveMessage_successfulAudit() throws Exception {
        PageAuditMessage auditMsg = new PageAuditMessage();
        auditMsg.setPageAuditId(1L);
        auditMsg.setAccountId(100L);

        String json = mapper.writeValueAsString(auditMsg);
        String encoded = Base64.getEncoder().encodeToString(json.getBytes());

        Body.Message message = mock(Body.Message.class);
        when(message.getData()).thenReturn(encoded);

        Body body = mock(Body.class);
        when(body.getMessage()).thenReturn(message);

        Domain domain = mock(Domain.class);
        when(domain.getId()).thenReturn(1L);
        when(domain_service.findByAuditRecord(1L)).thenReturn(domain);

        DesignSystem designSystem = mock(DesignSystem.class);
        when(domain_service.getDesignSystem(1L)).thenReturn(Optional.of(designSystem));

        AuditRecord auditRecord = mock(AuditRecord.class);
        when(auditRecord.getId()).thenReturn(1L);
        when(audit_record_service.findById(1L)).thenReturn(Optional.of(auditRecord));

        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(page_state_service.getPageStateForAuditRecord(1L)).thenReturn(pageState);

        List<ElementState> elements = new ArrayList<>();
        when(page_state_service.getElementStates(1L)).thenReturn(elements);

        Set<Audit> existingAudits = new HashSet<>();
        when(audit_record_service.getAllAudits(1L)).thenReturn(existingAudits);

        Audit textContrastAudit = mock(Audit.class);
        when(textContrastAudit.getName()).thenReturn(AuditName.TEXT_BACKGROUND_CONTRAST);
        when(text_contrast_audit_impl.execute(any(), any(), any())).thenReturn(textContrastAudit);

        Audit nonTextContrastAudit = mock(Audit.class);
        when(nonTextContrastAudit.getName()).thenReturn(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
        when(non_text_contrast_audit_impl.execute(any(), any(), any())).thenReturn(nonTextContrastAudit);

        Audit imageCopyrightAudit = mock(Audit.class);
        when(imageCopyrightAudit.getName()).thenReturn(AuditName.IMAGE_COPYRIGHT);
        when(imageCopyrightAudit.getId()).thenReturn(3L);
        when(image_audit.execute(any(), any(), any())).thenReturn(imageCopyrightAudit);

        Audit imagePolicyResult = mock(Audit.class);
        when(imagePolicyResult.getName()).thenReturn(AuditName.IMAGE_POLICY);
        when(imagePolicyResult.getId()).thenReturn(4L);
        when(image_policy_audit.execute(any(), any(), any())).thenReturn(imagePolicyResult);

        ResponseEntity<String> response = auditController.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Successfully completed visual design audit", response.getBody());
        verify(audit_update_topic).publish(anyString());
    }

    @Test
    public void testReceiveMessage_withExistingAudits() throws Exception {
        PageAuditMessage auditMsg = new PageAuditMessage();
        auditMsg.setPageAuditId(1L);
        auditMsg.setAccountId(100L);

        String json = mapper.writeValueAsString(auditMsg);
        String encoded = Base64.getEncoder().encodeToString(json.getBytes());

        Body.Message message = mock(Body.Message.class);
        when(message.getData()).thenReturn(encoded);

        Body body = mock(Body.class);
        when(body.getMessage()).thenReturn(message);

        when(domain_service.findByAuditRecord(1L)).thenReturn(null);

        AuditRecord auditRecord = mock(AuditRecord.class);
        when(auditRecord.getId()).thenReturn(1L);
        when(audit_record_service.findById(1L)).thenReturn(Optional.of(auditRecord));

        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(page_state_service.getPageStateForAuditRecord(1L)).thenReturn(pageState);

        List<ElementState> elements = new ArrayList<>();
        when(page_state_service.getElementStates(1L)).thenReturn(elements);

        // Pre-existing audits for all types
        Set<Audit> existingAudits = new HashSet<>();
        Audit existingTextAudit = mock(Audit.class);
        when(existingTextAudit.getName()).thenReturn(AuditName.TEXT_BACKGROUND_CONTRAST);
        existingAudits.add(existingTextAudit);

        Audit existingNonTextAudit = mock(Audit.class);
        when(existingNonTextAudit.getName()).thenReturn(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
        existingAudits.add(existingNonTextAudit);

        Audit existingImageAudit = mock(Audit.class);
        when(existingImageAudit.getName()).thenReturn(AuditName.IMAGE_COPYRIGHT);
        existingAudits.add(existingImageAudit);

        Audit existingImagePolicyAudit = mock(Audit.class);
        when(existingImagePolicyAudit.getName()).thenReturn(AuditName.IMAGE_POLICY);
        existingAudits.add(existingImagePolicyAudit);

        when(audit_record_service.getAllAudits(1L)).thenReturn(existingAudits);

        ResponseEntity<String> response = auditController.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // No audits should be executed since they all already exist
        verify(text_contrast_audit_impl, never()).execute(any(), any(), any());
        verify(non_text_contrast_audit_impl, never()).execute(any(), any(), any());
        verify(image_audit, never()).execute(any(), any(), any());
        verify(image_policy_audit, never()).execute(any(), any(), any());
    }

    @Test
    public void testReceiveMessage_withNullDomain_usesDefaultDesignSystem() throws Exception {
        PageAuditMessage auditMsg = new PageAuditMessage();
        auditMsg.setPageAuditId(1L);
        auditMsg.setAccountId(100L);

        String json = mapper.writeValueAsString(auditMsg);
        String encoded = Base64.getEncoder().encodeToString(json.getBytes());

        Body.Message message = mock(Body.Message.class);
        when(message.getData()).thenReturn(encoded);

        Body body = mock(Body.class);
        when(body.getMessage()).thenReturn(message);

        when(domain_service.findByAuditRecord(1L)).thenReturn(null);

        AuditRecord auditRecord = mock(AuditRecord.class);
        when(auditRecord.getId()).thenReturn(1L);
        when(audit_record_service.findById(1L)).thenReturn(Optional.of(auditRecord));

        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(page_state_service.getPageStateForAuditRecord(1L)).thenReturn(pageState);

        List<ElementState> elements = new ArrayList<>();
        when(page_state_service.getElementStates(1L)).thenReturn(elements);

        Set<Audit> existingAudits = new HashSet<>();
        when(audit_record_service.getAllAudits(1L)).thenReturn(existingAudits);

        Audit textContrastAudit = mock(Audit.class);
        when(textContrastAudit.getName()).thenReturn(AuditName.TEXT_BACKGROUND_CONTRAST);
        when(text_contrast_audit_impl.execute(any(), any(), any())).thenReturn(textContrastAudit);

        Audit nonTextContrastAudit = mock(Audit.class);
        when(nonTextContrastAudit.getName()).thenReturn(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
        when(non_text_contrast_audit_impl.execute(any(), any(), any())).thenReturn(nonTextContrastAudit);

        Audit imageCopyrightAudit = mock(Audit.class);
        when(imageCopyrightAudit.getId()).thenReturn(3L);
        when(image_audit.execute(any(), any(), any())).thenReturn(imageCopyrightAudit);

        Audit imagePolicyResult = mock(Audit.class);
        when(imagePolicyResult.getId()).thenReturn(4L);
        when(image_policy_audit.execute(any(), any(), any())).thenReturn(imagePolicyResult);

        ResponseEntity<String> response = auditController.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Should not call getDesignSystem since domain is null
        verify(domain_service, never()).getDesignSystem(anyLong());
    }

    @Test
    public void testReceiveMessage_publishesAuditUpdate() throws Exception {
        PageAuditMessage auditMsg = new PageAuditMessage();
        auditMsg.setPageAuditId(1L);
        auditMsg.setAccountId(100L);

        String json = mapper.writeValueAsString(auditMsg);
        String encoded = Base64.getEncoder().encodeToString(json.getBytes());

        Body.Message message = mock(Body.Message.class);
        when(message.getData()).thenReturn(encoded);

        Body body = mock(Body.class);
        when(body.getMessage()).thenReturn(message);

        when(domain_service.findByAuditRecord(1L)).thenReturn(null);

        AuditRecord auditRecord = mock(AuditRecord.class);
        when(auditRecord.getId()).thenReturn(1L);
        when(audit_record_service.findById(1L)).thenReturn(Optional.of(auditRecord));

        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(page_state_service.getPageStateForAuditRecord(1L)).thenReturn(pageState);
        when(page_state_service.getElementStates(1L)).thenReturn(new ArrayList<>());

        Set<Audit> allExist = new HashSet<>();
        Audit a1 = mock(Audit.class); when(a1.getName()).thenReturn(AuditName.TEXT_BACKGROUND_CONTRAST); allExist.add(a1);
        Audit a2 = mock(Audit.class); when(a2.getName()).thenReturn(AuditName.NON_TEXT_BACKGROUND_CONTRAST); allExist.add(a2);
        Audit a3 = mock(Audit.class); when(a3.getName()).thenReturn(AuditName.IMAGE_COPYRIGHT); allExist.add(a3);
        Audit a4 = mock(Audit.class); when(a4.getName()).thenReturn(AuditName.IMAGE_POLICY); allExist.add(a4);
        when(audit_record_service.getAllAudits(1L)).thenReturn(allExist);

        auditController.receiveMessage(body);

        verify(audit_update_topic, times(1)).publish(anyString());
    }

    @Test
    public void testReceiveMessage_partialExistingAudits() throws Exception {
        PageAuditMessage auditMsg = new PageAuditMessage();
        auditMsg.setPageAuditId(1L);
        auditMsg.setAccountId(100L);

        String json = mapper.writeValueAsString(auditMsg);
        String encoded = Base64.getEncoder().encodeToString(json.getBytes());

        Body.Message message = mock(Body.Message.class);
        when(message.getData()).thenReturn(encoded);

        Body body = mock(Body.class);
        when(body.getMessage()).thenReturn(message);

        when(domain_service.findByAuditRecord(1L)).thenReturn(null);

        AuditRecord auditRecord = mock(AuditRecord.class);
        when(auditRecord.getId()).thenReturn(1L);
        when(audit_record_service.findById(1L)).thenReturn(Optional.of(auditRecord));

        PageState pageState = mock(PageState.class);
        when(pageState.getId()).thenReturn(1L);
        when(page_state_service.getPageStateForAuditRecord(1L)).thenReturn(pageState);
        when(page_state_service.getElementStates(1L)).thenReturn(new ArrayList<>());

        // Only text contrast exists
        Set<Audit> partialAudits = new HashSet<>();
        Audit existingTextAudit = mock(Audit.class);
        when(existingTextAudit.getName()).thenReturn(AuditName.TEXT_BACKGROUND_CONTRAST);
        partialAudits.add(existingTextAudit);
        when(audit_record_service.getAllAudits(1L)).thenReturn(partialAudits);

        Audit nonTextContrastAudit = mock(Audit.class);
        when(nonTextContrastAudit.getName()).thenReturn(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
        when(non_text_contrast_audit_impl.execute(any(), any(), any())).thenReturn(nonTextContrastAudit);

        Audit imageCopyrightAudit = mock(Audit.class);
        when(imageCopyrightAudit.getId()).thenReturn(3L);
        when(image_audit.execute(any(), any(), any())).thenReturn(imageCopyrightAudit);

        Audit imagePolicyResult = mock(Audit.class);
        when(imagePolicyResult.getId()).thenReturn(4L);
        when(image_policy_audit.execute(any(), any(), any())).thenReturn(imagePolicyResult);

        ResponseEntity<String> response = auditController.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Text contrast should NOT be executed, others should be
        verify(text_contrast_audit_impl, never()).execute(any(), any(), any());
        verify(non_text_contrast_audit_impl, times(1)).execute(any(), any(), any());
        verify(image_audit, times(1)).execute(any(), any(), any());
        verify(image_policy_audit, times(1)).execute(any(), any(), any());
    }
}
