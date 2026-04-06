package com.looksee.models;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OutboxEventTest {

    @Test
    void constructor_setsEventIdAsUuid() {
        OutboxEvent event = new OutboxEvent("topic-1", "{\"key\":\"value\"}");
        assertNotNull(event.getEventId());
        assertDoesNotThrow(() -> UUID.fromString(event.getEventId()));
    }

    @Test
    void constructor_setsTopic() {
        OutboxEvent event = new OutboxEvent("my-topic", "payload");
        assertEquals("my-topic", event.getTopic());
    }

    @Test
    void constructor_setsPayload() {
        OutboxEvent event = new OutboxEvent("topic", "my-payload-data");
        assertEquals("my-payload-data", event.getPayload());
    }

    @Test
    void constructor_setsStatusToPending() {
        OutboxEvent event = new OutboxEvent("topic", "payload");
        assertEquals("PENDING", event.getStatus());
    }

    @Test
    void constructor_setsCreatedAtToCurrentTime() {
        LocalDateTime before = LocalDateTime.now().minus(1, ChronoUnit.SECONDS);
        OutboxEvent event = new OutboxEvent("topic", "payload");
        LocalDateTime after = LocalDateTime.now().plus(1, ChronoUnit.SECONDS);

        assertNotNull(event.getCreatedAt());
        assertFalse(event.getCreatedAt().isBefore(before));
        assertFalse(event.getCreatedAt().isAfter(after));
    }

    @Test
    void constructor_setsRetryCountToZero() {
        OutboxEvent event = new OutboxEvent("topic", "payload");
        assertEquals(0, event.getRetryCount());
    }

    @Test
    void noArgConstructor_createsEmptyEntity() {
        OutboxEvent event = new OutboxEvent();
        assertNull(event.getId());
        assertNull(event.getEventId());
        assertNull(event.getTopic());
        assertNull(event.getPayload());
        assertNull(event.getStatus());
        assertNull(event.getCreatedAt());
        assertNull(event.getProcessedAt());
        assertEquals(0, event.getRetryCount());
    }

    @Test
    void gettersAndSetters_workCorrectly() {
        OutboxEvent event = new OutboxEvent();

        event.setEventId("custom-event-id");
        assertEquals("custom-event-id", event.getEventId());

        event.setTopic("new-topic");
        assertEquals("new-topic", event.getTopic());

        event.setPayload("new-payload");
        assertEquals("new-payload", event.getPayload());

        event.setStatus("PROCESSED");
        assertEquals("PROCESSED", event.getStatus());

        LocalDateTime now = LocalDateTime.now();
        event.setCreatedAt(now);
        assertEquals(now, event.getCreatedAt());

        LocalDateTime processed = LocalDateTime.now().plusMinutes(5);
        event.setProcessedAt(processed);
        assertEquals(processed, event.getProcessedAt());

        event.setRetryCount(3);
        assertEquals(3, event.getRetryCount());
    }

    @Test
    void eventId_isUniqueAcrossInstances() {
        OutboxEvent event1 = new OutboxEvent("topic", "payload");
        OutboxEvent event2 = new OutboxEvent("topic", "payload");
        assertNotEquals(event1.getEventId(), event2.getEventId());
    }
}
