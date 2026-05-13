package com.looksee.auditManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.looksee.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.messaging.idempotency.IdempotencyGuard;
import com.looksee.messaging.observability.PubSubMetrics;
import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.enums.AuditName;
import com.looksee.models.message.PageAuditMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.OutboxPublishingGateway;
import com.looksee.services.PageStateService;

/**
 * Per-service tests for audit-manager after the migration to
 * {@link com.looksee.messaging.web.PubSubAuditController}. Envelope
 * validation, base64/JSON edge cases, idempotency claim/release, and metrics
 * emission are owned by the base class and covered in
 * {@code PubSubAuditControllerTest}; this file only exercises the
 * eligibility + persistence business logic in {@code handle(...)}.
 */
class AuditControllerTest {

	private static final String SERVICE = "audit-manager";

	private AuditController controller;
	private AuditRecordService auditRecordService;
	private PubSubPageAuditPublisherImpl auditRecordTopic;
	private PageStateService pageStateService;
	private IdempotencyGuard idempotencyService;
	private PubSubMetrics pubSubMetrics;
	private OutboxPublishingGateway outboxGateway;

	@BeforeEach
	void setUp() {
		controller = new AuditController();
		auditRecordService = mock(AuditRecordService.class);
		auditRecordTopic = mock(PubSubPageAuditPublisherImpl.class);
		pageStateService = mock(PageStateService.class);
		idempotencyService = mock(IdempotencyGuard.class);
		pubSubMetrics = mock(PubSubMetrics.class);
		outboxGateway = mock(OutboxPublishingGateway.class);

		ReflectionTestUtils.setField(controller, "auditRecordService", auditRecordService);
		ReflectionTestUtils.setField(controller, "auditRecordTopic", auditRecordTopic);
		ReflectionTestUtils.setField(controller, "pageStateService", pageStateService);
		ReflectionTestUtils.setField(controller, "idempotencyService", idempotencyService);
		ReflectionTestUtils.setField(controller, "objectMapper", new ObjectMapper());
		ReflectionTestUtils.setField(controller, "pubSubMetrics", pubSubMetrics);
		ReflectionTestUtils.setField(controller, "outboxGateway", outboxGateway);

		when(idempotencyService.claim(anyString(), anyString())).thenReturn(true);
	}

	@Test
	void shouldSkipWhenPageAlreadyAudited() throws Exception {
		when(auditRecordService.wasPageAlreadyAudited(3L, 2L)).thenReturn(true);
		when(pageStateService.isPageLandable(2L)).thenReturn(true);
		when(pageStateService.findById(2L)).thenReturn(Optional.of(new PageState()));
		when(auditRecordService.findById(3L)).thenReturn(Optional.empty());

		ResponseEntity<String> response = controller.receiveMessage(buildBody("msg-skip", validJson()));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(outboxGateway, never()).enqueue(any(), any(), any());
		verify(auditRecordService, never()).save(any());
	}

	@Test
	void shouldSkipWhenPageIsNotLandable() throws Exception {
		when(auditRecordService.wasPageAlreadyAudited(3L, 2L)).thenReturn(false);
		when(pageStateService.isPageLandable(2L)).thenReturn(false);
		when(pageStateService.findById(2L)).thenReturn(Optional.of(new PageState()));
		when(auditRecordService.findById(3L)).thenReturn(Optional.empty());

		ResponseEntity<String> response = controller.receiveMessage(buildBody("msg-nonlandable", validJson()));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(outboxGateway, never()).enqueue(any(), any(), any());
	}

	@Test
	void shouldSkipWhenPageStateMissing() throws Exception {
		when(auditRecordService.wasPageAlreadyAudited(3L, 2L)).thenReturn(false);
		when(pageStateService.isPageLandable(2L)).thenReturn(true);
		when(pageStateService.findById(2L)).thenReturn(Optional.empty());
		when(auditRecordService.findById(3L)).thenReturn(Optional.empty());

		ResponseEntity<String> response = controller.receiveMessage(buildBody("msg-nostate", validJson()));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(outboxGateway, never()).enqueue(any(), any(), any());
	}

	@Test
	void shouldPublishAuditWhenEligible() throws Exception {
		PageState pageState = new PageState();
		AuditRecord savedRecord = mock(AuditRecord.class);

		when(auditRecordService.wasPageAlreadyAudited(3L, 2L)).thenReturn(false);
		when(pageStateService.isPageLandable(2L)).thenReturn(true);
		when(pageStateService.findById(2L)).thenReturn(Optional.of(pageState));
		when(auditRecordService.findById(3L)).thenReturn(Optional.empty());
		when(auditRecordService.save(any())).thenReturn(savedRecord);
		when(savedRecord.getId()).thenReturn(99L);

		ResponseEntity<String> response = controller.receiveMessage(buildBody("msg-go", validJson()));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(auditRecordService).addPageAuditToDomainAudit(3L, 99L);
		verify(auditRecordService).addPageToAuditRecord(99L, 2L);
		ArgumentCaptor<PageAuditMessage> payloadCaptor = ArgumentCaptor.forClass(PageAuditMessage.class);
		verify(outboxGateway).enqueue(any(), payloadCaptor.capture(), any());
		assertEquals(1L, payloadCaptor.getValue().getAccountId());
		assertEquals(99L, payloadCaptor.getValue().getPageAuditId());
	}

