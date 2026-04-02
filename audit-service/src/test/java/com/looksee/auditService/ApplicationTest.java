package com.looksee.auditService;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class ApplicationTest {

    @Test
    void shouldHaveSpringBootApplicationAnnotation() {
        SpringBootApplication annotation = Application.class.getAnnotation(SpringBootApplication.class);

        assertNotNull(annotation);
    }
}
