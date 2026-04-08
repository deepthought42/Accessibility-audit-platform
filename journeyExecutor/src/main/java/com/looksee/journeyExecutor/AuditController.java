package com.looksee.journeyExecutor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchSessionException;
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

import com.looksee.models.config.JacksonConfig;
import com.looksee.gcp.PubSubDiscardedJourneyPublisherImpl;
import com.looksee.gcp.PubSubJourneyVerifiedPublisherImpl;
import com.looksee.gcp.PubSubPageBuiltPublisherImpl;
import com.looksee.mapper.Body;
import com.looksee.browser.Browser;
import com.looksee.models.Domain;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.browser.enums.BrowserEnvironment;
import com.looksee.browser.enums.BrowserType;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.journeys.DomainMap;
import com.looksee.models.journeys.Journey;
import com.looksee.models.journeys.LandingStep;
import com.looksee.models.journeys.Step;
import com.looksee.models.message.DiscardedJourneyMessage;
import com.looksee.models.message.JourneyCandidateMessage;
import com.looksee.models.message.PageBuiltMessage;
import com.looksee.models.message.VerifiedJourneyMessage;
import com.looksee.services.BrowserService;
import com.looksee.services.DomainMapService;
import com.looksee.services.IdempotencyService;
import com.looksee.services.DomainService;
import com.looksee.services.ElementStateService;
import com.looksee.services.JourneyService;
import com.looksee.services.PageStateService;
import com.looksee.services.StepExecutor;
import com.looksee.services.StepService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.PathUtils;
import com.looksee.utils.TimingUtils;

