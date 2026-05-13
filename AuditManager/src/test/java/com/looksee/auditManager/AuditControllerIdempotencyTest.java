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
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import com.looksee.services.AuditRecordService;
import com.looksee.services.OutboxPublishingGateway;
import com.looksee.services.PageStateService;

/**
 * Idempotency-focused tests for {@link AuditController}. The atomic
 * {@code claim()} call lives in the {@link com.looksee.messaging.web.PubSubAuditController}
 * base class, so these cases assert the contract from the audit-manager side:
 * a duplicate claim returns 200 without doing any work, and the success path
 * does not hit {@code release()}.
 */
class AuditControllerIdempotencyTest {

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
	}

	@Test
	void duplicateClaim_shortCircuits_returns200() throws Exception {
		when(idempotencyService.claim("dup-msg", SERVICE)).thenReturn(false);

		ResponseEntity<String> response = controller.receiveMessage(
			buildBody("dup-msg", "{\"accountId\":1,\"pageId\":10,\"auditRecordId\":100}"));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.getBody().contains("Duplicate"));
		verify(outboxGateway, never()).enqueue(any(), any(), any());
		verify(auditRecordService, never()).save(any());
		verify(idempotencyService, never()).release(anyString(), anyString());
	}

	@Test
	void successPath_doesNotReleaseTheClaim() throws Exception {
		when(idempotencyService.claim("ok-msg", SERVICE)).thenReturn(true);

		DomainAuditRecord domainRecord = mock(DomainAuditRecord.class);
		HashSet<AuditName> labels = new HashSet<>();
		labels.add(AuditName.ALT_TEXT);
		when(domainRecord.getAuditLabels()).thenReturn(labels);
		when(auditRecordService.findById(100L)).thenReturn(Optional.of(domainRecord));
		when(auditRecordService.wasPageAlreadyAudited(100L, 10L)).thenReturn(false);
		when(pageStateService.isPageLandable(10L)).thenReturn(true);
		when(pageStateService.findById(10L)).thenReturn(Optional.of(new PageState()));

		PageAuditRecord savedRecord = mock(PageAuditRecord.class);
		when(savedRecord.getId()).thenReturn(200L);
		when(auditRecordService.save(any(AuditRecord.class))).thenReturn(savedRecord);

		ResponseEntity<String> response = controller.receiveMessage(
			buildBody("ok-msg", "{\"accountId\":1,\"pageId\":10,\"auditRecordId\":100}"));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(outboxGateway, times(1)).enqueue(any(), any(), any());
		verify(idempotencyService, never()).release(anyString(), anyString());
	}

	@Test
	void skippedEligibility_doesNotReleaseTheClaim() throws Exception {
		when(idempotencyService.claim("skip-msg", SERVICE)).thenReturn(true);
		when(auditRecordService.findById(100L)).thenReturn(Optional.empty());
		when(auditRecordService.wasPageAlreadyAudited(100L, 10L)).thenReturn(true);
		when(pageStateService.isPageLandable(10L)).thenReturn(true);
		when(pageStateService.findById(10L)).thenReturn(Optional.of(new PageState()));

		ResponseEntity<String> response = controller.receiveMessage(
			buildBody("skip-msg", "{\"accountId\":1,\"pageId\":10,\"auditRecordId\":100}"));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		verify(outboxGateway, never()).enqueue(any(), any(), any());
		verify(idempotencyService, never()).release(anyString(), anyString());
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
