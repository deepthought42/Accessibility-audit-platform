package com.looksee.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.looksee.models.ProcessedMessage;
import com.looksee.models.repository.ProcessedMessageRepository;

/**
 * Integration tests for {@link IdempotencyService} validating the full
 * idempotency lifecycle, concurrency behavior, and cleanup. A missing
 * repository is now a fail-fast condition at startup (covered in
 * {@link IdempotencyServiceTest}), not a graceful-degradation path.
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyIntegrationTest {

    @Mock
    private ProcessedMessageRepository processedMessageRepository;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService();
        injectRepository(processedMessageRepository);
    }

    /**
     * Injects the mock repository into the service's @Autowired field via reflection.
     */
    private void injectRepository(ProcessedMessageRepository repo) {
        try {
            java.lang.reflect.Field field = IdempotencyService.class.getDeclaredField("processedMessageRepository");
            field.setAccessible(true);
            field.set(idempotencyService, repo);
        } catch (Exception e) {
            fail("Failed to inject repository: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Full lifecycle: check (false) -> process -> mark -> check (true)")
    void testFullLifecycle() {
        String msgId = "lifecycle-msg-001";
        String service = "page-builder";

        // Initially not processed
        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName(msgId, service))
                .thenReturn(false);
        assertFalse(idempotencyService.isAlreadyProcessed(msgId, service));

        // Mark as processed
        when(processedMessageRepository.save(any(ProcessedMessage.class)))
                .thenAnswer(invocation -> {
                    ProcessedMessage pm = invocation.getArgument(0);
                    pm.setId(1L);
                    return pm;
                });
        idempotencyService.markProcessed(msgId, service);

        // Now it should be flagged as processed
        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName(msgId, service))
                .thenReturn(true);
        assertTrue(idempotencyService.isAlreadyProcessed(msgId, service));

        // Verify interactions
        verify(processedMessageRepository, times(2))
                .existsByPubsubMessageIdAndServiceName(msgId, service);
        verify(processedMessageRepository).save(any(ProcessedMessage.class));
    }

    @Test
    @DisplayName("Concurrent access: two threads check simultaneously")
    void testConcurrentAccess() throws InterruptedException {
        String msgId = "concurrent-msg-001";
        String service = "audit-manager";

        // Both threads will see the message as not yet processed initially
        AtomicInteger checkCount = new AtomicInteger(0);
        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName(msgId, service))
                .thenAnswer(invocation -> {
                    // First two checks return false, subsequent checks return true
                    return checkCount.getAndIncrement() >= 2;
                });
        when(processedMessageRepository.save(any(ProcessedMessage.class)))
                .thenAnswer(invocation -> {
                    ProcessedMessage pm = invocation.getArgument(0);
                    pm.setId(1L);
                    return pm;
                });

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger processedCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (!idempotencyService.isAlreadyProcessed(msgId, service)) {
                        processedCount.incrementAndGet();
                        idempotencyService.markProcessed(msgId, service);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Both threads should complete within timeout");
        executor.shutdown();

        // Both threads saw false initially, so both attempted processing
        assertEquals(2, processedCount.get(),
                "Both threads should see the message as unprocessed in this race condition scenario");
        // After both are done, subsequent checks should return true
        assertTrue(idempotencyService.isAlreadyProcessed(msgId, service));
    }

    @Test
    @DisplayName("Cleanup removes old records via deleteOlderThan")
    void testCleanupRemovesOldRecords() {
        doNothing().when(processedMessageRepository).deleteOlderThan(3);

        idempotencyService.cleanupOldRecords();

        verify(processedMessageRepository).deleteOlderThan(3);
    }

    @Test
    @DisplayName("Cleanup handles repository exception gracefully")
    void testCleanupHandlesException() {
        doThrow(new RuntimeException("DB connection failed"))
                .when(processedMessageRepository).deleteOlderThan(3);

        // Should not throw
        assertDoesNotThrow(() -> idempotencyService.cleanupOldRecords());
        verify(processedMessageRepository).deleteOlderThan(3);
    }

    @Test
    @DisplayName("Startup fails fast when repository is null")
    void testStartupFailsFast_repositoryNull() {
        IdempotencyService bare = new IdempotencyService();
        IllegalStateException ex = assertThrows(IllegalStateException.class, bare::requireRepository);
        assertTrue(ex.getMessage().contains("ProcessedMessageRepository"),
                "message should name the missing dependency, was: " + ex.getMessage());
    }

    @Test
    @DisplayName("Graceful degradation when repository throws on save")
    void testGracefulDegradation_repositoryThrowsOnSave() {
        String msgId = "failing-save-msg";
        String service = "content-audit";

        when(processedMessageRepository.save(any(ProcessedMessage.class)))
                .thenThrow(new RuntimeException("Neo4j connection refused"));

        // markProcessed should not propagate the exception
        assertDoesNotThrow(() -> idempotencyService.markProcessed(msgId, service),
                "markProcessed should handle repository exceptions gracefully");
    }

    @Test
    @DisplayName("isAlreadyProcessed returns false for null or empty messageId")
    void testNullOrEmptyMessageId() {
        assertFalse(idempotencyService.isAlreadyProcessed(null, "service"),
                "Null messageId should return false");
        assertFalse(idempotencyService.isAlreadyProcessed("", "service"),
                "Empty messageId should return false");
    }

    @Test
    @DisplayName("markProcessed does nothing for null or empty messageId")
    void testMarkProcessed_nullOrEmptyMessageId() {
        assertDoesNotThrow(() -> idempotencyService.markProcessed(null, "service"));
        assertDoesNotThrow(() -> idempotencyService.markProcessed("", "service"));

        // Repository save should never have been called
        verify(processedMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Different services can independently track the same message")
    void testDifferentServicesTrackIndependently() {
        String msgId = "shared-msg-001";

        // page-builder has processed it
        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName(msgId, "page-builder"))
                .thenReturn(true);
        // content-audit has not
        when(processedMessageRepository.existsByPubsubMessageIdAndServiceName(msgId, "content-audit"))
                .thenReturn(false);

        assertTrue(idempotencyService.isAlreadyProcessed(msgId, "page-builder"));
        assertFalse(idempotencyService.isAlreadyProcessed(msgId, "content-audit"));
    }
}