	@Test
	void shouldUseDomainAuditLabelsWhenDomainRecordExists() throws Exception {
		PageState pageState = new PageState();
		PageAuditRecord savedRecord = mock(PageAuditRecord.class);
		DomainAuditRecord domainRecord = mock(DomainAuditRecord.class);
		Set<AuditName> labels = Set.of(AuditName.ALT_TEXT);

		when(auditRecordService.wasPageAlreadyAudited(3L, 2L)).thenReturn(false);
		when(pageStateService.isPageLandable(2L)).thenReturn(true);
		when(pageStateService.findById(2L)).thenReturn(Optional.of(pageState));
		when(auditRecordService.findById(3L)).thenReturn(Optional.of(domainRecord));
		when(domainRecord.getAuditLabels()).thenReturn(labels);
		when(auditRecordService.save(any())).thenReturn(savedRecord);
		when(savedRecord.getId()).thenReturn(22L);

		ResponseEntity<String> response = controller.receiveMessage(buildBody("msg-labels", validJson()));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(domainRecord).getAuditLabels();
		verify(auditRecordService).addPageAuditToDomainAudit(3L, 22L);
	}

	@Test
	void shouldReturnInternalServerErrorWhenOutboxStagingFails() throws Exception {
		// Under the outbox architecture there is no synchronous Pub/Sub publish
		// to fail inside handle(); the analogous failure mode is the outbox
		// gateway raising during the staging write. When that happens the base
		// class must still release the eager idempotency claim so Pub/Sub
		// redelivery can retry the inbound message.
		PageState pageState = new PageState();
		AuditRecord savedRecord = mock(AuditRecord.class);

		when(auditRecordService.wasPageAlreadyAudited(3L, 2L)).thenReturn(false);
		when(pageStateService.isPageLandable(2L)).thenReturn(true);
		when(pageStateService.findById(2L)).thenReturn(Optional.of(pageState));
		when(auditRecordService.findById(3L)).thenReturn(Optional.empty());
		when(auditRecordService.save(any())).thenReturn(savedRecord);
		when(savedRecord.getId()).thenReturn(88L);
		org.mockito.Mockito.doThrow(new RuntimeException("staging failed"))
			.when(outboxGateway)
			.enqueue(any(), any(), any());

		ResponseEntity<String> response = controller.receiveMessage(buildBody("msg-pub-fail", validJson()));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		verify(idempotencyService).release("msg-pub-fail", SERVICE);
	}

	@Test
	void redelivery_doesNotDoubleProcess_thePerIssue83Contract() throws Exception {
		PageState pageState = new PageState();
		AuditRecord savedRecord = mock(AuditRecord.class);

		// Stateful claim: first call wins, every subsequent call sees the row.
		when(idempotencyService.claim("dup-msg", SERVICE))
			.thenReturn(true)
			.thenReturn(false);

		when(auditRecordService.wasPageAlreadyAudited(3L, 2L)).thenReturn(false);
		when(pageStateService.isPageLandable(2L)).thenReturn(true);
		when(pageStateService.findById(2L)).thenReturn(Optional.of(pageState));
		when(auditRecordService.findById(3L)).thenReturn(Optional.empty());
		when(auditRecordService.save(any())).thenReturn(savedRecord);
		when(savedRecord.getId()).thenReturn(42L);

		Body envelope = buildBody("dup-msg", validJson());

		ResponseEntity<String> first = controller.receiveMessage(envelope);
		ResponseEntity<String> second = controller.receiveMessage(envelope);

		assertEquals(HttpStatus.OK, first.getStatusCode());
		assertEquals("ok", first.getBody());
		assertEquals(HttpStatus.OK, second.getStatusCode());
		assertTrue(second.getBody().contains("Duplicate"),
			"second redelivery must short-circuit on duplicate, got: " + second.getBody());

		// The audit must be persisted + published exactly once across redeliveries.
		verify(auditRecordService, times(1)).save(any());
		verify(outboxGateway, times(1)).enqueue(any(), any(), any());
		verify(auditRecordService, times(1)).addPageAuditToDomainAudit(3L, 42L);
	}

	private static String validJson() {
		return "{\"accountId\":1,\"pageId\":2,\"auditRecordId\":3}";
	}

	private static Body buildBody(String messageId, String json) {
		String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
		Body body = new Body();
		Body.Message msg = body.new Message();
		msg.setMessageId(messageId);
		msg.setData(encoded);
		body.setMessage(msg);
		return body;
	}
}
