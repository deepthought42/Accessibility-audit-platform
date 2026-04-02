package com.looksee.mapper;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for mapper classes.
 */
class MapperClassesTest {

    // ===== Body =====
    @Test
    void bodyDefaultConstructor() {
        Body body = new Body();
        assertNotNull(body);
        assertNull(body.getMessage());
    }

    @Test
    void bodySetMessage() {
        Body body = new Body();
        // Body.Message is an inner class, needs enclosing instance
        Body.Message msg = body.new Message("msg123", "2024-01-01T00:00:00Z", "dGVzdA==");
        body.setMessage(msg);
        assertNotNull(body.getMessage());
        assertEquals("msg123", body.getMessage().getMessageId());
        assertEquals("2024-01-01T00:00:00Z", body.getMessage().getPublishTime());
        assertEquals("dGVzdA==", body.getMessage().getData());
    }

    @Test
    void bodyMessageDefaultConstructor() {
        Body body = new Body();
        Body.Message msg = body.new Message();
        assertNull(msg.getMessageId());
        assertNull(msg.getData());
    }

    // ===== Base64StringDeserializer =====
    @Test
    void base64StringDeserializerInstance() {
        Base64StringDeserializer deserializer = new Base64StringDeserializer();
        assertNotNull(deserializer);
    }

    // ===== Base64JsonDeserializer =====
    @Test
    void base64JsonDeserializerInstance() {
        Base64JsonDeserializer deserializer = new Base64JsonDeserializer();
        assertNotNull(deserializer);
    }
}
