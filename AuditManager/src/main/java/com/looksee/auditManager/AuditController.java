package com.looksee.auditManager;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.models.PageState;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.message.PageAuditMessage;
import com.looksee.models.message.PageBuiltMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.PageStateService;

/**
 * REST controller that receives page-built notifications from Google Cloud
 * Pub/Sub and orchestrates the creation of page audit records.
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li><b>Precondition:</b> All constructor dependencies must be non-null.</li>
 *   <li><b>Postcondition:</b> Every HTTP response uses the correct status code
 *       ({@code 200}, {@code 400}, or {@code 500}) and includes a human-readable
 *       body describing the outcome.</li>
 *   <li><b>Invariant:</b> A page is never audited more than once within the
 *       same domain audit, and only landable pages with a persisted
 *       {@link PageState} are eligible for auditing.</li>
 * </ul>
 */
@RestController
public class AuditController {
	private static final Logger log = LoggerFactory.getLogger(AuditController.class);

	private static final ObjectMapper INPUT_MAPPER = new ObjectMapper();
	private static final JsonMapper OUTPUT_MAPPER = JsonMapper.builder().addModule(new JavaTimeModule()).build();

	private final AuditRecordService auditRecordService;
	private final PubSubPageAuditPublisherImpl auditRecordTopic;
	private final PageStateService pageStateService;

	/**
	 * Creates a new {@code AuditController}.
	 *
	 * @param auditRecordService service for persisting and querying audit records; must not be {@code null}
	 * @param auditRecordTopic   Pub/Sub publisher for page-audit messages; must not be {@code null}
	 * @param pageStateService   service for querying page state and landability; must not be {@code null}
	 * @throws NullPointerException if any argument is {@code null}
	 */
	public AuditController(
		AuditRecordService auditRecordService,
		PubSubPageAuditPublisherImpl auditRecordTopic,
		PageStateService pageStateService) {
		this.auditRecordService = Objects.requireNonNull(auditRecordService, "auditRecordService must not be null");
		this.auditRecordTopic = Objects.requireNonNull(auditRecordTopic, "auditRecordTopic must not be null");
		this.pageStateService = Objects.requireNonNull(pageStateService, "pageStateService must not be null");
	}

