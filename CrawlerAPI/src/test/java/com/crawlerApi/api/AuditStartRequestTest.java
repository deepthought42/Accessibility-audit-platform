package com.crawlerApi.api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.looksee.models.enums.AuditLevel;

class AuditStartRequestTest {

    @Test
    void testNoArgsConstructor() {
        AuditStartRequest request = new AuditStartRequest();
        assertNull(request.getUrl());
        assertNull(request.getLevel());
    }

    @Test
    void testAllArgsConstructor() {
        AuditStartRequest request = new AuditStartRequest("https://example.com", AuditLevel.PAGE);
        assertEquals("https://example.com", request.getUrl());
        assertEquals(AuditLevel.PAGE, request.getLevel());
    }

    @Test
    void testSetUrl() {
        AuditStartRequest request = new AuditStartRequest();
        request.setUrl("https://test.com");
        assertEquals("https://test.com", request.getUrl());
    }

    @Test
    void testSetLevel() {
        AuditStartRequest request = new AuditStartRequest();
        request.setLevel(AuditLevel.DOMAIN);
        assertEquals(AuditLevel.DOMAIN, request.getLevel());
    }

    @Test
    void testAllArgsConstructorWithDomainLevel() {
        AuditStartRequest request = new AuditStartRequest("https://example.com", AuditLevel.DOMAIN);
        assertEquals(AuditLevel.DOMAIN, request.getLevel());
    }

    @Test
    void testSetNullValues() {
        AuditStartRequest request = new AuditStartRequest("https://example.com", AuditLevel.PAGE);
        request.setUrl(null);
        request.setLevel(null);
        assertNull(request.getUrl());
        assertNull(request.getLevel());
    }
}
