package com.looksee.pageBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

import com.looksee.pageBuilder.gcp.PubSubErrorPublisherImpl;
import com.looksee.pageBuilder.gcp.PubSubJourneyVerifiedPublisherImpl;
import com.looksee.pageBuilder.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.pageBuilder.gcp.PubSubPageCreatedPublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.services.BrowserService;
import com.looksee.services.ElementStateService;
import com.looksee.services.PageStateService;

@ExtendWith(MockitoExtension.class)
class AuditControllerIdempotencyTest {

    @Mock private BrowserService browserService;
    @Mock private PageStateService pageStateService;
    @Mock private ElementStateService elementStateService;
    @Mock private PubSubErrorPublisherImpl errorPublisher;
    @Mock private PubSubJourneyVerifiedPublisherImpl journeyVerifiedPublisher;
    @Mock private PubSubPageCreatedPublisherImpl pageCreatedPublisher;
    @Mock private PubSubPageAuditPublisherImpl pageAuditPublisher;

    private AuditController controller;

    @BeforeEach
    void setUp() {
        controller = new AuditController();
        ReflectionTestUtils.setField(controller, "browser_service", browserService);
        ReflectionTestUtils.setField(controller, "page_state_service", pageStateService);
        ReflectionTestUtils.setField(controller, "element_state_service", elementStateService);
        ReflectionTestUtils.setField(controller, "pubSubErrorPublisherImpl", errorPublisher);
        ReflectionTestUtils.setField(controller, "pubSubJourneyVerifiedPublisherImpl", journeyVerifiedPublisher);
        ReflectionTestUtils.setField(controller, "pubSubPageCreatedPublisherImpl", pageCreatedPublisher);
        ReflectionTestUtils.setField(controller, "audit_record_topic", pageAuditPublisher);
    }

    @Test
    void shouldReturnOkForNullBody() throws Exception {
        ResponseEntity<String> response = controller.receiveMessage(null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
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
        String uniqueId = "dedup-test-" + System.nanoTime();
        Body body1 = createValidBody(uniqueId, "{\"accountId\":1,\"pageId\":100,\"auditRecordId\":200}");

        // First call should process (may fail on missing page, but that's OK - we're testing dedup)
        try {
            controller.receiveMessage(body1);
        } catch (Exception e) {
            // Expected - we don't have full mock setup for page processing
        }

        // Second call with same messageId should return duplicate
        Body body2 = createValidBody(uniqueId, "{\"accountId\":1,\"pageId\":100,\"auditRecordId\":200}");
        ResponseEntity<String> response = controller.receiveMessage(body2);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Duplicate"));
    }

    @Test
    void shouldReturnOkForInvalidBase64() throws Exception {
        Body body = new Body();
        Body.Message msg = body.new Message();
        msg.setMessageId("test-invalid-base64");
        msg.setData("not-valid-base64!!!");
        body.setMessage(msg);

        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldReturnOkForInvalidJson() throws Exception {
        String encoded = Base64.getEncoder().encodeToString("not json".getBytes(StandardCharsets.UTF_8));
        Body body = new Body();
        Body.Message msg = body.new Message();
        msg.setMessageId("test-invalid-json");
        msg.setData(encoded);
        body.setMessage(msg);

        ResponseEntity<String> response = controller.receiveMessage(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
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
