package com.looksee.models.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for DTO classes.
 */
class DtoClassesTest {

    // ===== Subscription =====
    @Test
    void subscriptionDefaultConstructor() {
        Subscription sub = new Subscription();
        assertNull(sub.getPlan());
        assertNull(sub.getPriceId());
    }

    @Test
    void subscriptionAllArgsConstructor() {
        Subscription sub = new Subscription("PRO", "price_123");
        assertEquals("PRO", sub.getPlan());
        assertEquals("price_123", sub.getPriceId());
    }

    @Test
    void subscriptionSetters() {
        Subscription sub = new Subscription();
        sub.setPlan("ENTERPRISE");
        sub.setPriceId("price_456");
        assertEquals("ENTERPRISE", sub.getPlan());
        assertEquals("price_456", sub.getPriceId());
    }

    // ===== AuditUpdateDto =====
    @Test
    void auditUpdateDtoDefaultConstructor() {
        AuditUpdateDto dto = new AuditUpdateDto();
        assertEquals(0, dto.getId());
        assertNull(dto.getLevel());
    }

    @Test
    void auditUpdateDtoAllArgsConstructor() {
        AuditUpdateDto dto = new AuditUpdateDto(
            1L, AuditLevel.PAGE, 0.5, 0.6, 0.7, 0.8, 0.9, 0.4, 0.3, 0.2, 0.1, "msg", "running"
        );
        assertEquals(1L, dto.getId());
        assertEquals(AuditLevel.PAGE, dto.getLevel());
        assertEquals(0.5, dto.getContentScore());
    }

    // ===== PageBuiltMessage =====
    @Test
    void pageBuiltMessageDefaultConstructor() {
        PageBuiltMessage msg = new PageBuiltMessage();
        assertNull(msg.getAccountId());
        assertEquals(0, msg.getPageId());
    }

    @Test
    void pageBuiltMessageTwoArgConstructor() {
        PageBuiltMessage msg = new PageBuiltMessage(10L, 20L);
        assertEquals(10L, msg.getPageId());
        assertEquals(20L, msg.getDomainAuditId());
    }

    @Test
    void pageBuiltMessageAllArgsConstructor() {
        PageBuiltMessage msg = new PageBuiltMessage("acc1", 10L, 20L, 30L);
        assertEquals("acc1", msg.getAccountId());
        assertEquals(10L, msg.getPageId());
        assertEquals(20L, msg.getDomainId());
        assertEquals(30L, msg.getDomainAuditId());
    }

    // ===== DomainDto =====
    @Test
    void domainDtoDefaultConstructor() {
        DomainDto dto = new DomainDto();
        assertNull(dto.getUrl());
        assertEquals(0, dto.getId());
    }

    @Test
    void domainDtoThreeArgConstructor() {
        DomainDto dto = new DomainDto(1L, "https://example.com", 0.5);
        assertEquals(1L, dto.getId());
        assertEquals("https://example.com", dto.getUrl());
        assertEquals(0.5, dto.getContentProgress());
    }

    @Test
    void domainDtoSetters() {
        DomainDto dto = new DomainDto();
        dto.setId(1L);
        dto.setUrl("https://example.com");
        dto.setPageCount(10);
        dto.setPagesAudited(5);
        dto.setContentScore(0.8);
        dto.setAuditRunning(true);
        dto.setMessage("running");
        dto.setStatus(ExecutionStatus.RUNNING);
        assertEquals(1L, dto.getId());
        assertEquals("https://example.com", dto.getUrl());
        assertEquals(10, dto.getPageCount());
        assertTrue(dto.isAuditRunning());
        assertEquals(ExecutionStatus.RUNNING, dto.getStatus());
    }

    // ===== CompetitorDto =====
    @Test
    void competitorDtoDefaultConstructor() {
        CompetitorDto dto = new CompetitorDto();
        assertNull(dto.getCompanyName());
        assertFalse(dto.isAnalysisRunning());
    }

