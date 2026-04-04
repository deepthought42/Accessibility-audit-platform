package com.looksee.auditService;

import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.looksee.mapper.Body;
import com.looksee.models.config.JacksonConfig;
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
import com.looksee.models.message.PageAuditProgressMessage;
import com.looksee.models.message.VerifiedJourneyMessage;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.DomainService;
import com.looksee.services.MessageBroadcaster;
import com.looksee.services.PageStateService;
import com.looksee.utils.AuditUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * REST controller that receives audit progress messages from a message broker
 * (via GCP Pub/Sub push subscriptions) and broadcasts real-time audit updates
 * to connected clients.
 *
 * <p><b>Class invariant:</b> all {@code @Autowired} service dependencies are
 * non-null after Spring context initialization.</p>
 */
@RestController
public class AuditController {
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

	@Autowired
	private IdempotencyService idempotencyService;

	/**
	 * Receives a Pub/Sub push message, deserializes it into one of the supported
	 * message types, and broadcasts an audit progress update to connected clients.
	 *
	 * <p><b>Preconditions:</b></p>
	 * <ul>
	 *   <li>{@code body} must be non-null with a non-null {@code message} containing
	 *       non-null, valid Base64-encoded {@code data}.</li>
	 * </ul>
	 *
	 * <p><b>Postconditions:</b></p>
	 * <ul>
	 *   <li>Returns {@code 200 OK} when the message is successfully deserialized and
	 *       the audit update is broadcast.</li>
	 *   <li>Returns {@code 400 Bad Request} when preconditions are violated or the
	 *       message cannot be deserialized into any supported type.</li>
	 * </ul>
	 *
	 * <p>Supported message types (tried in order):
	 * {@link AuditProgressUpdate}, {@link PageAuditProgressMessage},
	 * {@link JourneyCandidateMessage}, {@link VerifiedJourneyMessage},
	 * {@link DiscardedJourneyMessage}.</p>
	 *
	 * @param body the Pub/Sub push message wrapper; must not be null
	 * @return a response entity indicating success or failure
	 */
	@Operation(summary = "Receive audit progress message", description = "Receives a message from the message broker and processes audit progress updates")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Message processed successfully"),
		@ApiResponse(responseCode = "400", description = "Bad request - invalid message format"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@Transactional
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) {
		// Precondition: body, message, and data must all be non-null
		if (body == null || body.getMessage() == null || body.getMessage().getData() == null) {
			log.warn("Invalid message payload received");
			return new ResponseEntity<String>("Invalid message payload", HttpStatus.BAD_REQUEST);
		}

		if (idempotencyService.isAlreadyProcessed(body.getMessage().getMessageId(), "audit-service")) {
			return ResponseEntity.ok("Duplicate message, already processed");
		}

		Body.Message message = body.getMessage();
		String data = message.getData();
		String target = "";
		if (!data.isEmpty()) {
			try {
				target = new String(Base64.getDecoder().decode(data));
			} catch (IllegalArgumentException e) {
				log.warn("Invalid base64 payload received, acknowledging to prevent infinite retries", e);
				return new ResponseEntity<String>("Invalid message payload", HttpStatus.OK);
			}
		}

		try {
			// First, read the messageType from the payload
			JsonNode rootNode = JacksonConfig.mapper().readTree(target);
			String messageType = rootNode.has("messageType") ? rootNode.get("messageType").asText() : null;

			ResponseEntity<String> result = null;
			if (messageType != null) {
				switch (messageType) {
					case "AuditProgressUpdate":
						result = handleAuditProgressUpdate(target);
						break;
					case "PageAuditProgressMessage":
						result = handlePageAuditProgressMessage(target);
						break;
					case "JourneyCandidateMessage":
						result = handleJourneyCandidateMessage(target);
						break;
					case "VerifiedJourneyMessage":
						result = handleVerifiedJourneyMessage(target);
						break;
					case "DiscardedJourneyMessage":
						result = handleDiscardedJourneyMessage(target);
						break;
					default:
						log.warn("Unknown messageType={}, attempting fallback deserialization", messageType);
				}
			}

			if (result == null) {
				// Fallback: try legacy cascading deserialization for messages without messageType
				result = handleLegacyMessage(target);
			}

			idempotencyService.markProcessed(body.getMessage().getMessageId(), "audit-service");
			return result;
		} catch (Exception e) {
			log.warn("Failed to process message, acknowledging to prevent infinite retries", e);
			return new ResponseEntity<String>("Error occurred while updating audit progress", HttpStatus.OK);
		}
	}

