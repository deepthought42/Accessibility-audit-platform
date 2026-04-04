package com.looksee.visualDesignAudit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.looksee.gcp.PubSubAuditUpdatePublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.models.Domain;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.PageStateService;
import com.looksee.visualDesignAudit.audit.ImageAudit;
import com.looksee.visualDesignAudit.audit.ImagePolicyAudit;
import com.looksee.visualDesignAudit.audit.NonTextColorContrastAudit;
import com.looksee.visualDesignAudit.audit.TextColorContrastAudit;

/**
 * Unit tests for {@link AuditController} focusing on idempotency,
 * error handling, and message processing.
 */
class AuditControllerIdempotencyTest {

    private AuditController controller;

    private IdempotencyService idempotencyService;
    private AuditRecordService auditRecordService;
    private DomainService domainService;
    private PageStateService pageStateService;
    private TextColorContrastAudit textContrastAudit;
    private NonTextColorContrastAudit nonTextContrastAudit;
    private ImageAudit imageAudit;
    private ImagePolicyAudit imagePolicyAudit;
    private PubSubAuditUpdatePublisherImpl auditUpdateTopic;

    @BeforeEach
    void setUp() {
        controller = new AuditController();

        idempotencyService = mock(IdempotencyService.class);
        auditRecordService = mock(AuditRecordService.class);
        domainService = mock(DomainService.class);
        pageStateService = mock(PageStateService.class);
        textContrastAudit = mock(TextColorContrastAudit.class);
        nonTextContrastAudit = mock(NonTextColorContrastAudit.class);
        imageAudit = mock(ImageAudit.class);
        imagePolicyAudit = mock(ImagePolicyAudit.class);
        auditUpdateTopic = mock(PubSubAuditUpdatePublisherImpl.class);

        ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
        ReflectionTestUtils.setField(controller, "audit_record_service", auditRecordService);
        ReflectionTestUtils.setField(controller, "domain_service", domainService);
        ReflectionTestUtils.setField(controller, "page_state_service", pageStateService);
        ReflectionTestUtils.setField(controller, "text_contrast_audit_impl", textContrastAudit);
        ReflectionTestUtils.setField(controller, "non_text_contrast_audit_impl", nonTextContrastAudit);
        ReflectionTestUtils.setField(controller, "image_audit", imageAudit);
        ReflectionTestUtils.setField(controller, "image_policy_audit", imagePolicyAudit);
        ReflectionTestUtils.setField(controller, "audit_update_topic", auditUpdateTopic);
    }

    // --- Idempotency tests ---

    @Test
    void shouldReturnOkForDuplicateMessage() throws Exception {
        String validPayload = "{\"accountId\":1,\"pageAuditId\":100}";
        Body body = createValidBody("test-msg-id", validPayload);
        when(idempotencyService.isAlreadyProcessed("test-msg-id", "visual-design-audit")).thenReturn(true);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("already processed"));
        verify(auditUpdateTopic, never()).publish(anyString());
        verify(idempotencyService, never()).markProcessed(anyString(), anyString());
    }

    // --- Invalid payload tests ---

    @Test
    void shouldReturnOkForNullBody() throws Exception {
        ResponseEntity<String> response = controller.receiveMessage(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForNullMessage() throws Exception {
        Body body = new Body();
        body.setMessage(null);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForNullData() throws Exception {
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

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForInvalidBase64Data() throws Exception {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-2", "2024-01-01T00:00:00Z", "not-valid-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-2", "visual-design-audit")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(auditUpdateTopic, never()).publish(anyString());
    }

    @Test
    void shouldReturnOkForInvalidJson() throws Exception {
        String invalidJson = "this is not json";
        Body body = createValidBody("msg-3", invalidJson);
        when(idempotencyService.isAlreadyProcessed("msg-3", "visual-design-audit")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(auditUpdateTopic, never()).publish(anyString());
    }

    // --- Successful processing tests ---

    @Test
    void shouldCallMarkProcessedOnSuccess() throws Exception {
        String payload = "{\"accountId\":1,\"pageAuditId\":100}";
        Body body = createValidBody("msg-4", payload);
        when(idempotencyService.isAlreadyProcessed("msg-4", "visual-design-audit")).thenReturn(false);

        Domain domain = mock(Domain.class);
        when(domain.getId()).thenReturn(10L);
        when(domainService.findByAuditRecord(100L)).thenReturn(domain);
        when(domainService.getDesignSystem(10L)).thenReturn(Optional.of(new DesignSystem()));

        AuditRecord auditRecord = mock(AuditRecord.class);
        when(auditRecord.getId()).thenReturn(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(auditRecord));

        PageState page = mock(PageState.class);
        when(page.getId()).thenReturn(50L);
        when(pageStateService.getPageStateForAuditRecord(100L)).thenReturn(page);
        when(pageStateService.getElementStates(50L)).thenReturn(new ArrayList<>());
        when(auditRecordService.getAllAudits(100L)).thenReturn(new HashSet<>());

        Audit mockAudit = mock(Audit.class);
        when(mockAudit.getId()).thenReturn(1L);
        when(textContrastAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(nonTextContrastAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(imageAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(imagePolicyAudit.execute(any(), any(), any())).thenReturn(mockAudit);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Successfully"));
        verify(idempotencyService).markProcessed("msg-4", "visual-design-audit");
    }

    // --- Error handling tests ---

    @Test
    void shouldNotCallMarkProcessedOnInvalidPayload() throws Exception {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-5", "2024-01-01T00:00:00Z", "bad-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-5", "visual-design-audit")).thenReturn(false);

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
