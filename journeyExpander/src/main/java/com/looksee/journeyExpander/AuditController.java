package com.looksee.journeyExpander;


import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

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
import com.looksee.gcp.PubSubJourneyCandidatePublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.models.config.JacksonConfig;
import com.looksee.models.Domain;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.enums.Action;
import com.looksee.browser.enums.BrowserType;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.journeys.DomainMap;
import com.looksee.models.journeys.Journey;
import com.looksee.models.journeys.LandingStep;
import com.looksee.models.journeys.SimpleStep;
import com.looksee.models.journeys.Step;
import com.looksee.models.message.JourneyCandidateMessage;
import com.looksee.models.message.VerifiedJourneyMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.BrowserService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.DomainMapService;
import com.looksee.services.DomainService;
import com.looksee.services.JourneyService;
import com.looksee.services.PageStateService;
import com.looksee.services.StepService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ElementStateUtils;

/**
 * REST controller that receives verified journeys via Google Cloud Pub/Sub push
 * and expands them into candidate journeys by discovering interactive elements
 * on the journey's resulting page.
 *
 * <p><b>Contract:</b></p>
 * <ul>
 *   <li>Accepts HTTP POST on {@code /} with a Pub/Sub {@link Body} wrapper.</li>
 *   <li>Returns {@code 400 BAD REQUEST} when the payload is missing, malformed,
 *       or contains an invalid journey.</li>
 *   <li>Returns {@code 200 OK} when the request is handled (including cases
 *       where no expansion is performed).</li>
 *   <li>Returns {@code 500 INTERNAL SERVER ERROR} on unexpected failures.</li>
 * </ul>
 */
@RestController
public class AuditController {
	private static final Logger log = LoggerFactory.getLogger(AuditController.class);
	
	@Autowired
	private DomainService domain_service;

	@Autowired
	private JourneyService journey_service;
	
	@Autowired
	private DomainMapService domain_map_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private StepService step_service;
	
	@Autowired
	private PubSubJourneyCandidatePublisherImpl journey_candidate_topic;

	@Autowired
	private IdempotencyService idempotencyService;

