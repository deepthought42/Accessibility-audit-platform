package com.crawlerApi.generators;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IntegerGeneratorTest {

    @Test
    void testGenerateValueReturnsLong() {
        IntegerGenerator generator = new IntegerGenerator();
        Long value = generator.generateValue();
        assertNotNull(value);
    }

    @Test
    void testImplementsIFieldGenerator() {
        IntegerGenerator generator = new IntegerGenerator();
        assertInstanceOf(IFieldGenerator.class, generator);
    }

    @Test
    void testGenerateValueProducesDifferentValues() {
        IntegerGenerator generator = new IntegerGenerator();
        Long value1 = generator.generateValue();
        boolean foundDifferent = false;
        for (int i = 0; i < 50; i++) {
            Long value2 = generator.generateValue();
            if (!value1.equals(value2)) {
                foundDifferent = true;
                break;
            }
        }
        assertTrue(foundDifferent, "Generator should produce different values");
    }

    @Test
    void testMultipleGenerationsAllReturnNonNull() {
        IntegerGenerator generator = new IntegerGenerator();
        for (int i = 0; i < 20; i++) {
            assertNotNull(generator.generateValue());
        }
    }
}
