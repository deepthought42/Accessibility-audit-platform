package com.looksee.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.looksee.models.OutboxEvent;
import com.looksee.models.OutboxEventStatus;
import com.looksee.models.OutboxSerializationException;
import com.looksee.models.message.PageBuiltMessage;
import com.looksee.models.repository.OutboxEventRepository;

/**
 * Verifies the gateway's wiring: it accepts arbitrary payloads, serializes
 * them with the shared Jackson mapper, persists them through the repository,
 * and surfaces serialization failures as the unchecked
 * {@link OutboxSerializationException}.
 *
 * <p>Transactional-propagation behaviour (MANDATORY vs REQUIRES_NEW) is a
 * Spring runtime concern and is exercised in the Testcontainers-backed
 * integration tests in the downstream services; this class only covers the
 * gateway's local contract.</p>
 */
@ExtendWith(MockitoExtension.class)
class OutboxPublishingGatewayTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxPublishingGateway gateway;

    @Test
    void enqueue_serializesPojoAndPersistsEventWithCorrelationId() {
        PageBuiltMessage payload = new PageBuiltMessage(1L, 100L, 42L);

        gateway.enqueue("topic-a", payload,
            "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertEquals("topic-a", saved.getTopic());
        assertEquals("00-0123456789abcdef0123456789abcdef-0123456789abcdef-01",
            saved.getCorrelationId());
        assertEquals(OutboxEventStatus.PENDING.name(), saved.getStatus());
        assertNotNull(saved.getPayload());
        assertTrue(saved.getPayload().contains("\"accountId\""),
            "Jackson-serialized JSON should embed the PageBuiltMessage fields");
        assertEquals(saved.getCreatedAt(), saved.getNextAttemptAt(),
            "nextAttemptAt defaults to createdAt so the first scheduler tick picks the row up");
    }

    @Test
    void enqueue_acceptsPreSerializedStringPayloadVerbatim() {
        String json = "{\"already\":\"serialized\"}";

        gateway.enqueue("topic-b", json, null);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertEquals(json, captor.getValue().getPayload());
        assertNull(captor.getValue().getCorrelationId());
    }

    @Test
    void enqueueOutOfBand_persistsThroughTheSameRepositoryPath() {
        PageBuiltMessage payload = new PageBuiltMessage(2L, 200L, 99L);

        gateway.enqueueOutOfBand("error-topic", payload, null);

        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void enqueue_throwsOutboxSerializationExceptionOnUnserializablePayload() {
        Unserializable unserializable = new Unserializable();

        OutboxSerializationException ex = assertThrows(
            OutboxSerializationException.class,
            () -> gateway.enqueue("topic-c", unserializable, null));

        assertNotNull(ex.getCause(), "Underlying JsonProcessingException must be preserved as the cause");
        verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
    }

    @Test
    void enqueue_dropsPublishWhenRepositoryUnwired() {
        OutboxPublishingGateway barren = new OutboxPublishingGateway();
        assertDoesNotThrow(() -> barren.enqueue("topic-d", "{}", null));
    }

    /**
     * A type Jackson's default mapper cannot serialize: no public properties,
     * no annotations, no introspectable accessors. Triggers
     * {@code FAIL_ON_EMPTY_BEANS} which {@code JacksonConfig.mapper()} leaves
     * enabled.
     */
    private static final class Unserializable {
    }
}