	/**
	 * Receives a verified journey via Pub/Sub push and expands it into candidate
	 * journeys by appending interactive-element click steps to the journey's
	 * resulting page.
	 *
	 * <p><b>Preconditions:</b></p>
	 * <ul>
	 *   <li>{@code body} must contain a non-null {@code message} with Base64-encoded
	 *       JSON data deserializable to {@link VerifiedJourneyMessage}.</li>
	 *   <li>The decoded journey must have at least one {@link Step}.</li>
	 * </ul>
	 *
	 * <p><b>Postconditions:</b></p>
	 * <ul>
	 *   <li>On success, zero or more {@link JourneyCandidateMessage} payloads are
	 *       published to the journey-candidate Pub/Sub topic.</li>
	 *   <li>Each published candidate journey is persisted in the domain map.</li>
	 * </ul>
	 *
	 * @param body the Pub/Sub push wrapper containing the Base64-encoded message
	 * @return {@code 200 OK} when handled successfully, {@code 400 BAD REQUEST}
	 *         for invalid input, or {@code 500 INTERNAL SERVER ERROR} on failure
	 */
	@Transactional
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body) {
		if(body == null || body.getMessage() == null || body.getMessage().getData() == null || body.getMessage().getData().isBlank()) {
			log.warn("IGNORING JOURNEY! request payload missing message data");
			return new ResponseEntity<String>("Message data is required", HttpStatus.OK);
		}

		if (idempotencyService.isAlreadyProcessed(body.getMessage().getMessageId(), "journey-expander")) {
			return ResponseEntity.ok("Duplicate message, already processed");
		}

		VerifiedJourneyMessage journey_msg;
		try {
			String target = new String(Base64.getDecoder().decode(body.getMessage().getData()), StandardCharsets.UTF_8);
			journey_msg = JacksonConfig.mapper().readValue(target, VerifiedJourneyMessage.class);
		}
		catch(IllegalArgumentException | JsonProcessingException e) {
			log.warn("IGNORING JOURNEY! failed to parse incoming Pub/Sub payload", e);
			return new ResponseEntity<String>("Invalid message payload", HttpStatus.OK);
		}

		Journey journey = journey_msg.getJourney();
		if(journey == null || journey.getSteps() == null || journey.getSteps().isEmpty()) {
			log.warn("IGNORING JOURNEY! journey or journey steps missing");
			return new ResponseEntity<String>("Journey has no steps", HttpStatus.OK);
		}

		if(!shouldBeExpanded(journey)) {
			log.warn("IGNORING JOURNEY! journey should not be expanded");
			return new ResponseEntity<String>("Journey should not be expanded", HttpStatus.OK);
		}

		try {
			List<Step> journey_steps = journey.getSteps();
			Step last_step = journey_steps.get(journey_steps.size()-1);
			PageState journey_result_page = last_step instanceof LandingStep ? last_step.getStartPage() : last_step.getEndPage();
			if(journey_result_page == null) {
				return new ResponseEntity<String>("Journey result page not found", HttpStatus.BAD_REQUEST);
			}
			if(journey_result_page.getUrl() == null || journey_result_page.getUrl().isBlank()) {
				return new ResponseEntity<String>("Journey result page url not found", HttpStatus.BAD_REQUEST);
			}
			
			Domain domain = domain_service.findByAuditRecord(journey_msg.getAuditRecordId());
			if(domain == null) {
				log.warn("IGNORING JOURNEY! no domain found for audit record {}", journey_msg.getAuditRecordId());
				return new ResponseEntity<String>("Domain not found for audit record", HttpStatus.BAD_REQUEST);
			}
			if(BrowserUtils.isExternalLink(domain.getUrl(), journey_result_page.getUrl())) {
				return new ResponseEntity<String>("Last page of journey is external. No further expansion is allowed", HttpStatus.OK);
			}
			
			DomainMap domain_map = domain_map_service.findByDomainAuditId(journey_msg.getAuditRecordId());
			if(domain_map != null) {
				List<Step> page_steps = step_service.getStepsWithStartPage(journey_result_page, domain_map.getId());
				if(page_steps != null && page_steps.size() > 1) {
					return new ResponseEntity<String>("RETURNING WITHOUT EXPANSION!!!!  Steps were found that start with page with key"+journey_result_page.getKey(), HttpStatus.OK);
				}
			}
			
			List<Action> actions = List.of(Action.CLICK);
			List<ElementState> page_elements = page_state_service.getElementStates(journey_result_page.getId());
			List<ElementState> leaf_elements = page_elements == null ? new ArrayList<>() : new ArrayList<>(page_elements);
			journey_result_page.setElements(leaf_elements);
			log.warn(leaf_elements.size()+" leaf elements found for url = "+journey_result_page.getUrl() + " with id = "+journey_result_page.getId());
			
			leaf_elements.removeIf(element -> element == null
							|| BrowserService.isStructureTag(element.getName())
							|| !ElementStateUtils.isInteractiveElement(element));

			log.warn(leaf_elements.size()+" leaf elements after filtering");

			int journey_cnt = 0;
			for(ElementState leaf_element : leaf_elements) {
				for(Action action: actions) {
					Step step = new SimpleStep(journey_result_page,
												leaf_element,
											action,
											"",
											null,
											JourneyStatus.CANDIDATE);
					
					if(existsInJourney(journey, step)) {
						log.warn("IGNORING JOURNEY! step already exists within journey");
						continue;
					}
					
					if(domain_map == null) {
						domain_map = domain_map_service.save(new DomainMap());
						audit_record_service.addDomainMap(journey_msg.getAuditRecordId(), domain_map.getId());
					}
					
					Step step_record = step_service.findByCandidateKey(step.getKey(), domain_map.getId());
					if(step_record != null) {
						log.warn("IGNORING STEP!!  Step with candidate key already exists!!");
						continue;
					}
					
					step = step_service.save(step);
					List<Step> steps = new ArrayList<>(journey_steps);
					steps.add(step);
					
					Journey expanded_journey = new Journey(steps, JourneyStatus.CANDIDATE);
					Journey journey_record = journey_service.findByCandidateKey(expanded_journey.getCandidateKey());
					if(journey_record != null) {
						continue;
					}

					journey_record = journey_service.save(domain_map.getId(), expanded_journey);
					long journey_id = journey_record.getId();
					journey_record.setSteps(steps);
					steps.forEach(temp_step -> journey_service.addStep(journey_id, temp_step.getId()));
					domain_map_service.addJourneyToDomainMap(journey_record.getId(), domain_map.getId());

					JourneyCandidateMessage candidate = new JourneyCandidateMessage(journey_record,
																	BrowserType.CHROME,
																	journey_msg.getAccountId(),
																	journey_msg.getAuditRecordId(),
																	domain_map.getId());
					String candidate_json = JacksonConfig.mapper().writeValueAsString(candidate);
					journey_candidate_topic.publish(candidate_json);
					journey_cnt++;
				}
			}

			log.warn("generated "+journey_cnt+" journeys to explore");
			idempotencyService.markProcessed(body.getMessage().getMessageId(), "journey-expander");
			return new ResponseEntity<String>("Successfully generated journey expansions", HttpStatus.OK);
		}
		catch(Exception e) {
			log.error("Exception occurred while expanding journey {}", journey.getId(), e);
			return new ResponseEntity<String>("Error occurred while expanding journey", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Determines whether a journey is eligible for expansion.
	 *
	 * <p>A journey should be expanded when its last step is a {@link LandingStep}
	 * with a non-null start page, or a {@link SimpleStep} whose start and end
	 * pages differ (indicating a page-state change).</p>
	 *
	 * <p><b>Precondition:</b> {@code journey} must be non-null with a non-empty
	 * step list. Defensive null checks are retained for robustness.</p>
	 *
	 * @param journey the {@link Journey} to evaluate; must not be {@code null}
	 * @return {@code true} if the journey qualifies for expansion, {@code false} otherwise
	 */
	private boolean shouldBeExpanded(Journey journey) {
		assert journey != null : "Precondition violated: journey must not be null";

		if(journey.getSteps() == null || journey.getSteps().isEmpty()) {
			return false;
		}

		Step last_step = journey.getSteps().get(journey.getSteps().size()-1);
		if(last_step == null) {
			return false;
		}

		if(last_step instanceof LandingStep) {
			return last_step.getStartPage() != null;
		}
		else if(last_step instanceof SimpleStep) {
			if(last_step.getStartPage() == null || last_step.getEndPage() == null) {
				return false;
			}
			return !Objects.equals(last_step.getStartPage().getKey(), last_step.getEndPage().getKey());
		}
		return false;
	}
	
	/**
	 * Checks whether an equivalent {@link Step} already exists within the given
	 * {@link Journey}.
	 *
	 * <p>Two {@link LandingStep}s match when their start-page keys are equal.
	 * Two {@link SimpleStep}s match when their start-page URLs, element-state
	 * keys, actions, and action inputs are all equal.</p>
	 *
	 * <p><b>Precondition:</b> {@code journey} and {@code step} must be non-null.
	 * Defensive null checks are retained for robustness.</p>
	 *
	 * @param journey the {@link Journey} whose steps are searched; must not be {@code null}
	 * @param step    the {@link Step} to look for; must not be {@code null}
	 * @return {@code true} if an equivalent step is found, {@code false} otherwise
	 */
	private boolean existsInJourney(Journey journey, Step step) {
		assert journey != null : "Precondition violated: journey must not be null";
		assert step != null : "Precondition violated: step must not be null";

		if(journey.getSteps() == null) {
			return false;
		}
		for(Step journey_step : journey.getSteps()) {
			if(journey_step == null) {
				continue;
			}
			
			if(step instanceof LandingStep && journey_step instanceof LandingStep) {
				if(step.getStartPage() != null
						&& journey_step.getStartPage() != null
						&& Objects.equals(step.getStartPage().getKey(), journey_step.getStartPage().getKey())) {
					return true;
				}
			}
			else if(step instanceof SimpleStep && journey_step instanceof SimpleStep) {
				SimpleStep temp1 = (SimpleStep)step;
				SimpleStep temp2 = (SimpleStep)journey_step;
				if(temp1.getStartPage() != null
						&& temp2.getStartPage() != null
						&& temp1.getElementState() != null
						&& temp2.getElementState() != null
						&& Objects.equals(temp1.getStartPage().getUrl(), temp2.getStartPage().getUrl())
						&& Objects.equals(temp1.getElementState().getKey(), temp2.getElementState().getKey())
						&& Objects.equals(temp1.getAction(), temp2.getAction())
						&& Objects.equals(temp1.getActionInput(), temp2.getActionInput())) {
					return true;
				}
			}
		}
		return false;
	}

}
