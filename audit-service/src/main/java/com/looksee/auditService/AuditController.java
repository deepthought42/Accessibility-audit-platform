package com.looksee.auditService;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.looksee.messaging.web.PubSubAuditController;
import com.looksee.models.Domain;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.dto.AuditUpdateDto;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.DiscardedJourneyMessage;
import com.looksee.models.message.JourneyCandidateMessage;
import com.looksee.models.message.Message;
import com.looksee.models.message.PageAuditProgressMessage;
import com.looksee.models.message.VerifiedJourneyMessage;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DomainService;
import com.looksee.services.MessageBroadcaster;
import com.looksee.services.PageStateService;
import com.looksee.utils.AuditUtils;

/**
 * Pub/Sub controller that broadcasts real-time audit progress updates to
 * connected clients. Envelope validation, base64 decode, atomic idempotency
 * claim, trace-context propagation, metrics emission, and the
 * poison-vs-transient distinction are inherited from
 * {@link PubSubAuditController}; this subclass owns only the per-message-type
 * dispatch and broadcast logic in {@link #processInTransaction(Message)}.
 *
 * <p>Dispatch is driven by Jackson polymorphism configured on the
 * service-local {@link com.fasterxml.jackson.databind.ObjectMapper}
 * bean — see {@link Application#auditServiceObjectMapper()} — which routes
 * each envelope to the correct concrete {@link Message} subclass via the
 * {@code messageType} discriminator before {@code handle(...)} is invoked.
 */
@RestController
public class AuditController extends PubSubAuditController<Message> {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private AuditRecordService audit_record_service;

	@Autowired
	private AccountService account_service;

	@Autowired
	private DomainService domain_service;

	@Autowired
	private PageStateService page_state_service;

	@Autowired
	private MessageBroadcaster messageBroadcaster;

	/**
	 * Spring-managed self-reference. Routes {@link #processInTransaction}
	 * calls through the proxy so the {@code @Transactional} boundary is
	 * actually applied — a direct {@code this.processInTransaction(...)}
	 * would be self-invocation and bypass the proxy.
	 */
	@Autowired
	@Lazy
	private AuditController self;

	@Override
	protected String serviceName() {
		return "audit-service";
	}

	@Override
	protected String topicName() {
		return "audit_update";
	}

	@Override
	protected Class<Message> payloadType() {
		return Message.class;
	}

	@Override
	protected void handle(Message payload) throws Exception {
		self.processInTransaction(payload);
	}

	/**
	 * Transactional dispatch core. {@code public} so the Spring proxy on
	 * {@link #self} can intercept the call from {@link #handle(Message)};
	 * package-private or protected methods are not visible on the proxy.
	 */
	@Transactional
	public void processInTransaction(Message payload) throws Exception {
		if (payload instanceof AuditProgressUpdate auditUpdate) {
			dispatchAuditProgressUpdate(auditUpdate);
		} else if (payload instanceof PageAuditProgressMessage pageProgress) {
			dispatchPageAuditProgress(pageProgress);
		} else if (payload instanceof JourneyCandidateMessage journeyCandidate) {
			dispatchJourneyCandidate(journeyCandidate);
		} else if (payload instanceof VerifiedJourneyMessage verified) {
			dispatchVerifiedJourney(verified);
		} else if (payload instanceof DiscardedJourneyMessage discarded) {
			dispatchDiscardedJourney(discarded);
		} else {
			log.warn("Unhandled Message subtype: {}",
				payload == null ? "null" : payload.getClass().getSimpleName());
		}
	}

	private void dispatchAuditProgressUpdate(AuditProgressUpdate audit_msg) throws JsonProcessingException {
		Optional<AuditRecord> audit_record = audit_record_service.findById(audit_msg.getPageAuditId());

		if(audit_record.isPresent()) {
			AuditUpdateDto audit_update = buildPageAuditUpdatedDto(audit_msg.getPageAuditId());
			messageBroadcaster.sendAuditUpdate(audit_record.get().getId()+"", audit_update);

			Optional<DomainAuditRecord> domain_audit_record_opt = audit_record_service.getDomainAuditRecordForPageRecord(audit_msg.getPageAuditId());
			if(domain_audit_record_opt.isPresent()){

				audit_update = buildDomainAuditRecordDTO(domain_audit_record_opt.get().getId());
				messageBroadcaster.sendAuditUpdate(domain_audit_record_opt.get().getId()+"", audit_update);

				if( ExecutionStatus.COMPLETE.equals(audit_update.getStatus())) {
					account_service.findById(audit_msg.getAccountId()).ifPresent(account -> {
						Domain domain = domain_service.findByAuditRecord(domain_audit_record_opt.get().getId());
						log.warn("sending email to user = " + account.getEmail() + " for domain=" + domain.getUrl());
					});
				}
			}
			else{
				if( ExecutionStatus.COMPLETE.equals(audit_update.getStatus())) {
					PageState page_state = audit_record_service.getPageStateForAuditRecord(audit_msg.getPageAuditId());
					account_service.findById(audit_msg.getAccountId()).ifPresent(account -> {
						log.warn("sending email to user = " + account.getEmail() + " for page=" + (page_state != null ? page_state.getUrl() : "unknown"));
					});
				}
			}
		}
		else {
			log.warn("Unknown record type found");
		}
	}

