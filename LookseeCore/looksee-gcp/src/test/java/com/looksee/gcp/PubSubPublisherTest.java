package com.looksee.gcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;

@ExtendWith(MockitoExtension.class)
class PubSubPublisherTest {

    private static final String TEST_TOPIC = "test-topic";

    @Mock
    private PubSubTemplate pubSubTemplate;

    private PubSubPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new PubSubPublisher() {
            @Override
            protected String topic() {
                return TEST_TOPIC;
            }
        };
        ReflectionTestUtils.setField(publisher, "pubSubTemplate", pubSubTemplate);
    }

    private ListenableFuture<String> successFuture() {
        SettableListenableFuture<String> future = new SettableListenableFuture<>();
        future.set("message-id");
        return future;
    }

    private ListenableFuture<String> failedFuture(String errorMessage) {
        SettableListenableFuture<String> future = new SettableListenableFuture<>();
        future.setException(new RuntimeException(errorMessage));
        return future;
    }

    @Test
    void publish_successOnFirstAttempt() throws Exception {
        when(pubSubTemplate.publish(eq(TEST_TOPIC), anyString())).thenReturn(successFuture());

        assertDoesNotThrow(() -> publisher.publish("test message"));
        verify(pubSubTemplate, times(1)).publish(eq(TEST_TOPIC), eq("test message"));
    }

    @Test
    void publish_retriesOnFirstFailure_succeedsOnSecond() throws Exception {
        when(pubSubTemplate.publish(eq(TEST_TOPIC), anyString()))
                .thenReturn(failedFuture("first failure"))
                .thenReturn(successFuture());

        assertDoesNotThrow(() -> publisher.publish("test message"));
        verify(pubSubTemplate, times(2)).publish(eq(TEST_TOPIC), eq("test message"));
    }

    @Test
    void publish_retriesOnTwoFailures_succeedsOnThird() throws Exception {
        when(pubSubTemplate.publish(eq(TEST_TOPIC), anyString()))
                .thenReturn(failedFuture("first failure"))
                .thenReturn(failedFuture("second failure"))
                .thenReturn(successFuture());

        assertDoesNotThrow(() -> publisher.publish("test message"));
        verify(pubSubTemplate, times(3)).publish(eq(TEST_TOPIC), eq("test message"));
    }

    @Test
    void publish_allRetriesFail_throwsExecutionException() throws Exception {
        when(pubSubTemplate.publish(eq(TEST_TOPIC), anyString()))
                .thenReturn(failedFuture("failure 1"))
                .thenReturn(failedFuture("failure 2"))
                .thenReturn(failedFuture("failure 3"));

        ExecutionException thrown = assertThrows(ExecutionException.class, () -> publisher.publish("test message"));
        assertNotNull(thrown);
        verify(pubSubTemplate, times(3)).publish(eq(TEST_TOPIC), eq("test message"));
    }

    @Test
    void publish_nullMessage_throwsAssertionError() {
        assertThrows(AssertionError.class, () -> publisher.publish(null));
        verifyNoInteractions(pubSubTemplate);
    }

    @Test
    void publish_emptyMessage_throwsAssertionError() {
        assertThrows(AssertionError.class, () -> publisher.publish(""));
        verifyNoInteractions(pubSubTemplate);
    }

    @Test
    void publish_interruptedException_propagates() throws Exception {
        when(pubSubTemplate.publish(eq(TEST_TOPIC), anyString()))
                .thenReturn(failedFuture("failure"));

        Thread.currentThread().interrupt();
        assertThrows(InterruptedException.class, () -> publisher.publish("test message"));
        Thread.interrupted();
    }

    @Test
    void publish_backoff_increasesWithAttempts() throws Exception {
        when(pubSubTemplate.publish(eq(TEST_TOPIC), anyString()))
                .thenReturn(failedFuture("failure 1"))
                .thenReturn(failedFuture("failure 2"))
                .thenReturn(failedFuture("failure 3"));

        long start = System.currentTimeMillis();
        assertThrows(ExecutionException.class, () -> publisher.publish("test message"));
        long elapsed = System.currentTimeMillis() - start;

        // BASE_BACKOFF_MS=1000, attempt 1 sleeps 1000ms, attempt 2 sleeps 2000ms
        assertTrue(elapsed >= 2500, "Expected at least 2500ms of backoff, but was " + elapsed + "ms");
    }
}
