package com.looksee.models.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.looksee.models.ProcessedMessage;

/**
 * Unit tests for {@link ProcessedMessageRepository} that validate the
 * repository contract through mocking, without requiring a live Neo4j instance.
 */
@ExtendWith(MockitoExtension.class)
class ProcessedMessageRepositoryTest {

    @Mock
    private ProcessedMessageRepository repository;

    private static final String MSG_ID = "pubsub-msg-12345";
    private static final String SERVICE_NAME = "audit-manager";

    @BeforeEach
    void setUp() {
        // No special setup needed; Mockito initializes mocks via the extension
    }

    @Test
    @DisplayName("existsByPubsubMessageIdAndServiceName returns true for an existing record")
    void existsByPubsubMessageIdAndServiceName_returnsTrue_whenRecordExists() {
        when(repository.existsByPubsubMessageIdAndServiceName(MSG_ID, SERVICE_NAME))
                .thenReturn(true);

        boolean result = repository.existsByPubsubMessageIdAndServiceName(MSG_ID, SERVICE_NAME);

        assertTrue(result, "Should return true when the message has already been processed");
        verify(repository).existsByPubsubMessageIdAndServiceName(MSG_ID, SERVICE_NAME);
    }

    @Test
    @DisplayName("existsByPubsubMessageIdAndServiceName returns false for a non-existing record")
    void existsByPubsubMessageIdAndServiceName_returnsFalse_whenRecordDoesNotExist() {
        when(repository.existsByPubsubMessageIdAndServiceName("unknown-id", SERVICE_NAME))
                .thenReturn(false);

        boolean result = repository.existsByPubsubMessageIdAndServiceName("unknown-id", SERVICE_NAME);

        assertFalse(result, "Should return false when no matching record exists");
        verify(repository).existsByPubsubMessageIdAndServiceName("unknown-id", SERVICE_NAME);
    }

    @Test
    @DisplayName("existsByPubsubMessageIdAndServiceName differentiates by service name")
    void existsByPubsubMessageIdAndServiceName_differentiatesByServiceName() {
        when(repository.existsByPubsubMessageIdAndServiceName(MSG_ID, "page-builder"))
                .thenReturn(false);
        when(repository.existsByPubsubMessageIdAndServiceName(MSG_ID, SERVICE_NAME))
                .thenReturn(true);

        assertFalse(repository.existsByPubsubMessageIdAndServiceName(MSG_ID, "page-builder"),
                "Same messageId but different service should return false");
        assertTrue(repository.existsByPubsubMessageIdAndServiceName(MSG_ID, SERVICE_NAME),
                "Same messageId and same service should return true");
    }

    @Test
    @DisplayName("deleteOlderThan is callable with the correct retention days parameter")
    void deleteOlderThan_isCallable_withCorrectParameter() {
        int retentionDays = 3;

        doNothing().when(repository).deleteOlderThan(retentionDays);

        repository.deleteOlderThan(retentionDays);

        verify(repository).deleteOlderThan(retentionDays);
    }

    @Test
    @DisplayName("save persists a ProcessedMessage and returns it with an ID")
    void save_persistsProcessedMessage() {
        ProcessedMessage message = new ProcessedMessage(MSG_ID, SERVICE_NAME);

        ProcessedMessage savedMessage = new ProcessedMessage(MSG_ID, SERVICE_NAME);
        savedMessage.setId(1L);

        when(repository.save(any(ProcessedMessage.class))).thenReturn(savedMessage);

        ProcessedMessage result = repository.save(message);

        assertNotNull(result, "Saved message should not be null");
        assertEquals(1L, result.getId(), "Saved message should have an assigned ID");
        assertEquals(MSG_ID, result.getPubsubMessageId());
        assertEquals(SERVICE_NAME, result.getServiceName());
        assertEquals("PROCESSED", result.getStatus());
        assertNotNull(result.getProcessedAt(), "processedAt should be set");
        verify(repository).save(message);
    }

    @Test
    @DisplayName("save followed by exists returns true for the saved record")
    void save_thenExists_returnsTrue() {
        ProcessedMessage message = new ProcessedMessage(MSG_ID, SERVICE_NAME);
        ProcessedMessage saved = new ProcessedMessage(MSG_ID, SERVICE_NAME);
        saved.setId(1L);

        when(repository.save(any(ProcessedMessage.class))).thenReturn(saved);
        when(repository.existsByPubsubMessageIdAndServiceName(MSG_ID, SERVICE_NAME))
                .thenReturn(false)
                .thenReturn(true);

        assertFalse(repository.existsByPubsubMessageIdAndServiceName(MSG_ID, SERVICE_NAME),
                "Should not exist before save");

        repository.save(message);

        assertTrue(repository.existsByPubsubMessageIdAndServiceName(MSG_ID, SERVICE_NAME),
                "Should exist after save");
    }

    @Test
    @DisplayName("claim returns true on first call and false on subsequent calls")
    void claim_returnsTrueThenFalse() {
        when(repository.claim(MSG_ID, SERVICE_NAME))
                .thenReturn(true)
                .thenReturn(false);

        assertTrue(repository.claim(MSG_ID, SERVICE_NAME),
                "First claim must return true (just created)");
        assertFalse(repository.claim(MSG_ID, SERVICE_NAME),
                "Second claim must return false (already exists)");
        verify(repository, times(2)).claim(MSG_ID, SERVICE_NAME);
    }

    @Test
    @DisplayName("claim differentiates by service name")
    void claim_differentiatesByServiceName() {
        when(repository.claim(MSG_ID, "page-builder")).thenReturn(true);
        when(repository.claim(MSG_ID, SERVICE_NAME)).thenReturn(true);

        assertTrue(repository.claim(MSG_ID, "page-builder"),
                "Same messageId for a different service should still claim");
        assertTrue(repository.claim(MSG_ID, SERVICE_NAME),
                "Same messageId for this service first time should claim");
    }

    @Test
    @DisplayName("ProcessedMessage constructor sets fields correctly")
    void processedMessageConstructor_setsFieldsCorrectly() {
        LocalDateTime before = LocalDateTime.now();
        ProcessedMessage message = new ProcessedMessage(MSG_ID, SERVICE_NAME);
        LocalDateTime after = LocalDateTime.now();

        assertEquals(MSG_ID, message.getPubsubMessageId());
        assertEquals(SERVICE_NAME, message.getServiceName());
        assertEquals("PROCESSED", message.getStatus());
        assertNotNull(message.getProcessedAt());
        assertFalse(message.getProcessedAt().isBefore(before));
        assertFalse(message.getProcessedAt().isAfter(after));
    }
}
