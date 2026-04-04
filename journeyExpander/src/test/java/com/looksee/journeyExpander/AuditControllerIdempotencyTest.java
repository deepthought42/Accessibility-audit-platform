package com.looksee.journeyExpander;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.looksee.gcp.PubSubJourneyCandidatePublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainMapService;
import com.looksee.services.DomainService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.JourneyService;
import com.looksee.services.PageStateService;
import com.looksee.services.StepService;

/**
 * Unit tests for {@link AuditController} focusing on idempotency,
 * error handling, and message processing.
 */
class AuditControllerIdempotencyTest {

    private AuditController controller;

    private IdempotencyService idempotencyService;
    private DomainService domainService;
    private JourneyService journeyService;
    private DomainMapService domainMapService;
    private AuditRecordService auditRecordService;
    private PageStateService pageStateService;
    private StepService stepService;
    private PubSubJourneyCandidatePublisherImpl journeyCandidateTopic;

    @BeforeEach
    void setUp() {
        controller = new AuditController();

        idempotencyService = mock(IdempotencyService.class);
        domainService = mock(DomainService.class);
        journeyService = mock(JourneyService.class);
        domainMapService = mock(DomainMapService.class);
        auditRecordService = mock(AuditRecordService.class);
        pageStateService = mock(PageStateService.class);
        stepService = mock(StepService.class);
        journeyCandidateTopic = mock(PubSubJourneyCandidatePublisherImpl.class);

        ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
        ReflectionTestUtils.setField(controller, "domain_service", domainService);
        ReflectionTestUtils.setField(controller, "journey_service", journeyService);
        ReflectionTestUtils.setField(controller, "domain_map_service", domainMapService);
        ReflectionTestUtils.setField(controller, "audit_record_service", auditRecordService);
        ReflectionTestUtils.setField(controller, "page_state_service", pageStateService);
        ReflectionTestUtils.setField(controller, "step_service", stepService);
        ReflectionTestUtils.setField(controller, "journey_candidate_topic", journeyCandidateTopic);
    }

    // --- Idempotency tests ---

    @Test
    void shouldReturnOkForDuplicateMessage() {
        String validPayload = "{\"journey\":{\"id\":1,\"steps\":[{\"type\":\"LandingStep\",\"startPage\":{\"url\":\"https://example.com\",\"key\":\"k1\"}}],\"status\":\"VERIFIED\"},\"browser\":\"CHROME\",\"accountId\":1,\"auditRecordId\":100}";
        Body body = createValidBody("test-msg-id", validPayload);
        when(idempotencyService.isAlreadyProcessed("test-msg-id", "journey-expander")).thenReturn(true);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("already processed"));
        verify(journeyCandidateTopic, never()).publish(anyString());
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

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForInvalidBase64Data() {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-3", "2024-01-01T00:00:00Z", "not-valid-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-3", "journey-expander")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(journeyCandidateTopic, never()).publish(anyString());
    }

    @Test
    void shouldReturnOkForInvalidJson() {
        String invalidJson = "this is not json";
        Body body = createValidBody("msg-4", invalidJson);
        when(idempotencyService.isAlreadyProcessed("msg-4", "journey-expander")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(journeyCandidateTopic, never()).publish(anyString());
    }

    @Test
    void shouldReturnOkForJourneyWithNoSteps() {
        String payload = "{\"journey\":{\"id\":1,\"steps\":[]},\"browser\":\"CHROME\",\"accountId\":1,\"auditRecordId\":100}";
        Body body = createValidBody("msg-5", payload);
        when(idempotencyService.isAlreadyProcessed("msg-5", "journey-expander")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- Error handling tests ---

    @Test
    void shouldNotCallMarkProcessedOnInvalidPayload() {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-6", "2024-01-01T00:00:00Z", "bad-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-6", "journey-expander")).thenReturn(false);

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
