package com.looksee.models.audit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.enums.*;

/**
 * Unit tests for audit model classes.
 */
class AuditModelClassesTest {

    // ===== Audit =====
    @Test
    void auditDefaultConstructor() {
        Audit audit = new Audit();
        assertNotNull(audit.getMessages());
        assertTrue(audit.getMessages().isEmpty());
    }

    @Test
    void auditParameterizedConstructor() {
        Set<UXIssueMessage> issues = new HashSet<>();
        Audit audit = new Audit(
            AuditCategory.CONTENT,
            AuditSubcategory.WRITTEN_CONTENT,
            AuditName.PARAGRAPHING,
            8,
            issues,
            AuditLevel.PAGE,
            10,
            "https://example.com",
            "Content matters",
            "Check paragraphing",
            true
        );
        assertEquals(AuditCategory.CONTENT, audit.getCategory());
        assertEquals(AuditSubcategory.WRITTEN_CONTENT, audit.getSubcategory());
        assertEquals(AuditName.PARAGRAPHING, audit.getName());
        assertEquals(8, audit.getPoints());
        assertEquals(10, audit.getTotalPossiblePoints());
        assertEquals(AuditLevel.PAGE, audit.getLevel());
        assertEquals("https://example.com", audit.getUrl());
        assertTrue(audit.isAccessible());
        assertNotNull(audit.getKey());
    }

    @Test
    void auditClone() {
        Set<UXIssueMessage> issues = new HashSet<>();
        Audit original = new Audit(
            AuditCategory.AESTHETICS, AuditSubcategory.COLOR_MANAGEMENT,
            AuditName.COLOR_PALETTE, 5, issues, AuditLevel.PAGE, 10,
            "https://example.com", "why", "desc", false
        );
        Audit clone = original.clone();
        assertEquals(original.getCategory(), clone.getCategory());
        assertEquals(original.getName(), clone.getName());
        assertNotSame(original, clone);
    }

    @Test
    void auditGenerateKey() {
        Set<UXIssueMessage> issues = new HashSet<>();
        Audit audit = new Audit(
            AuditCategory.CONTENT, AuditSubcategory.WRITTEN_CONTENT,
            AuditName.ALT_TEXT, 5, issues, AuditLevel.PAGE, 10,
            "url", "why", "desc", true
        );
        assertNotNull(audit.getKey());
        assertTrue(audit.getKey().startsWith("audit"));
    }

    @Test
    void auditToString() {
        Set<UXIssueMessage> issues = new HashSet<>();
        Audit audit = new Audit(
            AuditCategory.CONTENT, AuditSubcategory.WRITTEN_CONTENT,
            AuditName.ALT_TEXT, 5, issues, AuditLevel.PAGE, 10,
            "url", "why", "desc", true
        );
        assertEquals(audit.getKey(), audit.toString());
    }

    // ===== AuditRecord =====
    @Test
    void auditRecordDefaultConstructor() {
        AuditRecord record = new AuditRecord();
        assertNotNull(record.getStartTime());
        assertEquals(ExecutionStatus.UNKNOWN, record.getStatus());
        assertEquals("", record.getUrl());
        assertEquals(AuditLevel.UNKNOWN, record.getLevel());
        assertEquals(0.0, record.getContentAuditProgress());
        assertEquals(0.0, record.getContentAuditScore());
        assertNotNull(record.getColors());
    }

    @Test
    void auditRecordParameterizedConstructor() {
        LocalDateTime now = LocalDateTime.now();
        AuditRecord record = new AuditRecord(
            1L, ExecutionStatus.RUNNING, AuditLevel.PAGE, "key1",
            now, 0.5, 0.6, 0.7, 0.8, 0.9, 0.4, 0.3,
            now, now.plusHours(1), "https://example.com"
        );
        assertEquals(1L, record.getId());
        assertEquals(ExecutionStatus.RUNNING, record.getStatus());
        assertEquals(AuditLevel.PAGE, record.getLevel());
        assertEquals("https://example.com", record.getUrl());
    }

    @Test
    void auditRecordGenerateKey() {
        AuditRecord record = new AuditRecord();
        String key = record.generateKey();
        assertNotNull(key);
        assertTrue(key.startsWith("auditrecord:"));
    }

    @Test
    void auditRecordIsComplete() {
        AuditRecord record = new AuditRecord();
        assertFalse(record.isComplete());

        record.setAestheticAuditProgress(1.0);
        record.setContentAuditProgress(1.0);
        record.setInfoArchitectureAuditProgress(1.0);
        record.setDataExtractionProgress(1.0);
        assertTrue(record.isComplete());
    }

    @Test
    void auditRecordAddColor() {
        AuditRecord record = new AuditRecord();
        assertTrue(record.addColor("red"));
        assertTrue(record.addColor("red")); // duplicate returns true
        assertEquals(1, record.getColors().size());
        assertTrue(record.addColor("blue"));
        assertEquals(2, record.getColors().size());
    }

    @Test
    void auditRecordClone() {
        LocalDateTime now = LocalDateTime.now();
        AuditRecord original = new AuditRecord(
            1L, ExecutionStatus.COMPLETE, AuditLevel.PAGE, "key1",
            now, 0.5, 0.6, 0.7, 0.8, 0.9, 0.4, 0.3,
            now, now.plusHours(1), "https://example.com"
        );
        AuditRecord clone = original.clone();
        assertEquals(original.getStatus(), clone.getStatus());
        assertEquals(original.getUrl(), clone.getUrl());
    }

