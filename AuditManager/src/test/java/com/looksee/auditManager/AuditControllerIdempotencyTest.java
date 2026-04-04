package com.looksee.auditManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.looksee.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.services.AuditRecordService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.PageStateService;

/**
 * Unit tests for {@link AuditController} focusing on idempotency,
 * error handling, and message processing.
 */
class AuditControllerIdempotencyTest {

    private AuditController controller;

    private AuditRecordService auditRecordService;
    private PubSubPageAuditPublisherImpl auditRecordTopic;
    private PageStateService pageStateService;
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        auditRecordService = mock(AuditRecordService.class);
        auditRecordTopic = mock(PubSubPageAuditPublisherImpl.class);
        pageStateService = mock(PageStateService.class);
        idempotencyService = mock(IdempotencyService.class);

        // Constructor injection
        controller = new AuditController(
            auditRecordService,
            auditRecordTopic,
            pageStateService,
            idempotencyService
        );
    }

    // --- Idempotency tests ---

    @Test
    void shouldReturnOkForDuplicateMessage() {
        String validPayload = "{\"accountId\":1,\"pageId\":10,\"auditRecordId\":100}";
        Body body = createValidBody("test-msg-id", validPayload);
        when(idempotencyService.isAlreadyProcessed("test-msg-id", "audit-manager")).thenReturn(true);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("already processed"));
        verify(auditRecordTopic, never()).publish(anyString());
        verify(idempotencyService, never()).markProcessed(anyString(), anyString());
    }

    // --- Invalid payload tests (returns 200 not 400) ---

    @Test
    void shouldReturnOkForNullBody() {
        ResponseEntity<String> response = controller.receiveMessage(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForNullMessage() {
        Body body = new Body();
        body.setMessage(null);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForNullData() {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-1", "2024-01-01T00:00:00Z", "placeholder");
        // Use reflection to set data to null since constructor asserts non-null
        try {
            java.lang.reflect.Field dataField = Body.Message.class.getDeclaredField("data");
            dataField.setAccessible(true);
            dataField.set(msg, null);
        } catch (Exception e) {
            fail("Failed to set data field to null via reflection");
        }
        body.setMessage(msg);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForEmptyData() {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-2", "2024-01-01T00:00:00Z", "");
        body.setMessage(msg);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForInvalidBase64Data() {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-3", "2024-01-01T00:00:00Z", "not-valid-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-3", "audit-manager")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(auditRecordTopic, never()).publish(anyString());
    }

    @Test
    void shouldReturnOkForInvalidJson() {
        String invalidJson = "this is not json";
        Body body = createValidBody("msg-4", invalidJson);
        when(idempotencyService.isAlreadyProcessed("msg-4", "audit-manager")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(auditRecordTopic, never()).publish(anyString());
    }

    // --- Successful processing tests ---

    @Test
    void shouldCreateAndPublishAuditForEligiblePage() throws Exception {
        String payload = "{\"accountId\":1,\"pageId\":10,\"auditRecordId\":100}";
        Body body = createValidBody("msg-5", payload);
        when(idempotencyService.isAlreadyProcessed("msg-5", "audit-manager")).thenReturn(false);

        // Set up audit record with labels
        DomainAuditRecord domainRecord = mock(DomainAuditRecord.class);
        HashSet<AuditName> labels = new HashSet<>();
        labels.add(AuditName.ALT_TEXT);
        when(domainRecord.getAuditLabels()).thenReturn(labels);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(domainRecord));

        when(auditRecordService.wasPageAlreadyAudited(100L, 10L)).thenReturn(false);
        when(pageStateService.isPageLandable(10L)).thenReturn(true);

        PageState pageState = new PageState();
        when(pageStateService.findById(10L)).thenReturn(Optional.of(pageState));

        PageAuditRecord savedRecord = mock(PageAuditRecord.class);
        when(savedRecord.getId()).thenReturn(200L);
        when(auditRecordService.save(any(AuditRecord.class))).thenReturn(savedRecord);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(auditRecordTopic).publish(anyString());
        verify(idempotencyService).markProcessed("msg-5", "audit-manager");
    }

    @Test
    void shouldMarkProcessedWhenPageAlreadyAudited() {
        String payload = "{\"accountId\":1,\"pageId\":10,\"auditRecordId\":100}";
        Body body = createValidBody("msg-6", payload);
        when(idempotencyService.isAlreadyProcessed("msg-6", "audit-manager")).thenReturn(false);

        when(auditRecordService.findById(100L)).thenReturn(Optional.empty());
        when(auditRecordService.wasPageAlreadyAudited(100L, 10L)).thenReturn(true);
        when(pageStateService.isPageLandable(10L)).thenReturn(true);
        when(pageStateService.findById(10L)).thenReturn(Optional.of(new PageState()));

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(auditRecordTopic, never()).publish(anyString());
        verify(idempotencyService).markProcessed("msg-6", "audit-manager");
    }

    // --- Error handling tests ---

    @Test
    void shouldNotCallMarkProcessedOnInvalidPayload() {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-7", "2024-01-01T00:00:00Z", "bad-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-7", "audit-manager")).thenReturn(false);

        controller.receiveMessage(body);

        verify(idempotencyService, never()).markProcessed(anyString(), anyString());
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
