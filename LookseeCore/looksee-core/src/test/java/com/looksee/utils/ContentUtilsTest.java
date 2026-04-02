package com.looksee.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.looksee.utils.ContentUtils;
import org.junit.jupiter.api.Test;

class ContentUtilsTest {

    @Test
    void getReadingGradeLevelReturnsExpectedRanges() {
        assertEquals("5th grade", ContentUtils.getReadingGradeLevel(90));
        assertEquals("6th grade", ContentUtils.getReadingGradeLevel(80));
        assertEquals("7th grade", ContentUtils.getReadingGradeLevel(70));
        assertEquals("8th and 9th grade", ContentUtils.getReadingGradeLevel(60));
        assertEquals("10th to 12th grade", ContentUtils.getReadingGradeLevel(50));
        assertEquals("college", ContentUtils.getReadingGradeLevel(30));
        assertEquals("college graduate", ContentUtils.getReadingGradeLevel(10));
        assertEquals("professional", ContentUtils.getReadingGradeLevel(9.99));
    }

    @Test
    void getReadingDifficultyRatingReturnsExpectedRanges() {
        assertEquals("very easy", ContentUtils.getReadingDifficultyRating(95));
        assertEquals("easy", ContentUtils.getReadingDifficultyRating(80));
        assertEquals("fairly easy", ContentUtils.getReadingDifficultyRating(70));
        assertEquals("somewhat difficult", ContentUtils.getReadingDifficultyRating(60));
        assertEquals("fairly difficult", ContentUtils.getReadingDifficultyRating(50));
        assertEquals("difficult", ContentUtils.getReadingDifficultyRating(30));
        assertEquals("very difficult", ContentUtils.getReadingDifficultyRating(10));
        assertEquals("extremely difficult", ContentUtils.getReadingDifficultyRating(0));
    }

    @Test
    void getReadingDifficultyRatingByEducationLevelUsesHighSchoolRulesForNullAndUnknownEducation() {
        assertEquals("very easy", ContentUtils.getReadingDifficultyRatingByEducationLevel(95, null));
        assertEquals("somewhat difficult", ContentUtils.getReadingDifficultyRatingByEducationLevel(65, "Unknown"));
    }

    @Test
    void getReadingDifficultyRatingByEducationLevelUsesCollegeRules() {
        assertEquals("very easy", ContentUtils.getReadingDifficultyRatingByEducationLevel(85, "College"));
        assertEquals("easy", ContentUtils.getReadingDifficultyRatingByEducationLevel(75, "College"));
        assertEquals("difficult", ContentUtils.getReadingDifficultyRatingByEducationLevel(20, "College"));
    }

    @Test
    void getReadingDifficultyRatingByEducationLevelUsesAdvancedRules() {
        assertEquals("easy", ContentUtils.getReadingDifficultyRatingByEducationLevel(65, "Advanced"));
        assertEquals("fairly easy", ContentUtils.getReadingDifficultyRatingByEducationLevel(55, "Advanced"));
        assertEquals("difficult", ContentUtils.getReadingDifficultyRatingByEducationLevel(5, "Advanced"));
    }
}
