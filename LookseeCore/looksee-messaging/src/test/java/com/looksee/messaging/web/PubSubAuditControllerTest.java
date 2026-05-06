package com.looksee.messaging.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.mapper.Body;
import com.looksee.messaging.idempotency.IdempotencyGuard;
import com.looksee.messaging.observability.PubSubMetrics;

/**
 * Base-class tests for the atomic-claim wiring introduced by issue #82
 * (umbrella #28). These cover the happy path, duplicate path, and
 * transient-error path that every consumer extending PubSubAuditController
 * inherits — without depending on any concrete service.
 */
class PubSubAuditControllerTest {

    private static final String SERVICE = "test-svc";
    private static final String TOPIC = "test-topic";

    private TestPayload lastHandled;
    private RuntimeException handleFailure;
    private AtomicInteger handleInvocations;

    private TestController controller;
    private IdempotencyGuard idempotencyService;
    private PubSubMetrics pubSubMetrics;

    @BeforeEach
    void setUp() {
        lastHandled = null;
        handleFailure = null;
        handleInvocations = new AtomicInteger();

        controller = new TestController();
        idempotencyService = mock(IdempotencyGuard.class);
        pubSubMetrics = mock(PubSubMetrics.class);

        ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(controller, "pubSubMetrics", pubSubMetrics);
    }

    @Test
    void firstDelivery_invokesHandle_andRecordsSuccess() {
        when(idempotencyService.claim("msg-1", SERVICE)).thenReturn(true);

        ResponseEntity<String> response = controller.receiveMessage(
            buildBody("msg-1", "{\"value\":\"hello\"}"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ok", response.getBody());
        assertEquals(1, handleInvocations.get());
        assertEquals("hello", lastHandled.getValue());
        verify(idempotencyService, times(1)).claim("msg-1", SERVICE);
        verify(pubSubMetrics, times(1)).recordSuccess(SERVICE, TOPIC);
        verify(pubSubMetrics, never()).recordDuplicate(anyString(), anyString());
        verify(pubSubMetrics, times(1)).recordDuration(eq(SERVICE), eq(TOPIC), anyLong());
    }

    @Test
    void duplicateDelivery_skipsHandle_andRecordsDuplicate() {
        when(idempotencyService.claim("msg-2", SERVICE)).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(
            buildBody("msg-2", "{\"value\":\"ignored\"}"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Duplicate"));
        assertEquals(0, handleInvocations.get());
        verify(idempotencyService, times(1)).claim("msg-2", SERVICE);
        verify(pubSubMetrics, times(1)).recordDuplicate(SERVICE, TOPIC);
        verify(pubSubMetrics, never()).recordSuccess(anyString(), anyString());
        verify(pubSubMetrics, never()).recordDuration(anyString(), anyString(), anyLong());
    }

    @Test
    void transientFailure_returnsServerError_andRecordsError() {
        when(idempotencyService.claim("msg-3", SERVICE)).thenReturn(true);
        handleFailure = new RuntimeException("downstream blew up");

        ResponseEntity<String> response = controller.receiveMessage(
            buildBody("msg-3", "{\"value\":\"boom\"}"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(1, handleInvocations.get());
        verify(idempotencyService, times(1)).claim("msg-3", SERVICE);
        verify(pubSubMetrics, never()).recordSuccess(anyString(), anyString());
        verify(pubSubMetrics, times(1)).recordError(eq(SERVICE), eq(TOPIC), any(Throwable.class));
        verify(pubSubMetrics, times(1)).recordDuration(eq(SERVICE), eq(TOPIC), anyLong());
    }

    private static Body buildBody(String messageId, String json) {
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        Body body = new Body();
        Body.Message message = body.new Message(messageId, "2026-05-06T00:00:00Z", encoded);
        body.setMessage(message);
        return body;
    }

    public static class TestPayload {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private class TestController extends PubSubAuditController<TestPayload> {
        @Override
        protected String serviceName() {
            return SERVICE;
        }

        @Override
        protected String topicName() {
            return TOPIC;
        }

        @Override
        protected Class<TestPayload> payloadType() {
            return TestPayload.class;
        }

        @Override
        protected void handle(TestPayload payload) {
            handleInvocations.incrementAndGet();
            lastHandled = payload;
            if (handleFailure != null) {
                throw handleFailure;
            }
        }
    }
}
