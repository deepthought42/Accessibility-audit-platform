package com.looksee.models.competitiveanalysis;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.looksee.models.competitiveanalysis.brand.Brand;

/**
 * Unit tests for competitive analysis classes.
 */
class CompetitiveAnalysisTest {

    // ===== Competitor =====
    @Test
    void competitorDefaultConstructor() {
        Competitor competitor = new Competitor();
        assertNotNull(competitor);
    }

    @Test
    void competitorSetters() {
        Competitor competitor = new Competitor();
        competitor.setCompanyName("Acme Corp");
        competitor.setUrl("https://acme.com");
        competitor.setIndustry("Technology");
        assertEquals("Acme Corp", competitor.getCompanyName());
        assertEquals("https://acme.com", competitor.getUrl());
        assertEquals("Technology", competitor.getIndustry());
    }

    // ===== Brand =====
    @Test
    void brandDefaultConstructor() {
        Brand brand = new Brand();
        assertNotNull(brand);
    }

    @Test
    void brandSetters() {
        Brand brand = new Brand();
        List<String> colors = new ArrayList<>();
        colors.add("#FF0000");
        colors.add("#00FF00");
        brand.setColors(colors);
        assertEquals(2, brand.getColors().size());
    }
}
