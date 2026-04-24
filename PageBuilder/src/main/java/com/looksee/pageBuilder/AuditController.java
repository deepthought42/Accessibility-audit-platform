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
	 *       published to the appropriate downstream topic
	 *       ({@code PageCreated}, {@code JourneyVerified},
	 *       {@code PageAudit}, or {@code AuditError}).</li>
	 *   <li>On validation failure (HTTP 400): no side effects occur.</li>
	 *   <li>On processing error (HTTP 500): an {@code AuditError} message
	 *       is published if serialisation succeeds.</li>
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

	    PageState page_state = null;
		Browser browser = null;

	    try {
			int http_status = BrowserUtils.getHttpStatus(url);

			//usually code 301 is returned which is a redirect, which is usually transferring to https
			if(http_status == 404 || http_status == 408) {
				log.warn("Received " + http_status + "status for link :: "+url_msg.getUrl());
				//send message to audit manager letting it know that an error occurred
				PageDataExtractionError page_extraction_err = new PageDataExtractionError(url_msg.getAccountId(),
																						  url_msg.getAuditId(),
																						  url_msg.getUrl().toString(),
																						  "Received "+http_status+" status while building page state "+url_msg.getUrl());

				String error_json = JacksonConfig.mapper().writeValueAsString(page_extraction_err);
				pubSubErrorPublisherImpl.publish(error_json);

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

			// Phase 4a.2: capturePage is mode-agnostic — works byte-identically in
			// local and remote mode, single round-trip on the remote side, no raw
			// getDriver() reach-through. Replaces the prior open-browser +
			// buildPageState dance. See browser-service/phase-4-consumer-cutover.md.
			page_state = browser_service.capturePage(url, BrowserType.CHROME, url_msg.getAuditId());

			//CHECK IF PAGE STATE EXISTS IN DOMAIN AUDIT ALREADY. IF IT DOESN'T, OR IT DOES AND
			// THERE AREN'T ANY ELEMENTS ASSOCIATED IN DB THEN BUILD PAGE ELEMENTS,
			PageState page_state_record = audit_record_service.findPageWithKey(url_msg.getAuditId(),
																				page_state.getKey());

			if(page_state_record == null
					|| element_state_service.getAllExistingKeys(page_state_record.getId()).isEmpty())
			{
				log.warn("Extracting element states...");
				// Element-state extraction still needs a live Browser. In remote
				// mode this becomes a RemoteBrowser, which requires phase-3b code
				// to implement findElement/extractAttributes — do not flip
				// looksee.browsing.mode=remote on PageBuilder until phase 3b has
				// shipped. See phase-4-consumer-cutover.md §14.2.
				browser = browser_service.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
				browser.navigateTo(url.toString());
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

				String page_built_str = JacksonConfig.mapper().writeValueAsString(page_built_msg);
				log.warn("sending page built message to PageCreated topic : "+page_built_str);
				pubSubPageCreatedPublisherImpl.publish(page_built_str);
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
				String journey_msg_str = JacksonConfig.mapper().writeValueAsString(journey_msg);
				log.warn("Publishing to verified journey topic = "+journey_msg_str);

				pubSubJourneyVerifiedPublisherImpl.publish(journey_msg_str);
			}
			else if(AuditLevel.PAGE.equals(url_msg.getType())){
				audit_record_service.addPageToAuditRecord(url_msg.getAuditId(), page_state.getId());
				//send message to page audit message topic
				PageAuditMessage audit_msg = new PageAuditMessage(	url_msg.getAccountId(),
																	url_msg.getAuditId());
				log.warn("sending page audit message = "+audit_msg.getPageAuditId());
				String audit_record_json = JacksonConfig.mapper().writeValueAsString(audit_msg);
				log.warn("(PageAudit) Sending PageAuditMessage to Pub/Sub = "+audit_record_json);
				audit_record_topic.publish(audit_record_json);
			}

			idempotencyService.markProcessed(pubsubMessageId, "page-builder");
			return new ResponseEntity<String>("Successfully sent message to verified journey topic", HttpStatus.OK);
		}
		catch(Exception e) {
			PageDataExtractionError page_extraction_err = new PageDataExtractionError(url_msg.getAccountId(),
																						url_msg.getAuditId(),
																						url_msg.getUrl().toString(),
																						"An exception occurred while building page state "+url_msg.getUrl()+".\n"+e.getMessage());

			try {
				String element_extraction_str = JacksonConfig.mapper().writeValueAsString(page_extraction_err);
				pubSubErrorPublisherImpl.publish(element_extraction_str);
			}
			catch(JsonProcessingException serializationException) {
				log.error("Failed to serialize PageDataExtractionError for publication", serializationException);
			}
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
