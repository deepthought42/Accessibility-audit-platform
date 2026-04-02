package com.looksee.models.message;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.looksee.models.enums.*;

/**
 * Unit tests for message classes.
 */
class MessageClassesTest {

    // ===== AuditError =====
    @Test
    void auditErrorConstructor() {
        AuditError error = new AuditError(1L, 2L, "something failed", AuditCategory.CONTENT, 0.5);
        assertEquals("something failed", error.getErrorMessage());
        assertEquals(AuditCategory.CONTENT, error.getAuditCategory());
        assertEquals(0.5, error.getProgress());
        assertEquals(2L, error.getAuditRecordId());
    }

    @Test
    void auditErrorInvalidProgress() {
        assertThrows(IllegalArgumentException.class, () ->
            new AuditError(1L, 2L, "err", AuditCategory.CONTENT, 1.5)
        );
    }

    // ===== AuditProgressUpdate =====
    @Test
    void auditProgressUpdateDefaultConstructor() {
        AuditProgressUpdate msg = new AuditProgressUpdate();
        assertNotNull(msg);
    }

    // ===== UrlMessage =====
    @Test
    void urlMessageDefaultConstructor() {
        UrlMessage msg = new UrlMessage();
        assertNotNull(msg);
    }

    // ===== PageBuiltMessage =====
    @Test
    void pageBuiltMessageDefaultConstructor() {
        PageBuiltMessage msg = new PageBuiltMessage();
        assertNotNull(msg);
    }

    // ===== ElementExtractionMessage =====
    @Test
    void elementExtractionMessageDefaultConstructor() {
        ElementExtractionMessage msg = new ElementExtractionMessage();
        assertNotNull(msg);
    }

    // ===== ElementExtractionError =====
    @Test
    void elementExtractionErrorDefaultConstructor() {
        ElementExtractionError msg = new ElementExtractionError();
        assertNotNull(msg);
    }

    // ===== PageDataExtractionError =====
    @Test
    void pageDataExtractionErrorDefaultConstructor() {
        PageDataExtractionError msg = new PageDataExtractionError();
        assertNotNull(msg);
    }

    // ===== ExceededSubscriptionMessage =====
    @Test
    void exceededSubscriptionMessageDefaultConstructor() {
        ExceededSubscriptionMessage msg = new ExceededSubscriptionMessage();
        assertNotNull(msg);
    }

    // ===== DiscoveryActionRequest =====
    @Test
    void discoveryActionRequestDefaultConstructor() {
        DiscoveryActionRequest msg = new DiscoveryActionRequest();
        assertNotNull(msg);
    }

    // ===== AuditStartMessage =====
    @Test
    void auditStartMessageDefaultConstructor() {
        AuditStartMessage msg = new AuditStartMessage();
        assertNotNull(msg);
    }

    // ===== BugMessage =====
    @Test
    void bugMessageDefaultConstructor() {
        BugMessage msg = new BugMessage();
        assertNotNull(msg);
    }

    // ===== AuditMessage =====
    @Test
    void auditMessageDefaultConstructor() {
        AuditMessage msg = new AuditMessage();
        assertNotNull(msg);
    }

    // ===== AuditSetMessage =====
    @Test
    void auditSetMessageConstructor() {
        AuditSetMessage msg = new AuditSetMessage(1L, new ArrayList<>(), "https://example.com");
        assertNotNull(msg);
        assertEquals("https://example.com", msg.getUrl());
        assertNotNull(msg.getAudits());
    }
}
