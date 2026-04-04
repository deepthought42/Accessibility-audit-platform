package com.looksee.models.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.models.message.Message;

class JacksonConfigTest {

    private static class SimpleMessage extends Message {
        public SimpleMessage() { super(); }
        public SimpleMessage(long accountId) { super(accountId); }
    }

    @Test
    void mapper_returnsNonNull() {
        assertNotNull(JacksonConfig.mapper());
    }

    @Test
    void mapper_returnsSameInstanceOnMultipleCalls() {
        ObjectMapper first = JacksonConfig.mapper();
        ObjectMapper second = JacksonConfig.mapper();
        assertSame(first, second, "mapper() should return the same singleton instance");
    }

    @Test
    void serializesLocalDateTime_asIso8601String() throws JsonProcessingException {
        ObjectMapper mapper = JacksonConfig.mapper();
        LocalDateTime dateTime = LocalDateTime.of(2025, 3, 20, 10, 15, 30);

        String json = mapper.writeValueAsString(dateTime);
        assertEquals("\"2025-03-20T10:15:30\"", json);
    }

    @Test
    void serializesLocalDateTime_notAsArray() throws JsonProcessingException {
        ObjectMapper mapper = JacksonConfig.mapper();
        LocalDateTime dateTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0);

        String json = mapper.writeValueAsString(dateTime);
        assertFalse(json.startsWith("["), "LocalDateTime should not serialize as array");
        assertTrue(json.startsWith("\""), "LocalDateTime should serialize as a quoted string");
    }

    @Test
    void deserializesLocalDateTime_fromIso8601String() throws JsonProcessingException {
        ObjectMapper mapper = JacksonConfig.mapper();
        String json = "\"2025-07-04T12:00:00\"";

        LocalDateTime result = mapper.readValue(json, LocalDateTime.class);
        assertEquals(LocalDateTime.of(2025, 7, 4, 12, 0, 0), result);
    }

    @Test
    void unknownProperties_doNotCauseExceptions() {
        ObjectMapper mapper = JacksonConfig.mapper();
        String json = "{\"messageId\":\"test-id\",\"unknownProp\":\"value\",\"extra\":42}";

        assertDoesNotThrow(() -> mapper.readValue(json, SimpleMessage.class));
    }

    @Test
    void handlesNullFieldsGracefully() throws JsonProcessingException {
        ObjectMapper mapper = JacksonConfig.mapper();
        SimpleMessage msg = new SimpleMessage();
        msg.setMessageId(null);
        msg.setCorrelationId(null);
        msg.setPublishTime(null);

        String json = assertDoesNotThrow(() -> mapper.writeValueAsString(msg));
        assertNotNull(json);

        SimpleMessage deserialized = mapper.readValue(json, SimpleMessage.class);
        assertNull(deserialized.getMessageId());
        assertNull(deserialized.getCorrelationId());
        assertNull(deserialized.getPublishTime());
    }

    @Test
    void roundtripSerialization_ofMessageSubclass() throws JsonProcessingException {
        ObjectMapper mapper = JacksonConfig.mapper();
        SimpleMessage original = new SimpleMessage(77L);

        String json = mapper.writeValueAsString(original);
        assertNotNull(json);
        assertFalse(json.isEmpty());

        SimpleMessage deserialized = mapper.readValue(json, SimpleMessage.class);
        assertEquals(original.getMessageId(), deserialized.getMessageId());
        assertEquals(original.getAccountId(), deserialized.getAccountId());
        assertEquals(original.getPublishTime(), deserialized.getPublishTime());
        assertEquals(original.getCorrelationId(), deserialized.getCorrelationId());
        assertEquals(original.getMessageType(), deserialized.getMessageType());
    }
}
