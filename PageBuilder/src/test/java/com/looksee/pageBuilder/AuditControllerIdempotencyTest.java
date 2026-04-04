package com.looksee.pageBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.looksee.gcp.PubSubErrorPublisherImpl;
import com.looksee.gcp.PubSubJourneyVerifiedPublisherImpl;
import com.looksee.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.gcp.PubSubPageCreatedPublisherImpl;
import com.looksee.pageBuilder.schemas.BodySchema;
import com.looksee.pageBuilder.schemas.MessageSchema;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainMapService;
import com.looksee.services.ElementStateService;
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
    private AuditRecordService auditRecordService;
    private BrowserService browserService;
    private JourneyService journeyService;
    private StepService stepService;
    private PageStateService pageStateService;
    private ElementStateService elementStateService;
    private DomainMapService domainMapService;
    private PubSubErrorPublisherImpl pubSubErrorPublisher;
    private PubSubJourneyVerifiedPublisherImpl pubSubJourneyVerifiedPublisher;
    private PubSubPageCreatedPublisherImpl pubSubPageCreatedPublisher;
    private PubSubPageAuditPublisherImpl auditRecordTopic;

    @BeforeEach
    void setUp() {
        controller = new AuditController();

        idempotencyService = mock(IdempotencyService.class);
        auditRecordService = mock(AuditRecordService.class);
        browserService = mock(BrowserService.class);
        journeyService = mock(JourneyService.class);
        stepService = mock(StepService.class);
        pageStateService = mock(PageStateService.class);
        elementStateService = mock(ElementStateService.class);
        domainMapService = mock(DomainMapService.class);
        pubSubErrorPublisher = mock(PubSubErrorPublisherImpl.class);
        pubSubJourneyVerifiedPublisher = mock(PubSubJourneyVerifiedPublisherImpl.class);
        pubSubPageCreatedPublisher = mock(PubSubPageCreatedPublisherImpl.class);
        auditRecordTopic = mock(PubSubPageAuditPublisherImpl.class);

        ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
        ReflectionTestUtils.setField(controller, "audit_record_service", auditRecordService);
        ReflectionTestUtils.setField(controller, "browser_service", browserService);
        ReflectionTestUtils.setField(controller, "journey_service", journeyService);
        ReflectionTestUtils.setField(controller, "step_service", stepService);
        ReflectionTestUtils.setField(controller, "page_state_service", pageStateService);
        ReflectionTestUtils.setField(controller, "element_state_service", elementStateService);
        ReflectionTestUtils.setField(controller, "domain_map_service", domainMapService);
        ReflectionTestUtils.setField(controller, "pubSubErrorPublisherImpl", pubSubErrorPublisher);
        ReflectionTestUtils.setField(controller, "pubSubJourneyVerifiedPublisherImpl", pubSubJourneyVerifiedPublisher);
        ReflectionTestUtils.setField(controller, "pubSubPageCreatedPublisherImpl", pubSubPageCreatedPublisher);
        ReflectionTestUtils.setField(controller, "audit_record_topic", auditRecordTopic);
    }

    // --- Idempotency tests ---

    @Test
    void shouldReturnOkForDuplicateMessage() throws Exception {
        String validPayload = "{\"url\":\"https://example.com\",\"type\":\"PAGE\",\"accountId\":1,\"auditId\":100}";
        BodySchema body = createValidBody("test-msg-id", validPayload);
        when(idempotencyService.isAlreadyProcessed("test-msg-id", "page-builder")).thenReturn(true);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("already processed"));
        verify(pubSubPageCreatedPublisher, never()).publish(anyString());
        verify(pubSubJourneyVerifiedPublisher, never()).publish(anyString());
        verify(auditRecordTopic, never()).publish(anyString());
        verify(idempotencyService, never()).markProcessed(anyString(), anyString());
    }

    // --- Null / empty body tests ---

    @Test
    void shouldReturnBadRequestForNullBody() throws Exception {
        ResponseEntity<String> response = controller.receiveMessage(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestForNullMessage() throws Exception {
        BodySchema body = new BodySchema();
        body.setMessage(null);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestForNullData() throws Exception {
        BodySchema body = new BodySchema();
        MessageSchema msg = new MessageSchema();
        msg.setMessageId("msg-1");
        msg.setData(null);
        body.setMessage(msg);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestForBlankData() throws Exception {
        BodySchema body = new BodySchema();
        MessageSchema msg = new MessageSchema();
        msg.setMessageId("msg-1");
        msg.setData("   ");
        body.setMessage(msg);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // --- Invalid Base64 / JSON tests ---

    @Test
    void shouldReturnOkForInvalidBase64Data() throws Exception {
        BodySchema body = new BodySchema();
        MessageSchema msg = new MessageSchema();
        msg.setMessageId("msg-2");
        msg.setData("not-valid-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-2", "page-builder")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(pubSubPageCreatedPublisher, never()).publish(anyString());
    }

    @Test
    void shouldReturnOkForInvalidJson() throws Exception {
        String invalidJson = "this is not json";
        String encoded = Base64.getEncoder().encodeToString(invalidJson.getBytes(StandardCharsets.UTF_8));
        BodySchema body = new BodySchema();
        MessageSchema msg = new MessageSchema();
        msg.setMessageId("msg-3");
        msg.setData(encoded);
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-3", "page-builder")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(pubSubPageCreatedPublisher, never()).publish(anyString());
    }

    // --- markProcessed verification ---

    @Test
    void shouldNotCallMarkProcessedOnInvalidPayload() throws Exception {
        BodySchema body = new BodySchema();
        MessageSchema msg = new MessageSchema();
        msg.setMessageId("msg-4");
        msg.setData("bad-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-4", "page-builder")).thenReturn(false);

        controller.receiveMessage(body);

        verify(idempotencyService, never()).markProcessed(anyString(), anyString());
    }

    // --- Helper ---

    private BodySchema createValidBody(String messageId, String jsonPayload) {
        String encoded = Base64.getEncoder().encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));
        BodySchema body = new BodySchema();
        MessageSchema msg = new MessageSchema();
        msg.setMessageId(messageId);
        msg.setData(encoded);
        body.setMessage(msg);
        return body;
    }
}
