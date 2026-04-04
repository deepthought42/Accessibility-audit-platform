package com.looksee.auditManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.looksee.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.enums.AuditName;
import com.looksee.services.AuditRecordService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.PageStateService;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

	@Mock
	private AuditRecordService auditRecordService;

	@Mock
	private PubSubPageAuditPublisherImpl auditRecordTopic;

	@Mock
	private PageStateService pageStateService;

	@Mock
	private IdempotencyService idempotencyService;

	private AuditController controller;

	@BeforeEach
	void setup() {
		controller = new AuditController(auditRecordService, auditRecordTopic, pageStateService, idempotencyService);
	}

	@Test
	void shouldReturnBadRequestWhenBodyIsMissing() {
		ResponseEntity<String> response = controller.receiveMessage(null);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Acknowledged invalid Pub/Sub payload", response.getBody());
	}

	@Test
	void shouldReturnBadRequestWhenMessageIsNull() {
		Body body = mock(Body.class);
		when(body.getMessage()).thenReturn(null);

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Acknowledged invalid Pub/Sub payload", response.getBody());
	}

	@Test
	void shouldReturnBadRequestWhenMessageDataIsNull() {
		Body body = mock(Body.class);
		Body.Message message = mock(Body.Message.class);
		when(body.getMessage()).thenReturn(message);
		when(message.getData()).thenReturn(null);

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Acknowledged invalid Pub/Sub payload", response.getBody());
	}

	@Test
	void shouldReturnBadRequestWhenMessageDataIsMissing() {
		Body body = mock(Body.class);
		Body.Message message = mock(Body.Message.class);
		when(body.getMessage()).thenReturn(message);
		when(message.getData()).thenReturn("");

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Acknowledged invalid Pub/Sub payload", response.getBody());
	}

	@Test
	void shouldReturnBadRequestWhenMessageDataIsNotBase64() throws Exception {
		Body body = mockBodyWithData("not-base64");

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Acknowledged invalid message encoding", response.getBody());
		verify(auditRecordTopic, never()).publish(any());
	}

	@Test
	void shouldReturnBadRequestWhenDecodedPayloadIsNotJson() throws Exception {
		String encoded = Base64.getEncoder().encodeToString("invalid-json".getBytes(StandardCharsets.UTF_8));
		Body body = mockBodyWithData(encoded);

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals("Acknowledged invalid message format", response.getBody());
		verify(auditRecordTopic, never()).publish(any());
	}

	@Test
	void shouldSkipWhenPageAlreadyAudited() throws Exception {
		Body body = createValidBody();

		when(auditRecordService.wasPageAlreadyAudited(3L, 2L)).thenReturn(true);
		when(pageStateService.isPageLandable(2L)).thenReturn(true);
		when(pageStateService.findById(2L)).thenReturn(Optional.of(new PageState()));
		when(auditRecordService.findById(3L)).thenReturn(Optional.empty());

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(auditRecordTopic, never()).publish(any());
	}

	@Test
	void shouldSkipWhenPageIsNotLandable() throws Exception {
		Body body = createValidBody();

		when(auditRecordService.wasPageAlreadyAudited(3L, 2L)).thenReturn(false);
		when(pageStateService.isPageLandable(2L)).thenReturn(false);
		when(pageStateService.findById(2L)).thenReturn(Optional.of(new PageState()));
		when(auditRecordService.findById(3L)).thenReturn(Optional.empty());

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(auditRecordTopic, never()).publish(any());
	}

	@Test
	void shouldSkipWhenPageStateMissing() throws Exception {
		Body body = createValidBody();

		when(auditRecordService.wasPageAlreadyAudited(3L, 2L)).thenReturn(false);
		when(pageStateService.isPageLandable(2L)).thenReturn(true);
		when(pageStateService.findById(2L)).thenReturn(Optional.empty());
		when(auditRecordService.findById(3L)).thenReturn(Optional.empty());

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(auditRecordTopic, never()).publish(any());
	}

	@Test
	void shouldPublishAuditWhenEligible() throws Exception {
		Body body = createValidBody();
		PageState pageState = new PageState();
		AuditRecord savedRecord = mock(AuditRecord.class);

		when(auditRecordService.wasPageAlreadyAudited(3L, 2L)).thenReturn(false);
		when(pageStateService.isPageLandable(2L)).thenReturn(true);
		when(pageStateService.findById(2L)).thenReturn(Optional.of(pageState));
		when(auditRecordService.findById(3L)).thenReturn(Optional.empty());
		when(auditRecordService.save(any())).thenReturn(savedRecord);
		when(savedRecord.getId()).thenReturn(99L);

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(auditRecordService).addPageAuditToDomainAudit(3L, 99L);
		verify(auditRecordService).addPageToAuditRecord(99L, 2L);
		ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
		verify(auditRecordTopic).publish(payloadCaptor.capture());
		assertTrue(payloadCaptor.getValue().contains("\"accountId\":1"));
		assertTrue(payloadCaptor.getValue().contains("\"pageAuditId\":99"));
	}

	@Test
	void shouldUseDomainAuditLabelsWhenDomainRecordExists() throws Exception {
		Body body = createValidBody();
		PageState pageState = new PageState();
		AuditRecord savedRecord = mock(AuditRecord.class);
		DomainAuditRecord domainRecord = mock(DomainAuditRecord.class);
		Set<AuditName> labels = Set.of(AuditName.ALT_TEXT);

		when(auditRecordService.wasPageAlreadyAudited(3L, 2L)).thenReturn(false);
		when(pageStateService.isPageLandable(2L)).thenReturn(true);
		when(pageStateService.findById(2L)).thenReturn(Optional.of(pageState));
		when(auditRecordService.findById(3L)).thenReturn(Optional.of(domainRecord));
		when(domainRecord.getAuditLabels()).thenReturn(labels);
		when(auditRecordService.save(any())).thenReturn(savedRecord);
		when(savedRecord.getId()).thenReturn(22L);

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(auditRecordService).save(any());
		verify(domainRecord).getAuditLabels();
		verify(auditRecordService).addPageAuditToDomainAudit(3L, 22L);
	}

	@Test
	void shouldReturnInternalServerErrorWhenPublishingFails() throws Exception {
		Body body = createValidBody();
		PageState pageState = new PageState();
		AuditRecord savedRecord = mock(AuditRecord.class);

		when(auditRecordService.wasPageAlreadyAudited(3L, 2L)).thenReturn(false);
		when(pageStateService.isPageLandable(2L)).thenReturn(true);
		when(pageStateService.findById(2L)).thenReturn(Optional.of(pageState));
		when(auditRecordService.findById(3L)).thenReturn(Optional.empty());
		when(auditRecordService.save(any())).thenReturn(savedRecord);
		when(savedRecord.getId()).thenReturn(88L);
		doExecutionFailure();

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals("Failed to process message", response.getBody());
	}

	@Test
	void shouldReturnInternalServerErrorWhenInterrupted() throws Exception {
		Body body = createValidBody();
		PageState pageState = new PageState();
		AuditRecord savedRecord = mock(AuditRecord.class);

		when(auditRecordService.wasPageAlreadyAudited(3L, 2L)).thenReturn(false);
		when(pageStateService.isPageLandable(2L)).thenReturn(true);
		when(pageStateService.findById(2L)).thenReturn(Optional.of(pageState));
		when(auditRecordService.findById(3L)).thenReturn(Optional.empty());
		when(auditRecordService.save(any())).thenReturn(savedRecord);
		when(savedRecord.getId()).thenReturn(77L);
		org.mockito.Mockito.doThrow(new InterruptedException("stop"))
			.when(auditRecordTopic)
			.publish(any(String.class));

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals("Audit processing interrupted", response.getBody());
	}

	@Test
	void shouldReturnInternalServerErrorForUnexpectedException() {
		Body body = createValidBody();

		when(auditRecordService.findById(3L)).thenThrow(new RuntimeException("boom"));

		ResponseEntity<String> response = controller.receiveMessage(body);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertEquals("Failed to process message", response.getBody());
	}

	private void doExecutionFailure() throws Exception {
		org.mockito.Mockito.doThrow(new ExecutionException(new RuntimeException("pubsub")))
			.when(auditRecordTopic)
			.publish(any(String.class));
	}

	private Body createValidBody() {
		String payload = "{\"accountId\":1,\"pageId\":2,\"auditRecordId\":3}";
		String encoded = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
		return mockBodyWithData(encoded);
	}

	private Body mockBodyWithData(String data) {
		Body body = mock(Body.class);
		Body.Message message = mock(Body.Message.class);
		when(body.getMessage()).thenReturn(message);
		when(message.getData()).thenReturn(data);
		return body;
	}
}
