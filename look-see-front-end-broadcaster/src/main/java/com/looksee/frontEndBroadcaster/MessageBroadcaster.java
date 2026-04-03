package com.looksee.frontEndBroadcaster;

import org.aspectj.weaver.ast.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.models.dto.PageStateDto;
import com.looksee.models.Account;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.Form;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.frontEndBroadcaster.models.message.AuditProgressUpdate;

/**
 * Defines methods for emitting data to subscribed clients
 */
@Component
public class MessageBroadcaster {
	private static Logger log = LoggerFactory.getLogger(MessageBroadcaster.class);
	
    @Autowired
    private PusherConnector pusher;
	
	/**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public void broadcastAudit(String host, Audit audit) throws JsonProcessingException {
        //Object to JSON in String
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String audit_json = mapper.writeValueAsString(audit);
		pusher.getPusher().trigger(host, "audit-update", audit_json);
	}
	
	/**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	/**
	 * send {@link AuditRecord} to the users pusher channel
	 * @param account_id
	 * @param audit
	 */
	public void broadcastSubscriptionExceeded(Account account) throws JsonProcessingException {
        //Object to JSON in String
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        int id_start_idx = account.getUserId().indexOf('|');
		String user_id = account.getUserId().substring(id_start_idx+1);
        
		pusher.getPusher().trigger(user_id, "subscription-exceeded", "");
	}
	
    /**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public void broadcastDiscoveredTest(Test test, String host, String user_id) throws JsonProcessingException {	
        //Object to JSON in String        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String test_json = mapper.writeValueAsString(test);

		pusher.getPusher().trigger(user_id+host, "test-discovered", test_json);
	}

    /**
     * Message emitter that sends {@link Form} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
     */
	public void broadcastDiscoveredForm(Form form, long domain_id) throws JsonProcessingException {	
		log.info("Broadcasting discovered form !!!");
		
        //Object to JSON in String        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String form_json = mapper.writeValueAsString(form);
        
		pusher.getPusher().trigger(""+domain_id, "discovered-form", form_json);
		log.info("broadcasted a discovered form");
	}
	
	/**
     * Message emitter that sends {@link Test} to all registered clients
     * 
     * @param test {@link Test} to be emitted to clients
     * @throws JsonProcessingException 
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
		
        pusher.getPusher().trigger(host, "test", test_json);
	}

	public void sendIssueMessage(long page_id, UXIssueMessage issue) {
		ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

		try {
			String audit_record_json = mapper.writeValueAsString(issue);
			pusher.getPusher().trigger(page_id+"", "ux-issue-added", audit_record_json);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Broadcasts a {@link PageStateDto} to Pusher topic for front end consumption
	 */
    public  void broadcastPageState(String user_id, PageStateDto page_state_dto) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

		String page_state_dto_json = mapper.writeValueAsString(page_state_dto);
		pusher.getPusher().trigger(user_id, "pageFound", page_state_dto_json);
    }

    public void broadcastAuditUpdate(String user_id, AuditProgressUpdate audit_update_message) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

		String audit_update_message_json = mapper.writeValueAsString(audit_update_message);
		pusher.getPusher().trigger(user_id, "auditUpdate", audit_update_message_json);
    }
}
