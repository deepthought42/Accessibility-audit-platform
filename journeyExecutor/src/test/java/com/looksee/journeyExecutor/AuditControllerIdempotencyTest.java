package com.looksee.journeyExecutor;

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

import com.looksee.gcp.PubSubDiscardedJourneyPublisherImpl;
import com.looksee.gcp.PubSubJourneyVerifiedPublisherImpl;
import com.looksee.gcp.PubSubPageBuiltPublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainMapService;
import com.looksee.services.DomainService;
import com.looksee.services.ElementStateService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.JourneyService;
import com.looksee.services.PageStateService;
import com.looksee.services.StepExecutor;
import com.looksee.services.StepService;

/**
 * Unit tests for {@link AuditController} focusing on idempotency,
 * error handling, and message processing.
 */
class AuditControllerIdempotencyTest {

    private AuditController controller;

    private IdempotencyService idempotencyService;
    private BrowserService browserService;
    private ElementStateService elementStateService;
    private PageStateService pageStateService;
    private StepService stepService;
    private JourneyService journeyService;
    private DomainMapService domainMapService;
    private DomainService domainService;
    private PubSubJourneyVerifiedPublisherImpl verifiedJourneyTopic;
    private PubSubDiscardedJourneyPublisherImpl discardedJourneyTopic;
    private StepExecutor stepExecutor;
    private PubSubPageBuiltPublisherImpl pageBuiltTopic;

    @BeforeEach
    void setUp() {
        controller = new AuditController();

        idempotencyService = mock(IdempotencyService.class);
        browserService = mock(BrowserService.class);
        elementStateService = mock(ElementStateService.class);
        pageStateService = mock(PageStateService.class);
        stepService = mock(StepService.class);
        journeyService = mock(JourneyService.class);
        domainMapService = mock(DomainMapService.class);
        domainService = mock(DomainService.class);
        verifiedJourneyTopic = mock(PubSubJourneyVerifiedPublisherImpl.class);
        discardedJourneyTopic = mock(PubSubDiscardedJourneyPublisherImpl.class);
        stepExecutor = mock(StepExecutor.class);
        pageBuiltTopic = mock(PubSubPageBuiltPublisherImpl.class);

        ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
        ReflectionTestUtils.setField(controller, "browser_service", browserService);
        ReflectionTestUtils.setField(controller, "element_state_service", elementStateService);
        ReflectionTestUtils.setField(controller, "page_state_service", pageStateService);
        ReflectionTestUtils.setField(controller, "step_service", stepService);
        ReflectionTestUtils.setField(controller, "journey_service", journeyService);
        ReflectionTestUtils.setField(controller, "domain_map_service", domainMapService);
        ReflectionTestUtils.setField(controller, "domain_service", domainService);
        ReflectionTestUtils.setField(controller, "verified_journey_topic", verifiedJourneyTopic);
        ReflectionTestUtils.setField(controller, "discarded_journey_topic", discardedJourneyTopic);
        ReflectionTestUtils.setField(controller, "step_executor", stepExecutor);
        ReflectionTestUtils.setField(controller, "page_built_topic", pageBuiltTopic);
    }

    // --- Idempotency tests ---

    @Test
    void shouldReturnOkForDuplicateMessage() throws Exception {
        String validPayload = "{\"journey\":{\"id\":1,\"steps\":[],\"status\":\"CANDIDATE\"},\"browser\":\"CHROME\",\"accountId\":1,\"auditRecordId\":100,\"mapId\":10}";
        Body body = createValidBody("test-msg-id", validPayload);
        when(idempotencyService.isAlreadyProcessed("test-msg-id", "journey-executor")).thenReturn(true);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("already processed"));
        verify(verifiedJourneyTopic, never()).publish(anyString());
        verify(discardedJourneyTopic, never()).publish(anyString());
        verify(pageBuiltTopic, never()).publish(anyString());
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
    void shouldReturnOkForEmptyData() throws Exception {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-2", "2024-01-01T00:00:00Z", "");
        body.setMessage(msg);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForInvalidBase64Data() throws Exception {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-3", "2024-01-01T00:00:00Z", "not-valid-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-3", "journey-executor")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(verifiedJourneyTopic, never()).publish(anyString());
    }

    @Test
    void shouldReturnOkForInvalidJson() throws Exception {
        String invalidJson = "this is not json";
        Body body = createValidBody("msg-4", invalidJson);
        when(idempotencyService.isAlreadyProcessed("msg-4", "journey-executor")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(verifiedJourneyTopic, never()).publish(anyString());
    }

    @Test
    void shouldReturnOkForMissingJourney() throws Exception {
        String payload = "{\"journey\":null,\"browser\":\"CHROME\",\"accountId\":1,\"auditRecordId\":100,\"mapId\":10}";
        Body body = createValidBody("msg-5", payload);
        when(idempotencyService.isAlreadyProcessed("msg-5", "journey-executor")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForJourneyWithNullId() throws Exception {
        String payload = "{\"journey\":{\"steps\":[]},\"browser\":\"CHROME\",\"accountId\":1,\"auditRecordId\":100,\"mapId\":10}";
        Body body = createValidBody("msg-6", payload);
        when(idempotencyService.isAlreadyProcessed("msg-6", "journey-executor")).thenReturn(false);

        ResponseEntity<String> response = controller.receiveMessage(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // --- Error handling tests ---

    @Test
    void shouldNotCallMarkProcessedOnInvalidPayload() throws Exception {
        Body body = new Body();
        Body.Message msg = body.new Message("msg-7", "2024-01-01T00:00:00Z", "bad-base64!!!");
        body.setMessage(msg);
        when(idempotencyService.isAlreadyProcessed("msg-7", "journey-executor")).thenReturn(false);

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
