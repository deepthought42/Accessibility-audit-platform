package com.looksee.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.looksee.models.Account;
import com.looksee.models.Domain;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.enums.*;
import com.looksee.models.repository.*;
import com.looksee.services.*;

/**
 * Unit tests for service classes using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class ServiceClassesTest {

    // ===== AuditService =====
    @Mock
    private AuditRepository auditRepository;

    @Mock
    private JourneyRepository journeyRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void auditServiceSave() {
        Audit audit = new Audit();
        audit.setCategory(AuditCategory.CONTENT);
        audit.setSubcategory(AuditSubcategory.WRITTEN_CONTENT);
        audit.setName(AuditName.PARAGRAPHING);
        audit.setLevel(AuditLevel.PAGE);
        when(auditRepository.save(any(Audit.class))).thenReturn(audit);
        Audit saved = auditService.save(audit);
        assertNotNull(saved);
        verify(auditRepository).save(audit);
    }

    @Test
    void auditServiceFindById() {
        Audit audit = new Audit();
        when(auditRepository.findById(1L)).thenReturn(Optional.of(audit));
        Optional<Audit> found = auditService.findById(1L);
        assertTrue(found.isPresent());
    }

    @Test
    void auditServiceFindByKey() {
        Audit audit = new Audit();
        when(auditRepository.findByKey("key1")).thenReturn(audit);
        Audit found = auditService.findByKey("key1");
        assertNotNull(found);
    }

    @Test
    void auditServiceGetIssuesFiltersFullScore() {
        Set<UXIssueMessage> issues = new HashSet<>();
        when(auditRepository.findIssueMessages(1L)).thenReturn(issues);
        Set<UXIssueMessage> result = auditService.getIssues(1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void auditServiceGroupAuditsByPageEmptySet() {
        Set<Audit> audits = new HashSet<>();
        List<com.looksee.models.audit.PageStateAudits> result = auditService.groupAuditsByPage(audits);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void auditServiceSaveAllSkipsNull() {
        List<Audit> audits = new ArrayList<>();
        audits.add(null);
        List<Audit> result = auditService.saveAll(audits);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void auditServiceSaveAllHandlesExisting() {
        Audit audit = new Audit();
        audit.setCategory(AuditCategory.CONTENT);
        audit.setSubcategory(AuditSubcategory.WRITTEN_CONTENT);
        audit.setName(AuditName.PARAGRAPHING);
        audit.setLevel(AuditLevel.PAGE);
        audit.setKey(audit.generateKey());

        when(auditRepository.findByKey(anyString())).thenReturn(audit);
        List<Audit> result = auditService.saveAll(List.of(audit));
        assertEquals(1, result.size());
    }

    @Test
    void auditServiceCalculateDataExtractionProgressNoJourneys() {
        when(journeyRepository.findAllNonStatusJourneysForDomainAudit(anyLong(), anyString())).thenReturn(0);
        when(journeyRepository.findAllJourneysForDomainAudit(anyLong(), anyString())).thenReturn(0);
        double progress = auditService.calculateDataExtractionProgress(1L);
        assertEquals(0.01, progress, 0.001);
    }

    @Test
    void auditServiceCalculateDataExtractionProgressWithJourneys() {
        when(journeyRepository.findAllNonStatusJourneysForDomainAudit(anyLong(), anyString())).thenReturn(3);
        when(journeyRepository.findAllJourneysForDomainAudit(anyLong(), anyString())).thenReturn(2);
        double progress = auditService.calculateDataExtractionProgress(1L);
        assertTrue(progress > 0.0 && progress <= 1.0);
    }

    @Test
    void auditServiceGenerateElementIssuesMapEmptyAudits() {
        Set<Audit> audits = new HashSet<>();
        Map<String, Set<String>> result = auditService.generateElementIssuesMap(audits);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void auditServiceGenerateIssueElementMapEmptyAudits() {
        Set<Audit> audits = new HashSet<>();
        Map<String, String> result = auditService.generateIssueElementMap(audits);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void auditServiceCountAuditBySubcategoryEmptySet() {
        Set<Audit> audits = new HashSet<>();
        int count = auditService.countAuditBySubcategory(audits, AuditSubcategory.WRITTEN_CONTENT);
        assertEquals(0, count);
    }

    @Test
    void auditServiceCountIssuesByAuditNameEmptySet() {
        Set<Audit> audits = new HashSet<>();
        int count = auditService.countIssuesByAuditName(audits, AuditName.ALT_TEXT);
        assertEquals(0, count);
    }

    @Test
    void auditServiceRetrieveUXIssuesEmptySet() {
        Set<Audit> audits = new HashSet<>();
        Collection<UXIssueMessage> result = auditService.retrieveUXIssues(audits);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void auditServiceRetrieveElementSetEmptySet() {
        Collection<UXIssueMessage> issues = new HashSet<>();
        Collection<com.looksee.models.SimpleElement> result = auditService.retrieveElementSet(issues);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