    @Test
    void competitorDtoSetters() {
        CompetitorDto dto = new CompetitorDto();
        dto.setId(1L);
        dto.setCompanyName("Acme");
        dto.setUrl("https://acme.com");
        dto.setIndustry("tech");
        dto.setAnalysisRunning(true);
        assertEquals(1L, dto.getId());
        assertEquals("Acme", dto.getCompanyName());
        assertTrue(dto.isAnalysisRunning());
    }

    // ===== DomainSettingsDto =====
    @Test
    void domainSettingsDtoDefaultConstructor() {
        DomainSettingsDto dto = new DomainSettingsDto();
        assertNull(dto.getDesignSystem());
        assertNull(dto.getTestUsers());
    }

    // ===== UXIssueReportDto =====
    @Test
    void uxIssueReportDtoDefaultConstructor() {
        UXIssueReportDto dto = new UXIssueReportDto();
        assertNull(dto.getTitle());
    }

    @Test
    void uxIssueReportDtoSetters() {
        UXIssueReportDto dto = new UXIssueReportDto();
        dto.setTitle("Missing alt text");
        dto.setDescription("Image missing alt attribute");
        dto.setWhyItMatters("Accessibility");
        dto.setRecommendation("Add alt text");
        dto.setPriority(Priority.HIGH);
        dto.setType(ObservationType.ELEMENT);
        dto.setCategory(AuditCategory.ACCESSIBILITY);
        dto.setWcagCompliance("AA");
        dto.setElementSelector("img.hero");
        dto.setPageUrl("https://example.com");
        Set<String> labels = new HashSet<>();
        labels.add("a11y");
        dto.setLabels(labels);

        assertEquals("Missing alt text", dto.getTitle());
        assertEquals(Priority.HIGH, dto.getPriority());
        assertEquals(ObservationType.ELEMENT, dto.getType());
        assertEquals(AuditCategory.ACCESSIBILITY, dto.getCategory());
        assertEquals("AA", dto.getWcagCompliance());
        assertEquals(1, dto.getLabels().size());
    }

    // ===== User =====
    @Test
    void userConstructor() {
        User user = new User("test@example.com", "John");
        assertEquals("test@example.com", user.getEmail());
        assertEquals("John", user.getName());
    }

    @Test
    void userSetters() {
        User user = new User("a@b.com", "Name");
        user.setId("user123");
        user.setCompanyName("Acme");
        user.setCompanyDomain("acme.com");
        assertEquals("user123", user.getId());
    }

    // ===== AuditRecordDto =====
    @Test
    void auditRecordDtoDefaultConstructor() {
        AuditRecordDto dto = new AuditRecordDto();
        assertEquals(ExecutionStatus.UNKNOWN, dto.getStatus());
        assertEquals(AuditLevel.UNKNOWN, dto.getLevel());
        assertEquals("", dto.getUrl());
    }

    @Test
    void auditRecordDtoSetters() {
        AuditRecordDto dto = new AuditRecordDto();
        dto.setId(1L);
        dto.setUrl("https://example.com");
        dto.setStatus(ExecutionStatus.COMPLETE);
        dto.setLevel(AuditLevel.PAGE);
        dto.setContentAuditScore(0.8);
        dto.setInfoArchScore(0.7);
        dto.setAestheticScore(0.6);
        assertEquals(1L, dto.getId());
        assertEquals("https://example.com", dto.getUrl());
        assertEquals(ExecutionStatus.COMPLETE, dto.getStatus());
        assertEquals(AuditLevel.PAGE, dto.getLevel());
    }

    // ===== PageStatisticDto =====
    @Test
    void pageStatisticDtoConstructor() {
        PageStatisticDto dto = new PageStatisticDto(
            1L, "https://example.com", "https://img.com/ss.png",
            0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2,
            42L, 50L, 100L, true, 0.95
        );
        assertEquals(1L, dto.getId());
        assertEquals("https://example.com", dto.getUrl());
        assertEquals(42L, dto.getAuditRecordId());
        assertTrue(dto.isComplete());
        assertEquals(100L, dto.getElementsExtracted());
    }
}
