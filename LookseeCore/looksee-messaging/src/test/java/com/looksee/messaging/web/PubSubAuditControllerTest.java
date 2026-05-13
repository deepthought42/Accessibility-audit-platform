package com.looksee.messaging.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.mapper.Body;
import com.looksee.messaging.idempotency.IdempotencyGuard;
import com.looksee.messaging.observability.PubSubMetrics;
import com.looksee.messaging.poison.PoisonMessagePublisher;
import com.looksee.models.message.PoisonMessageEnvelope;

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
    private PoisonMessagePublisher poisonPublisher;

    @BeforeEach
    void setUp() {
        lastHandled = null;
        handleFailure = null;
        handleInvocations = new AtomicInteger();

        controller = new TestController();
        idempotencyService = mock(IdempotencyGuard.class);
        pubSubMetrics = mock(PubSubMetrics.class);
        poisonPublisher = mock(PoisonMessagePublisher.class);

        ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(controller, "pubSubMetrics", pubSubMetrics);
        ReflectionTestUtils.setField(controller, "poisonPublisher", poisonPublisher);
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
    void transientFailure_returnsServerError_recordsError_andReleasesClaim() {
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
        // Eager claim must be released so Pub/Sub redelivery can retry handle().
        verify(idempotencyService, times(1)).release("msg-3", SERVICE);
    }

    @Test
    void firstDelivery_doesNotReleaseClaim() {
        when(idempotencyService.claim("msg-4", SERVICE)).thenReturn(true);

        controller.receiveMessage(buildBody("msg-4", "{\"value\":\"ok\"}"));

        verify(idempotencyService, never()).release(anyString(), anyString());
    }

    @Test
    void duplicateDelivery_doesNotReleaseClaim() {
        when(idempotencyService.claim("msg-5", SERVICE)).thenReturn(false);

        controller.receiveMessage(buildBody("msg-5", "{\"value\":\"ignored\"}"));

        verify(idempotencyService, never()).release(anyString(), anyString());
    }

    @Test
    void invalidBase64Payload_publishesPoison_andDoesNotReleaseClaim() {
        when(idempotencyService.claim("msg-6", SERVICE)).thenReturn(true);

        Body body = new Body();
        Body.Message message = body.new Message("msg-6", "2026-05-06T00:00:00Z", "!!!not-base64!!!");
        body.setMessage(message);

        ResponseEntity<String> response = controller.receiveMessage(body);

        // Poison: 200 so Pub/Sub stops redelivering. Claim is NOT released
        // (no point retrying a payload that will never decode).
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid payload"));
        assertEquals(0, handleInvocations.get());
        verify(pubSubMetrics, times(1)).recordError(eq(SERVICE), eq(TOPIC), any(Throwable.class));
        verify(pubSubMetrics, never()).recordSuccess(anyString(), anyString());
        verify(idempotencyService, never()).release(anyString(), anyString());

        ArgumentCaptor<PoisonMessageEnvelope> captor = ArgumentCaptor.forClass(PoisonMessageEnvelope.class);
        verify(poisonPublisher, times(1)).publishPoison(captor.capture(), anyString());
        PoisonMessageEnvelope envelope = captor.getValue();
        assertEquals(SERVICE, envelope.getServiceName());
        assertEquals(TOPIC, envelope.getTopic());
        assertEquals("msg-6", envelope.getOriginalMessageId());
        assertEquals("IllegalArgumentException", envelope.getErrorClass());
        assertEquals("!!!not-base64!!!", envelope.getBase64Data());
        assertNotNull(envelope.getTimestamp());
    }

    @Test
    void invalidJsonPayload_publishesPoison_andDoesNotReleaseClaim() {
        when(idempotencyService.claim("msg-7", SERVICE)).thenReturn(true);

        // Valid base64 but the bytes don't parse as TestPayload JSON.
        ResponseEntity<String> response = controller.receiveMessage(
            buildBody("msg-7", "this is not json at all"));

        // Same poison contract as base64: 200 + no release.
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid payload"));
        assertEquals(0, handleInvocations.get());
        verify(pubSubMetrics, times(1)).recordError(eq(SERVICE), eq(TOPIC), any(Throwable.class));
        verify(pubSubMetrics, never()).recordSuccess(anyString(), anyString());
        verify(idempotencyService, never()).release(anyString(), anyString());

        ArgumentCaptor<PoisonMessageEnvelope> captor = ArgumentCaptor.forClass(PoisonMessageEnvelope.class);
        verify(poisonPublisher, times(1)).publishPoison(captor.capture(), anyString());
        PoisonMessageEnvelope envelope = captor.getValue();
        assertEquals("msg-7", envelope.getOriginalMessageId());
        // The Jackson exception subclass name is what gets captured here
        // (e.g. UnrecognizedTokenException) — assert via prefix to stay
        // robust against minor Jackson version churn.
        assertTrue(envelope.getErrorClass().endsWith("Exception"),
            "expected an Exception subtype, got " + envelope.getErrorClass());
        assertNotNull(envelope.getBase64Data());
    }

    @Test
    void emptyEnvelope_publishesPoison_andSkipsClaim() {
        Body body = new Body();
        Body.Message message = body.new Message("msg-8", "2026-05-06T00:00:00Z", "");
        body.setMessage(message);

        ResponseEntity<String> response = controller.receiveMessage(body);

        // Acknowledge with 200 so Pub/Sub stops redelivering an envelope
        // we can never act on. recordInvalid is called; claim is never
        // attempted because there's no payload to dedupe.
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid pubsub payload"));
        verify(pubSubMetrics, times(1)).recordInvalid(SERVICE, TOPIC);
        verify(idempotencyService, never()).claim(anyString(), anyString());
        verify(idempotencyService, never()).release(anyString(), anyString());

        ArgumentCaptor<PoisonMessageEnvelope> captor = ArgumentCaptor.forClass(PoisonMessageEnvelope.class);
        verify(poisonPublisher, times(1)).publishPoison(captor.capture(), anyString());
        PoisonMessageEnvelope envelope = captor.getValue();
        assertEquals("EmptyEnvelope", envelope.getErrorClass());
        assertEquals("msg-8", envelope.getOriginalMessageId());
    }

    @Test
    void nullBody_publishesPoison_andSkipsClaim() {
        ResponseEntity<String> response = controller.receiveMessage(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid pubsub payload"));
        verify(pubSubMetrics, times(1)).recordInvalid(SERVICE, TOPIC);
        verify(idempotencyService, never()).claim(anyString(), anyString());

        ArgumentCaptor<PoisonMessageEnvelope> captor = ArgumentCaptor.forClass(PoisonMessageEnvelope.class);
        verify(poisonPublisher, times(1)).publishPoison(captor.capture(), anyString());
        PoisonMessageEnvelope envelope = captor.getValue();
        assertEquals("EmptyEnvelope", envelope.getErrorClass());
        // No body, no message id to preserve.
        assertEquals(null, envelope.getOriginalMessageId());
    }

    @Test
    void transientFailure_doesNotPublishPoison() {
        when(idempotencyService.claim("msg-9", SERVICE)).thenReturn(true);
        handleFailure = new RuntimeException("downstream blew up");

        ResponseEntity<String> response = controller.receiveMessage(
            buildBody("msg-9", "{\"value\":\"boom\"}"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(poisonPublisher, never()).publishPoison(any(), anyString());
    }

    @Test
    void poisonPublishItselfFails_returns500_releasesClaim() {
        when(idempotencyService.claim("msg-10", SERVICE)).thenReturn(true);
        doThrow(new RuntimeException("outbox down"))
            .when(poisonPublisher).publishPoison(any(), anyString());

        ResponseEntity<String> response = controller.receiveMessage(
            buildBody("msg-10", "this is not json at all"));

        // Poison-publish failure must not silently ack — escalate to 500
        // so Pub/Sub redelivers and a later attempt can capture the
        // poison row. Claim must be released so the redelivery is not
        // short-circuited as a duplicate.
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("poison publish failed"));
        verify(idempotencyService, times(1)).release("msg-10", SERVICE);
        // The original deserialization error AND the poison-publish error
        // are both recorded — operators see two error events for one
        // failed delivery.
        verify(pubSubMetrics, times(2)).recordError(eq(SERVICE), eq(TOPIC), any(Throwable.class));
    }

    @Test
    void emptyEnvelopePoisonPublishFails_returns500() {
        doThrow(new RuntimeException("outbox down"))
            .when(poisonPublisher).publishPoison(any(), anyString());

        Body body = new Body();
        Body.Message message = body.new Message("msg-11", "2026-05-06T00:00:00Z", "");
        body.setMessage(message);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("poison publish failed"));
        // No claim was made on this path so no release is expected.
        verify(idempotencyService, never()).release(anyString(), anyString());
        verify(pubSubMetrics, times(1)).recordError(eq(SERVICE), eq(TOPIC), any(Throwable.class));
    }

    @Test
    void poisonPublisherNotWired_existingBehaviorPreserved() {
        // Services without looksee-persistence on their classpath leave
        // the publisher field null. The base controller must continue to
        // return 200 + emit metrics rather than NPE.
        ReflectionTestUtils.setField(controller, "poisonPublisher", null);
        when(idempotencyService.claim("msg-12", SERVICE)).thenReturn(true);

        ResponseEntity<String> base64Response = controller.receiveMessage(
            buildBodyRaw("msg-12", "!!!not-base64!!!"));
        assertEquals(HttpStatus.OK, base64Response.getStatusCode());

        when(idempotencyService.claim("msg-13", SERVICE)).thenReturn(true);
        ResponseEntity<String> jsonResponse = controller.receiveMessage(
            buildBody("msg-13", "this is not json at all"));
        assertEquals(HttpStatus.OK, jsonResponse.getStatusCode());

        Body empty = new Body();
        empty.setMessage(empty.new Message("msg-14", "2026-05-06T00:00:00Z", ""));
        ResponseEntity<String> emptyResponse = controller.receiveMessage(empty);
        assertEquals(HttpStatus.OK, emptyResponse.getStatusCode());

        verify(poisonPublisher, never()).publishPoison(any(), anyString());
    }

    private static Body buildBody(String messageId, String json) {
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        Body body = new Body();
        Body.Message message = body.new Message(messageId, "2026-05-06T00:00:00Z", encoded);
        body.setMessage(message);
        return body;
    }

    /**
     * Builds a Body whose {@code data} is set to the raw string verbatim
     * (not Base64-encoded). Used to drive the bad-Base64 poison branch
     * with a value the decoder will reject.
     */
    private static Body buildBodyRaw(String messageId, String rawData) {
        Body body = new Body();
        Body.Message message = body.new Message(messageId, "2026-05-06T00:00:00Z", rawData);
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