	private void dispatchPageAuditProgress(PageAuditProgressMessage audit_msg) throws JsonProcessingException {
		Optional<AuditRecord> auditRecordOpt = audit_record_service.findById(audit_msg.getPageAuditId());
		if (auditRecordOpt.isEmpty() || !(auditRecordOpt.get() instanceof PageAuditRecord)) {
			return;
		}
		PageAuditRecord audit_record = (PageAuditRecord) auditRecordOpt.get();

		Optional<DomainAuditRecord> domain_audit = audit_record_service.getDomainAuditRecordForPageRecord(audit_record.getId());

		if(domain_audit.isPresent()) {
			AuditUpdateDto audit_update = buildDomainAuditRecordDTO(domain_audit.get().getId());
			messageBroadcaster.sendAuditUpdate(domain_audit.get().getId()+"", audit_update);
		}
		else {
			AuditUpdateDto audit_update = buildPageAuditUpdatedDto(audit_record.getId());
			messageBroadcaster.sendAuditUpdate(audit_record.getId()+"", audit_update);
		}

		log.warn("successfully sent update for single page audit");
	}

	private void dispatchJourneyCandidate(JourneyCandidateMessage journey_candidate_msg) throws JsonProcessingException {
		broadcastDomainUpdateIfPresent(journey_candidate_msg.getAuditRecordId(), "JourneyCandidateMessage");
	}

	private void dispatchVerifiedJourney(VerifiedJourneyMessage verified_journey_msg) throws JsonProcessingException {
		broadcastDomainUpdateIfPresent(verified_journey_msg.getAuditRecordId(), "VerifiedJourneyMessage");
	}

	private void dispatchDiscardedJourney(DiscardedJourneyMessage discarded_journey_msg) throws JsonProcessingException {
		log.warn("DiscardedJourneyMessage message deserialized");
		broadcastDomainUpdateIfPresent(discarded_journey_msg.getAuditRecordId(), "DiscardedJourneyMessage");
	}

	/**
	 * Builds and broadcasts a domain-level audit update for the given audit
	 * record id. If the record is missing or is not a {@link DomainAuditRecord}
	 * (e.g., it was deleted, or the message references the wrong id), this
	 * silently acknowledges with a log line — a missing record is a
	 * non-retryable condition, so escalating it to HTTP 500 would just trigger
	 * Pub/Sub redelivery up to {@code max_delivery_attempts=10} before
	 * eventually dead-lettering. Same shape as the guard in
	 * {@link #dispatchPageAuditProgress}.
	 */
	private void broadcastDomainUpdateIfPresent(long auditRecordId, String messageType) throws JsonProcessingException {
		Optional<AuditRecord> auditRecordOpt = audit_record_service.findById(auditRecordId);
		if (auditRecordOpt.isEmpty() || !(auditRecordOpt.get() instanceof DomainAuditRecord)) {
			log.warn("Skipping {} for auditRecordId={} — record missing or not a DomainAuditRecord",
				messageType, auditRecordId);
			return;
		}
		AuditUpdateDto audit_update = buildDomainAuditRecordDTO(auditRecordId);
		messageBroadcaster.sendAuditUpdate(auditRecordId + "", audit_update);
	}