	private ResponseEntity<String> handleAuditProgressUpdate(String target) throws Exception {
		AuditProgressUpdate audit_msg = JacksonConfig.mapper().readValue(target, AuditProgressUpdate.class);
		//get AuditRecord from database
		Optional<AuditRecord> audit_record = audit_record_service.findById(audit_msg.getPageAuditId());

		if(audit_record.isPresent()) {
			//build page audit progress
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

		return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);
	}

	private ResponseEntity<String> handlePageAuditProgressMessage(String target) throws Exception {
		PageAuditProgressMessage audit_msg = JacksonConfig.mapper().readValue(target, PageAuditProgressMessage.class);
		//update audit record
		Optional<AuditRecord> auditRecordOpt = audit_record_service.findById(audit_msg.getPageAuditId());
		if (auditRecordOpt.isEmpty() || !(auditRecordOpt.get() instanceof PageAuditRecord)) {
			return new ResponseEntity<String>("Page audit record not found", HttpStatus.BAD_REQUEST);
		}
		PageAuditRecord audit_record = (PageAuditRecord) auditRecordOpt.get();

		Optional<DomainAuditRecord> domain_audit = audit_record_service.getDomainAuditRecordForPageRecord(audit_record.getId());

			// If domain audit exists send a domain level audit update
		if(domain_audit.isPresent()) {
			 //Broadcast audit update message to messageBroadcaster
			AuditUpdateDto audit_update = buildDomainAuditRecordDTO(domain_audit.get().getId());
			messageBroadcaster.sendAuditUpdate(domain_audit.get().getId()+"", audit_update);
		}
		else {
			 //Broadcast audit update message to messageBroadcaster
			AuditUpdateDto audit_update = buildPageAuditUpdatedDto(audit_record.getId());
			messageBroadcaster.sendAuditUpdate(audit_record.getId()+"", audit_update);
		}

		log.warn("successfully sent update for single page audit");
		return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);
	}

	private ResponseEntity<String> handleJourneyCandidateMessage(String target) throws Exception {
		JourneyCandidateMessage journey_candidate_msg = JacksonConfig.mapper().readValue(target, JourneyCandidateMessage.class);
		AuditUpdateDto audit_update = buildDomainAuditRecordDTO(journey_candidate_msg.getAuditRecordId());
		messageBroadcaster.sendAuditUpdate(journey_candidate_msg.getAuditRecordId()+"", audit_update);

		return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);
	}

	private ResponseEntity<String> handleVerifiedJourneyMessage(String target) throws Exception {
		VerifiedJourneyMessage verified_journey_msg = JacksonConfig.mapper().readValue(target, VerifiedJourneyMessage.class);

		AuditUpdateDto audit_update = buildDomainAuditRecordDTO(verified_journey_msg.getAuditRecordId());
		messageBroadcaster.sendAuditUpdate(verified_journey_msg.getAuditRecordId()+"", audit_update);
		return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);
	}

	private ResponseEntity<String> handleDiscardedJourneyMessage(String target) throws Exception {
		DiscardedJourneyMessage discarded_journey_msg = JacksonConfig.mapper().readValue(target, DiscardedJourneyMessage.class);
		log.warn("DiscardedJourneyMessage message deserialized");

		AuditUpdateDto audit_update = buildDomainAuditRecordDTO(discarded_journey_msg.getAuditRecordId());
		messageBroadcaster.sendAuditUpdate(discarded_journey_msg.getAuditRecordId()+"", audit_update);

		return new ResponseEntity<String>("Successfully sent audit update to user", HttpStatus.OK);
	}

	private ResponseEntity<String> handleLegacyMessage(String target) throws Exception {
		try {
			return handleAuditProgressUpdate(target);
		} catch(Exception e) {
			log.warn("Unable to process AuditProgressUpdate message", e);
		}

		try {
			return handlePageAuditProgressMessage(target);
		} catch(Exception e) {
			// not a PageAuditProgressMessage, try next type
		}

		try {
			return handleJourneyCandidateMessage(target);
		} catch(Exception e) {
			// not a JourneyCandidateMessage, try next type
		}

		try {
			return handleVerifiedJourneyMessage(target);
		} catch(Exception e) {
			// not a VerifiedJourneyMessage, try next type
		}

		try {
			return handleDiscardedJourneyMessage(target);
		} catch(Exception e) {
			// not a DiscardedJourneyMessage
		}

		log.warn("All handlers failed, acknowledging message to prevent infinite retries");
		return new ResponseEntity<String>("Error occurred while updating audit progress", HttpStatus.OK);
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
