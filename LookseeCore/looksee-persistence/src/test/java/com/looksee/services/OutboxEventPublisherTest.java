package com.looksee.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import com.looksee.messaging.TracingPubSubPublisher;
import com.looksee.messaging.observability.PubSubMetrics;
import com.looksee.models.OutboxEvent;
import com.looksee.models.OutboxEventStatus;
import com.looksee.models.repository.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private TracingPubSubPublisher tracingPubSubPublisher;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private PubSubMetrics pubSubMetrics;

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    private ListenableFuture<String> successFuture() {
        SettableListenableFuture<String> future = new SettableListenableFuture<>();
        future.set("msg-id");
        return future;
    }

    private ListenableFuture<String> failedFuture(String errorMessage) {
        SettableListenableFuture<String> future = new SettableListenableFuture<>();
        future.setException(new RuntimeException(errorMessage));
        return future;
    }

    private OutboxEvent createTestEvent(String topic, String payload, int retryCount) {
        OutboxEvent event = new OutboxEvent(topic, payload);
        event.setRetryCount(retryCount);
        return event;
    }

    @Test
    void publishPendingEvents_doesNothingWhenNoDueEvents() {
        when(outboxEventRepository.findDueEvents(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        outboxEventPublisher.publishPendingEvents();

        verify(outboxEventRepository).findDueEvents(any(LocalDateTime.class));
        verifyNoInteractions(tracingPubSubPublisher);
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void publishPendingEvents_publishesEventAndMarksAsProcessed() {
        OutboxEvent event = createTestEvent("test-topic", "test-payload", 0);
        when(outboxEventRepository.findDueEvents(any(LocalDateTime.class))).thenReturn(List.of(event));
        when(tracingPubSubPublisher.publishWithCorrelation(eq("test-topic"), eq("test-payload"), any()))
                .thenReturn(successFuture());

        outboxEventPublisher.publishPendingEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertEquals(OutboxEventStatus.PROCESSED.name(), saved.getStatus());
        assertNotNull(saved.getProcessedAt());
        verify(pubSubMetrics).recordOutboxPublished("test-topic", "success");
        verify(pubSubMetrics).recordOutboxLag(eq("test-topic"), anyLong());
    }

    @Test
    void publishPendingEvents_incrementsRetryCountAndSetsBackoffOnFailure() {
        OutboxEvent event = createTestEvent("test-topic", "test-payload", 1);
        LocalDateTime before = LocalDateTime.now();
        when(outboxEventRepository.findDueEvents(any(LocalDateTime.class))).thenReturn(List.of(event));
        when(tracingPubSubPublisher.publishWithCorrelation(eq("test-topic"), eq("test-payload"), any()))
                .thenReturn(failedFuture("publish failed"));

        outboxEventPublisher.publishPendingEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertEquals(2, saved.getRetryCount());
        assertEquals(OutboxEventStatus.PENDING.name(), saved.getStatus());

        // Backoff for retryCount=2 (after increment) = 5 * 2^2 = 20s.
        LocalDateTime expectedEarliest = before.plus(Duration.ofSeconds(20)).minusSeconds(2);
        assertNotNull(saved.getNextAttemptAt());
        assertTrue(saved.getNextAttemptAt().isAfter(expectedEarliest),
            "nextAttemptAt should reflect ~20s backoff, was " + saved.getNextAttemptAt());

        verify(pubSubMetrics).recordOutboxPublished("test-topic", "retrying");
        verify(pubSubMetrics).recordOutboxFailed(eq("test-topic"), anyString());
    }

    @Test
    void publishPendingEvents_marksAsFailedAfter5RetriesWithProcessedAt() {
        OutboxEvent event = createTestEvent("test-topic", "test-payload", 4);
        when(outboxEventRepository.findDueEvents(any(LocalDateTime.class))).thenReturn(List.of(event));
        when(tracingPubSubPublisher.publishWithCorrelation(eq("test-topic"), eq("test-payload"), any()))
                .thenReturn(failedFuture("publish failed"));

        outboxEventPublisher.publishPendingEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertEquals(5, saved.getRetryCount());
        assertEquals(OutboxEventStatus.FAILED.name(), saved.getStatus());
        assertNotNull(saved.getProcessedAt(),
            "processedAt must be set on FAILED so deleteOldFailedEvents can sweep it");
        verify(pubSubMetrics).recordOutboxPublished("test-topic", "exhausted");
        verify(pubSubMetrics).recordOutboxFailed("test-topic", "exhausted");
    }

    @Test
    void publishPendingEvents_passesCorrelationIdThrough() {
        OutboxEvent event = createTestEvent("test-topic", "test-payload", 0);
        event.setCorrelationId("00-0123456789abcdef0123456789abcdef-0123456789abcdef-01");
        when(outboxEventRepository.findDueEvents(any(LocalDateTime.class))).thenReturn(List.of(event));
        when(tracingPubSubPublisher.publishWithCorrelation(anyString(), anyString(), anyString()))
                .thenReturn(successFuture());

        outboxEventPublisher.publishPendingEvents();

        verify(tracingPubSubPublisher).publishWithCorrelation(
            "test-topic", "test-payload",
            "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01");
    }

    @Test
    void publishPendingEvents_doesNothingWhenPublisherIsNull() {
        OutboxEventPublisher publisherWithNoDeps = new OutboxEventPublisher();
        assertDoesNotThrow(publisherWithNoDeps::publishPendingEvents);
    }

    @Test
    void publishPendingEvents_doesNothingWhenRepositoryIsNull() {
        OutboxEventPublisher publisherWithNoDeps = new OutboxEventPublisher();
        assertDoesNotThrow(publisherWithNoDeps::publishPendingEvents);
    }

    @Test
    void cleanupOldEvents_callsDeleteOldProcessedEvents() {
        outboxEventPublisher.cleanupOldEvents();
        verify(outboxEventRepository).deleteOldProcessedEvents();
    }

    @Test
    void cleanupOldFailedEvents_callsDeleteOldFailedEvents() {
        outboxEventPublisher.cleanupOldFailedEvents();
        verify(outboxEventRepository).deleteOldFailedEvents();
    }

    @Test
    void cleanupOldEvents_doesNothingWhenRepositoryIsNull() {
        OutboxEventPublisher publisherWithNullRepo = new OutboxEventPublisher();
        assertDoesNotThrow(publisherWithNullRepo::cleanupOldEvents);
        assertDoesNotThrow(publisherWithNullRepo::cleanupOldFailedEvents);
    }

    @Test
    void publishPendingEvents_handlesMultipleEvents() {
        OutboxEvent event1 = createTestEvent("topic-1", "payload-1", 0);
        OutboxEvent event2 = createTestEvent("topic-2", "payload-2", 0);
        when(outboxEventRepository.findDueEvents(any(LocalDateTime.class)))
                .thenReturn(List.of(event1, event2));
        when(tracingPubSubPublisher.publishWithCorrelation(anyString(), anyString(), any()))
                .thenReturn(successFuture());

        outboxEventPublisher.publishPendingEvents();

        verify(tracingPubSubPublisher).publishWithCorrelation(eq("topic-1"), eq("payload-1"), any());
        verify(tracingPubSubPublisher).publishWithCorrelation(eq("topic-2"), eq("payload-2"), any());
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }

    @Test
    void publishPendingEvents_registersPendingGaugeOnFirstRun() {
        when(outboxEventRepository.findDueEvents(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        outboxEventPublisher.publishPendingEvents();
        outboxEventPublisher.publishPendingEvents();

        verify(pubSubMetrics, times(1)).registerOutboxPendingGauge(any());
    }
}
