package com.looksee.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.looksee.models.ProcessedMessage;
import com.looksee.models.repository.ProcessedMessageRepository;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private ProcessedMessageRepository processedMessageRepository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Test
    void isAlreadyProcessed_returnsFalseForNewMessage() {
        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName("msg-1", "svc-a"))
                .thenReturn(false);

        assertFalse(idempotencyService.isAlreadyProcessed("msg-1", "svc-a"));
    }

    @Test
    void isAlreadyProcessed_returnsTrueForAlreadyProcessedMessage() {
        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName("msg-2", "svc-b"))
                .thenReturn(true);

        assertTrue(idempotencyService.isAlreadyProcessed("msg-2", "svc-b"));
    }

    @Test
    void markProcessed_savesNewProcessedMessage() {
        when(processedMessageRepository.save(any(ProcessedMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        idempotencyService.markProcessed("msg-3", "svc-c");

        ArgumentCaptor<ProcessedMessage> captor = ArgumentCaptor.forClass(ProcessedMessage.class);
        verify(processedMessageRepository).save(captor.capture());

        ProcessedMessage saved = captor.getValue();
        assertEquals("msg-3", saved.getPubsubMessageId());
        assertEquals("svc-c", saved.getServiceName());
        assertEquals("PROCESSED", saved.getStatus());
        assertNotNull(saved.getProcessedAt());
    }

    @Test
    void isAlreadyProcessed_returnsFalseWhenRepositoryIsNull() {
        IdempotencyService serviceWithNullRepo = new IdempotencyService();
        // processedMessageRepository is null by default (not injected)
        assertFalse(serviceWithNullRepo.isAlreadyProcessed("msg-4", "svc-d"));
    }

    @Test
    void markProcessed_doesNothingWhenRepositoryIsNull() {
        IdempotencyService serviceWithNullRepo = new IdempotencyService();
        // Should not throw
        assertDoesNotThrow(() -> serviceWithNullRepo.markProcessed("msg-5", "svc-e"));
    }

    @Test
    void isAlreadyProcessed_returnsFalseForNullPubsubMessageId() {
        assertFalse(idempotencyService.isAlreadyProcessed(null, "svc-f"));
        verifyNoInteractions(processedMessageRepository);
    }

    @Test
    void isAlreadyProcessed_returnsFalseForEmptyPubsubMessageId() {
        assertFalse(idempotencyService.isAlreadyProcessed("", "svc-g"));
        verifyNoInteractions(processedMessageRepository);
    }

    @Test
    void markProcessed_handlesSaveExceptionGracefully() {
        when(processedMessageRepository.save(any(ProcessedMessage.class)))
                .thenThrow(new RuntimeException("DB connection failed"));

        assertDoesNotThrow(() -> idempotencyService.markProcessed("msg-6", "svc-h"));
        verify(processedMessageRepository).save(any(ProcessedMessage.class));
    }

    @Test
    void cleanupOldRecords_callsRepositoryDeleteOlderThan3() {
        idempotencyService.cleanupOldRecords();
        verify(processedMessageRepository).deleteOlderThan(3);
    }

    @Test
    void cleanupOldRecords_doesNothingWhenRepositoryIsNull() {
        IdempotencyService serviceWithNullRepo = new IdempotencyService();
        assertDoesNotThrow(serviceWithNullRepo::cleanupOldRecords);
    }

    @Test
    void markProcessed_doesNothingForNullPubsubMessageId() {
        idempotencyService.markProcessed(null, "svc-i");
        verifyNoInteractions(processedMessageRepository);
    }

    @Test
    void markProcessed_doesNothingForEmptyPubsubMessageId() {
        idempotencyService.markProcessed("", "svc-j");
        verifyNoInteractions(processedMessageRepository);
    }

    @Test
    void claim_returnsTrueOnFirstCall() {
        when(processedMessageRepository.claim("msg-claim-1", "svc-k")).thenReturn(true);

        assertTrue(idempotencyService.claim("msg-claim-1", "svc-k"));
        verify(processedMessageRepository).claim("msg-claim-1", "svc-k");
    }

    @Test
    void claim_returnsFalseOnDuplicate() {
        when(processedMessageRepository.claim("msg-claim-2", "svc-l")).thenReturn(false);

        assertFalse(idempotencyService.claim("msg-claim-2", "svc-l"));
        verify(processedMessageRepository).claim("msg-claim-2", "svc-l");
    }

    @Test
    void claim_returnsTrueWhenRepositoryIsNull() {
        IdempotencyService serviceWithNullRepo = new IdempotencyService();
        // Fail-open: cannot dedupe → caller must process
        assertTrue(serviceWithNullRepo.claim("msg-claim-3", "svc-m"));
    }

    @Test
    void claim_returnsTrueForNullPubsubMessageId() {
        assertTrue(idempotencyService.claim(null, "svc-n"));
        verifyNoInteractions(processedMessageRepository);
    }

    @Test
    void claim_returnsTrueForEmptyPubsubMessageId() {
        assertTrue(idempotencyService.claim("", "svc-o"));
        verifyNoInteractions(processedMessageRepository);
    }

    @Test
    void claim_failsOpenOnRepositoryException() {
        when(processedMessageRepository.claim(eq("msg-claim-4"), eq("svc-p")))
                .thenThrow(new RuntimeException("Neo4j unreachable"));

        // Fail-open: at-least-once is preferable to silently dropping the message
        assertTrue(idempotencyService.claim("msg-claim-4", "svc-p"));
    }

    @Test
    void release_callsRepositoryDelete() {
        idempotencyService.release("msg-rel-1", "svc-q");

        verify(processedMessageRepository).release("msg-rel-1", "svc-q");
    }

    @Test
    void release_doesNothingWhenRepositoryIsNull() {
        IdempotencyService serviceWithNullRepo = new IdempotencyService();
        assertDoesNotThrow(() -> serviceWithNullRepo.release("msg-rel-2", "svc-r"));
    }

    @Test
    void release_doesNothingForNullPubsubMessageId() {
        idempotencyService.release(null, "svc-s");
        verifyNoInteractions(processedMessageRepository);
    }

    @Test
    void release_doesNothingForEmptyPubsubMessageId() {
        idempotencyService.release("", "svc-t");
        verifyNoInteractions(processedMessageRepository);
    }

    @Test
    void release_swallowsRepositoryException() {
        doThrow(new RuntimeException("Neo4j unreachable"))
                .when(processedMessageRepository).release("msg-rel-3", "svc-u");

        // Best-effort: a stuck claim is preferable to masking the original handler error.
        assertDoesNotThrow(() -> idempotencyService.release("msg-rel-3", "svc-u"));
    }
}
