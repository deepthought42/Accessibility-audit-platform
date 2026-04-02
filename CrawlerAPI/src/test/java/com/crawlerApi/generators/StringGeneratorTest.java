package com.crawlerApi.generators;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StringGeneratorTest {

    @Test
    void testGenerateValueReturnsString() {
        StringGenerator generator = new StringGenerator();
        String value = generator.generateValue();
        assertNotNull(value);
    }

    @Test
    void testImplementsIFieldGenerator() {
        StringGenerator generator = new StringGenerator();
        assertInstanceOf(IFieldGenerator.class, generator);
    }

    @Test
    void testDefaultConstructor() {
        StringGenerator generator = new StringGenerator();
        assertNotNull(generator);
    }

    @Test
    void testMultipleGenerationsAllReturnNonNull() {
        StringGenerator generator = new StringGenerator();
        for (int i = 0; i < 20; i++) {
            String value = generator.generateValue();
            assertNotNull(value, "Generated string should not be null");
        }
    }
}
