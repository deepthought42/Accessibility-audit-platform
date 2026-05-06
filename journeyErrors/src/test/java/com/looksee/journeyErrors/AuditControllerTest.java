package com.looksee.journeyErrors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.mapper.Body;
import com.looksee.messaging.idempotency.IdempotencyGuard;
import com.looksee.messaging.observability.PubSubMetrics;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.services.JourneyService;

/**
 * Per-service tests for journeyErrors after the migration to
 * {@link com.looksee.messaging.web.PubSubAuditController}. Envelope
 * validation, base64/JSON edge cases, idempotency claim/release, and metrics
 * emission are owned by the base class and covered in
 * {@code PubSubAuditControllerTest}; this file only exercises the
 * journey-status business logic in {@code handle(...)} and the redelivery
 * regression contract from issue #83.
 */
class AuditControllerTest {

    private static final String SERVICE = "journey-errors";
    private static final long JOURNEY_ID = 10L;

    private AuditController controller;
    private JourneyService journeyService;
    private IdempotencyGuard idempotencyService;
    private PubSubMetrics pubSubMetrics;

    @BeforeEach
    void setUp() {
        controller = new AuditController();
        journeyService = mock(JourneyService.class);
        idempotencyService = mock(IdempotencyGuard.class);
        pubSubMetrics = mock(PubSubMetrics.class);

        ReflectionTestUtils.setField(controller, "journey_service", journeyService);
        ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
        ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(controller, "pubSubMetrics", pubSubMetrics);
    }

    @Test
    void firstDelivery_updatesStatusToError_whenJourneyIsCandidate() {
        when(idempotencyService.claim("msg-1", SERVICE)).thenReturn(true);

        ResponseEntity<String> response = controller.receiveMessage(
            buildBody("msg-1", journeyJson(JOURNEY_ID, "CANDIDATE")));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("ok", response.getBody());
        verify(journeyService, times(1)).updateStatus(JOURNEY_ID, JourneyStatus.ERROR);
    }

    @ParameterizedTest
    @EnumSource(value = JourneyStatus.class, names = {"VERIFIED", "DISCARDED", "ERROR", "REVIEWING"})
    void firstDelivery_isNoOp_whenJourneyIsNotCandidate(JourneyStatus status) {
        when(idempotencyService.claim(anyString(), anyString())).thenReturn(true);

        ResponseEntity<String> response = controller.receiveMessage(
            buildBody("msg-noop-" + status, journeyJson(JOURNEY_ID, status.name())));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(journeyService, never()).updateStatus(anyLong(), any(JourneyStatus.class));
    }

    @Test
    void redelivery_doesNotDoubleProcess_thePerIssue83Contract() {
        // Stateful claim: first call wins, every subsequent call sees the row.
        when(idempotencyService.claim("dup-msg", SERVICE))
            .thenReturn(true)
            .thenReturn(false);

        Body envelope = buildBody("dup-msg", journeyJson(JOURNEY_ID, "CANDIDATE"));

        ResponseEntity<String> first = controller.receiveMessage(envelope);
        ResponseEntity<String> second = controller.receiveMessage(envelope);

        assertEquals(HttpStatus.OK, first.getStatusCode());
        assertEquals("ok", first.getBody());

        assertEquals(HttpStatus.OK, second.getStatusCode());
        assertTrue(second.getBody().contains("Duplicate"),
            "second redelivery must short-circuit on duplicate, got: " + second.getBody());

        verify(journeyService, times(1)).updateStatus(JOURNEY_ID, JourneyStatus.ERROR);
    }

    private static Body buildBody(String messageId, String json) {
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        Body body = new Body();
        Body.Message msg = body.new Message();
        msg.setMessageId(messageId);
        msg.setData(encoded);
        body.setMessage(msg);
        return body;
    }

    private static String journeyJson(long id, String status) {
        return "{\"accountId\":1,"
            + "\"journey\":{\"id\":" + id + ",\"status\":\"" + status + "\",\"candidateKey\":\"k\"},"
            + "\"browser\":\"CHROME\",\"auditRecordId\":100,\"mapId\":1}";
    }
}
