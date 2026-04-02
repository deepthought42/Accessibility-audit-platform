package com.looksee.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.looksee.models.ColorData;
import com.looksee.models.PaletteColor;
import com.looksee.models.audit.Score;
import com.looksee.models.enums.ColorScheme;

/**
 * Unit tests for ColorPaletteUtils.
 */
class ColorPaletteUtilsTest {

    @Test
    void getColorSchemeEmptyPalette() {
        Collection<PaletteColor> palette = new ArrayList<>();
        ColorScheme scheme = ColorPaletteUtils.getColorScheme(palette);
        assertEquals(ColorScheme.GRAYSCALE, scheme);
    }

    @Test
    void getColorSchemeTwoColors() {
        List<PaletteColor> palette = new ArrayList<>();
        palette.add(new PaletteColor("rgb(255, 0, 0)", 50.0, new HashMap<>()));
        palette.add(new PaletteColor("rgb(0, 255, 0)", 50.0, new HashMap<>()));
        ColorScheme scheme = ColorPaletteUtils.getColorScheme(palette);
        assertEquals(ColorScheme.COMPLEMENTARY, scheme);
    }

    @Test
    void getColorSchemeFourColors() {
        List<PaletteColor> palette = new ArrayList<>();
        palette.add(new PaletteColor("rgb(255, 0, 0)", 25.0, new HashMap<>()));
        palette.add(new PaletteColor("rgb(0, 255, 0)", 25.0, new HashMap<>()));
        palette.add(new PaletteColor("rgb(0, 0, 255)", 25.0, new HashMap<>()));
        palette.add(new PaletteColor("rgb(255, 255, 0)", 25.0, new HashMap<>()));
        ColorScheme scheme = ColorPaletteUtils.getColorScheme(palette);
        assertEquals(ColorScheme.TETRADIC, scheme);
    }

    @Test
    void getPaletteScoreGrayscale() {
        List<PaletteColor> palette = new ArrayList<>();
        Score score = ColorPaletteUtils.getPaletteScore(palette, ColorScheme.GRAYSCALE);
        assertEquals(3, score.getPointsAchieved());
        assertEquals(3, score.getMaxPossiblePoints());
    }

    @Test
    void getPaletteScoreUnknown() {
        List<PaletteColor> palette = new ArrayList<>();
        Score score = ColorPaletteUtils.getPaletteScore(palette, ColorScheme.UNKNOWN);
        assertEquals(0, score.getPointsAchieved());
    }

    @Test
    void isGrayScaleTrue() {
        ColorData gray = new ColorData(128, 128, 128);
        assertTrue(ColorPaletteUtils.isGrayScale(gray));
    }

    @Test
    void isGrayScaleFalse() {
        ColorData red = new ColorData(255, 0, 0);
        assertFalse(ColorPaletteUtils.isGrayScale(red));
    }

    @Test
    void isGrayScaleBlack() {
        ColorData black = new ColorData(0, 0, 0);
        assertTrue(ColorPaletteUtils.isGrayScale(black));
    }

    @Test
    void isGrayScaleWhite() {
        ColorData white = new ColorData(255, 255, 255);
        assertTrue(ColorPaletteUtils.isGrayScale(white));
    }

    @Test
    void isSimilarHueSameColor() {
        ColorData c1 = new ColorData(255, 0, 0);
        ColorData c2 = new ColorData(250, 5, 5);
        assertTrue(ColorPaletteUtils.isSimilarHue(c1, c2));
    }

    @Test
    void isSimilarHueDifferentColors() {
        ColorData red = new ColorData(255, 0, 0);
        ColorData blue = new ColorData(0, 0, 255);
        assertFalse(ColorPaletteUtils.isSimilarHue(red, blue));
    }

    @Test
    void isSimilarHueBothGrayscale() {
        ColorData gray1 = new ColorData(100, 100, 100);
        ColorData gray2 = new ColorData(200, 200, 200);
        assertTrue(ColorPaletteUtils.isSimilarHue(gray1, gray2));
    }

    @Test
    void isSimilarReturnsTrueForSameColor() {
        ColorData c1 = new ColorData(100, 150, 200);
        ColorData c2 = new ColorData(100, 150, 200);
        assertTrue(ColorPaletteUtils.isSimilar(c1, c2));
    }

    @Test
    void groupColorsEmptyList() {
        List<ColorData> colors = new ArrayList<>();
        Set<Set<ColorData>> groups = ColorPaletteUtils.groupColors(colors);
        assertNotNull(groups);
        assertTrue(groups.isEmpty());
    }

    @Test
    void extractPaletteEmptyList() {
        List<ColorData> colors = new ArrayList<>();
        List<PaletteColor> palette = ColorPaletteUtils.extractPalette(colors);
        assertNotNull(palette);
        assertTrue(palette.isEmpty());
    }

    @Test
    void extractColorsFromList() {
        List<ColorData> colors = new ArrayList<>();
        ColorData c = new ColorData(255, 0, 0);
        c.setUsagePercent(0.5);
        colors.add(c);
        List<PaletteColor> palette = ColorPaletteUtils.extractColors(colors);
        assertNotNull(palette);
        assertEquals(1, palette.size());
    }

    @Test
    void getMaxReturnsCorrectValue() {
        ColorData color = new ColorData(100, 200, 150);
        assertEquals(200, ColorPaletteUtils.getMax(color));
    }

    @Test
    void getMinReturnsCorrectValue() {
        ColorData color = new ColorData(100, 200, 150);
        assertEquals(100, ColorPaletteUtils.getMin(color));
    }

    @Test
    void retrieveNonCompliantColorsAllCompliant() {
        List<String> palette = Arrays.asList("rgb(255, 0, 0)");
        List<ColorData> colors = new ArrayList<>();
        colors.add(new ColorData(255, 0, 0));
        Map<String, Boolean> result = ColorPaletteUtils.retrieveNonCompliantColors(palette, colors);
        assertTrue(result.isEmpty());
    }

    @Test
    void retrieveNonCompliantColorsHasNonCompliant() {
        List<String> palette = Arrays.asList("rgb(255, 0, 0)");
        List<ColorData> colors = new ArrayList<>();
        colors.add(new ColorData(0, 0, 255));
        Map<String, Boolean> result = ColorPaletteUtils.retrieveNonCompliantColors(palette, colors);
        assertFalse(result.isEmpty());
    }
}
