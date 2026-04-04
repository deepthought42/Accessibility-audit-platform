package com.looksee.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.looksee.models.OutboxEvent;
import com.looksee.models.repository.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private PubSubTemplate pubSubTemplate;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    private OutboxEvent createTestEvent(String topic, String payload, int retryCount) {
        OutboxEvent event = new OutboxEvent(topic, payload);
        event.setRetryCount(retryCount);
        return event;
    }

    @Test
    void publishPendingEvents_doesNothingWhenNoPendingEvents() {
        when(outboxEventRepository.findRetryableEvents()).thenReturn(Collections.emptyList());

        outboxEventPublisher.publishPendingEvents();

        verify(outboxEventRepository).findRetryableEvents();
        verifyNoInteractions(pubSubTemplate);
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void publishPendingEvents_publishesEventAndMarksAsProcessed() {
        OutboxEvent event = createTestEvent("test-topic", "test-payload", 0);
        when(outboxEventRepository.findRetryableEvents()).thenReturn(List.of(event));
        when(pubSubTemplate.publish(eq("test-topic"), eq("test-payload")))
                .thenReturn(CompletableFuture.completedFuture("msg-id"));

        outboxEventPublisher.publishPendingEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertEquals("PROCESSED", saved.getStatus());
        assertNotNull(saved.getProcessedAt());
    }

    @Test
    void publishPendingEvents_incrementsRetryCountOnFailure() {
        OutboxEvent event = createTestEvent("test-topic", "test-payload", 1);
        when(outboxEventRepository.findRetryableEvents()).thenReturn(List.of(event));

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("publish failed"));
        when(pubSubTemplate.publish(eq("test-topic"), eq("test-payload")))
                .thenReturn(failedFuture);

        outboxEventPublisher.publishPendingEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertEquals(2, saved.getRetryCount());
        assertEquals("PENDING", saved.getStatus());
    }

    @Test
    void publishPendingEvents_marksAsFailedAfter5Retries() {
        OutboxEvent event = createTestEvent("test-topic", "test-payload", 4);
        when(outboxEventRepository.findRetryableEvents()).thenReturn(List.of(event));

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("publish failed"));
        when(pubSubTemplate.publish(eq("test-topic"), eq("test-payload")))
                .thenReturn(failedFuture);

        outboxEventPublisher.publishPendingEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertEquals(5, saved.getRetryCount());
        assertEquals("FAILED", saved.getStatus());
    }

    @Test
    void publishPendingEvents_doesNothingWhenPubSubTemplateIsNull() {
        OutboxEventPublisher publisherWithNullTemplate = new OutboxEventPublisher();
        assertDoesNotThrow(publisherWithNullTemplate::publishPendingEvents);
    }

    @Test
    void publishPendingEvents_doesNothingWhenRepositoryIsNull() {
        OutboxEventPublisher publisherWithNullRepo = new OutboxEventPublisher();
        assertDoesNotThrow(publisherWithNullRepo::publishPendingEvents);
    }

    @Test
    void cleanupOldEvents_callsDeleteOldProcessedEvents() {
        outboxEventPublisher.cleanupOldEvents();
        verify(outboxEventRepository).deleteOldProcessedEvents();
    }

    @Test
    void cleanupOldEvents_doesNothingWhenRepositoryIsNull() {
        OutboxEventPublisher publisherWithNullRepo = new OutboxEventPublisher();
        assertDoesNotThrow(publisherWithNullRepo::cleanupOldEvents);
    }

    @Test
    void publishPendingEvents_handlesMultipleEvents() {
        OutboxEvent event1 = createTestEvent("topic-1", "payload-1", 0);
        OutboxEvent event2 = createTestEvent("topic-2", "payload-2", 0);
        when(outboxEventRepository.findRetryableEvents()).thenReturn(List.of(event1, event2));
        when(pubSubTemplate.publish(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("msg-id"));

        outboxEventPublisher.publishPendingEvents();

        verify(pubSubTemplate).publish(eq("topic-1"), eq("payload-1"));
        verify(pubSubTemplate).publish(eq("topic-2"), eq("payload-2"));
        verify(outboxEventRepository, times(2)).save(any(OutboxEvent.class));
    }
}
