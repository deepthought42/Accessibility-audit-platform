package com.crawlerApi.analytics;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.looksee.models.Test;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;

public class SegmentAnalyticsHelper {
	
	public static Analytics buildSegment() {
		return Analytics.builder("2fhUNnmoIEy5DZj9yhysv9j7QQcgWQlT").build();
	}
	
	public static void sendTestFinishedRunningEvent(String userId, Test test) {
		identify(userId);
		Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("testId", test.getKey());
        final String anonymousId = UUID.randomUUID().toString();

		buildSegment().enqueue(
	            TrackMessage.builder("Test Finished Running")
	                .properties(properties)
	                .anonymousId(anonymousId)
	                .userId(userId));
	}

	public static void testRunStarted(String userId, int test_count) {
		identify(userId);
		//Fire test run started event
	   	Map<String, String> run_test_batch_props= new HashMap<String, String>();
	   	run_test_batch_props.put("total tests", Integer.toString(test_count));
	   	buildSegment().enqueue(TrackMessage.builder("Running tests")
   		    .userId(userId)
   		    .properties(run_test_batch_props)
   		);
	}
	
	public static void testCreated(String userId, String test_key) {
		//Fire test created event
	   	Map<String, String> test_created_props= new HashMap<String, String>();
	   	test_created_props.put("key", test_key);
	   	buildSegment().enqueue(TrackMessage.builder("Test Created")
								   		    .userId(userId)
								   		    .properties(test_created_props)
   		);
	}
	
	public static void formDiscovered(String userId, String form_key) {
		identify(userId);
		//Fire test created event
	   	Map<String, String> form_created_props= new HashMap<String, String>();
	   	form_created_props.put("key", form_key);
	   	buildSegment().enqueue(TrackMessage.builder("Form Discovered")
   		    .userId(userId)
   		    .properties(form_created_props)
   		);
	}

	public static void signupEvent(String userId) {
		assert userId != null;
		assert !userId.isEmpty();
		identify(userId);
		Map<String, String> account_signup_properties = new HashMap<String, String>();
    	account_signup_properties.put("plan", "FREE");
    	buildSegment().enqueue(TrackMessage.builder("Signed Up")
    		    .userId(userId)
    		    .properties(account_signup_properties)
    		);
	}

	public static void identify(String account_id) {
		assert account_id != null;
		assert !account_id.isEmpty();
		
        buildSegment().enqueue(IdentifyMessage.builder()
    		    .userId(account_id)
    		);
	}

	public static void sendPageStateError(String userId, String error) {
		identify(userId);
		//Fire test created event
	   	Map<String, String> error_props= new HashMap<String, String>();
	   	error_props.put("error", error);
	   	buildSegment().enqueue(TrackMessage.builder("Page State Save Error")
   		    .userId(userId)
   		    .properties(error_props)
   		);
	}

	public static void sendFormSaveError(String userId, String error) {
		identify(userId);

		Map<String, String> error_props= new HashMap<String, String>();
	   	error_props.put("error", error);
	   	buildSegment().enqueue(TrackMessage.builder("Form Save Error")
	   			.userId(userId)
	   		    .properties(error_props)
	    );
	}

	public static void sendTestCreatedInRecorder(String userId, String test_key) {
		identify(userId);

		//Fire test created event
	   	Map<String, String> test_created_props= new HashMap<String, String>();
	   	test_created_props.put("key", test_key);
	   	buildSegment().enqueue(TrackMessage.builder("Test Created via Recorder")
   		    .userId(userId)
   		    .properties(test_created_props)
   		);
	}

	public static void sendDomainCreatedInRecorder(String username, String key) {
		identify(username);

		//Fire test created event
	   	Map<String, String> domain_created_props= new HashMap<String, String>();
	   	domain_created_props.put("key", key);
	   	buildSegment().enqueue(TrackMessage.builder("Domain Created via Recorder")
   		    .userId(username)
   		    .properties(domain_created_props)
   		);
	}
}
