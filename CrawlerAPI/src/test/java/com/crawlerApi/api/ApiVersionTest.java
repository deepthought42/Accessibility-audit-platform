package com.crawlerApi.api;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ApiVersionTest {

    @ApiVersion
    static class DefaultAnnotatedClass {}

    @ApiVersion(value = "v2", produces = "text/plain", consumes = "text/plain")
    static class CustomAnnotatedClass {}

    @Test
    void testDefaultValues() {
        ApiVersion annotation = DefaultAnnotatedClass.class.getAnnotation(ApiVersion.class);
        assertNotNull(annotation);
        assertEquals("v1", annotation.value());
        assertArrayEquals(new String[]{MediaType.APPLICATION_JSON_VALUE}, annotation.produces());
        assertArrayEquals(new String[]{MediaType.APPLICATION_JSON_VALUE}, annotation.consumes());
    }

    @Test
    void testCustomValues() {
        ApiVersion annotation = CustomAnnotatedClass.class.getAnnotation(ApiVersion.class);
        assertNotNull(annotation);
        assertEquals("v2", annotation.value());
        assertArrayEquals(new String[]{"text/plain"}, annotation.produces());
        assertArrayEquals(new String[]{"text/plain"}, annotation.consumes());
    }

    @Test
    void testAnnotationRetention() {
        Retention retention = ApiVersion.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    void testAnnotationTarget() {
        Target target = ApiVersion.class.getAnnotation(Target.class);
        assertNotNull(target);
        ElementType[] targets = target.value();
        assertEquals(2, targets.length);
        assertTrue(java.util.Arrays.asList(targets).contains(ElementType.TYPE));
        assertTrue(java.util.Arrays.asList(targets).contains(ElementType.METHOD));
    }
}
