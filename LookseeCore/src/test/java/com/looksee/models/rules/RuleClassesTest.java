package com.looksee.models.rules;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for rule model classes.
 */
class RuleClassesTest {

    // ===== RuleType =====
    @Test
    void ruleTypeCreateValid() {
        assertEquals(RuleType.PATTERN, RuleType.create("pattern"));
        assertEquals(RuleType.REQUIRED, RuleType.create("required"));
        assertEquals(RuleType.ALPHABETIC_RESTRICTION, RuleType.create("alphabetic_restriction"));
        assertEquals(RuleType.SPECIAL_CHARACTER_RESTRICTION, RuleType.create("special_character_restriction"));
        assertEquals(RuleType.NUMERIC_RESTRICTION, RuleType.create("numeric_restriction"));
        assertEquals(RuleType.DISABLED, RuleType.create("disabled"));
        assertEquals(RuleType.NO_VALIDATE, RuleType.create("no_validate"));
        assertEquals(RuleType.READ_ONLY, RuleType.create("read_only"));
        assertEquals(RuleType.MIN_LENGTH, RuleType.create("min_length"));
        assertEquals(RuleType.MAX_LENGTH, RuleType.create("max_length"));
        assertEquals(RuleType.MIN_VALUE, RuleType.create("min_value"));
        assertEquals(RuleType.MAX_VALUE, RuleType.create("max_value"));
        assertEquals(RuleType.EMAIL_PATTERN, RuleType.create("email_pattern"));
        assertEquals(RuleType.CLICKABLE, RuleType.create("clickable"));
        assertEquals(RuleType.DOUBLE_CLICKABLE, RuleType.create("double_clickable"));
        assertEquals(RuleType.MOUSE_RELEASE, RuleType.create("mouse_release"));
        assertEquals(RuleType.MOUSE_OVER, RuleType.create("mouse_over"));
        assertEquals(RuleType.SCROLLABLE, RuleType.create("scrollable"));
    }

    @Test
    void ruleTypeCreateNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> RuleType.create(null));
    }

    @Test
    void ruleTypeCreateInvalidThrows() {
        assertThrows(IllegalArgumentException.class, () -> RuleType.create("nonexistent"));
    }

    @Test
    void ruleTypeToString() {
        assertEquals("pattern", RuleType.PATTERN.toString());
        assertEquals("required", RuleType.REQUIRED.toString());
    }

    @Test
    void ruleTypeRoundTrip() {
        for (RuleType v : RuleType.values()) {
            assertEquals(v, RuleType.create(v.toString()));
        }
    }

    @Test
    void ruleTypeValues() {
        assertEquals(18, RuleType.values().length);
    }

    // ===== RuleFactory =====
    @Test
    void ruleFactoryBuildAlphabeticRestriction() {
        Rule rule = RuleFactory.build("alphabetic_restriction", "");
        assertNotNull(rule);
        assertTrue(rule instanceof AlphabeticRestrictionRule);
    }

    @Test
    void ruleFactoryBuildDisabled() {
        Rule rule = RuleFactory.build("disabled", "");
        assertNotNull(rule);
        assertTrue(rule instanceof DisabledRule);
    }

    @Test
    void ruleFactoryBuildEmailPattern() {
        Rule rule = RuleFactory.build("email_pattern", "");
        assertNotNull(rule);
        assertTrue(rule instanceof EmailPatternRule);
    }

    @Test
    void ruleFactoryBuildMaxLength() {
        Rule rule = RuleFactory.build("max_length", "100");
        assertNotNull(rule);
        assertTrue(rule instanceof NumericRule);
    }

    @Test
    void ruleFactoryBuildMinLength() {
        Rule rule = RuleFactory.build("min_length", "5");
        assertNotNull(rule);
        assertTrue(rule instanceof NumericRule);
    }

    @Test
    void ruleFactoryBuildMaxValue() {
        Rule rule = RuleFactory.build("max_value", "999");
        assertNotNull(rule);
    }

    @Test
    void ruleFactoryBuildMinValue() {
        Rule rule = RuleFactory.build("min_value", "1");
        assertNotNull(rule);
    }

    @Test
    void ruleFactoryBuildNumericRestriction() {
        Rule rule = RuleFactory.build("numeric_restriction", "");
        assertNotNull(rule);
        assertTrue(rule instanceof NumericRestrictionRule);
    }

    @Test
    void ruleFactoryBuildPattern() {
        Rule rule = RuleFactory.build("pattern", "[a-z]+");
        assertNotNull(rule);
        assertTrue(rule instanceof PatternRule);
    }

    @Test
    void ruleFactoryBuildReadOnly() {
        Rule rule = RuleFactory.build("read_only", "");
        assertNotNull(rule);
        assertTrue(rule instanceof ReadOnlyRule);
    }

    @Test
    void ruleFactoryBuildRequired() {
        Rule rule = RuleFactory.build("required", "");
        assertNotNull(rule);
        assertTrue(rule instanceof RequirementRule);
    }

    @Test
    void ruleFactoryBuildSpecialCharRestriction() {
        Rule rule = RuleFactory.build("special_character_restriction", "");
        assertNotNull(rule);
        assertTrue(rule instanceof SpecialCharacterRestriction);
    }

    @Test
    void ruleFactoryBuildUnknownReturnsNull() {
        Rule rule = RuleFactory.build("unknown_type", "");
        assertNull(rule);
    }

    @Test
    void ruleFactoryBuildCaseInsensitive() {
        Rule rule = RuleFactory.build("DISABLED", "");
        assertNotNull(rule);
        assertTrue(rule instanceof DisabledRule);
    }
}