	/**
	 * Builds an {@link AuditUpdateDto} representing the current progress and scores
	 * for a page-level audit.
	 *
	 * <p><b>Precondition:</b> {@code page_audit_id} must be a positive ID
	 * corresponding to an existing page audit record.</p>
	 *
	 * <p><b>Postcondition:</b> returns a non-null {@link AuditUpdateDto} with
	 * {@link AuditLevel#PAGE} level, scores in the range [0, 100], progress
	 * values in [0.0, 1.0], and a non-null execution status.</p>
	 *
	 * @param page_audit_id the positive database ID of the page audit record
	 * @return a fully populated page-level audit update DTO; never null
	 * @throws IllegalArgumentException if {@code page_audit_id} is not positive
	 */
	private AuditUpdateDto buildPageAuditUpdatedDto(long page_audit_id) {
		assert page_audit_id > 0 : "Precondition failed: page_audit_id must be positive, got " + page_audit_id;

		Set<Audit> audits = audit_record_service.getAllAudits(page_audit_id);
		Set<AuditName> audit_labels = new HashSet<AuditName>();
		audit_labels.add(AuditName.TEXT_BACKGROUND_CONTRAST);
		audit_labels.add(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
		audit_labels.add(AuditName.TITLES);
		audit_labels.add(AuditName.IMAGE_COPYRIGHT);
		audit_labels.add(AuditName.IMAGE_POLICY);
		audit_labels.add(AuditName.LINKS);
		audit_labels.add(AuditName.ALT_TEXT);
		audit_labels.add(AuditName.METADATA);
		audit_labels.add(AuditName.READING_COMPLEXITY);
		audit_labels.add(AuditName.PARAGRAPHING);
		audit_labels.add(AuditName.ENCRYPTED);
		double visual_design_progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS,
																1,
																audits,
																AuditUtils.getAuditLabels(AuditCategory.AESTHETICS,
																audit_labels));

		double content_progress = AuditUtils.calculateProgress(AuditCategory.CONTENT,
																1,
																audits,
																audit_labels);

		double info_architecture_progress = AuditUtils.calculateProgress(AuditCategory.INFORMATION_ARCHITECTURE,
																		1,
																		audits,
																		audit_labels);

		double content_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.CONTENT);
		double info_architecture_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.INFORMATION_ARCHITECTURE);
		double visual_design_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.AESTHETICS);
		double a11y_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.ACCESSIBILITY);

		double data_extraction_progress = getPageDataExtractionProgress(page_audit_id);
		String message = "";
		if(data_extraction_progress < 0.5) {
			message = "Setting up browser";
		}
		else if(data_extraction_progress < 0.6) {
			message = "Analyzing elements";
		}

		ExecutionStatus execution_status = ExecutionStatus.UNKNOWN;
		if(visual_design_progress < 1 || content_progress < 1 || info_architecture_progress < 1) {
			execution_status = ExecutionStatus.IN_PROGRESS;
		}
		else {
			execution_status = ExecutionStatus.COMPLETE;
		}

		AuditUpdateDto result = new AuditUpdateDto( page_audit_id,
									AuditLevel.PAGE,
									content_score,
									content_progress,
									info_architecture_score,
									info_architecture_progress,
									a11y_score,
									visual_design_score,
									visual_design_progress,
									data_extraction_progress,
									message,
									execution_status);

		assert result != null : "Postcondition failed: result must not be null";
		assert result.getStatus() != null : "Postcondition failed: execution status must not be null";
		return result;
	}

	/**
	 * Builds an {@link AuditUpdateDto} representing the current progress and scores
	 * for a domain-level audit by aggregating results across all child page audits.
	 *
	 * <p><b>Precondition:</b> {@code audit_record_id} must be a positive ID that
	 * resolves to an existing {@link DomainAuditRecord}.</p>
	 *
	 * <p><b>Postcondition:</b> returns a non-null {@link AuditUpdateDto} with
	 * {@link AuditLevel#DOMAIN} level, scores in the range [0, 100], progress
	 * values in [0.0, 1.0], and a non-null execution status.</p>
	 *
	 * @param audit_record_id the positive database ID of the domain audit record
	 * @return a fully populated domain-level audit update DTO; never null
	 * @throws IllegalArgumentException if {@code audit_record_id} is not positive or
	 *         does not correspond to a {@link DomainAuditRecord}
	 */
	private AuditUpdateDto buildDomainAuditRecordDTO(long audit_record_id) {
		assert audit_record_id > 0 : "Precondition failed: audit_record_id must be positive, got " + audit_record_id;

		Optional<AuditRecord> auditRecordOpt = audit_record_service.findById(audit_record_id);
		if (auditRecordOpt.isEmpty() || !(auditRecordOpt.get() instanceof DomainAuditRecord)) {
			throw new IllegalArgumentException("Domain audit record not found for id=" + audit_record_id);
		}
		DomainAuditRecord domain_audit = (DomainAuditRecord) auditRecordOpt.get();
	    Set<PageAuditRecord> page_audits = audit_record_service.getAllPageAudits(domain_audit.getId());
	    log.warn("total page audits found = "+page_audits.size());
	    int total_pages = page_audits.size();
	    Set<AuditName> audit_labels = domain_audit.getAuditLabels();

	    Set<Audit> audits = new HashSet<Audit>();
	    for(AuditRecord page_audit: page_audits) {
	    	audits.addAll(audit_record_service.getAllAuditsForPageAuditRecord(page_audit.getId()));
	    }

	    //calculate percentage of audits that are currently complete for each category
		double visual_design_progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS, total_pages, audits, audit_labels);
		double content_progress = AuditUtils.calculateProgress(AuditCategory.CONTENT, total_pages, audits, audit_labels);
		double info_architecture_progress = AuditUtils.calculateProgress(AuditCategory.INFORMATION_ARCHITECTURE, total_pages, audits, audit_labels);

		double data_extraction_progress = getDomainDataExtractionProgress(domain_audit);

		double content_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.CONTENT);
		double info_architecture_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.INFORMATION_ARCHITECTURE);
		double visual_design_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.AESTHETICS);
		double a11y_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.ACCESSIBILITY);

		ExecutionStatus execution_status = ExecutionStatus.UNKNOWN;
		if(visual_design_progress < 1 || content_progress < 1 || info_architecture_progress < 1 || data_extraction_progress < 1) {
			execution_status = ExecutionStatus.IN_PROGRESS;
		}
		else {
			execution_status = ExecutionStatus.COMPLETE;
		}

		String message = "";

		AuditUpdateDto result = new AuditUpdateDto( audit_record_id,
									AuditLevel.DOMAIN,
									content_score,
									content_progress,
									info_architecture_score,
									info_architecture_progress,
									a11y_score,
									visual_design_score,
									visual_design_progress,
									data_extraction_progress,
									message,
									execution_status);

		assert result != null : "Postcondition failed: result must not be null";
		assert result.getStatus() != null : "Postcondition failed: execution status must not be null";
		return result;
	}

	/**
	 * Calculates the data extraction progress for a domain audit based on the
	 * ratio of non-candidate journeys to total journeys.
	 *
	 * <p><b>Precondition:</b> {@code domain_audit} must not be null.</p>
	 *
	 * <p><b>Postcondition:</b> the returned value is in the range [0.0, 1.0].
	 * Returns {@code 0.01} when there are 0 or 1 total journeys (indicating
	 * that journey discovery has not yet begun).</p>
	 *
	 * @param domain_audit the domain audit record to calculate progress for; must not be null
	 * @return a progress value between 0.0 and 1.0 (inclusive)
	 * @throws AssertionError if {@code domain_audit} is null (when assertions are enabled)
	 */
	private double getDomainDataExtractionProgress(DomainAuditRecord domain_audit) {
		assert domain_audit != null : "Precondition failed: domain_audit must not be null";

		int candidate_count = audit_record_service.getNumberOfJourneysWithStatus(domain_audit.getId(), JourneyStatus.CANDIDATE);
		int total_journeys = audit_record_service.getNumberOfJourneys(domain_audit.getId());

		if(total_journeys <= 1) {
			return 0.01;
		}

		double progress = (double)(total_journeys - candidate_count) / (double)total_journeys;

		assert progress >= 0.0 && progress <= 1.0
			: "Postcondition failed: progress must be in [0.0, 1.0], got " + progress;
		return progress;
	}

	/**
	 * Calculates the data extraction progress for a page audit based on the
	 * availability of audits, the associated page, and its element count.
	 *
	 * <p><b>Precondition:</b> {@code audit_record_id} must be a positive ID
	 * corresponding to an existing audit record.</p>
	 *
	 * <p><b>Postcondition:</b> the returned value is in the range [0.0, 1.0].</p>
	 *
	 * @param audit_record_id the positive database ID of the audit record
	 * @return a progress value between 0.0 and 1.0 (inclusive)
	 * @throws IllegalArgumentException if {@code audit_record_id} is not positive
	 */
	private double getPageDataExtractionProgress(long audit_record_id) {
		assert audit_record_id > 0 : "Precondition failed: audit_record_id must be positive, got " + audit_record_id;
		double milestone_count = 0;

		PageState page = audit_record_service.findPage(audit_record_id);

		int audit_count = audit_record_service.getAllAudits(audit_record_id).size();
		//if the audit_record has audits return 1
		if(audit_count > 0) {
			return 1.0;
		}

		//if audit_record has page associated with it add 1 point
		if(page != null) {
			milestone_count += 1;
		}
		else {
			return 0.0;
		}

		int element_count = page_state_service.getElementStateCount(page.getId());

		int max_elements = 1000;
		if(element_count > 0) {
			if(element_count > max_elements) {
				max_elements = element_count;
			}
			milestone_count += element_count / (double)max_elements;
		}

		double progress = milestone_count / 2.0;

		assert progress >= 0.0 && progress <= 1.0
			: "Postcondition failed: progress must be in [0.0, 1.0], got " + progress;
		return progress;
	}
}
