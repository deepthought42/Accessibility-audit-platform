package com.looksee.journeyExecutor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

class RetryConfigTest {
    @Test
    void retryConfigHasExpectedAnnotations() {
        assertTrue(RetryConfig.class.isAnnotationPresent(Configuration.class));
        assertTrue(RetryConfig.class.isAnnotationPresent(EnableRetry.class));
    }
}
