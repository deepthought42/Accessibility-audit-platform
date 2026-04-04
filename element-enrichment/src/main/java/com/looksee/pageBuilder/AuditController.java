package com.looksee.pageBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.pageBuilder.gcp.PubSubErrorPublisherImpl;
import com.looksee.pageBuilder.gcp.PubSubJourneyVerifiedPublisherImpl;
import com.looksee.pageBuilder.gcp.PubSubPageAuditPublisherImpl;
import com.looksee.pageBuilder.gcp.PubSubPageCreatedPublisherImpl;
import com.looksee.pageBuilder.mapper.Body;
import com.looksee.pageBuilder.models.Browser;
import com.looksee.pageBuilder.models.ElementState;
import com.looksee.pageBuilder.models.PageState;
import com.looksee.pageBuilder.models.enums.BrowserEnvironment;
import com.looksee.pageBuilder.models.enums.BrowserType;
import com.looksee.pageBuilder.models.message.PageBuiltMessage;
import com.looksee.pageBuilder.services.BrowserService;
import com.looksee.pageBuilder.services.ElementStateService;
import com.looksee.pageBuilder.services.PageStateService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ElementStateUtils;


/**
 * API Controller with main endpoint for running the page builder script
 * 
 */
@RestController
public class AuditController {
	private static Logger log = LoggerFactory.getLogger(AuditController.class);
	private static final Set<String> processedMessages = java.util.concurrent.ConcurrentHashMap.newKeySet();
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ElementStateService element_state_service;
	
	@Autowired
	private PubSubErrorPublisherImpl pubSubErrorPublisherImpl;
	
	@Autowired
	private PubSubJourneyVerifiedPublisherImpl pubSubJourneyVerifiedPublisherImpl;
	
	@Autowired
	private PubSubPageCreatedPublisherImpl pubSubPageCreatedPublisherImpl;

	@Autowired
	private PubSubPageAuditPublisherImpl audit_record_topic;
	
	@Transactional
	@RequestMapping(value = "/", method = RequestMethod.POST)
	public ResponseEntity<String> receiveMessage(@RequestBody Body body)
			throws JsonMappingException, JsonProcessingException, ExecutionException, InterruptedException, MalformedURLException
	{
		if(body == null || body.getMessage() == null || body.getMessage().getData() == null || body.getMessage().getData().isEmpty()) {
			log.warn("Received empty Pub/Sub message payload");
			return new ResponseEntity<String>("Empty message payload", HttpStatus.OK);
		}

		String pubsubMsgId = body.getMessage().getMessageId();
		if (pubsubMsgId != null && !processedMessages.add(pubsubMsgId)) {
			return ResponseEntity.ok("Duplicate");
		}

		Body.Message message = body.getMessage();
		String data = message.getData();

		String target;
		PageBuiltMessage url_msg;
		try {
			target = new String(Base64.getDecoder().decode(data));
			ObjectMapper input_mapper = new ObjectMapper().registerModule(new JavaTimeModule()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			url_msg = input_mapper.readValue(target, PageBuiltMessage.class);
		}
		catch(IllegalArgumentException | JsonProcessingException e) {
			log.warn("Invalid Pub/Sub message payload. Unable to decode/deserialize: {}", e.getMessage());
			return new ResponseEntity<String>("Invalid message payload", HttpStatus.OK);
		}
        
	    PageState page_state = page_state_service.findById(url_msg.getPageId()).get();
		URL url = new URL(BrowserUtils.sanitizeUserUrl(page_state.getUrl()));
		
		Browser browser = null;

	    try {

			log.warn("Extracting element states...");
			browser = browser_service.getConnection(BrowserType.CHROME, BrowserEnvironment.DISCOVERY);
			browser.navigateTo(url.toString());
			browser.removeDriftChat();

			List<ElementState> element_states = element_state_service.findAllForPage(page_state.getId());
			element_states = browser_service.enrichElementStates(element_states, page_state, browser, url.getHost());
			element_states = ElementStateUtils.enrichBackgroundColor(element_states).collect(Collectors.toList());
			
			element_states.stream().map(element -> element_state_service.save(page_state.getId(), element)).collect(Collectors.toList());
			log.warn("saving page state with element states = "+element_states.size() );

			// SEND PAGE-ENRICHED MESSAGE
			return new ResponseEntity<String>("Successfully enriched element states", HttpStatus.OK);
		}
		catch(Exception e) {
			/* 
			PageDataExtractionError page_extracton_err = new PageDataExtractionError(url_msg.getAccountId(),
																						url_msg.getAuditId(),
																						url.toString(),
																						"An exception occurred while building page state "+url_msg.getUrl()+".\n"+e.getMessage());

			String element_extraction_str = mapper.writeValueAsString(page_extracton_err);
			pubSubErrorPublisherImpl.publish(element_extraction_str);
			log.error("An exception occurred that bubbled up to the page state builder : "+e.getMessage());
			e.printStackTrace();
			*/
			return new ResponseEntity<String>("Error building page state for url "+url, HttpStatus.INTERNAL_SERVER_ERROR);
		}
		finally {
			if(browser != null) {
				browser.close();
			}
		}
	}
	
	/**
	 * Retrieves keys for all existing element states that are connected the the page with the given page state id
	 * 
	 * NOTE: This is best for a database with significant memory as the size of data can be difficult to process all at once
	 * on smaller machines
	 * 
	 * @param page_state_id
	 * @param element_states
	 * @return {@link List} of {@link ElementState} ids 
	 */
	private List<ElementState> saveNewElements(long page_state_id, List<ElementState> element_states) {		
		return element_states.stream()
							   .map(element -> element_state_service.save(element))
							   .collect(Collectors.toList());
	}	

}