	/**
	 * Receives a Base64-encoded {@link PageBuiltMessage} wrapped in a Pub/Sub
	 * {@link Body} and, when eligible, creates a {@link PageAuditRecord} and
	 * publishes a {@link PageAuditMessage}.
	 *
	 * <h4>Contract</h4>
	 * <ul>
	 *   <li><b>Precondition:</b> {@code body} contains a non-null message with
	 *       non-empty, valid Base64 data that deserializes to a
	 *       {@link PageBuiltMessage}.</li>
	 *   <li><b>Postcondition (success):</b> Returns {@code 200 OK}. If the page
	 *       was eligible, a new audit record has been persisted and a message
	 *       published to Pub/Sub.</li>
	 *   <li><b>Postcondition (client error):</b> Returns {@code 400 Bad Request}
	 *       when preconditions are violated.</li>
	 *   <li><b>Postcondition (server error):</b> Returns {@code 500} when an
	 *       unexpected or infrastructure error occurs.</li>
	 * </ul>
	 *
	 * @param body the Pub/Sub push message envelope
	 * @return a {@link ResponseEntity} indicating the processing outcome
	 */
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) {
		if (!hasValidPayload(body)) {
			log.warn("Received invalid Pub/Sub payload: message or data is missing");
			return badRequest("Invalid Pub/Sub payload");
		}

		String payload = decodePayload(body.getMessage().getData());
		if (payload == null) {
			return badRequest("Invalid message encoding");
		}

		PageBuiltMessage pageBuiltMessage = parseMessage(payload);
		if (pageBuiltMessage == null) {
			return badRequest("Invalid message format");
		}

		return processMessage(pageBuiltMessage);
	}

	/**
	 * Determines whether a page is eligible for auditing and, if so, creates
	 * and publishes the audit record.
	 *
	 * @param pageBuiltMessage the validated message; must not be {@code null}
	 * @return {@code 200 OK} on success, {@code 500} on infrastructure failure
	 */
	private ResponseEntity<String> processMessage(PageBuiltMessage pageBuiltMessage) {
		assert pageBuiltMessage != null : "pageBuiltMessage must not be null at this point";

		try {
			Set<AuditName> auditNames = buildAuditNames(pageBuiltMessage.getAuditRecordId());

			boolean alreadyAudited = auditRecordService.wasPageAlreadyAudited(pageBuiltMessage.getAuditRecordId(), pageBuiltMessage.getPageId());
			boolean isLandable = pageStateService.isPageLandable(pageBuiltMessage.getPageId());
			Optional<PageState> pageState = pageStateService.findById(pageBuiltMessage.getPageId());

			if (!alreadyAudited && isLandable && pageState.isPresent()) {
				return createAndPublishAudit(pageBuiltMessage, pageState.get(), auditNames);
			}

			log.info("Skipping pageId={} (alreadyAudited={}, landable={}, pageStatePresent={})",
				pageBuiltMessage.getPageId(), alreadyAudited, isLandable, pageState.isPresent());
			return ResponseEntity.ok("Successfully processed message");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Audit processing interrupted", e);
			return new ResponseEntity<>("Audit processing interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (ExecutionException e) {
			log.error("Error publishing audit message", e);
			return new ResponseEntity<>("Failed to process message", HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			log.error("Unexpected error while handling PageBuiltMessage", e);
			return new ResponseEntity<>("Failed to process message", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Creates a {@link PageAuditRecord}, persists it, links it to the parent
	 * domain audit, and publishes a {@link PageAuditMessage} to Pub/Sub.
	 *
	 * @param pageBuiltMessage the source message; must not be {@code null}
	 * @param pageState        the page to audit; must not be {@code null}
	 * @param auditNames       audit types to run; must not be {@code null} or empty
	 * @return {@code 200 OK} after successful persistence and publishing
	 * @throws JsonProcessingException if the audit message cannot be serialized
	 * @throws ExecutionException      if the Pub/Sub publish future fails
	 * @throws InterruptedException    if the current thread is interrupted while publishing
	 */
	private ResponseEntity<String> createAndPublishAudit(PageBuiltMessage pageBuiltMessage, PageState pageState, Set<AuditName> auditNames)
		throws JsonProcessingException, ExecutionException, InterruptedException {
		assert pageBuiltMessage != null : "pageBuiltMessage must not be null";
		assert pageState != null : "pageState must not be null";
		assert auditNames != null && !auditNames.isEmpty() : "auditNames must not be null or empty";

		log.info("Received page for auditing, pageId={}, url={}", pageBuiltMessage.getPageId(), pageState.getUrl());

		AuditRecord auditRecord = new PageAuditRecord(
			ExecutionStatus.BUILDING_PAGE,
			new HashSet<>(),
			pageState,
			true,
			auditNames);

		auditRecord = auditRecordService.save(auditRecord);
		assert auditRecord != null : "auditRecordService.save() must return a non-null record";

		auditRecordService.addPageAuditToDomainAudit(pageBuiltMessage.getAuditRecordId(), auditRecord.getId());
		auditRecordService.addPageToAuditRecord(auditRecord.getId(), pageBuiltMessage.getPageId());

		PageAuditMessage auditMessage = new PageAuditMessage(pageBuiltMessage.getAccountId(), auditRecord.getId());
		String auditRecordJson = OUTPUT_MAPPER.writeValueAsString(auditMessage);
		log.info("Sending PageAuditMessage to Pub/Sub for pageAuditId={}", auditRecord.getId());
		auditRecordTopic.publish(auditRecordJson);
		return ResponseEntity.ok("Successfully processed message");
	}

	/**
	 * Resolves the set of audit types to execute. Uses the labels from the
	 * parent {@link DomainAuditRecord} when available, otherwise falls back to
	 * a default set.
	 *
	 * @param auditRecordId the parent audit record identifier
	 * @return a non-null, non-empty set of {@link AuditName}s
	 */
	private Set<AuditName> buildAuditNames(long auditRecordId) {
		Optional<AuditRecord> optRecord = auditRecordService.findById(auditRecordId);
		if (optRecord.isPresent() && optRecord.get() instanceof DomainAuditRecord) {
			DomainAuditRecord record = (DomainAuditRecord) optRecord.get();
			Set<AuditName> labels = record.getAuditLabels();
			if (labels != null && !labels.isEmpty()) {
				return labels;
			}
		}

		Set<AuditName> defaults = buildDefaultAuditNames();
		assert !defaults.isEmpty() : "default audit names must never be empty";
		return defaults;
	}

	/**
	 * Returns the default set of audit types applied when no domain-level
	 * configuration is available.
	 *
	 * @return a non-null, non-empty set of {@link AuditName}s
	 */
	private Set<AuditName> buildDefaultAuditNames() {
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

	/**
	 * Validates that the Pub/Sub envelope contains a message with non-empty data.
	 *
	 * @param body the request body to validate; may be {@code null}
	 * @return {@code true} if the payload is structurally valid
	 */
	private boolean hasValidPayload(Body body) {
		return body != null
			&& body.getMessage() != null
			&& body.getMessage().getData() != null
			&& !body.getMessage().getData().isEmpty();
	}

	/**
	 * Decodes a Base64-encoded string to UTF-8 text.
	 *
	 * @param encodedData the Base64 string; must not be {@code null}
	 * @return the decoded payload, or {@code null} if decoding fails
	 */
	private String decodePayload(String encodedData) {
		assert encodedData != null : "encodedData must not be null when called";
		try {
			return new String(Base64.getDecoder().decode(encodedData), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			log.warn("Received invalid Base64 data from Pub/Sub message", e);
			return null;
		}
	}

	/**
	 * Deserializes a JSON string into a {@link PageBuiltMessage}.
	 *
	 * @param payload the JSON payload; must not be {@code null}
	 * @return the parsed message, or {@code null} if parsing fails
	 */
	private PageBuiltMessage parseMessage(String payload) {
		assert payload != null : "payload must not be null when called";
		try {
			return INPUT_MAPPER.readValue(payload, PageBuiltMessage.class);
		} catch (JsonProcessingException e) {
			log.error("Error occurred while mapping payload to PageBuiltMessage", e);
			return null;
		}
	}

	/**
	 * Convenience factory for {@code 400 Bad Request} responses.
	 *
	 * @param msg the response body; must not be {@code null}
	 * @return a {@link ResponseEntity} with status {@code 400}
	 */
	private ResponseEntity<String> badRequest(String msg) {
		return new ResponseEntity<>(msg, HttpStatus.BAD_REQUEST);
	}
}
