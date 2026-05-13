package com.looksee.pageBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.looksee.models.config.JacksonConfig;
import com.looksee.gcp.PubSubErrorPublisherImpl;
import com.looksee.gcp.PubSubJourneyVerifiedPublisherImpl;
import com.looksee.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.gcp.PubSubPageCreatedPublisherImpl;
import com.looksee.browser.Browser;
import com.looksee.messaging.observability.TraceContextPropagation;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.AuditLevel;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.journeys.DomainMap;
import com.looksee.models.journeys.Journey;
import com.looksee.models.journeys.LandingStep;
import com.looksee.models.journeys.Step;
import com.looksee.models.message.AuditStartMessage;
import com.looksee.models.message.PageAuditMessage;
import com.looksee.models.message.PageBuiltMessage;
import com.looksee.models.message.PageDataExtractionError;
import com.looksee.models.message.VerifiedJourneyMessage;
import com.looksee.pageBuilder.schemas.BodySchema;
import com.looksee.services.AuditRecordService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.OutboxPublishingGateway;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainMapService;
import com.looksee.services.ElementStateService;
import com.looksee.services.JourneyService;
import com.looksee.services.PageStateService;
import com.looksee.services.StepService;
import com.looksee.utils.BrowserUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * REST controller that receives Google Cloud Pub/Sub push messages, builds
 * {@link PageState} objects from the target URL, and publishes downstream
 * audit messages.
 *
 * <h3>Class Invariants</h3>
 * <ul>
 *   <li>All {@code @Autowired} service dependencies are non-null after
 *       Spring context initialisation.</li>
 *   <li>The controller is stateless between requests; all mutable state is
 *       local to {@link #receiveMessage(BodySchema)}.</li>
 * </ul>
 */
@RestController
@Tag(name = "Audit", description = "API endpoints for building page objects")
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	@Autowired
	private IdempotencyService idempotencyService;

	@Autowired
	private AuditRecordService audit_record_service;

	@Autowired
	private BrowserService browser_service;

	@Autowired
	private JourneyService journey_service;

	@Autowired
	private StepService step_service;

	@Autowired
	private PageStateService page_state_service;

	@Autowired
	private ElementStateService element_state_service;

	@Autowired
	private DomainMapService domain_map_service;

	@Autowired
	private OutboxPublishingGateway outboxGateway;

	@Autowired
	private PubSubErrorPublisherImpl pubSubErrorPublisherImpl;

	@Autowired
	private PubSubJourneyVerifiedPublisherImpl pubSubJourneyVerifiedPublisherImpl;

	@Autowired
	private PubSubPageCreatedPublisherImpl pubSubPageCreatedPublisherImpl;

	@Autowired
	private PubSubPageAuditPublisherImpl audit_record_topic;

	/**
	 * Receives a Pub/Sub push message, extracts page data from the enclosed
	 * URL, and publishes the appropriate downstream messages.
	 *
	 * <h4>Preconditions</h4>
	 * <ul>
	 *   <li>{@code body} must not be {@code null}.</li>
	 *   <li>{@code body.getMessage()} must not be {@code null}.</li>
	 *   <li>{@code body.getMessage().getData()} must be a non-blank,
	 *       Base64-encoded JSON string representing an
	 *       {@link AuditStartMessage}.</li>
	 *   <li>The decoded {@code url} field must be a valid, well-formed URL.</li>
	 * </ul>
	 *
	 * <h4>Postconditions</h4>
	 * <ul>
	 *   <li>On success (HTTP 200): at least one Pub/Sub message has been
	 *       staged in the transactional outbox for the appropriate
	 *       downstream topic ({@code PageCreated}, {@code JourneyVerified},
	 *       {@code PageAudit}, or {@code AuditError}). The outbox row
	 *       commits with the same Neo4j transaction as the domain write.</li>
	 *   <li>On validation failure (HTTP 400): no side effects occur.</li>
	 *   <li>On processing error (HTTP 500): an {@code AuditError} message
	 *       is staged out-of-band so the signal survives an outer
	 *       transaction rollback.</li>
	 *   <li>The {@link Browser} resource, if acquired, is always closed
	 *       in the {@code finally} block.</li>
	 * </ul>
	 *
	 * @param body the Pub/Sub push envelope containing the audit message
	 * @return {@code 200 OK} on success, {@code 400 Bad Request} for
	 *         invalid payloads, or {@code 500 Internal Server Error} on
	 *         processing failures
	 * @throws ExecutionException   if an async operation fails
	 * @throws InterruptedException if the thread is interrupted
	 * @throws MalformedURLException if the decoded URL is malformed
	 */
	@Operation(
		summary = "Extract page data from a given URL",
		description = "Receives an audit message, processes the page, and publishes appropriate messages to Pub/Sub topics"
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "Successfully processed audit message",
			content = @Content(schema = @Schema(implementation = String.class))
		),
		@ApiResponse(
			responseCode = "400",
			description = "Invalid request payload; expected message.data with Base64-encoded AuditStartMessage JSON",
			content = @Content(schema = @Schema(implementation = String.class))
		),
		@ApiResponse(
			responseCode = "500",
			description = "Internal server error while processing audit message",
			content = @Content(schema = @Schema(implementation = String.class))
		)
	})
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
		description = "Audit message containing URL and audit details",
		required = true,
		content = @Content(schema = @Schema(implementation = BodySchema.class))
	)
	@Transactional
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(
		@Valid @RequestBody BodySchema body
	) throws ExecutionException, InterruptedException, MalformedURLException {
		// Precondition: validate input envelope
		if(body == null || body.getMessage() == null || body.getMessage().getData() == null || body.getMessage().getData().isBlank()) {
			log.warn("Received empty message body while attempting to extract page data");
			return new ResponseEntity<String>("Request must include message.data containing a Base64-encoded AuditStartMessage payload", HttpStatus.BAD_REQUEST);
		}

		String pubsubMessageId = body.getMessage().getMessageId();
		if (idempotencyService.isAlreadyProcessed(pubsubMessageId, "page-builder")) {
			return ResponseEntity.ok("Duplicate message, already processed");
		}

		AuditStartMessage url_msg;
		try {
			String data = body.getMessage().getData();
			String target = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
			url_msg = JacksonConfig.mapper().readValue(target, AuditStartMessage.class);
		}
		catch(IllegalArgumentException | JsonProcessingException e) {
			log.warn("Received an invalid message payload; acknowledging to prevent redelivery", e);
			return new ResponseEntity<String>("Request message.data must be valid Base64-encoded JSON for AuditStartMessage", HttpStatus.OK);
		}

		assert url_msg != null : "Postcondition of deserialization: url_msg must not be null";

		URL url = new URL(BrowserUtils.sanitizeUserUrl(url_msg.getUrl()));

		// The BodySchema here does not surface inbound Pub/Sub attributes, so
		// no parent traceparent is available — mint a fresh W3C-compliant id
		// per inbound message and stamp it onto every outbox event we stage.
		final String traceparent = TraceContextPropagation.currentOrMintTraceparent(null);

	    PageState page_state = null;
		Browser browser = null;

	    try {
	    	boolean is_secure = BrowserUtils.checkIfSecure(url);
			int http_status = BrowserUtils.getHttpStatus(url);

			//usually code 301 is returned which is a redirect, which is usually transferring to https
			if(http_status == 404 || http_status == 408) {
				log.warn("Received " + http_status + "status for link :: "+url_msg.getUrl());
				//send message to audit manager letting it know that an error occurred
				PageDataExtractionError page_extraction_err = new PageDataExtractionError(url_msg.getAccountId(),
																						  url_msg.getAuditId(),
																						  url_msg.getUrl().toString(),
																						  "Received "+http_status+" status while building page state "+url_msg.getUrl());

				outboxGateway.enqueueOutOfBand(
					pubSubErrorPublisherImpl.getTopic(),
					page_extraction_err,
					traceparent);

				return new ResponseEntity<String>("Successfully sent message to page extraction error", HttpStatus.OK);
			}

			DomainMap domain_map = null;
			if(AuditLevel.DOMAIN.equals(url_msg.getType())) {
				domain_map = domain_map_service.findByDomainAuditId(url_msg.getAuditId());
				if(domain_map == null) {
					domain_map = domain_map_service.save(new DomainMap());
					log.warn("adding domain map to audit record = " + url_msg.getAuditId());
					audit_record_service.addDomainMap(url_msg.getAuditId(), domain_map.getId());
				}
				assert domain_map != null : "Invariant: domain_map must be non-null for DOMAIN audits";
			}

			// Phase 4a.2 is deferred to after phase-3b code lands. The naive
			// migration (capturePage for page-state + a fresh getConnection for
			// element extraction) splits DOM capture and xpath resolution across
			// two page loads — on pages that render differently under ads /
			// personalization / cookie overlays, the xpaths derived from the
			// first capture may not resolve in the second browser and
			// getDomElementStates would persist incorrect element data.
			//
			// Per phase-4 plan §14.2 option (b), keep the one-browser flow until
			// phase-3b code is merged and element extraction can also go
			// server-side (via /v1/capture with extract=[source, attributes_for_xpath]
			// or equivalent). Instrumentation (4a.1) and env-var plumbing (4a.3)
			// ship independently.
			browser = browser_service.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
			page_state = browser_service.buildPageState(url, browser, is_secure, http_status, url_msg.getAuditId());

			//CHECK IF PAGE STATE EXISTS IN DOMAIN AUDIT ALREADY. IF IT DOESN'T, OR IT DOES AND
			// THERE AREN'T ANY ELEMENTS ASSOCIATED IN DB THEN BUILD PAGE ELEMENTS,
			PageState page_state_record = audit_record_service.findPageWithKey(url_msg.getAuditId(),
																				page_state.getKey());

			if(page_state_record == null
					|| element_state_service.getAllExistingKeys(page_state_record.getId()).isEmpty())
			{
				log.warn("Extracting element states...");
				List<String> xpaths = browser_service.extractAllUniqueElementXpaths(page_state.getSrc());
				List<ElementState> element_states = browser_service.getDomElementStates(	page_state,
																							xpaths,
																							browser,
																							url_msg.getAuditId());
				page_state.setElements(element_states);
				page_state = page_state_service.save(page_state);
			}
			else {
				page_state = page_state_record;
			}

			assert page_state != null : "Invariant: page_state must be non-null after build/retrieve";

			List<ElementState> elements = page_state_service.getElementStates(page_state.getId());
			page_state.setElements(elements);

		    if(AuditLevel.DOMAIN.equals(url_msg.getType())) {
				domain_map_service.addPageToDomainMap(domain_map.getId(), page_state.getId());
				PageBuiltMessage page_built_msg = new PageBuiltMessage(url_msg.getAccountId(),
																		page_state.getId(),
																		url_msg.getAuditId());

				log.warn("staging page built message to PageCreated topic for auditId={}", url_msg.getAuditId());
				outboxGateway.enqueue(
					pubSubPageCreatedPublisherImpl.getTopic(),
					page_built_msg,
					traceparent);
				List<Step> steps = new ArrayList<>();
				Step step = new LandingStep(page_state, JourneyStatus.VERIFIED);
				step = step_service.save(step);
				steps.add(step);
				Journey journey = new Journey(steps, JourneyStatus.VERIFIED);
				journey.setCandidateKey(journey.generateKey());
				Journey saved_journey = journey_service.save(journey);
				journey.setId(saved_journey.getId());

				domain_map_service.addJourneyToDomainMap(journey.getId(), domain_map.getId());

				VerifiedJourneyMessage journey_msg = new VerifiedJourneyMessage(journey,
																				BrowserType.CHROME,
																				url_msg.getAccountId(),
																				url_msg.getAuditId());
				log.warn("journey steps = "+ journey.getSteps());
				log.warn("staging verified journey for auditId={}", url_msg.getAuditId());
				outboxGateway.enqueue(
					pubSubJourneyVerifiedPublisherImpl.getTopic(),
					journey_msg,
					traceparent);
			}
			else if(AuditLevel.PAGE.equals(url_msg.getType())){
				audit_record_service.addPageToAuditRecord(url_msg.getAuditId(), page_state.getId());
				//send message to page audit message topic
				PageAuditMessage audit_msg = new PageAuditMessage(	url_msg.getAccountId(),
																	url_msg.getAuditId());
				log.warn("staging page audit message = "+audit_msg.getPageAuditId());
				outboxGateway.enqueue(
					audit_record_topic.getTopic(),
					audit_msg,
					traceparent);
			}

			idempotencyService.markProcessed(pubsubMessageId, "page-builder");
			return new ResponseEntity<String>("Successfully sent message to verified journey topic", HttpStatus.OK);
		}
		catch(Exception e) {
			PageDataExtractionError page_extraction_err = new PageDataExtractionError(url_msg.getAccountId(),
																						url_msg.getAuditId(),
																						url_msg.getUrl().toString(),
																						"An exception occurred while building page state "+url_msg.getUrl()+".\n"+e.getMessage());

			outboxGateway.enqueueOutOfBand(
				pubSubErrorPublisherImpl.getTopic(),
				page_extraction_err,
				traceparent);
			log.error("An exception occurred that bubbled up to the page state builder", e);

			return new ResponseEntity<String>("Error building page state for url "+url_msg.getUrl(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		finally {
			if(browser != null) {
				browser.close();
			}
		}
	}

}
