package com.looksee.visualDesignAudit;

import static org.junit.Assert.*;

import org.junit.Test;

public class ApplicationTest {

    @Test
    public void testApplicationClassExists() {
        Application app = new Application();
        assertNotNull(app);
    }

    @Test
    public void testApplicationHasMainMethod() throws NoSuchMethodException {
        // Verify main method exists with correct signature
        assertNotNull(Application.class.getMethod("main", String[].class));
    }

    @Test
    public void testApplicationHasSpringBootAnnotation() {
        // Verify that the class has the SpringBootApplication annotation
        assertTrue(Application.class.isAnnotationPresent(
            org.springframework.boot.autoconfigure.SpringBootApplication.class
        ));
    }

    @Test
    public void testApplicationScanBasePackages() {
        org.springframework.boot.autoconfigure.SpringBootApplication annotation =
            Application.class.getAnnotation(org.springframework.boot.autoconfigure.SpringBootApplication.class);
        assertNotNull(annotation);
        String[] scanBasePackages = annotation.scanBasePackages();
        assertEquals(1, scanBasePackages.length);
        assertEquals("com.looksee.visualDesignAudit.audit", scanBasePackages[0]);
    }

    @Test
    public void testApplicationHasPropertySourceAnnotation() {
        assertTrue(Application.class.isAnnotationPresent(
            org.springframework.context.annotation.PropertySources.class
        ));
    }
}