/**
 * Controller for the journey executor.
 *
 * This controller is responsible for receiving a {@link JourneyCandidateMessage} and processing it.
 *
 * If the journey is in a {@link JourneyStatus#CANDIDATE} status, it will be evaluated.
 *
 * If the journey is in a {@link JourneyStatus#REVIEWING} status, it will be skipped.
 *
 * If the journey is in a {@link JourneyStatus#VERIFIED} status, it will be skipped.
 *
 * If the journey is in a {@link JourneyStatus#DISCARDED} status, it will be skipped.
 *
 * If the journey is in a {@link JourneyStatus#ERROR} status, it will be skipped.
 *
 * If the journey is in a {@link JourneyStatus#CANCELLED} status, it will be skipped.
 */
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);

	private static final int MAX_REVIEW_ATTEMPTS = 4;
	private static final Map<Long,Integer> review_map = new ConcurrentHashMap<>();

	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private ElementStateService element_state_service;

	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private StepService step_service;
	
	@Autowired
	private JourneyService journey_service;
	
	@Autowired
	private DomainMapService domain_map_service;
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private PubSubJourneyVerifiedPublisherImpl verified_journey_topic;
	
	@Autowired
	private PubSubDiscardedJourneyPublisherImpl discarded_journey_topic;
	
	@Autowired
	private StepExecutor step_executor;
	
	@Autowired
	private PubSubPageBuiltPublisherImpl page_built_topic;

	@Autowired
	private IdempotencyService idempotencyService;
	
	/**
	 * Receives a {@link JourneyCandidateMessage} and processes it.
	 * Only journeys with {@link JourneyStatus#CANDIDATE} status are evaluated;
	 * all other statuses (REVIEWING, VERIFIED, DISCARDED, ERROR, CANCELLED) are skipped.
	 *
	 * @param body {@link Body} containing the {@link JourneyCandidateMessage}. May be null or contain
	 *             an empty/invalid message, in which case an OK response with an error description is returned.
	 * @return {@link ResponseEntity} containing the result of the journey evaluation. Never null.
	 * @throws Exception if an unrecoverable error occurs while evaluating the journey
	 *
	 * @post return value is never null
	 */
	@Transactional
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body)
			throws Exception
	{
		Browser browser = null;
		Journey journey = null;
		List<Step> steps = new ArrayList<>();
		String current_url = "";
		PageState final_page = null;
		long domain_audit_id = -1;
		JourneyCandidateMessage journey_msg = null;
		boolean is_external_link = false;
		Domain domain = null;
		boolean clear_review_counter = false;
		
		if(body == null || body.getMessage() == null || body.getMessage().getData() == null || body.getMessage().getData().isEmpty()) {
			log.warn("Received empty Pub/Sub message payload");
			return new ResponseEntity<String>("Empty message payload", HttpStatus.OK);
		}

		if (idempotencyService.isAlreadyProcessed(body.getMessage().getMessageId(), "journey-executor")) {
			return ResponseEntity.ok("Duplicate message, already processed");
		}

		Body.Message message = body.getMessage();
		String data = message.getData();
		String target = "";
		try {
			target = new String(Base64.getDecoder().decode(data), StandardCharsets.UTF_8);
			journey_msg = JacksonConfig.mapper().readValue(target, JourneyCandidateMessage.class);
			journey = journey_msg.getJourney();
		}
		catch(IllegalArgumentException | IOException e) {
			log.warn("Invalid Pub/Sub message payload. Unable to decode/deserialize journey candidate: {}", safeMessage(e));
			return new ResponseEntity<String>("Invalid message payload", HttpStatus.OK);
		}
		if(journey == null || journey.getId() == null) {
			log.warn("Message missing journey or journey id");
			return new ResponseEntity<String>("Journey payload missing id", HttpStatus.OK);
		}

		long journey_id = journey.getId();
		int review_count = review_map.merge(journey_id, 1, Integer::sum);
		if(review_count > MAX_REVIEW_ATTEMPTS){
			journey_service.updateStatus(journey_id, JourneyStatus.ERROR);
			review_map.remove(journey_id);
			return new ResponseEntity<String>("Test errored too much", HttpStatus.OK);
		}

		try {
			steps = new ArrayList<>(journey.getSteps());
			domain_audit_id = journey_msg.getAuditRecordId();
		
			Optional<Journey> journey_opt = journey_service.findById(journey.getId());

			if(!journey_opt.isPresent()) {
				clear_review_counter = true;
				return new ResponseEntity<String>("Journey " + journey.getId() + " was not found", HttpStatus.OK);
			}

			if(!JourneyStatus.CANDIDATE.equals(journey_opt.get().getStatus())){
				clear_review_counter = true;
				return new ResponseEntity<String>("Journey "+ journey_opt.get().getId()+" does not have CANDIDATE status. It has already been evaluated", HttpStatus.OK);
			}
			else {
				journey = journey_opt.get();
			}

			//update journey status to REVIEWING
			journey_service.updateStatus(journey.getId(), JourneyStatus.REVIEWING);
			
			//if journey with same candidate key exists that also has a status of VERIFIED or DISCARDED then don't iterate
			domain = domain_service.findByAuditRecord(journey_msg.getAuditRecordId());
		
			URL domain_url = new URL(BrowserUtils.sanitizeUserUrl(domain.getUrl()));
			browser = browser_service.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
			
			String browser_url = performJourneyStepsInBrowser(steps, browser);
			TimingUtils.pauseThread(3000L);
			String sanitized_url = BrowserUtils.sanitizeUserUrl(browser_url);
			current_url = BrowserUtils.getPageUrl(sanitized_url);
			is_external_link = BrowserUtils.isExternalLink(domain_url.getHost(), new URL(sanitized_url).getHost());
			//if current url is external URL then create ExternalPageState
			if(is_external_link) {
				final_page = new PageState();
				final_page.setUrl(current_url);
				final_page.setSrc("External Links are not mapped");
				final_page.setBrowser(BrowserType.CHROME);
				final_page.setElementExtractionComplete(true);
				final_page.setAuditRecordId(journey_msg.getAuditRecordId());
				final_page.setKey(final_page.generateKey());
				final_page = page_state_service.save( journey_msg.getMapId(), final_page);
			}
			else {
				//if current url is different than second to last page then try to lookup page in database before building page
				final_page = buildPage(browser, journey_msg.getMapId(), journey_msg.getAuditRecordId(), browser_url);
				log.warn("created page "+final_page.getUrl() + " with key =   "+final_page.getKey());
			}
		}
		catch(JavascriptException e) {
			log.warn("Javascript Exception for steps = " + steps + "; journey = "+journey.getId() + "  with status = "+journey.getStatus());
		    journey_service.updateStatus(journey.getId(), JourneyStatus.CANDIDATE);
			return new ResponseEntity<String>("Error occured while validating journey with id = "+journey.getId(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		catch(NoSuchSessionException e) {
			log.warn("Failed to acquire browser session; journey = {} with status = {} --> {}",
				journey.getId(),
				journey.getStatus(),
				safeMessage(e));
		    journey_service.updateStatus(journey.getId(), JourneyStatus.CANDIDATE);
			return new ResponseEntity<String>("Failed to acquire browser connection", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		catch(NoSuchElementException e) {
			log.warn("Failed to acquire browser element for journey = {} with status = {} on page = {} --> {}",
				journey.getId(),
				journey.getStatus(),
				current_url,
				safeMessage(e));
		    journey_service.updateStatus(journey.getId(), JourneyStatus.CANDIDATE);
			return new ResponseEntity<String>("Element not found", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		catch(org.openqa.selenium.interactions.MoveTargetOutOfBoundsException e) {
			log.debug("MOVE TO TARGET EXCEPTION FOR ELEMENT; journey = {} with status = {} --> {}",
				journey.getId(),
				journey.getStatus(),
				safeMessage(e));
		    journey_service.updateStatus(journey.getId(), JourneyStatus.CANDIDATE);
			return new ResponseEntity<String>("MoveToTarget Exception occured while validating journey with id = "+journey.getId()+". Returning ERROR in hopes it works out in another journey", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		catch(MalformedURLException e){
			log.warn("MalformedUrlException = "+current_url);
			journey_service.updateStatus(journey.getId(), JourneyStatus.DISCARDED);
			clear_review_counter = true;
			return new ResponseEntity<String>(current_url + " is malformed", HttpStatus.OK);
		}
		catch(NullPointerException e){
			log.warn("NullPointerException occurred");
			e.printStackTrace();
			return new ResponseEntity<String>(current_url + " experience Null Pointer Exception", HttpStatus.INTERNAL_SERVER_ERROR);
		}
		catch(Exception e) {
			log.warn("Exception occurred! Returning FAILURE;   message = "+e.getMessage());
			//e.printStackTrace();
			journey_service.updateStatus(journey.getId(), JourneyStatus.CANDIDATE);
			return new ResponseEntity<String>("Error occured while validating journey with id = "+journey.getId(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		finally {
			if(browser != null) {
				browser.close();
			}
			if(clear_review_counter) {
				review_map.remove(journey_id);
			}
		}
		
		try{
			//STEP AND JOURNEY SETUP
			Step final_step = steps.get(steps.size()-1);
			final_step.setEndPage(final_page);

			if(final_step.getId() == null) {
				final_step.setKey(final_step.generateKey());
				Step result_record = step_service.save(final_step);
				journey_service.addStep(journey.getId(), result_record.getId());
				final_step.setId(result_record.getId());
				steps.set(steps.size()-1, final_step);
			}
			else {
				//step_service.updateStatus(final_step.getId(), JourneyStatus.VERIFIED);
				step_service.addEndPage(final_step.getId(), final_page.getId());
				step_service.updateKey(final_step.getId(), final_step.getKey());
			}
			
			//UPDATE JOURNEY
			journey.setSteps(steps);
			journey.setKey(journey.generateKey());
			JourneyStatus status = getVerifiedOrDiscarded(journey, domain);
			Journey updated_journey = journey_service.updateFields(journey.getId(),
																status,
																journey.getKey(),
																journey.getOrderedIds());
			updated_journey.setSteps(steps);
			//Save all steps to be attached to journey record
			//for(Step step: steps) {
			//	journey_service.addStep(updated_journey.getId(), step.getId());
			//}
			
			//add Journey to domain map
			DomainMap domain_map = domain_map_service.findByDomainAuditId(domain_audit_id);
			
			if(JourneyStatus.DISCARDED.equals(updated_journey.getStatus()) 
				|| existsInJourney(steps.subList(0,  steps.size()-1), final_step)) {
				log.warn("DISCARDED Journey! "+updated_journey.getId() + " with status = "+updated_journey.getStatus());
				
				DiscardedJourneyMessage journey_message = new DiscardedJourneyMessage(	journey,
																						journey_msg.getBrowser(),
																						domain.getId(),
																						journey_msg.getAccountId(),
																						journey_msg.getAuditRecordId());

				String discarded_journey_json = JacksonConfig.mapper().writeValueAsString(journey_message);
				discarded_journey_topic.publish(discarded_journey_json);
			}
			else if(!final_step.getStartPage().getUrl().equals(final_step.getEndPage().getUrl()))
			{
				log.warn("VERIFIED Journey! "+updated_journey.getId() + " with status = "+updated_journey.getStatus());

				//if page state isn't associated with domain audit then send pageBuilt message
				PageBuiltMessage page_built_msg = new PageBuiltMessage(journey_msg.getAccountId(),
																		final_page.getId(),
																		journey_msg.getAuditRecordId());

				String page_built_str = JacksonConfig.mapper().writeValueAsString(page_built_msg);
				log.warn("SENDING page built message ...");
				page_built_topic.publish(page_built_str);
				
				//create landing step and make it the first record in a new list of steps
				Step landing_step = new LandingStep(final_page, JourneyStatus.VERIFIED);
				landing_step = step_service.save(landing_step);
				landing_step.setStartPage(final_page);

				steps = new ArrayList<>();
				steps.add(landing_step);
				
				//CREATE JOURNEY
				Journey new_journey = new Journey();
				List<Long> ordered_ids = steps.stream()
												.map(step -> step.getId())
												.collect(Collectors.toList());

				new_journey.setStatus(JourneyStatus.VERIFIED);
				new_journey.setOrderedIds(ordered_ids);
				new_journey.setCandidateKey(new_journey.generateCandidateKey());
				new_journey.setKey(new_journey.generateKey());
				new_journey = journey_service.save(domain_map.getId(), new_journey);
				
				for(Step step: steps) {
					journey_service.addStep(new_journey.getId(), step.getId());
				}
				new_journey.setSteps(steps);
				//send candidate message with new landing step journey
				VerifiedJourneyMessage journey_message = new VerifiedJourneyMessage(new_journey,
																					BrowserType.CHROME, 
																					journey_msg.getAccountId(), 
																					journey_msg.getAuditRecordId());
				String journey_json = JacksonConfig.mapper().writeValueAsString(journey_message);
				verified_journey_topic.publish(journey_json);
			}
			else {
				log.warn("VERIFIED Journey! "+updated_journey.getId() + " with status = "+updated_journey.getStatus());

				VerifiedJourneyMessage journey_message = new VerifiedJourneyMessage(updated_journey,
																					BrowserType.CHROME,
																					journey_msg.getAccountId(),
																					journey_msg.getAuditRecordId());

				String journey_json = JacksonConfig.mapper().writeValueAsString(journey_message);
				verified_journey_topic.publish(journey_json);

			}
			review_map.remove(journey_id);
			idempotencyService.markProcessed(body.getMessage().getMessageId(), "journey-executor");
			return new ResponseEntity<String>("Successfully verified journey", HttpStatus.OK);
		} catch(Exception e){
			e.printStackTrace();
			journey_service.updateStatus(journey.getId(), JourneyStatus.CANDIDATE);
			return new ResponseEntity<String>("Error verifying journey", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Evaluates a {@link Journey} and returns {@link JourneyStatus#VERIFIED} or {@link JourneyStatus#DISCARDED}.
	 * A journey is discarded when it has more than one step, does not end in a {@link LandingStep},
	 * and the final page either matches the second-to-last page or is an external link.
	 *
	 * @param journey the journey to evaluate
	 * @param domain  the domain used to determine external-link status
	 * @return {@link JourneyStatus#VERIFIED} or {@link JourneyStatus#DISCARDED}
	 * @throws MalformedURLException if the domain URL is malformed
	 *
	 * @pre journey != null
	 * @pre journey.getSteps() != null
	 * @pre !journey.getSteps().isEmpty()
	 * @pre domain != null
	 * @pre domain.getUrl() != null
	 * @post return value is either {@link JourneyStatus#VERIFIED} or {@link JourneyStatus#DISCARDED}
	 */
	private JourneyStatus getVerifiedOrDiscarded(Journey journey, Domain domain) throws MalformedURLException {
		assert journey != null : "journey must not be null";
		assert journey.getSteps() != null : "journey steps must not be null";
		assert !journey.getSteps().isEmpty() : "journey must have at least one step";
		assert domain != null : "domain must not be null";
		assert domain.getUrl() != null : "domain URL must not be null";

		Step last_step = journey.getSteps().get(journey.getSteps().size()-1);
		PageState second_to_last_page = PathUtils.getSecondToLastPageState(journey.getSteps());
		PageState final_page = PathUtils.getLastPageState(journey.getSteps());
		
		JourneyStatus result;
		if((journey.getSteps().size() > 1 && !(last_step instanceof LandingStep))
				&& (final_page.equals(second_to_last_page)
						|| BrowserUtils.isExternalLink(new URL(BrowserUtils.sanitizeUserUrl(domain.getUrl())).getHost(), final_page.getUrl())))
		{
			result = JourneyStatus.DISCARDED;
		}
		else {
			result = JourneyStatus.VERIFIED;
		}

		assert result == JourneyStatus.VERIFIED || result == JourneyStatus.DISCARDED
				: "result must be VERIFIED or DISCARDED";
		return result;
	}

	/**
	 * Constructs a {@link PageState page} including all {@link ElementState elements} on the page.
	 * Extracts element xpaths from the page source, saves the page, and performs interactive
	 * element extraction if not already complete.
	 *
	 * @param browser        the browser session used to extract page content
	 * @param domain_map_id  the domain map identifier to associate the page with
	 * @param audit_record_id the audit record identifier for this page build
	 * @param browser_url    the URL currently loaded in the browser
	 * @return the constructed and persisted {@link PageState}
	 * @throws Exception if fewer than 3 xpaths or zero elements are found, or on I/O errors
	 *
	 * @pre browser != null
	 * @pre browser_url != null
	 * @pre domain_map_id > 0
	 * @pre audit_record_id > 0
	 * @post return value is not null
	 * @post return value has a non-null URL
	 */
	private PageState buildPage(Browser browser,
								long domain_map_id,
								long audit_record_id,
								String browser_url)
							throws Exception {
		assert browser != null : "browser must not be null";
		assert browser_url != null : "browser_url must not be null";
		assert domain_map_id > 0 : "domain_map_id must be positive";
		assert audit_record_id > 0 : "audit_record_id must be positive";
		
		PageState page_state = browser_service.buildPageState(browser, audit_record_id, browser_url);
		
		List<String> xpaths = browser_service.extractAllUniqueElementXpaths(page_state.getSrc());
		if(xpaths.size() <= 2){
			log.warn("ONLY 2 XPATHS WERE FOUND!!!   url =  "+page_state.getUrl() + ";;   source = "+page_state.getSrc() + ";;   xpaths = "+xpaths);
			throw new Exception("Error! Only 2 xpaths found");
		}
		page_state = page_state_service.save(domain_map_id, page_state);
		
		if(!page_state.isInteractiveElementExtractionComplete()){
			BufferedImage full_page_screenshot = ImageIO.read(new URL(page_state.getFullPageScreenshotUrl()));
			List<ElementState> element_states = browser_service.getDomElementStates(page_state, 
																					xpaths,
																					browser,
																					domain_map_id,
																					full_page_screenshot,
																					browser_url);
			if(page_state.getUrl().contains("blog")){
				for(ElementState element: element_states){
					if(element == null){
						continue;
					}
					log.warn("element xpath = "+element.getXpath());
				}
			}

			if(element_states.size() == 0){
				log.warn("Uh oh! No elements were found. WELL THIS IS CONCERNING!!! XPATHS used for element build = "+xpaths.size()+"url = "+page_state.getUrl() +" for page id = "+page_state.getKey());
				throw new Exception("Error! No elements were found");
			}

			log.warn("Extracted "+element_states.size() +" elements from DOM for page "+page_state.getUrl());
			long page_state_id = page_state.getId();
			
			element_states.stream()
							.filter(Objects::nonNull)
							.map( element -> element_state_service.save(domain_map_id, element))
							.map( element -> page_state_service.addElement(page_state_id, element.getId()))
							.collect(Collectors.toList());

			log.warn("Extracted1 "+element_states.size() +" elements from DOM for page "+page_state.getUrl());
			if(page_state.getUrl().contains("blog")){
				for(ElementState element: element_states){
					log.warn("element xpath = "+element.getXpath());
				}
			}

			page_state_service.updateInteractiveElementExtractionComplete(page_state.getId(), true);
		}

		assert page_state != null : "page_state must not be null after build";
		assert page_state.getUrl() != null : "page_state URL must not be null after build";
		return page_state;
	}

	/**
	 * Executes all journey {@link Step steps} sequentially in the given browser, waiting for each
	 * page to load between steps.
	 *
	 * @param steps   the ordered list of steps to execute
	 * @param browser the browser session to execute steps in
	 * @return the URL of the browser after all steps have been executed
	 * @throws Exception if any step execution fails
	 *
	 * @pre steps != null
	 * @pre !steps.isEmpty()
	 * @pre browser != null
	 * @post return value is not null
	 */
	private String performJourneyStepsInBrowser(List<Step> steps, Browser browser) throws Exception  {
		assert steps != null : "steps must not be null";
		assert !steps.isEmpty() : "steps must not be empty";
		assert browser != null : "browser must not be null";
		
		String last_url = "";
		String current_url = "";
		//execute all steps sequentially in the journey
		for(Step step: steps) {
			step_executor.execute(browser, step);
			TimingUtils.pauseThread(2000L);
			current_url = browser.getDriver().getCurrentUrl();
			if(!last_url.equals(current_url)) {
				try{
					browser.waitForPageToLoad();
				}catch(Exception e){
					log.warn("waiting for page to load timed out..."+e.getMessage());
				}
			}
			last_url = current_url;
		}

		assert current_url != null : "current_url must not be null after executing steps";
		return current_url;
	}

	/**
	 * Checks if a step with a matching key already exists within the given list of steps.
	 *
	 * @param steps the list of steps to search through
	 * @param step  the step whose key to look for
	 * @return {@code true} if any step in the list has a key equal to the given step's key
	 *
	 * @pre steps != null
	 * @pre step != null
	 * @pre step.getKey() != null
	 */
	private boolean existsInJourney(List<Step> steps, Step step) {
		assert steps != null : "steps must not be null";
		assert step != null : "step must not be null";
		assert step.getKey() != null : "step key must not be null";
		for(Step journey_step : steps) {
			if(journey_step.getKey().contentEquals(step.getKey())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Extracts a safe, truncated message from an exception for use in log output.
	 * Returns a fallback string when the exception has no message or a blank message.
	 *
	 * @param e the exception to extract a message from
	 * @return a non-null, non-empty string of at most 100 characters
	 *
	 * @pre e != null
	 * @post return value is not null
	 * @post return value is not empty
	 * @post return value length is at most 100
	 */
	private String safeMessage(Exception e) {
		assert e != null : "exception must not be null";

		String message = e.getLocalizedMessage();
		if(message == null || message.trim().isEmpty()) {
			return "no exception message provided";
		}

		int end_index = Math.min(message.length(), 100);
		String result = message.substring(0, end_index);

		assert result != null : "result must not be null";
		assert !result.isEmpty() : "result must not be empty";
		assert result.length() <= 100 : "result must be at most 100 characters";
		return result;
	}
}
// [END run_pubsub_handler]
// [END cloudrun_pubsub_handler]