    // ===== PageAuditRecord =====
    @Test
    void pageAuditRecordDefaultConstructor() {
        PageAuditRecord record = new PageAuditRecord();
        assertNotNull(record.getAudits());
        assertTrue(record.getAudits().isEmpty());
        assertNotNull(record.getKey());
    }

    @Test
    void pageAuditRecordGenerateKey() {
        PageAuditRecord record = new PageAuditRecord();
        String key = record.generateKey();
        assertTrue(key.startsWith("pageauditrecord:"));
    }

    @Test
    void pageAuditRecordAddAudit() {
        PageAuditRecord record = new PageAuditRecord();
        Audit audit = new Audit();
        record.addAudit(audit);
        assertEquals(1, record.getAudits().size());
    }

    @Test
    void pageAuditRecordRemoveAudit() {
        PageAuditRecord record = new PageAuditRecord();
        Audit audit = new Audit();
        record.addAudit(audit);
        record.removeAudit(audit);
        assertTrue(record.getAudits().isEmpty());
    }

    // ===== DomainAuditRecord =====
    @Test
    void domainAuditRecordDefaultConstructor() {
        DomainAuditRecord record = new DomainAuditRecord();
        assertNotNull(record.getPageAuditRecords());
        assertTrue(record.getPageAuditRecords().isEmpty());
    }

    @Test
    void domainAuditRecordParameterizedConstructor() {
        Set<AuditName> auditNames = new HashSet<>();
        auditNames.add(AuditName.ALT_TEXT);
        DomainAuditRecord record = new DomainAuditRecord(ExecutionStatus.RUNNING, auditNames);
        assertEquals(ExecutionStatus.RUNNING, record.getStatus());
        assertEquals(AuditLevel.DOMAIN, record.getLevel());
        assertNotNull(record.getKey());
    }

    @Test
    void domainAuditRecordAddAudit() {
        DomainAuditRecord record = new DomainAuditRecord();
        PageAuditRecord pageRecord = new PageAuditRecord();
        record.addAudit(pageRecord);
        assertEquals(1, record.getPageAuditRecords().size());
    }

    @Test
    void domainAuditRecordAddAudits() {
        DomainAuditRecord record = new DomainAuditRecord();
        Set<PageAuditRecord> records = new HashSet<>();
        records.add(new PageAuditRecord());
        records.add(new PageAuditRecord());
        record.addAudits(records);
        assertTrue(record.getPageAuditRecords().size() >= 1);
    }

    @Test
    void domainAuditRecordGenerateKey() {
        DomainAuditRecord record = new DomainAuditRecord();
        String key = record.generateKey();
        assertTrue(key.startsWith("domainauditrecord:"));
    }

    // ===== Score =====
    @Test
    void scoreDefaultConstructor() {
        Score score = new Score();
        assertEquals(0, score.getPointsAchieved());
    }

    @Test
    void scoreParameterizedConstructor() {
        Set<UXIssueMessage> issues = new HashSet<>();
        Score score = new Score(8, 10, issues);
        assertEquals(8, score.getPointsAchieved());
        assertEquals(10, score.getMaxPossiblePoints());
        assertNotNull(score.getIssueMessages());
    }

    // ===== SimpleScore =====
    @Test
    void simpleScoreDefaultConstructor() {
        SimpleScore score = new SimpleScore();
        assertNull(score.getDatePerformed());
        assertEquals(0.0, score.getScore());
    }

    @Test
    void simpleScoreParameterizedConstructor() {
        LocalDateTime now = LocalDateTime.now();
        SimpleScore score = new SimpleScore(now, 0.95);
        assertEquals(now, score.getDatePerformed());
        assertEquals(0.95, score.getScore());
    }

    // ===== GenericIssue =====
    @Test
    void genericIssueDefaultConstructor() {
        GenericIssue issue = new GenericIssue();
        assertNull(issue.getDescription());
    }

    @Test
    void genericIssueAllArgsConstructor() {
        GenericIssue issue = new GenericIssue("desc", "title", ".selector", "fix it");
        assertEquals("desc", issue.getDescription());
        assertEquals("title", issue.getTitle());
        assertEquals(".selector", issue.getCssSelector());
        assertEquals("fix it", issue.getRecommendation());
    }

    // ===== AuditScore =====
    @Test
    void auditScoreConstructor() {
        AuditScore score = new AuditScore(
            0.9, 0.8, 0.7, 0.6, 0.5,
            0.85, 0.75, 0.65, 0.55, 0.45,
            0.95, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3
        );
        assertEquals(0.9, score.getContentScore());
        assertEquals(0.8, score.getReadability());
        assertEquals(0.7, score.getSpellingGrammar());
        assertEquals(0.6, score.getImageQuality());
        assertEquals(0.5, score.getAltText());
        assertEquals(0.85, score.getInformationArchitectureScore());
        assertEquals(0.75, score.getLinks());
        assertEquals(0.65, score.getMetadata());
        assertEquals(0.55, score.getSeo());
        assertEquals(0.45, score.getSecurity());
        assertEquals(0.95, score.getAestheticsScore());
        assertEquals(0.8, score.getColorContrastScore());
        assertEquals(0.7, score.getWhitespaceScore());
        assertEquals(0.6, score.getInteractivityScore());
        assertEquals(0.5, score.getAccessibilityScore());
        assertEquals(0.4, score.getTextContrastScore());
        assertEquals(0.3, score.getNonTextContrastScore());
    }
}
