package com.looksee.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.models.Account;
import com.looksee.models.DiscoveryRecord;
import com.looksee.models.Domain;
import com.looksee.models.Form;
import com.looksee.models.LookseeObject;
import com.looksee.models.Test;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.dto.AuditUpdateDto;
import com.looksee.models.dto.DomainDto;
import com.pusher.rest.Pusher;

/**
 * Defines methods for emitting data to subscribed clients
 * 
 * This service is always available and will use either a real Pusher client
 * (when properly configured) or a fallback client (when Pusher properties are missing).
 * When using the fallback client, operations are logged but no real messages are sent.
 * 
 * This class is instantiated as a @Bean in MessageBroadcasterAutoConfiguration to ensure guaranteed availability.
 */
public class MessageBroadcaster {
	private static Logger log = LoggerFactory.getLogger(MessageBroadcaster.class);
	
	private final Pusher pusher;
	private final boolean isRealPusher;
	
	/**
	 * Constructor for the message broadcaster
	 *
	 * @param pusher the configured Pusher client (real or fallback)
	 * @param environment Spring Environment for property resolution (optional, can be null)
	 *
	 * precondition: pusher != null
	 */
	public MessageBroadcaster(Pusher pusher, Environment environment) {
		assert pusher != null;
		this.pusher = pusher;
		
		// Detect if this is the fallback Pusher by checking if all required properties are set
		if (environment != null) {
			String appId = environment.getProperty("pusher.appId");
			String key = environment.getProperty("pusher.key");
			String secret = environment.getProperty("pusher.secret");
			String cluster = environment.getProperty("pusher.cluster");
			
			this.isRealPusher = appId != null && !appId.trim().isEmpty() &&
								key != null && !key.trim().isEmpty() &&
								secret != null && !secret.trim().isEmpty() &&
								cluster != null && !cluster.trim().isEmpty();
		} else {
			// Fallback to system properties and environment variables if Environment not available
			String appId = System.getProperty("pusher.appId", System.getenv("PUSHER_APP_ID"));
			String key = System.getProperty("pusher.key", System.getenv("PUSHER_KEY"));
			String secret = System.getProperty("pusher.secret", System.getenv("PUSHER_SECRET"));
			String cluster = System.getProperty("pusher.cluster", System.getenv("PUSHER_CLUSTER"));
			
			this.isRealPusher = appId != null && !appId.trim().isEmpty() &&
								key != null && !key.trim().isEmpty() &&
								secret != null && !secret.trim().isEmpty() &&
								cluster != null && !cluster.trim().isEmpty();
		}
		
		if (isRealPusher) {
			log.info("MessageBroadcaster initialized with real Pusher client - real-time messaging enabled");
		} else {
			log.warn("MessageBroadcaster initialized with fallback Pusher client - real-time messaging disabled");
			log.warn("Messages will be logged only. To enable real-time messaging, configure Pusher properties.");
		}
	}
	
	/**
	 * Legacy constructor for backward compatibility
	 *
	 * @param pusher the configured Pusher client (real or fallback)
	 *
	 * precondition: pusher != null
	 */
	public MessageBroadcaster(Pusher pusher) {
		this(pusher, null);
	}
	
	/**
	 * Checks if real-time messaging is available
	 * 
	 * @return true if using real Pusher client, false if using fallback
	 */
	public boolean isRealTimeMessagingEnabled() {
		return isRealPusher;
	}
	
	/**
	 * Helper method to conditionally trigger Pusher events based on whether real Pusher is available
	 * 
	 * @param channel the channel to trigger on
	 * @param event the event name
	 * @param data the data to send
	 */
	private void conditionalTrigger(String channel, String event, Object data) {
		if (isRealPusher) {
			try {
				pusher.trigger(channel, event, data);
				log.debug("Real-time message sent - Channel: {}, Event: {}", channel, event);
			} catch (Exception e) {
				log.error("Failed to send real-time message - Channel: {}, Event: {}, Error: {}", channel, event, e.getMessage());
			}
		} else {
			log.debug("Fallback mode - Would send message - Channel: {}, Event: {}", channel, event);
		}
	}
	
	/**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param host the host of the test
     * @param audit {@link Audit} to be emitted to clients
     * @throws JsonProcessingException if there is an error converting the audit to a JSON string
     *
     * precondition: host != null
     * precondition: !host.isEmpty()
     * precondition: audit != null
     */
	public void broadcastAudit(String host, Audit audit) throws JsonProcessingException {
		assert host != null;
		assert !host.isEmpty();
		assert audit != null;
        //Object to JSON in String
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String audit_json = mapper.writeValueAsString(audit);

		conditionalTrigger(host, "audit-update", audit_json);
	}
	
	/**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException if there is an error converting the account to a JSON string
     */
	/**
	 * send {@link Account} to the users pusher channel
	 * @param account {@link Account} to be emitted to clients
	 * @throws JsonProcessingException if there is an error converting the account to a JSON string
	 *
	 * precondition: account != null
	 */
	public void broadcastSubscriptionExceeded(Account account) throws JsonProcessingException {
		assert account != null;
        //Object to JSON in String
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        int id_start_idx = account.getUserId().indexOf('|');
		String user_id = account.getUserId().substring(id_start_idx+1);
        
		conditionalTrigger(user_id, "subscription-exceeded", "");
	}
	
