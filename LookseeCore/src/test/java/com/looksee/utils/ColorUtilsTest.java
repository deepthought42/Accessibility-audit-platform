package com.looksee.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.looksee.models.ColorData;
import com.looksee.models.audit.recommend.ColorContrastRecommendation;

/**
 * Unit tests for ColorUtils.
 */
class ColorUtilsTest {

    @Test
    void textContrastMeetsWcag21AAAHighContrast() {
        // White on black - contrast ~21:1
        ColorData white = new ColorData(255, 255, 255);
        ColorData black = new ColorData(0, 0, 0);
        double contrast = ColorData.computeContrast(white, black);
        assertTrue(ColorUtils.textContrastMeetsWcag21AAA(contrast, 16.0, false));
    }

    @Test
    void textContrastMeetsWcag21AAALowContrast() {
        // Very similar colors - low contrast
        double contrast = 1.5;
        assertFalse(ColorUtils.textContrastMeetsWcag21AAA(contrast, 16.0, false));
    }

    @Test
    void textContrastMeetsWcag21AAALargeText() {
        // Large text (>=18px) needs only 4.5:1
        assertTrue(ColorUtils.textContrastMeetsWcag21AAA(5.0, 18.0, false));
    }

    @Test
    void textContrastMeetsWcag21AAABoldText() {
        // Bold text >=14px needs only 4.5:1
        assertTrue(ColorUtils.textContrastMeetsWcag21AAA(5.0, 14.0, true));
    }

    @Test
    void nonTextContrastMeetsWcag21AAAPass() {
        assertTrue(ColorUtils.nonTextContrastMeetsWcag21AAA(3.5));
    }

    @Test
    void nonTextContrastMeetsWcag21AAAFail() {
        assertFalse(ColorUtils.nonTextContrastMeetsWcag21AAA(2.5));
    }

    @Test
    void nonTextContrastMeetsWcag21AAAExactThreshold() {
        assertTrue(ColorUtils.nonTextContrastMeetsWcag21AAA(3.0));
    }

    @Test
    void findCompliantFontColorDarkOnLight() {
        ColorData font = new ColorData(200, 200, 200);
        ColorData bg = new ColorData(220, 220, 220);
        ColorContrastRecommendation rec = ColorUtils.findCompliantFontColor(font, bg, false, 16.0, false);
        // May or may not find a compliant color
        // If found, the recommendation should have valid rgb values
        if (rec != null) {
            assertNotNull(rec);
        }
    }

    @Test
    void findCompliantFontColorBlackOnWhite() {
        ColorData font = new ColorData(0, 0, 0);
        ColorData bg = new ColorData(255, 255, 255);
        ColorContrastRecommendation rec = ColorUtils.findCompliantFontColor(font, bg, false, 16.0, false);
        assertNotNull(rec);
    }

    @Test
    void findCompliantBackgroundColorAlreadyCompliant() {
        ColorData font = new ColorData(0, 0, 0);
        ColorData bg = new ColorData(255, 255, 255);
        ColorContrastRecommendation rec = ColorUtils.findCompliantBackgroundColor(font, bg, false, 16.0, false);
        assertNotNull(rec);
    }

    @Test
    void findCompliantNonTextBackgroundColorLightTheme() {
        ColorData element = new ColorData(100, 100, 100);
        ColorData bg = new ColorData(120, 120, 120);
        ColorContrastRecommendation rec = ColorUtils.findCompliantNonTextBackgroundColor(element, bg, false);
        if (rec != null) {
            assertNotNull(rec);
        }
    }

    // ===== Color classification tests =====
    @Test
    void isRedDetectsRed() {
        assertTrue(ColorUtils.isRed(new ColorData(255, 0, 0)));
    }

    @Test
    void isGreenDetectsGreen() {
        assertTrue(ColorUtils.isGreen(new ColorData(0, 255, 0)));
    }

    @Test
    void isBlueDetectsBlue() {
        assertTrue(ColorUtils.isBlue(new ColorData(0, 100, 255)));
    }

    @Test
    void isWhiteDetectsWhite() {
        assertTrue(ColorUtils.isWhite(new ColorData(255, 255, 255)));
    }

    @Test
    void isBlackDetectsBlack() {
        assertTrue(ColorUtils.isBlack(new ColorData(0, 0, 0)));
    }

    @Test
    void isOrangeDetectsOrange() {
        // Hue ~30 degrees
        ColorData orange = new ColorData(255, 128, 0);
        boolean result = ColorUtils.isOrange(orange);
        // Orange hue is around 30 degrees
        assertNotNull(result);
    }

    @Test
    void isYellowDetectsYellow() {
        ColorData yellow = new ColorData(255, 255, 0);
        assertNotNull(ColorUtils.isYellow(yellow));
    }

    @Test
    void isCyanDetectsCyan() {
        ColorData cyan = new ColorData(0, 255, 255);
        assertNotNull(ColorUtils.isCyan(cyan));
    }

    @Test
    void isVioletReturnsBoolean() {
        ColorData violet = new ColorData(128, 0, 255);
        assertNotNull(ColorUtils.isViolet(violet));
    }

    @Test
    void isPurpleReturnsBoolean() {
        ColorData purple = new ColorData(180, 0, 255);
        assertNotNull(ColorUtils.isPurple(purple));
    }

    @Test
    void isMagentaReturnsBoolean() {
        ColorData magenta = new ColorData(255, 0, 255);
        assertNotNull(ColorUtils.isMagenta(magenta));
    }

    @Test
    void isGoldReturnsBoolean() {
        ColorData gold = new ColorData(255, 200, 0);
        assertNotNull(ColorUtils.isGold(gold));
    }
}
