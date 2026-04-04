package com.looksee.models;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

class ProcessedMessageTest {

    @Test
    void constructor_setsAllFieldsCorrectly() {
        LocalDateTime before = LocalDateTime.now().minus(1, ChronoUnit.SECONDS);
        ProcessedMessage msg = new ProcessedMessage("msg-123", "test-service");
        LocalDateTime after = LocalDateTime.now().plus(1, ChronoUnit.SECONDS);

        assertEquals("msg-123", msg.getPubsubMessageId());
        assertEquals("test-service", msg.getServiceName());
        assertEquals("PROCESSED", msg.getStatus());
        assertNotNull(msg.getProcessedAt());
        assertFalse(msg.getProcessedAt().isBefore(before));
        assertFalse(msg.getProcessedAt().isAfter(after));
    }

    @Test
    void constructor_defaultStatusIsProcessed() {
        ProcessedMessage msg = new ProcessedMessage("msg-456", "service-a");
        assertEquals("PROCESSED", msg.getStatus());
    }

    @Test
    void constructor_processedAtIsSetToCurrentTime() {
        LocalDateTime before = LocalDateTime.now();
        ProcessedMessage msg = new ProcessedMessage("msg-789", "service-b");
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(msg.getProcessedAt());
        assertFalse(msg.getProcessedAt().isBefore(before));
        assertFalse(msg.getProcessedAt().isAfter(after));
    }

    @Test
    void constructor_nullPubsubMessageId_throwsAssertionError() {
        assertThrows(AssertionError.class, () -> new ProcessedMessage(null, "service"));
    }

    @Test
    void constructor_emptyPubsubMessageId_throwsAssertionError() {
        assertThrows(AssertionError.class, () -> new ProcessedMessage("", "service"));
    }

    @Test
    void constructor_nullServiceName_throwsAssertionError() {
        assertThrows(AssertionError.class, () -> new ProcessedMessage("msg-id", null));
    }

    @Test
    void constructor_emptyServiceName_throwsAssertionError() {
        assertThrows(AssertionError.class, () -> new ProcessedMessage("msg-id", ""));
    }

    @Test
    void noArgConstructor_createsInstance() {
        ProcessedMessage msg = new ProcessedMessage();
        assertNull(msg.getPubsubMessageId());
        assertNull(msg.getServiceName());
        assertNull(msg.getStatus());
        assertNull(msg.getProcessedAt());
        assertNull(msg.getId());
    }

    @Test
    void gettersAndSetters_workCorrectly() {
        ProcessedMessage msg = new ProcessedMessage();

        msg.setPubsubMessageId("custom-id");
        assertEquals("custom-id", msg.getPubsubMessageId());

        msg.setServiceName("my-service");
        assertEquals("my-service", msg.getServiceName());

        msg.setStatus("FAILED");
        assertEquals("FAILED", msg.getStatus());

        LocalDateTime time = LocalDateTime.of(2025, 6, 1, 12, 0);
        msg.setProcessedAt(time);
        assertEquals(time, msg.getProcessedAt());
    }
}