    /**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @param host the host of the test
     * @param user_id the user id of the test
     * @throws JsonProcessingException if there is an error converting the test to a JSON string
     */
	public void broadcastDiscoveredTest(Test test, String host, String user_id) throws JsonProcessingException {
        //Object to JSON in String
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String test_json = mapper.writeValueAsString(test);

		conditionalTrigger(user_id+host, "test-discovered", test_json);
	}

    /**
     * Message emitter that sends {@link Form} to all registered clients
     * 
     * @param form {@link Form} to be emitted to clients
     * @param domain_id the id of the domain
     * @throws JsonProcessingException if there is an error converting the form to a JSON string
     */
	public void broadcastDiscoveredForm(Form form, long domain_id) throws JsonProcessingException {
		log.info("Broadcasting discovered form !!!");
		
        //Object to JSON in String
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String form_json = mapper.writeValueAsString(form);
        
		conditionalTrigger(""+domain_id, "discovered-form", form_json);
		log.info("broadcasted a discovered form");
	}
	
	/**
     * Message emitter that sends {@link Test} to all registered clients
     * 
	 * @param test {@link Test} to be emitted to clients
	 * @param host the host of the test
     * @throws JsonProcessingException if there is an error converting the test to a JSON string
     */
	public void broadcastTest(Test test, String host) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        //Object to JSON in String
        String test_json = mapper.writeValueAsString(test);
        log.warn("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        log.warn("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        log.warn("host :: " + host);
        log.warn("TEST JSON :: " + test_json);
        log.warn("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        log.warn("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
		
        		conditionalTrigger(host, "test", test_json);
	}
	
	/**
     * Message emitter that sends {@link Test} to all registered clients
     * 
	 * @param path_object {@link LookseeObject} to be emitted to clients
	 * @param host the host of the path object
	 * @param user_id the user id of the path object
     * @throws JsonProcessingException if there is an error converting the path object to a JSON string
     */
	public void broadcastPathObject(LookseeObject path_object, String host, String user_id) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        //Object to JSON in String
        String path_object_json = mapper.writeValueAsString(path_object);
        
		conditionalTrigger(user_id+host, "path_object", path_object_json);
	}
	
	/**
     * Message emitter that sends {@link DiscoveryRecord} to all registered clients
     * 
     * @param record {@link DiscoveryRecord} to be emitted to clients
     * @throws JsonProcessingException if there is an error converting the discovery record to a JSON string
     */
	public void broadcastDiscoveryStatus(DiscoveryRecord record) throws JsonProcessingException {
		log.info("broadcasting discovery status");
		
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        //Object to JSON in String
        String discovery_json = mapper.writeValueAsString(record);
        
		conditionalTrigger(record.getDomainUrl(), "discovery-status", discovery_json);
	}

	/**
	 * send {@link DomainDto} to the users pusher channel
	 * @param user_id id of the user
	 * @param domain {@link Domain} to be emitted to clients
	 * @throws JsonProcessingException if there is an error converting the domain dto to a JSON string
	 */
	public void sendDomainAdded(String user_id, Domain domain) throws JsonProcessingException {
		/*
		DomainDto domain_dto = new DomainDto( domain.getId(), 
											  domain.getUrl(), 
											  domain.getPages().size(), 
											  0, 
											  0, 
											  0.0, 
											  0, 
											  0.0, 
											  0, 
											  0.0, 
											  0, 
											  0.0, 
											  false, 
											  0.0,
											  "Domain successfully created",
											  ExecutionStatus.COMPLETE);
		*/
		DomainDto domain_dto = new DomainDto( domain.getId(), domain.getUrl(), 0.01);

		
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

		String test_confirmation_json = mapper.writeValueAsString(domain_dto);
		conditionalTrigger(user_id.replace("|", ""), "domain-added", test_confirmation_json);
	}

	/**
	 * send {@link UXIssueMessage} to the users pusher channel
	 * @param page_id id of the page
	 * @param issue {@link UXIssueMessage} to be emitted to clients
	 */
	public void sendIssueMessage(String page_id, UXIssueMessage issue) {
		ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

		try {
			String audit_record_json = mapper.writeValueAsString(issue);
			conditionalTrigger(page_id+"", "ux-issue-added", audit_record_json);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * send {@link AuditRecord} to the users pusher channel
	 * @param user_id id of the user
	 * @param domain_dto {@link DomainDto} to be emitted to clients
	 * @throws JsonProcessingException if there is an error converting the domain dto to a JSON string
	 */
	public void sendAuditRecord(String user_id, DomainDto domain_dto) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

		String domain_dto_json = mapper.writeValueAsString(domain_dto);
		conditionalTrigger(user_id, "audit-record", domain_dto_json);
	}

	/**
	 * Sends {@linkplain AuditUpdateDto} to user via Pusher
	 * @param channel_id the channel id to send the audit update to
	 * @param audit_update the audit update to send
	 * @throws JsonProcessingException if there is an error converting the audit update to a JSON string
	 *
	 * precondition: channel_id != null
	 * precondition: channel_id is not empty
	 * precondition: audit_update != null
	 */
	public void sendAuditUpdate(String channel_id, AuditUpdateDto audit_update) throws JsonProcessingException {
		assert channel_id != null;
		assert !channel_id.isEmpty();
		assert audit_update != null;
		
		ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String audit_record_json = mapper.writeValueAsString(audit_update);
		conditionalTrigger(channel_id, "audit-progress", audit_record_json);
	}
}
