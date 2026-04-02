package com.looksee.models.designsystem;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.looksee.models.enums.AudienceProficiency;
import com.looksee.models.enums.WCAGComplianceLevel;

/**
 * Unit tests for DesignSystem classes.
 */
class DesignSystemTest {

    @Test
    void designSystemDefaultConstructor() {
        DesignSystem ds = new DesignSystem();
        assertNotNull(ds);
        assertEquals(WCAGComplianceLevel.AAA, ds.getWcagComplianceLevel());
        assertEquals(AudienceProficiency.GENERAL, ds.getAudienceProficiency());
        assertNotNull(ds.getColorPalette());
        assertNotNull(ds.getAllowedImageCharacteristics());
    }

    @Test
    void designSystemSetWcagLevel() {
        DesignSystem ds = new DesignSystem();
        ds.setWcagComplianceLevel(WCAGComplianceLevel.AA);
        assertEquals(WCAGComplianceLevel.AA, ds.getWcagComplianceLevel());
    }

    @Test
    void designSystemSetAudienceProficiency() {
        DesignSystem ds = new DesignSystem();
        ds.setAudienceProficiency(AudienceProficiency.EXPERT);
        assertEquals(AudienceProficiency.EXPERT, ds.getAudienceProficiency());
    }

    @Test
    void designSystemAddColor() {
        DesignSystem ds = new DesignSystem();
        assertTrue(ds.addColor("#FF0000"));
        assertTrue(ds.addColor("#FF0000")); // duplicate returns true
        assertEquals(1, ds.getColorPalette().size());
    }

    @Test
    void designSystemRemoveColor() {
        DesignSystem ds = new DesignSystem();
        ds.addColor("#FF0000");
        assertTrue(ds.removeColor("#FF0000"));
        assertTrue(ds.getColorPalette().isEmpty());
    }

    @Test
    void designSystemGenerateKey() {
        DesignSystem ds = new DesignSystem();
        String key = ds.generateKey();
        assertNotNull(key);
        assertTrue(key.startsWith("designsystem"));
    }
}
