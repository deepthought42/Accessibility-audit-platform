package com.looksee.models.message;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.models.config.JacksonConfig;

class MessageTest {

    private static class TestMessage extends Message {
        public TestMessage() { super(); }
        public TestMessage(long accountId) { super(accountId); }
    }

    // --- Default constructor tests ---

    @Test
    void defaultConstructor_setsMessageIdAsUuid() {
        TestMessage msg = new TestMessage();
        assertNotNull(msg.getMessageId());
        assertDoesNotThrow(() -> UUID.fromString(msg.getMessageId()));
    }

    @Test
    void defaultConstructor_setsPublishTimeNotNull() {
        TestMessage msg = new TestMessage();
        assertNotNull(msg.getPublishTime());
    }

    @Test
    void defaultConstructor_setsPublishTimeRecent() {
        LocalDateTime before = LocalDateTime.now().minus(1, ChronoUnit.SECONDS);
        TestMessage msg = new TestMessage();
        LocalDateTime after = LocalDateTime.now().plus(1, ChronoUnit.SECONDS);

        assertFalse(msg.getPublishTime().isBefore(before), "publishTime should not be before test start");
        assertFalse(msg.getPublishTime().isAfter(after), "publishTime should not be after test end");
    }

    @Test
    void defaultConstructor_setsAccountIdToNegativeOne() {
        TestMessage msg = new TestMessage();
        assertEquals(-1, msg.getAccountId());
    }

    @Test
    void defaultConstructor_setsCorrelationIdAsUuid() {
        TestMessage msg = new TestMessage();
        assertNotNull(msg.getCorrelationId());
        assertDoesNotThrow(() -> UUID.fromString(msg.getCorrelationId()));
    }

    @Test
    void defaultConstructor_setsMessageTypeToConcreteClassName() {
        TestMessage msg = new TestMessage();
        assertEquals("TestMessage", msg.getMessageType());
    }

    // --- Parameterized constructor tests ---

    @Test
    void parameterizedConstructor_setsAccountIdCorrectly() {
        TestMessage msg = new TestMessage(42L);
        assertEquals(42L, msg.getAccountId());
    }

    @Test
    void parameterizedConstructor_autoGeneratesOtherFields() {
        TestMessage msg = new TestMessage(99L);
        assertNotNull(msg.getMessageId());
        assertDoesNotThrow(() -> UUID.fromString(msg.getMessageId()));
        assertNotNull(msg.getPublishTime());
        assertNotNull(msg.getCorrelationId());
        assertDoesNotThrow(() -> UUID.fromString(msg.getCorrelationId()));
        assertEquals("TestMessage", msg.getMessageType());
    }

    // --- Uniqueness tests ---

    @Test
    void messageId_isUniqueAcrossInstances() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            ids.add(new TestMessage().getMessageId());
        }
        assertEquals(100, ids.size(), "All 100 messageIds should be unique");
    }

    @Test
    void correlationId_isUniqueAcrossInstances() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            ids.add(new TestMessage().getCorrelationId());
        }
        assertEquals(100, ids.size(), "All 100 correlationIds should be unique");
    }

    @Test
    void messageId_isDifferentFromCorrelationId() {
        TestMessage msg = new TestMessage();
        assertNotEquals(msg.getMessageId(), msg.getCorrelationId());
    }

    // --- Getter/setter tests ---

    @Test
    void gettersAndSetters_workCorrectly() {
        TestMessage msg = new TestMessage();

        msg.setMessageId("custom-id");
        assertEquals("custom-id", msg.getMessageId());

        msg.setAccountId(123L);
        assertEquals(123L, msg.getAccountId());

        LocalDateTime time = LocalDateTime.of(2025, 1, 15, 10, 30);
        msg.setPublishTime(time);
        assertEquals(time, msg.getPublishTime());

        msg.setCorrelationId("custom-correlation");
        assertEquals("custom-correlation", msg.getCorrelationId());

        msg.setMessageType("CustomType");
        assertEquals("CustomType", msg.getMessageType());
    }

    // --- JSON serialization tests ---

    @Test
    void jsonRoundtrip_preservesAllFields() throws Exception {
        ObjectMapper mapper = JacksonConfig.mapper();

        TestMessage original = new TestMessage(55L);
        String json = mapper.writeValueAsString(original);
        TestMessage deserialized = mapper.readValue(json, TestMessage.class);

        assertEquals(original.getMessageId(), deserialized.getMessageId());
        assertEquals(original.getAccountId(), deserialized.getAccountId());
        assertEquals(original.getPublishTime(), deserialized.getPublishTime());
        assertEquals(original.getCorrelationId(), deserialized.getCorrelationId());
        assertEquals(original.getMessageType(), deserialized.getMessageType());
    }

    @Test
    void jsonIgnoreProperties_allowsExtraFieldsWithoutFailing() throws Exception {
        ObjectMapper mapper = JacksonConfig.mapper();
        String json = "{\"messageId\":\"abc\",\"accountId\":10,\"unknownField\":\"value\",\"anotherExtra\":123}";
        assertDoesNotThrow(() -> mapper.readValue(json, TestMessage.class));
    }

    @Test
    void publishTime_serializesToIso8601Format() throws Exception {
        ObjectMapper mapper = JacksonConfig.mapper();
        TestMessage msg = new TestMessage();
        msg.setPublishTime(LocalDateTime.of(2025, 6, 15, 14, 30, 45));

        String json = mapper.writeValueAsString(msg);
        assertTrue(json.contains("2025-06-15T14:30:45"), "JSON should contain ISO-8601 formatted date, got: " + json);
        assertFalse(json.contains("[2025"), "Date should not be serialized as array");
    }
}
