package com.looksee.journeyErrors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.looksee.journeyErrors.mapper.Body;
import com.looksee.journeyErrors.services.JourneyService;

@ExtendWith(MockitoExtension.class)
class AuditControllerIdempotencyTest {

    @Mock
    private JourneyService journeyService;

    private AuditController controller;

    @BeforeEach
    void setUp() {
        controller = new AuditController();
        ReflectionTestUtils.setField(controller, "journey_service", journeyService);
    }

    @Test
    void shouldReturnOkForNullBody() throws Exception {
        ResponseEntity<String> response = controller.receiveMessage(null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Empty"));
    }

    @Test
    void shouldReturnOkForEmptyMessageData() throws Exception {
        Body body = new Body();
        Body.Message msg = body.new Message();
        msg.setData("");
        body.setMessage(msg);

        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldRejectDuplicateMessage() throws Exception {
        String uniqueId = "journey-errors-dedup-" + System.nanoTime();
        String payload = "{\"accountId\":1,\"journey\":{\"id\":10,\"status\":\"CANDIDATE\",\"candidateKey\":\"test\"},\"browser\":\"CHROME\",\"auditRecordId\":100,\"mapId\":1}";

        Body body1 = createValidBody(uniqueId, payload);
        // First call - process (may fail on journey processing, but that's OK)
        try {
            controller.receiveMessage(body1);
        } catch (Exception e) {
            // Expected
        }

        // Second call with same messageId
        Body body2 = createValidBody(uniqueId, payload);
        ResponseEntity<String> response = controller.receiveMessage(body2);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Duplicate"));
    }

    @Test
    void shouldReturnOkForInvalidBase64() throws Exception {
        Body body = new Body();
        Body.Message msg = body.new Message();
        msg.setMessageId("test-invalid-base64-" + System.nanoTime());
        msg.setData("!!!not-valid-base64!!!");
        body.setMessage(msg);

        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForInvalidJson() throws Exception {
        String encoded = Base64.getEncoder().encodeToString("not json at all".getBytes(StandardCharsets.UTF_8));
        Body body = new Body();
        Body.Message msg = body.new Message();
        msg.setMessageId("test-invalid-json-" + System.nanoTime());
        msg.setData(encoded);
        body.setMessage(msg);

        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldAllowDifferentMessageIds() throws Exception {
        String payload = "{\"accountId\":1,\"journey\":{\"id\":10,\"status\":\"ERROR\",\"candidateKey\":\"test\"},\"browser\":\"CHROME\",\"auditRecordId\":100,\"mapId\":1}";

        Body body1 = createValidBody("unique-1-" + System.nanoTime(), payload);
        Body body2 = createValidBody("unique-2-" + System.nanoTime(), payload);

        // Both should process (not be rejected as duplicates)
        ResponseEntity<String> response1 = controller.receiveMessage(body1);
        ResponseEntity<String> response2 = controller.receiveMessage(body2);

        // Neither should say "Duplicate"
        assertTrue(!response1.getBody().contains("Duplicate"));
        assertTrue(!response2.getBody().contains("Duplicate"));
    }

    private Body createValidBody(String messageId, String jsonPayload) {
        String encoded = Base64.getEncoder().encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));
        Body body = new Body();
        Body.Message msg = body.new Message();
        msg.setMessageId(messageId);
        msg.setData(encoded);
        body.setMessage(msg);
        return body;
    }
}
