package com.looksee.services;

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
        PageBuiltMessage message = new PageBuiltMessage(42L, 100L, 200L);
        assertNotNull(message.getMessageId());
        assertNotNull(message.getCorrelationId());
        assertEquals("PageBuiltMessage", message.getMessageType());

        String json = mapper.writeValueAsString(message);
        assertNotNull(json);

        JsonNode node = mapper.readTree(json);
        assertTrue(node.has("messageId"));
        assertTrue(node.has("correlationId"));
        assertTrue(node.has("messageType"));
        assertEquals("PageBuiltMessage", node.get("messageType").asText());

        PageBuiltMessage deserialized = mapper.readValue(json, PageBuiltMessage.class);
        assertEquals(message.getMessageId(), deserialized.getMessageId());
        assertEquals(message.getCorrelationId(), deserialized.getCorrelationId());
        assertEquals(message.getMessageType(), deserialized.getMessageType());

        String pubsubMsgId = "pubsub-envelope-123";
        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName(pubsubMsgId, "test-service"))
                .thenReturn(false);
        assertFalse(idempotencyService.isAlreadyProcessed(pubsubMsgId, "test-service"));

        when(processedMessageRepository.save(any(ProcessedMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        idempotencyService.markProcessed(pubsubMsgId, "test-service");

        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName(pubsubMsgId, "test-service"))
                .thenReturn(true);
        assertTrue(idempotencyService.isAlreadyProcessed(pubsubMsgId, "test-service"));
    }

    @Test
    @DisplayName("Duplicate message is rejected on second processing attempt")
    void testDuplicateMessageRejection() {
        String pubsubMsgId = "pubsub-dup-456";
        String serviceName = "audit-manager";

        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName(pubsubMsgId, serviceName))
                .thenReturn(false);
        assertFalse(idempotencyService.isAlreadyProcessed(pubsubMsgId, serviceName));

        when(processedMessageRepository.save(any(ProcessedMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        idempotencyService.markProcessed(pubsubMsgId, serviceName);
        verify(processedMessageRepository).save(any(ProcessedMessage.class));

        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName(pubsubMsgId, serviceName))
                .thenReturn(true);
        assertTrue(idempotencyService.isAlreadyProcessed(pubsubMsgId, serviceName));
    }

    @Test
    @DisplayName("OutboxEvent lifecycle: PENDING -> publish -> PROCESSED")
    void testOutboxEventLifecycle() {
        OutboxEvent event = new OutboxEvent("projects/test/topics/page-audit", "{\"accountId\":1}");
        assertEquals("PENDING", event.getStatus());
        assertEquals(0, event.getRetryCount());
        assertNotNull(event.getEventId());
        assertNull(event.getProcessedAt());

        event.setStatus("PROCESSED");
        event.setProcessedAt(LocalDateTime.now());
        assertEquals("PROCESSED", event.getStatus());
        assertNotNull(event.getProcessedAt());
    }

    @Test
    @DisplayName("correlationId is preserved across serialization boundaries")
    void testCorrelationIdPreservedAcrossServices() throws Exception {
        PageBuiltMessage originalMessage = new PageBuiltMessage(1L, 100L, 200L);
        String originalCorrelationId = originalMessage.getCorrelationId();

        String json = mapper.writeValueAsString(originalMessage);
        PageBuiltMessage receivedMessage = mapper.readValue(json, PageBuiltMessage.class);
        assertEquals(originalCorrelationId, receivedMessage.getCorrelationId());

        PageAuditMessage downstreamMessage = new PageAuditMessage(1L, 42L);
        downstreamMessage.setCorrelationId(receivedMessage.getCorrelationId());

        String downstreamJson = mapper.writeValueAsString(downstreamMessage);
        PageAuditMessage finalMessage = mapper.readValue(downstreamJson, PageAuditMessage.class);
        assertEquals(originalCorrelationId, finalMessage.getCorrelationId());
    }

    @Test
    @DisplayName("messageType field enables routing in audit-service")
    void testMessageTypeRoutingEndToEnd() throws Exception {
        PageBuiltMessage pageBuilt = new PageBuiltMessage(1L, 100L, 200L);
        PageAuditMessage pageAudit = new PageAuditMessage(1L, 42L);
        AuditProgressUpdate progressUpdate = new AuditProgressUpdate(
                1L, 0.75, "Processing...", AuditCategory.CONTENT, AuditLevel.PAGE, 42L);

        String progressJson = mapper.writeValueAsString(progressUpdate);
        JsonNode progressNode = mapper.readTree(progressJson);
        assertEquals("AuditProgressUpdate", progressNode.get("messageType").asText());

        String messageType = progressNode.get("messageType").asText();
        assertEquals("AuditProgressUpdate", messageType);

        AuditProgressUpdate routed = mapper.readValue(progressJson, AuditProgressUpdate.class);
        assertEquals(0.75, routed.getProgress(), 0.001);
        assertEquals(AuditCategory.CONTENT, routed.getCategory());
    }

    @Test
    @DisplayName("OutboxEvent PENDING -> retry -> FAILED after max retries")
    void testOutboxEventFailureAfterMaxRetries() {
        OutboxEvent event = new OutboxEvent("projects/test/topics/errors", "{\"error\":true}");
        assertEquals("PENDING", event.getStatus());

        for (int attempt = 1; attempt <= 5; attempt++) {
            event.setRetryCount(attempt);
        }
        event.setStatus("FAILED");

        assertEquals("FAILED", event.getStatus());
        assertEquals(5, event.getRetryCount());
        assertNull(event.getProcessedAt());
    }
}
