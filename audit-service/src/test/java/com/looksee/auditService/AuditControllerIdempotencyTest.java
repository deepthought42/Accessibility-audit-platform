package com.looksee.auditService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.mapper.Body;
import com.looksee.messaging.idempotency.IdempotencyGuard;
import com.looksee.messaging.observability.PubSubMetrics;
import com.looksee.messaging.poison.PoisonMessagePublisher;
import com.looksee.models.PageState;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.services.MessageBroadcaster;
import com.looksee.services.PageStateService;

/**
 * Direct-call (non-MockMvc) tests covering the idempotency claim/release
 * contract enforced by the inherited
 * {@link com.looksee.messaging.web.PubSubAuditController}. Envelope parsing,
 * Base64 decoding, polymorphic deserialization and metrics emission are
 * exercised here as a side effect, but only to validate that the eager
 * claim is released on transient errors and short-circuited on duplicates.
 */
class AuditControllerIdempotencyTest {

    private static final String SERVICE = "audit-service";

    private AuditController controller;
    private ObjectMapper mapper;

    private AuditRecordService auditRecordService;
    private AccountService accountService;
    private DomainService domainService;
    private PageStateService pageStateService;
    private MessageBroadcaster messageBroadcaster;
    private IdempotencyGuard idempotencyService;
    private PubSubMetrics pubSubMetrics;
    private PoisonMessagePublisher poisonPublisher;

    @BeforeEach
    void setUp() {
        controller = new AuditController();
        mapper = new Application().auditServiceObjectMapper();

        auditRecordService = mock(AuditRecordService.class);
        accountService = mock(AccountService.class);
        domainService = mock(DomainService.class);
        pageStateService = mock(PageStateService.class);
        messageBroadcaster = mock(MessageBroadcaster.class);
        idempotencyService = mock(IdempotencyGuard.class);
        pubSubMetrics = mock(PubSubMetrics.class);
        poisonPublisher = mock(PoisonMessagePublisher.class);

        ReflectionTestUtils.setField(controller, "audit_record_service", auditRecordService);
        ReflectionTestUtils.setField(controller, "account_service", accountService);
        ReflectionTestUtils.setField(controller, "domain_service", domainService);
        ReflectionTestUtils.setField(controller, "page_state_service", pageStateService);
        ReflectionTestUtils.setField(controller, "messageBroadcaster", messageBroadcaster);
        ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
        ReflectionTestUtils.setField(controller, "objectMapper", mapper);
        ReflectionTestUtils.setField(controller, "pubSubMetrics", pubSubMetrics);
        ReflectionTestUtils.setField(controller, "poisonPublisher", poisonPublisher);
        ReflectionTestUtils.setField(controller, "self", controller);
    }

    @Test
    void duplicateClaim_shortCircuitsWithoutDispatch() throws Exception {
        Body body = buildBody("dup-msg", auditProgressUpdateJson(100L));
        when(idempotencyService.claim("dup-msg", SERVICE)).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Duplicate"),
            "duplicate claim must short-circuit, got: " + response.getBody());
        verify(messageBroadcaster, never()).sendAuditUpdate(anyString(), any());
        verify(pubSubMetrics).recordDuplicate(SERVICE, "audit_update");
        verify(idempotencyService, never()).release(anyString(), anyString());
    }

    @Test
    void transientFailure_releasesClaimAndReturns500() throws Exception {
        Body body = buildBody("transient-msg", auditProgressUpdateJson(100L));
        when(idempotencyService.claim("transient-msg", SERVICE)).thenReturn(true);
        when(auditRecordService.findById(100L))
            .thenThrow(new TransientDataAccessResourceException("db unavailable"));

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(idempotencyService).release("transient-msg", SERVICE);
        verify(poisonPublisher, never()).publishPoison(any(), any());
    }

    @Test
    void successfulHandle_doesNotReleaseClaim() throws Exception {
        Body body = buildBody("ok-msg", auditProgressUpdateJson(100L));
        when(idempotencyService.claim("ok-msg", SERVICE)).thenReturn(true);

        PageAuditRecord record = mock(PageAuditRecord.class);
        when(record.getId()).thenReturn(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(record));
        when(auditRecordService.getAllAudits(100L)).thenReturn(new HashSet<>());
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());
        when(auditRecordService.findPage(100L)).thenReturn(mock(PageState.class));

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(idempotencyService, never()).release(anyString(), anyString());
        verify(pubSubMetrics).recordSuccess(SERVICE, "audit_update");
    }

    @Test
    void invalidBase64_returns200_publishesPoison_doesNotReleaseClaim() throws Exception {
        Body body = new Body();
        Body.Message msg = body.new Message("bad-b64", "2024-01-01T00:00:00Z", "not-valid-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.claim("bad-b64", SERVICE)).thenReturn(true);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(pubSubMetrics).recordError(eq(SERVICE), eq("audit_update"), any(IllegalArgumentException.class));
        verify(poisonPublisher).publishPoison(any(), any());
        verify(idempotencyService, never()).release(anyString(), anyString());
    }

    @Test
    void emptyEnvelope_recordsInvalidAndDoesNotClaim() throws Exception {
        Body body = new Body();
        body.setMessage(null);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(pubSubMetrics).recordInvalid(SERVICE, "audit_update");
        verify(idempotencyService, never()).claim(anyString(), anyString());
    }

    // ---- helpers ----

    private static String auditProgressUpdateJson(long pageAuditId) {
        return "{\"messageType\":\"AuditProgressUpdate\",\"accountId\":1,\"pageAuditId\":"
            + pageAuditId
            + ",\"progress\":1.0,\"message\":\"done\",\"category\":\"CONTENT\",\"level\":\"PAGE\"}";
    }

    private static Body buildBody(String messageId, String jsonPayload) {
        String encoded = Base64.getEncoder().encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));
        Body body = new Body();
        Body.Message msg = body.new Message(messageId, "2024-01-01T00:00:00Z", encoded);
        body.setMessage(msg);
        return body;
    }
}
