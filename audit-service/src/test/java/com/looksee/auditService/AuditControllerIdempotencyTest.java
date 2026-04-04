package com.looksee.auditService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.looksee.mapper.Body;
import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.MessageBroadcaster;
import com.looksee.services.PageStateService;

/**
 * Unit tests for {@link AuditController} focusing on idempotency,
 * messageType-based routing, error handling, and message processing.
 */
class AuditControllerIdempotencyTest {

    private AuditController controller;

    private AuditRecordService auditRecordService;
    private AccountService accountService;
    private DomainService domainService;
    private PageStateService pageStateService;
    private MessageBroadcaster messageBroadcaster;
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        controller = new AuditController();

        auditRecordService = mock(AuditRecordService.class);
        accountService = mock(AccountService.class);
        domainService = mock(DomainService.class);
        pageStateService = mock(PageStateService.class);
        messageBroadcaster = mock(MessageBroadcaster.class);
        idempotencyService = mock(IdempotencyService.class);

        ReflectionTestUtils.setField(controller, "audit_record_service", auditRecordService);
        ReflectionTestUtils.setField(controller, "account_service", accountService);
        ReflectionTestUtils.setField(controller, "domain_service", domainService);
        ReflectionTestUtils.setField(controller, "page_state_service", pageStateService);
        ReflectionTestUtils.setField(controller, "messageBroadcaster", messageBroadcaster);
        ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
    }

    // --- Idempotency tests ---

    @Test
    void shouldReturnOkForDuplicateMessage() {
        String validPayload = "{\"messageType\":\"AuditProgressUpdate\",\"accountId\":1,\"pageAuditId\":100,\"progress\":0.5,\"message\":\"test\",\"category\":\"CONTENT\",\"level\":\"PAGE\"}";
        Body body = createValidBody("test-msg-id", validPayload);
        when(idempotencyService.isAlreadyProcessed("test-msg-id", "audit-service")).thenReturn(true);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("already processed"));
        verify(messageBroadcaster, never()).sendAuditUpdate(anyString(), any());
        verify(idempotencyService, never()).markProcessed(anyString(), anyString());
    }

    // --- Invalid payload tests ---

    @Test
    void shouldReturnBadRequestForNullBody() {
        ResponseEntity<String> response = controller.receiveMessage(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestForNullMessage() {
        Body body = new Body();
        body.setMessage(null);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestForNullData() {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-1", "2024-01-01T00:00:00Z", "placeholder");
        try {
            java.lang.reflect.Field dataField = Body.Message.class.getDeclaredField("data");
            dataField.setAccessible(true);
            dataField.set(msg, null);
        } catch (Exception e) {
            fail("Failed to set data field to null via reflection");
        }
        body.setMessage(msg);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForInvalidBase64Data() {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-2", "2024-01-01T00:00:00Z", "not-valid-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-2", "audit-service")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(messageBroadcaster, never()).sendAuditUpdate(anyString(), any());
    }

    @Test
    void shouldReturnOkForInvalidJson() {
        String invalidJson = "this is not json at all";
        Body body = createValidBody("msg-3", invalidJson);
        when(idempotencyService.isAlreadyProcessed("msg-3", "audit-service")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- messageType routing tests ---

    @Test
    void shouldRouteAuditProgressUpdateByMessageType() {
        String payload = "{\"messageType\":\"AuditProgressUpdate\",\"accountId\":1,\"pageAuditId\":100,\"progress\":1.0,\"message\":\"done\",\"category\":\"CONTENT\",\"level\":\"PAGE\"}";
        Body body = createValidBody("msg-4", payload);
        when(idempotencyService.isAlreadyProcessed("msg-4", "audit-service")).thenReturn(false);

        PageAuditRecord auditRecord = mock(PageAuditRecord.class);
        when(auditRecord.getId()).thenReturn(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(auditRecord));
        when(auditRecordService.getAllAudits(100L)).thenReturn(new HashSet<>());
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());
        when(auditRecordService.findPage(100L)).thenReturn(null);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Successfully"));
        verify(idempotencyService).markProcessed("msg-4", "audit-service");
    }

    @Test
    void shouldRoutePageAuditProgressMessageByMessageType() {
        String payload = "{\"messageType\":\"PageAuditProgressMessage\",\"accountId\":1,\"pageAuditId\":100}";
        Body body = createValidBody("msg-5", payload);
        when(idempotencyService.isAlreadyProcessed("msg-5", "audit-service")).thenReturn(false);

        PageAuditRecord auditRecord = mock(PageAuditRecord.class);
        when(auditRecord.getId()).thenReturn(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(auditRecord));
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());
        when(auditRecordService.getAllAudits(100L)).thenReturn(new HashSet<>());
        when(auditRecordService.findPage(100L)).thenReturn(null);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(idempotencyService).markProcessed("msg-5", "audit-service");
    }

    @Test
    void shouldFallbackToLegacyParsingForMessagesWithoutMessageType() {
        // A payload without messageType field should use legacy cascading deserialization
        String payload = "{\"accountId\":1,\"pageAuditId\":100,\"progress\":1.0,\"message\":\"done\",\"category\":\"CONTENT\",\"level\":\"PAGE\"}";
        Body body = createValidBody("msg-6", payload);
        when(idempotencyService.isAlreadyProcessed("msg-6", "audit-service")).thenReturn(false);

        PageAuditRecord auditRecord = mock(PageAuditRecord.class);
        when(auditRecord.getId()).thenReturn(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(auditRecord));
        when(auditRecordService.getAllAudits(100L)).thenReturn(new HashSet<>());
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());
        when(auditRecordService.findPage(100L)).thenReturn(null);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(idempotencyService).markProcessed("msg-6", "audit-service");
    }

    @Test
    void shouldHandleUnknownMessageTypeWithFallback() {
        String payload = "{\"messageType\":\"UnknownType\",\"accountId\":1,\"pageAuditId\":100,\"progress\":1.0,\"message\":\"done\",\"category\":\"CONTENT\",\"level\":\"PAGE\"}";
        Body body = createValidBody("msg-7", payload);
        when(idempotencyService.isAlreadyProcessed("msg-7", "audit-service")).thenReturn(false);

        PageAuditRecord auditRecord = mock(PageAuditRecord.class);
        when(auditRecord.getId()).thenReturn(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(auditRecord));
        when(auditRecordService.getAllAudits(100L)).thenReturn(new HashSet<>());
        when(auditRecordService.getDomainAuditRecordForPageRecord(100L)).thenReturn(Optional.empty());
        when(auditRecordService.findPage(100L)).thenReturn(null);

        ResponseEntity<String> response = controller.receiveMessage(body);

        // Should fall through to legacy handler which tries AuditProgressUpdate
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(idempotencyService).markProcessed("msg-7", "audit-service");
    }

    // --- Error handling tests ---

    @Test
    void shouldNotCallMarkProcessedOnInvalidPayload() {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-8", "2024-01-01T00:00:00Z", "bad-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-8", "audit-service")).thenReturn(false);

        controller.receiveMessage(body);

        verify(idempotencyService, never()).markProcessed(anyString(), anyString());
    }

    @Test
    void shouldReturnOkOnProcessingException() {
        String payload = "{\"messageType\":\"AuditProgressUpdate\",\"accountId\":1,\"pageAuditId\":100,\"progress\":1.0,\"message\":\"done\",\"category\":\"CONTENT\",\"level\":\"PAGE\"}";
        Body body = createValidBody("msg-9", payload);
        when(idempotencyService.isAlreadyProcessed("msg-9", "audit-service")).thenReturn(false);
        when(auditRecordService.findById(100L)).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<String> response = controller.receiveMessage(body);

        // Controller catches all exceptions and returns 200 to prevent redelivery
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- Helper ---

    private Body createValidBody(String messageId, String jsonPayload) {
        String encoded = Base64.getEncoder().encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));
        Body body = new Body();
        Body.Message msg = body.new Message(messageId, "2024-01-01T00:00:00Z", encoded);
        body.setMessage(msg);
        return body;
    }
}
