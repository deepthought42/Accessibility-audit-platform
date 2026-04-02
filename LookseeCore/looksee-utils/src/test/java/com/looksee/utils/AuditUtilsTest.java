package com.looksee.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.looksee.models.ColorData;
import com.looksee.models.PaletteColor;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditScore;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.enums.*;

/**
 * Unit tests for AuditUtils.
 */
class AuditUtilsTest {

    @Test
    void calculateScoreWithEmptyAudits() {
        Set<Audit> audits = new HashSet<>();
        double score = AuditUtils.calculateScore(audits);
        assertEquals(-1.0, score, 0.01);
    }

    @Test
    void calculateScoreWithAudits() {
        Set<Audit> audits = new HashSet<>();
        audits.add(createAudit(AuditCategory.CONTENT, AuditSubcategory.WRITTEN_CONTENT, AuditName.PARAGRAPHING, 8, 10));
        audits.add(createAudit(AuditCategory.AESTHETICS, AuditSubcategory.COLOR_MANAGEMENT, AuditName.COLOR_PALETTE, 6, 10));
        double score = AuditUtils.calculateScore(audits);
        assertTrue(score >= 0.0 && score <= 100.0);
    }

    @Test
    void calculateScoreWithZeroPointAudits() {
        Set<Audit> audits = new HashSet<>();
        audits.add(createAudit(AuditCategory.CONTENT, AuditSubcategory.WRITTEN_CONTENT, AuditName.PARAGRAPHING, 0, 0));
        double score = AuditUtils.calculateScore(audits);
        assertEquals(-1.0, score, 0.01); // filtered out because totalPossiblePoints == 0
    }

    @Test
    void extractAuditScoreWithEmptyAudits() {
        Set<Audit> audits = new HashSet<>();
        AuditScore score = AuditUtils.extractAuditScore(audits);
        assertNotNull(score);
    }

    @Test
    void extractAuditScoreWithAudits() {
        Set<Audit> audits = new HashSet<>();
        audits.add(createAudit(AuditCategory.CONTENT, AuditSubcategory.WRITTEN_CONTENT, AuditName.PARAGRAPHING, 8, 10));
        audits.add(createAudit(AuditCategory.AESTHETICS, AuditSubcategory.COLOR_MANAGEMENT, AuditName.COLOR_PALETTE, 6, 10));
        audits.add(createAudit(AuditCategory.ACCESSIBILITY, AuditSubcategory.TEXT_CONTRAST, AuditName.TEXT_BACKGROUND_CONTRAST, 9, 10));
        AuditScore score = AuditUtils.extractAuditScore(audits);
        assertNotNull(score);
    }

    @Test
    void isSimilarTrue() {
        ColorData c1 = new ColorData(100, 100, 100);
        ColorData c2 = new ColorData(102, 100, 100);
        assertTrue(AuditUtils.isSimilar(c1, c2));
    }

    @Test
    void isSimilarFalse() {
        ColorData c1 = new ColorData(100, 100, 100);
        ColorData c2 = new ColorData(200, 50, 50);
        assertFalse(AuditUtils.isSimilar(c1, c2));
    }

    @Test
    void isSimilarHueSameHue() {
        ColorData c1 = new ColorData(255, 0, 0);
        ColorData c2 = new ColorData(250, 5, 5);
        assertTrue(AuditUtils.isSimilarHue(c1, c2));
    }

    @Test
    void extractPaletteFromEmptyList() {
        List<ColorData> colors = new ArrayList<>();
        List<com.looksee.models.PaletteColor> palette = AuditUtils.extractPalette(colors);
        assertNotNull(palette);
        assertTrue(palette.isEmpty());
    }

    @Test
    void extractColorsFromList() {
        List<ColorData> colors = new ArrayList<>();
        ColorData c = new ColorData(255, 0, 0);
        c.setUsagePercent(0.5);
        colors.add(c);
        List<com.looksee.models.PaletteColor> palette = AuditUtils.extractColors(colors);
        assertNotNull(palette);
        assertEquals(1, palette.size());
    }

    private Audit createAudit(AuditCategory cat, AuditSubcategory subcat, AuditName name, int pts, int maxPts) {
        return new Audit(cat, subcat, name, pts, new HashSet<>(), AuditLevel.PAGE, maxPts, "url", "why", "desc", true);
    }
}
