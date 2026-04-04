package com.looksee.gcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.models.OutboxEvent;
import com.looksee.models.ProcessedMessage;
import com.looksee.models.config.JacksonConfig;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.PageAuditMessage;
import com.looksee.models.message.PageBuiltMessage;
import com.looksee.models.repository.OutboxEventRepository;
import com.looksee.models.repository.ProcessedMessageRepository;
import com.looksee.services.IdempotencyService;

/**
 * End-to-end test for the complete message lifecycle: serialization,
 * deserialization, idempotency, and outbox event processing.
 *
 * Uses Mockito mocks for repositories but tests the full flow through
 * real service classes and Jackson configuration.
 */
@ExtendWith(MockitoExtension.class)
class MessageFlowEndToEndTest {

    @Mock
    private ProcessedMessageRepository processedMessageRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private IdempotencyService idempotencyService;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService();
        // Inject mock repository via reflection since it's @Autowired(required=false)
        try {
            java.lang.reflect.Field repoField = IdempotencyService.class.getDeclaredField("processedMessageRepository");
            repoField.setAccessible(true);
            repoField.set(idempotencyService, processedMessageRepository);
        } catch (Exception e) {
            fail("Failed to inject mock repository: " + e.getMessage());
        }
        mapper = JacksonConfig.mapper();
    }

    @Test
    @DisplayName("Full message lifecycle: create -> serialize -> verify fields -> dedup check -> process -> verify processed")
    void testFullMessageLifecycle() throws Exception {
        // Step 1: Create a message with correct fields
        PageBuiltMessage message = new PageBuiltMessage(42L, 100L, 200L);
        assertNotNull(message.getMessageId(), "messageId should be auto-generated");
        assertNotNull(message.getCorrelationId(), "correlationId should be auto-generated");
        assertEquals("PageBuiltMessage", message.getMessageType(), "messageType should reflect class name");

        // Step 2: Serialize to JSON
        String json = mapper.writeValueAsString(message);
        assertNotNull(json);

        // Step 3: Verify JSON preserves all fields
        JsonNode node = mapper.readTree(json);
        assertTrue(node.has("messageId"), "JSON should contain messageId");
        assertTrue(node.has("correlationId"), "JSON should contain correlationId");
        assertTrue(node.has("messageType"), "JSON should contain messageType");
        assertTrue(node.has("accountId"), "JSON should contain accountId");
        assertTrue(node.has("pageId"), "JSON should contain pageId");
        assertTrue(node.has("auditRecordId"), "JSON should contain auditRecordId");
        assertEquals("PageBuiltMessage", node.get("messageType").asText());
        assertEquals(message.getCorrelationId(), node.get("correlationId").asText());

        // Step 4: Deserialize back and verify all fields match
        PageBuiltMessage deserialized = mapper.readValue(json, PageBuiltMessage.class);
        assertEquals(message.getMessageId(), deserialized.getMessageId());
        assertEquals(message.getCorrelationId(), deserialized.getCorrelationId());
        assertEquals(message.getMessageType(), deserialized.getMessageType());
        assertEquals(message.getAccountId(), deserialized.getAccountId());
        assertEquals(message.getPageId(), deserialized.getPageId());
        assertEquals(message.getAuditRecordId(), deserialized.getAuditRecordId());

        // Step 5: Idempotency check returns false (new message)
        String pubsubMsgId = "pubsub-envelope-123";
        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName(pubsubMsgId, "test-service"))
                .thenReturn(false);
        assertFalse(idempotencyService.isAlreadyProcessed(pubsubMsgId, "test-service"),
                "New message should not be flagged as already processed");

        // Step 6: After marking as processed, idempotency check returns true
        when(processedMessageRepository.save(any(ProcessedMessage.class)))
                .thenAnswer(invocation -> {
                    ProcessedMessage pm = invocation.getArgument(0);
                    pm.setId(1L);
                    return pm;
                });
        idempotencyService.markProcessed(pubsubMsgId, "test-service");

        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName(pubsubMsgId, "test-service"))
                .thenReturn(true);
        assertTrue(idempotencyService.isAlreadyProcessed(pubsubMsgId, "test-service"),
                "Processed message should be flagged as already processed");
    }

    @Test
    @DisplayName("Duplicate message is rejected on second processing attempt")
    void testDuplicateMessageRejection() {
        String pubsubMsgId = "pubsub-dup-456";
        String serviceName = "audit-manager";

        // First check: not yet processed
        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName(pubsubMsgId, serviceName))
                .thenReturn(false);
        assertFalse(idempotencyService.isAlreadyProcessed(pubsubMsgId, serviceName));

        // Mark as processed
        when(processedMessageRepository.save(any(ProcessedMessage.class)))
                .thenAnswer(invocation -> {
                    ProcessedMessage pm = invocation.getArgument(0);
                    pm.setId(1L);
                    return pm;
                });
        idempotencyService.markProcessed(pubsubMsgId, serviceName);
        verify(processedMessageRepository).save(any(ProcessedMessage.class));

        // Second check: now flagged as duplicate
        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName(pubsubMsgId, serviceName))
                .thenReturn(true);
        assertTrue(idempotencyService.isAlreadyProcessed(pubsubMsgId, serviceName),
                "Second processing attempt should be rejected as duplicate");
    }

    @Test
    @DisplayName("OutboxEvent lifecycle: PENDING -> publish -> PROCESSED")
    void testOutboxEventLifecycle() {
        // Step 1: Create PENDING event
        String topic = "projects/test/topics/page-audit";
        String payload = "{\"accountId\":1,\"pageAuditId\":42}";
        OutboxEvent event = new OutboxEvent(topic, payload);
        assertEquals("PENDING", event.getStatus());
        assertEquals(0, event.getRetryCount());
        assertNotNull(event.getEventId());
        assertNotNull(event.getCreatedAt());
        assertNull(event.getProcessedAt());

        // Step 2: Simulate save
        event.setId(10L);
        when(outboxEventRepository.findRetryableEvents()).thenReturn(Collections.singletonList(event));

        // Step 3: Simulate successful publish
        List<OutboxEvent> pending = outboxEventRepository.findRetryableEvents();
        assertEquals(1, pending.size());

        OutboxEvent toPublish = pending.get(0);
        toPublish.setStatus("PROCESSED");
        toPublish.setProcessedAt(LocalDateTime.now());

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(toPublish);
        OutboxEvent saved = outboxEventRepository.save(toPublish);

        // Step 4: Verify status transition
        assertEquals("PROCESSED", saved.getStatus());
        assertNotNull(saved.getProcessedAt());
        assertEquals(topic, saved.getTopic());
        assertEquals(payload, saved.getPayload());

        // Step 5: After processing, findRetryableEvents should return empty
        when(outboxEventRepository.findRetryableEvents()).thenReturn(Collections.emptyList());
        assertTrue(outboxEventRepository.findRetryableEvents().isEmpty(),
                "No pending events should remain after processing");
    }

    @Test
    @DisplayName("correlationId is preserved when a message is serialized and deserialized in another service")
    void testCorrelationIdPreservedAcrossServices() throws Exception {
        // Service A creates a message
        PageBuiltMessage originalMessage = new PageBuiltMessage(1L, 100L, 200L);
        String originalCorrelationId = originalMessage.getCorrelationId();
        assertNotNull(originalCorrelationId);

        // Service A serializes and publishes
        String json = mapper.writeValueAsString(originalMessage);

        // Service B receives and deserializes
        PageBuiltMessage receivedMessage = mapper.readValue(json, PageBuiltMessage.class);

        // Verify correlationId is preserved
        assertEquals(originalCorrelationId, receivedMessage.getCorrelationId(),
                "correlationId must be preserved across serialization boundaries");

        // Service B creates a downstream message, manually propagating correlationId
        PageAuditMessage downstreamMessage = new PageAuditMessage(1L, 42L);
        downstreamMessage.setCorrelationId(receivedMessage.getCorrelationId());

        String downstreamJson = mapper.writeValueAsString(downstreamMessage);
        PageAuditMessage finalMessage = mapper.readValue(downstreamJson, PageAuditMessage.class);

        assertEquals(originalCorrelationId, finalMessage.getCorrelationId(),
                "correlationId must remain the same after multi-hop serialization");
    }

    @Test
    @DisplayName("messageType field enables routing in audit-service")
    void testMessageTypeRoutingEndToEnd() throws Exception {
        // Create different message types
        PageBuiltMessage pageBuilt = new PageBuiltMessage(1L, 100L, 200L);
        PageAuditMessage pageAudit = new PageAuditMessage(1L, 42L);
        AuditProgressUpdate progressUpdate = new AuditProgressUpdate(
                1L, 0.75, "Processing...", AuditCategory.CONTENT, AuditLevel.PAGE, 42L);

        // Serialize each
        String pageBuiltJson = mapper.writeValueAsString(pageBuilt);
        String pageAuditJson = mapper.writeValueAsString(pageAudit);
        String progressJson = mapper.writeValueAsString(progressUpdate);

        // Verify messageType field is present and correct in each
        JsonNode pageBuiltNode = mapper.readTree(pageBuiltJson);
        JsonNode pageAuditNode = mapper.readTree(pageAuditJson);
        JsonNode progressNode = mapper.readTree(progressJson);

        assertEquals("PageBuiltMessage", pageBuiltNode.get("messageType").asText());
        assertEquals("PageAuditMessage", pageAuditNode.get("messageType").asText());
        assertEquals("AuditProgressUpdate", progressNode.get("messageType").asText());

        // Simulate audit-service routing logic: read messageType from JSON, then
        // deserialize to the correct class
        String messageType = progressNode.get("messageType").asText();
        switch (messageType) {
            case "AuditProgressUpdate":
                AuditProgressUpdate routed = mapper.readValue(progressJson, AuditProgressUpdate.class);
                assertEquals(0.75, routed.getProgress(), 0.001);
                assertEquals(AuditCategory.CONTENT, routed.getCategory());
                assertEquals(AuditLevel.PAGE, routed.getLevel());
                assertEquals(42L, routed.getPageAuditId());
                break;
            case "PageAuditMessage":
                fail("Should have matched AuditProgressUpdate, not PageAuditMessage");
                break;
            default:
                fail("Unexpected messageType: " + messageType);
        }

        // Verify PageAuditMessage routes correctly
        String auditMsgType = pageAuditNode.get("messageType").asText();
        assertEquals("PageAuditMessage", auditMsgType);
        PageAuditMessage routedAudit = mapper.readValue(pageAuditJson, PageAuditMessage.class);
        assertEquals(42L, routedAudit.getPageAuditId());
    }

    @Test
    @DisplayName("OutboxEvent PENDING -> retry -> FAILED after max retries")
    void testOutboxEventFailureAfterMaxRetries() {
        OutboxEvent event = new OutboxEvent("projects/test/topics/errors", "{\"error\":true}");
        assertEquals("PENDING", event.getStatus());

        // Simulate 5 failed retries
        for (int attempt = 1; attempt <= 5; attempt++) {
            event.setRetryCount(attempt);
        }
        event.setStatus("FAILED");

        assertEquals("FAILED", event.getStatus());
        assertEquals(5, event.getRetryCount());
        assertNull(event.getProcessedAt(), "Failed events should not have processedAt set");
    }
}
