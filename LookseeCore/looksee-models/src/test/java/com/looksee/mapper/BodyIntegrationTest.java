package com.looksee.mapper;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.models.config.JacksonConfig;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.PageAuditMessage;
import com.looksee.models.message.PageBuiltMessage;

/**
 * Integration test for the complete PubSub push envelope deserialization
 * pipeline: JSON envelope -> Body -> Base64 decode -> inner message.
 */
class BodyIntegrationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = JacksonConfig.mapper();
    }

    /**
     * Builds a PubSub push envelope JSON string mimicking what GCP sends,
     * with the inner message encoded as Base64 in the data field.
     */
    private String buildPubSubEnvelope(String messageId, String innerMessageJson) {
        String base64Data = Base64.getEncoder().encodeToString(
                innerMessageJson.getBytes(StandardCharsets.UTF_8));
        return String.format(
                "{\"message\":{\"messageId\":\"%s\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"%s\"}}",
                messageId, base64Data);
    }

    @Test
    @DisplayName("Deserialize PubSub envelope containing a PageBuiltMessage")
    void testPageBuiltMessageEnvelope() throws Exception {
        // Create the inner message
        PageBuiltMessage innerMessage = new PageBuiltMessage(42L, 100L, 200L);
        String innerJson = mapper.writeValueAsString(innerMessage);

        // Build the PubSub envelope
        String envelopeJson = buildPubSubEnvelope("msg-pgbuilt-001", innerJson);

        // Deserialize the envelope into Body
        Body body = mapper.readValue(envelopeJson, Body.class);
        assertNotNull(body);
        assertNotNull(body.getMessage());
        assertEquals("msg-pgbuilt-001", body.getMessage().getMessageId());
        assertNotNull(body.getMessage().getData());

        // Base64 decode the data
        String decodedData = new String(
                Base64.getDecoder().decode(body.getMessage().getData()),
                StandardCharsets.UTF_8);

        // Deserialize the inner message
        PageBuiltMessage deserialized = mapper.readValue(decodedData, PageBuiltMessage.class);
        assertNotNull(deserialized);
        assertEquals(42L, deserialized.getAccountId());
        assertEquals(100L, deserialized.getPageId());
        assertEquals(200L, deserialized.getAuditRecordId());

        // Verify correlationId and messageType are preserved
        assertNotNull(deserialized.getCorrelationId(),
                "correlationId should survive envelope round-trip");
        assertEquals(innerMessage.getCorrelationId(), deserialized.getCorrelationId());
        assertEquals("PageBuiltMessage", deserialized.getMessageType());
    }

    @Test
    @DisplayName("Deserialize PubSub envelope containing a PageAuditMessage")
    void testPageAuditMessageEnvelope() throws Exception {
        PageAuditMessage innerMessage = new PageAuditMessage(10L, 555L);
        String innerJson = mapper.writeValueAsString(innerMessage);
        String envelopeJson = buildPubSubEnvelope("msg-pgaudit-002", innerJson);

        Body body = mapper.readValue(envelopeJson, Body.class);
        assertNotNull(body.getMessage());
        assertEquals("msg-pgaudit-002", body.getMessage().getMessageId());

        String decodedData = new String(
                Base64.getDecoder().decode(body.getMessage().getData()),
                StandardCharsets.UTF_8);
        PageAuditMessage deserialized = mapper.readValue(decodedData, PageAuditMessage.class);

        assertEquals(10L, deserialized.getAccountId());
        assertEquals(555L, deserialized.getPageAuditId());
        assertNotNull(deserialized.getCorrelationId());
        assertEquals(innerMessage.getCorrelationId(), deserialized.getCorrelationId());
        assertEquals("PageAuditMessage", deserialized.getMessageType());
    }

    @Test
    @DisplayName("Deserialize PubSub envelope containing an AuditProgressUpdate")
    void testAuditProgressUpdateEnvelope() throws Exception {
        AuditProgressUpdate innerMessage = new AuditProgressUpdate(
                5L, 0.85, "Content audit in progress",
                AuditCategory.CONTENT, AuditLevel.PAGE, 300L);
        String innerJson = mapper.writeValueAsString(innerMessage);
        String envelopeJson = buildPubSubEnvelope("msg-progress-003", innerJson);

        Body body = mapper.readValue(envelopeJson, Body.class);
        assertNotNull(body.getMessage());

        String decodedData = new String(
                Base64.getDecoder().decode(body.getMessage().getData()),
                StandardCharsets.UTF_8);
        AuditProgressUpdate deserialized = mapper.readValue(decodedData, AuditProgressUpdate.class);

        assertEquals(5L, deserialized.getAccountId());
        assertEquals(0.85, deserialized.getProgress(), 0.001);
        assertEquals("Content audit in progress", deserialized.getMessage());
        assertEquals(AuditCategory.CONTENT, deserialized.getCategory());
        assertEquals(AuditLevel.PAGE, deserialized.getLevel());
        assertEquals(300L, deserialized.getPageAuditId());
        assertNotNull(deserialized.getCorrelationId());
        assertEquals(innerMessage.getCorrelationId(), deserialized.getCorrelationId());
        assertEquals("AuditProgressUpdate", deserialized.getMessageType());
    }

    @Test
    @DisplayName("messageType field is preserved in JSON for routing purposes")
    void testMessageTypePreservedInJson() throws Exception {
        PageBuiltMessage pageBuilt = new PageBuiltMessage(1L, 10L, 20L);
        PageAuditMessage pageAudit = new PageAuditMessage(1L, 30L);
        AuditProgressUpdate progress = new AuditProgressUpdate(
                1L, 0.5, "half done", AuditCategory.CONTENT, AuditLevel.PAGE, 30L);

        // Serialize, wrap in envelope, deserialize, decode, and check messageType
        for (Object[] testCase : new Object[][]{
                {pageBuilt, "PageBuiltMessage"},
                {pageAudit, "PageAuditMessage"},
                {progress, "AuditProgressUpdate"}
        }) {
            Object msg = testCase[0];
            String expectedType = (String) testCase[1];

            String innerJson = mapper.writeValueAsString(msg);
            String envelope = buildPubSubEnvelope("msg-type-test", innerJson);

            Body body = mapper.readValue(envelope, Body.class);
            String decoded = new String(
                    Base64.getDecoder().decode(body.getMessage().getData()),
                    StandardCharsets.UTF_8);

            JsonNode node = mapper.readTree(decoded);
            assertTrue(node.has("messageType"),
                    "messageType should be present for " + expectedType);
            assertEquals(expectedType, node.get("messageType").asText(),
                    "messageType mismatch for " + expectedType);
        }
    }

    @Test
    @DisplayName("Envelope with empty data field is handled gracefully")
    void testEmptyDataField() throws Exception {
        String envelopeJson = "{\"message\":{\"messageId\":\"msg-empty\",\"publishTime\":\"2026-04-04T10:00:00Z\",\"data\":\"\"}}";
        Body body = mapper.readValue(envelopeJson, Body.class);

        assertNotNull(body.getMessage());
        assertEquals("msg-empty", body.getMessage().getMessageId());
        assertEquals("", body.getMessage().getData());
    }

    @Test
    @DisplayName("Envelope with null message is deserialized with null message field")
    void testNullMessage() throws Exception {
        String envelopeJson = "{\"message\":null}";
        Body body = mapper.readValue(envelopeJson, Body.class);
        assertNull(body.getMessage());
    }

    @Test
    @DisplayName("Multiple fields survive full envelope round-trip for PageBuiltMessage")
    void testFullRoundTripFieldPreservation() throws Exception {
        PageBuiltMessage original = new PageBuiltMessage(99L, 500L, 600L);
        String originalCorrelationId = original.getCorrelationId();
        String originalMessageId = original.getMessageId();

        // Serialize -> encode -> envelope -> decode -> deserialize
        String innerJson = mapper.writeValueAsString(original);
        String base64 = Base64.getEncoder().encodeToString(innerJson.getBytes(StandardCharsets.UTF_8));
        String envelopeJson = String.format(
                "{\"message\":{\"messageId\":\"envelope-id\",\"publishTime\":\"2026-04-04T12:00:00Z\",\"data\":\"%s\"}}",
                base64);

        Body body = mapper.readValue(envelopeJson, Body.class);
        String decoded = new String(Base64.getDecoder().decode(body.getMessage().getData()), StandardCharsets.UTF_8);
        PageBuiltMessage roundTripped = mapper.readValue(decoded, PageBuiltMessage.class);

        assertEquals(99L, roundTripped.getAccountId());
        assertEquals(500L, roundTripped.getPageId());
        assertEquals(600L, roundTripped.getAuditRecordId());
        assertEquals(originalCorrelationId, roundTripped.getCorrelationId());
        assertEquals(originalMessageId, roundTripped.getMessageId());
        assertEquals("PageBuiltMessage", roundTripped.getMessageType());
    }
}
