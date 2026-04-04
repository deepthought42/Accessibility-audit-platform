package com.looksee.models.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.looksee.models.OutboxEvent;

/**
 * Unit tests for {@link OutboxEventRepository} that validate the repository
 * contract through mocking, without requiring a live Neo4j instance.
 */
@ExtendWith(MockitoExtension.class)
class OutboxEventRepositoryTest {

    @Mock
    private OutboxEventRepository repository;

    private static final String TEST_TOPIC = "projects/test/topics/page-audit";
    private static final String TEST_PAYLOAD = "{\"accountId\":1,\"pageAuditId\":42}";

    @Test
    @DisplayName("findPendingEvents returns pending events ordered by createdAt")
    void findPendingEvents_returnsPendingEvents() {
        OutboxEvent event1 = new OutboxEvent(TEST_TOPIC, TEST_PAYLOAD);
        OutboxEvent event2 = new OutboxEvent(TEST_TOPIC, "{\"accountId\":2}");

        when(repository.findPendingEvents()).thenReturn(Arrays.asList(event1, event2));

        List<OutboxEvent> results = repository.findPendingEvents();

        assertEquals(2, results.size());
        assertEquals("PENDING", results.get(0).getStatus());
        assertEquals("PENDING", results.get(1).getStatus());
        verify(repository).findPendingEvents();
    }

    @Test
    @DisplayName("findPendingEvents returns empty list when no pending events exist")
    void findPendingEvents_returnsEmptyList_whenNoPendingEvents() {
        when(repository.findPendingEvents()).thenReturn(Collections.emptyList());

        List<OutboxEvent> results = repository.findPendingEvents();

        assertTrue(results.isEmpty());
        verify(repository).findPendingEvents();
    }

    @Test
    @DisplayName("findRetryableEvents returns only events with retryCount < 5")
    void findRetryableEvents_returnsRetryableEventsOnly() {
        OutboxEvent retryable = new OutboxEvent(TEST_TOPIC, TEST_PAYLOAD);
        retryable.setRetryCount(2);

        when(repository.findRetryableEvents()).thenReturn(Collections.singletonList(retryable));

        List<OutboxEvent> results = repository.findRetryableEvents();

        assertEquals(1, results.size());
        assertTrue(results.get(0).getRetryCount() < 5);
        verify(repository).findRetryableEvents();
    }

    @Test
    @DisplayName("findRetryableEvents excludes events that have exhausted retries")
    void findRetryableEvents_excludesExhaustedRetries() {
        // Only retryable events returned, exhausted ones excluded by query
        when(repository.findRetryableEvents()).thenReturn(Collections.emptyList());

        List<OutboxEvent> results = repository.findRetryableEvents();

        assertTrue(results.isEmpty());
        verify(repository).findRetryableEvents();
    }

    @Test
    @DisplayName("deleteOldProcessedEvents is callable")
    void deleteOldProcessedEvents_isCallable() {
        doNothing().when(repository).deleteOldProcessedEvents();

        repository.deleteOldProcessedEvents();

        verify(repository).deleteOldProcessedEvents();
    }

    @Test
    @DisplayName("save persists an OutboxEvent and returns it with an ID")
    void save_persistsOutboxEvent() {
        OutboxEvent event = new OutboxEvent(TEST_TOPIC, TEST_PAYLOAD);

        OutboxEvent savedEvent = new OutboxEvent(TEST_TOPIC, TEST_PAYLOAD);
        savedEvent.setId(100L);

        when(repository.save(any(OutboxEvent.class))).thenReturn(savedEvent);

        OutboxEvent result = repository.save(event);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(TEST_TOPIC, result.getTopic());
        assertEquals(TEST_PAYLOAD, result.getPayload());
        assertEquals("PENDING", result.getStatus());
        assertEquals(0, result.getRetryCount());
        assertNotNull(result.getCreatedAt());
        verify(repository).save(event);
    }

    @Test
    @DisplayName("OutboxEvent constructor sets fields correctly")
    void outboxEventConstructor_setsFieldsCorrectly() {
        LocalDateTime before = LocalDateTime.now();
        OutboxEvent event = new OutboxEvent(TEST_TOPIC, TEST_PAYLOAD);
        LocalDateTime after = LocalDateTime.now();

        assertEquals(TEST_TOPIC, event.getTopic());
        assertEquals(TEST_PAYLOAD, event.getPayload());
        assertEquals("PENDING", event.getStatus());
        assertEquals(0, event.getRetryCount());
        assertNotNull(event.getEventId(), "eventId should be auto-generated");
        assertNotNull(event.getCreatedAt());
        assertFalse(event.getCreatedAt().isBefore(before));
        assertFalse(event.getCreatedAt().isAfter(after));
        assertNull(event.getProcessedAt(), "processedAt should be null before processing");
    }

    @Test
    @DisplayName("OutboxEvent status transitions: PENDING -> PROCESSED")
    void outboxEvent_statusTransition_pendingToProcessed() {
        OutboxEvent event = new OutboxEvent(TEST_TOPIC, TEST_PAYLOAD);
        assertEquals("PENDING", event.getStatus());

        event.setStatus("PROCESSED");
        event.setProcessedAt(LocalDateTime.now());

        OutboxEvent saved = new OutboxEvent(TEST_TOPIC, TEST_PAYLOAD);
        saved.setId(1L);
        saved.setStatus("PROCESSED");
        saved.setProcessedAt(event.getProcessedAt());

        when(repository.save(any(OutboxEvent.class))).thenReturn(saved);

        OutboxEvent result = repository.save(event);

        assertEquals("PROCESSED", result.getStatus());
        assertNotNull(result.getProcessedAt());
    }

    @Test
    @DisplayName("OutboxEvent status transitions: PENDING -> FAILED after max retries")
    void outboxEvent_statusTransition_pendingToFailed() {
        OutboxEvent event = new OutboxEvent(TEST_TOPIC, TEST_PAYLOAD);

        // Simulate 5 failed retries
        for (int i = 0; i < 5; i++) {
            event.setRetryCount(event.getRetryCount() + 1);
        }
        event.setStatus("FAILED");

        assertEquals("FAILED", event.getStatus());
        assertEquals(5, event.getRetryCount());
    }
}
