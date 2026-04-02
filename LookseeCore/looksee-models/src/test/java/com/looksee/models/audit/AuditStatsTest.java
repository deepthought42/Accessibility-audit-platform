package com.looksee.models.audit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.looksee.models.audit.stats.*;

/**
 * Unit tests for audit stats classes.
 */
class AuditStatsTest {

    // ===== PageAuditStats =====
    @Test
    void pageAuditStatsDefaultConstructor() {
        PageAuditStats stats = new PageAuditStats();
        assertNotNull(stats);
    }

    @Test
    void pageAuditStatsSetters() {
        PageAuditStats stats = new PageAuditStats();
        stats.setContentScore(0.9);
        stats.setAccessibilityScore(0.8);
        stats.setAestheticScore(0.7);
        stats.setInfoArchitectureScore(0.6);
        assertEquals(0.9, stats.getContentScore());
        assertEquals(0.8, stats.getAccessibilityScore());
        assertEquals(0.7, stats.getAestheticScore());
        assertEquals(0.6, stats.getInfoArchitectureScore());
    }

    @Test
    void pageAuditStatsWithRecordId() {
        PageAuditStats stats = new PageAuditStats(42L);
        assertEquals(42L, stats.getAuditRecordId());
        assertNotNull(stats.getStartTime());
    }

    // ===== DomainAuditStats =====
    @Test
    void domainAuditStatsDefaultConstructor() {
        DomainAuditStats stats = new DomainAuditStats();
        assertNotNull(stats);
    }

    // ===== AuditSubcategoryStat =====
    @Test
    void auditSubcategoryStatDefaultConstructor() {
        AuditSubcategoryStat stat = new AuditSubcategoryStat();
        assertNotNull(stat);
    }

    // ===== ColorUsageStat =====
    @Test
    void colorUsageStatAuditDefaultConstructor() {
        com.looksee.models.audit.stats.ColorUsageStat stat = new com.looksee.models.audit.stats.ColorUsageStat();
        assertNotNull(stat);
    }
}
