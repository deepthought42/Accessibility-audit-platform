package com.looksee.contentAudit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

import com.looksee.contentAudit.models.AppletAltTextAudit;
import com.looksee.contentAudit.models.CanvasAltTextAudit;
import com.looksee.contentAudit.models.IframeAltTextAudit;
import com.looksee.contentAudit.models.ImageAltTextAudit;
import com.looksee.contentAudit.models.ObjectAltTextAudit;
import com.looksee.contentAudit.models.ParagraphingAudit;
import com.looksee.contentAudit.models.ReadabilityAudit;
import com.looksee.contentAudit.models.SVGAltTextAudit;
import com.looksee.gcp.PubSubAuditUpdatePublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.services.AuditRecordService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.PageStateService;

/**
 * Unit tests for {@link AuditController} focusing on idempotency,
 * error handling, and message processing.
 */
class AuditControllerIdempotencyTest {

    private AuditController controller;

    private IdempotencyService idempotencyService;
    private AuditRecordService auditRecordService;
    private PageStateService pageStateService;
    private ImageAltTextAudit imageAltTextAudit;
    private AppletAltTextAudit appletAltTextAudit;
    private CanvasAltTextAudit canvasAltTextAudit;
    private IframeAltTextAudit iframeAltTextAudit;
    private ObjectAltTextAudit objectAltTextAudit;
    private SVGAltTextAudit svgAltTextAudit;
    private ParagraphingAudit paragraphingAudit;
    private ReadabilityAudit readabilityAudit;
    private PubSubAuditUpdatePublisherImpl auditUpdateTopic;

    @BeforeEach
    void setUp() {
        controller = new AuditController();

        idempotencyService = mock(IdempotencyService.class);
        auditRecordService = mock(AuditRecordService.class);
        pageStateService = mock(PageStateService.class);
        imageAltTextAudit = mock(ImageAltTextAudit.class);
        appletAltTextAudit = mock(AppletAltTextAudit.class);
        canvasAltTextAudit = mock(CanvasAltTextAudit.class);
        iframeAltTextAudit = mock(IframeAltTextAudit.class);
        objectAltTextAudit = mock(ObjectAltTextAudit.class);
        svgAltTextAudit = mock(SVGAltTextAudit.class);
        paragraphingAudit = mock(ParagraphingAudit.class);
        readabilityAudit = mock(ReadabilityAudit.class);
        auditUpdateTopic = mock(PubSubAuditUpdatePublisherImpl.class);

        ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
        ReflectionTestUtils.setField(controller, "audit_record_service", auditRecordService);
        ReflectionTestUtils.setField(controller, "page_state_service", pageStateService);
        ReflectionTestUtils.setField(controller, "image_alt_text_auditor", imageAltTextAudit);
        ReflectionTestUtils.setField(controller, "appletAllAltTextAudit", appletAltTextAudit);
        ReflectionTestUtils.setField(controller, "canvasAltTextAudit", canvasAltTextAudit);
        ReflectionTestUtils.setField(controller, "iframeAltTextAudit", iframeAltTextAudit);
        ReflectionTestUtils.setField(controller, "objectAltTextAudit", objectAltTextAudit);
        ReflectionTestUtils.setField(controller, "svgAltTextAudit", svgAltTextAudit);
        ReflectionTestUtils.setField(controller, "paragraph_auditor", paragraphingAudit);
        ReflectionTestUtils.setField(controller, "readability_auditor", readabilityAudit);
        ReflectionTestUtils.setField(controller, "audit_update_topic", auditUpdateTopic);
    }

    // --- Idempotency tests ---

    @Test
    void shouldReturnOkForDuplicateMessage() {
        String validPayload = "{\"accountId\":1,\"pageAuditId\":100}";
        Body body = createValidBody("test-msg-id", validPayload);
        when(idempotencyService.isAlreadyProcessed("test-msg-id", "content-audit")).thenReturn(true);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("already processed"));
        verify(auditUpdateTopic, never()).publish(anyString());
        verify(idempotencyService, never()).markProcessed(anyString(), anyString());
    }

    // --- Invalid payload tests ---

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
    void shouldReturnOkForBlankData() {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-2", "2024-01-01T00:00:00Z", "   ");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-2", "content-audit")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForInvalidBase64Data() {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-3", "2024-01-01T00:00:00Z", "not-valid-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-3", "content-audit")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(auditUpdateTopic, never()).publish(anyString());
    }

    @Test
    void shouldReturnOkForInvalidJson() {
        String invalidJson = "this is not json";
        Body body = createValidBody("msg-4", invalidJson);
        when(idempotencyService.isAlreadyProcessed("msg-4", "content-audit")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(auditUpdateTopic, never()).publish(anyString());
    }

    @Test
    void shouldReturnOkForInvalidPageAuditId() {
        String payload = "{\"accountId\":1,\"pageAuditId\":0}";
        Body body = createValidBody("msg-5", payload);
        when(idempotencyService.isAlreadyProcessed("msg-5", "content-audit")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- Successful processing tests ---

    @Test
    void shouldCallMarkProcessedOnSuccess() throws Exception {
        String payload = "{\"accountId\":1,\"pageAuditId\":100}";
        Body body = createValidBody("msg-6", payload);
        when(idempotencyService.isAlreadyProcessed("msg-6", "content-audit")).thenReturn(false);

        AuditRecord auditRecord = mock(AuditRecord.class);
        when(auditRecord.getId()).thenReturn(100L);
        when(auditRecordService.findById(100L)).thenReturn(Optional.of(auditRecord));

        PageState page = mock(PageState.class);
        when(page.getId()).thenReturn(50L);
        when(pageStateService.findByAuditRecordId(100L)).thenReturn(page);
        when(pageStateService.getElementStates(50L)).thenReturn(new java.util.ArrayList<>());
        when(auditRecordService.getAllAudits(100L)).thenReturn(new HashSet<>());

        Audit mockAudit = mock(Audit.class);
        when(mockAudit.getId()).thenReturn(1L);
        when(imageAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(appletAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(canvasAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(iframeAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(objectAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(svgAltTextAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(readabilityAudit.execute(any(), any(), any())).thenReturn(mockAudit);
        when(paragraphingAudit.execute(any(), any(), any())).thenReturn(mockAudit);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Successfully"));
        verify(auditUpdateTopic).publish(anyString());
        verify(idempotencyService).markProcessed("msg-6", "content-audit");
    }

    // --- Error handling tests ---

    @Test
    void shouldReturnInternalServerErrorOnException() {
        String payload = "{\"accountId\":1,\"pageAuditId\":100}";
        Body body = createValidBody("msg-7", payload);
        when(idempotencyService.isAlreadyProcessed("msg-7", "content-audit")).thenReturn(false);
        when(auditRecordService.findById(100L)).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(idempotencyService, never()).markProcessed(anyString(), anyString());
    }

    @Test
    void shouldNotCallMarkProcessedOnInvalidPayload() {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-8", "2024-01-01T00:00:00Z", "bad-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-8", "content-audit")).thenReturn(false);

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
