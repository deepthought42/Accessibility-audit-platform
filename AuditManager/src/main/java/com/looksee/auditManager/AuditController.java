package com.looksee.auditManager;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import com.looksee.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.messaging.observability.TraceContextPropagation;
import com.looksee.messaging.web.PubSubAuditController;
import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.message.PageAuditMessage;
import com.looksee.models.message.PageBuiltMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.OutboxPublishingGateway;
import com.looksee.services.PageStateService;

/**
 * Pub/Sub controller that turns a {@link PageBuiltMessage} into a persisted
 * {@link PageAuditRecord} and forwards a {@link PageAuditMessage} to the
 * page-audit topic. Envelope validation, base64 decode, atomic idempotency
 * claim, trace-context propagation and metrics emission are inherited from
 * {@link PubSubAuditController}; this subclass only owns the per-service
 * eligibility and persistence logic in {@link #handle(PageBuiltMessage)}.
 */
@RestController
public class AuditController extends PubSubAuditController<PageBuiltMessage> {

	private static final Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private AuditRecordService auditRecordService;

	@Autowired
	private PubSubPageAuditPublisherImpl auditRecordTopic;

	@Autowired
	private PageStateService pageStateService;

	@Autowired
	private OutboxPublishingGateway outboxGateway;

	/**
	 * Spring-managed self-reference. Calls to {@link #processInTransaction}
	 * are routed through this proxy so the {@code @Transactional} boundary
	 * is actually applied — a direct {@code this.processInTransaction(...)}
	 * would be self-invocation and bypass the proxy, leaving
	 * {@link OutboxPublishingGateway#enqueue} (which is {@code MANDATORY})
	 * with no active transaction. {@code @Lazy} breaks the obvious
	 * circular-dependency cycle during bean creation.
	 */
	@Autowired
	@Lazy
	private AuditController self;

	@Override
	protected String serviceName() {
		return "audit-manager";
	}

	@Override
	protected String topicName() {
		return "page_built";
	}

	@Override
	protected Class<PageBuiltMessage> payloadType() {
		return PageBuiltMessage.class;
	}

	@Override
	protected void handle(PageBuiltMessage pageBuiltMessage) throws Exception {
		// Delegate through the Spring-managed proxy so the @Transactional
		// boundary on processInTransaction is actually applied. The inherited
		// receiveMessage entry point on PubSubAuditController is itself not
		// transactional, and Spring's proxy-based @Transactional cannot
		// intercept a self-invoked handle() call.
		self.processInTransaction(pageBuiltMessage);
	}

	/**
	 * Transactional core of the audit-manager flow. Eligibility checks and
	 * persistence of the new {@link PageAuditRecord} share a single Neo4j
	 * transaction with the {@link OutboxPublishingGateway#enqueue} call, so
	 * the staged outbox row commits or rolls back with the domain writes.
	 *
	 * <p>Must be {@code public} so the Spring proxy on {@code self} can
	 * intercept the call from {@link #handle(PageBuiltMessage)} — package-
	 * private or protected methods are not visible on the proxy and fall
	 * through to the underlying instance.</p>
	 */
	@Transactional
	public void processInTransaction(PageBuiltMessage pageBuiltMessage) throws Exception {
		Set<AuditName> auditNames = buildAuditNames(pageBuiltMessage.getAuditRecordId());

		boolean alreadyAudited = auditRecordService.wasPageAlreadyAudited(
			pageBuiltMessage.getAuditRecordId(), pageBuiltMessage.getPageId());
		boolean isLandable = pageStateService.isPageLandable(pageBuiltMessage.getPageId());
		Optional<PageState> pageState = pageStateService.findById(pageBuiltMessage.getPageId());

		if (alreadyAudited || !isLandable || pageState.isEmpty()) {
			log.info("Skipping pageId={} (alreadyAudited={}, landable={}, pageStatePresent={})",
				pageBuiltMessage.getPageId(), alreadyAudited, isLandable, pageState.isPresent());
			return;
		}

		createAndPublishAudit(pageBuiltMessage, pageState.get(), auditNames);
	}

	private void createAndPublishAudit(PageBuiltMessage pageBuiltMessage, PageState pageState,
	                                   Set<AuditName> auditNames) throws Exception {
		log.info("Received page for auditing, pageId={}, url={}",
			pageBuiltMessage.getPageId(), pageState.getUrl());

		AuditRecord auditRecord = new PageAuditRecord(
			ExecutionStatus.BUILDING_PAGE,
			new HashSet<>(),
			pageState,
			true,
			auditNames);

		auditRecord = auditRecordService.save(auditRecord);
		auditRecordService.addPageAuditToDomainAudit(pageBuiltMessage.getAuditRecordId(), auditRecord.getId());
		auditRecordService.addPageToAuditRecord(auditRecord.getId(), pageBuiltMessage.getPageId());

		PageAuditMessage auditMessage = new PageAuditMessage(
			pageBuiltMessage.getAccountId(), auditRecord.getId());
		log.info("Staging PageAuditMessage in outbox for pageAuditId={}", auditRecord.getId());
		outboxGateway.enqueue(
			auditRecordTopic.getTopic(),
			auditMessage,
			TraceContextPropagation.currentTraceparent());
	}

	private Set<AuditName> buildAuditNames(long auditRecordId) {
		Optional<AuditRecord> optRecord = auditRecordService.findById(auditRecordId);
		if (optRecord.isPresent() && optRecord.get() instanceof DomainAuditRecord) {
			DomainAuditRecord record = (DomainAuditRecord) optRecord.get();
			Set<AuditName> labels = record.getAuditLabels();
			if (labels != null && !labels.isEmpty()) {
				return labels;
			}
		}
		return defaultAuditNames();
	}

	private Set<AuditName> defaultAuditNames() {
		Set<AuditName> auditNames = new HashSet<>();
		auditNames.add(AuditName.TEXT_BACKGROUND_CONTRAST);
		auditNames.add(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
		auditNames.add(AuditName.LINKS);
		auditNames.add(AuditName.TITLES);
		auditNames.add(AuditName.ENCRYPTED);
		auditNames.add(AuditName.METADATA);
		auditNames.add(AuditName.ALT_TEXT);
		auditNames.add(AuditName.READING_COMPLEXITY);
		auditNames.add(AuditName.PARAGRAPHING);
		auditNames.add(AuditName.IMAGE_COPYRIGHT);
		auditNames.add(AuditName.IMAGE_POLICY);
		return auditNames;
	}
}
