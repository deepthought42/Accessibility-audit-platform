package com.crawlerApi.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class EntryPointTest {

    @Test
    void testEntryPointHasSpringBootApplicationAnnotation() {
        SpringBootApplication annotation = EntryPoint.class.getAnnotation(SpringBootApplication.class);
        assertNotNull(annotation, "EntryPoint should have @SpringBootApplication annotation");
    }

    @Test
    void testEntryPointClassExists() {
        assertDoesNotThrow(() -> Class.forName("com.crawlerApi.api.EntryPoint"));
    }
}
