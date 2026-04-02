package com.crawlerApi.api;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.crawlerApi.analytics.SegmentAnalyticsHelper;
import com.looksee.models.Account;
import com.looksee.models.ActionOLD;
import com.looksee.models.Domain;
import com.looksee.models.Element;
import com.looksee.models.PageState;
import com.looksee.models.Test;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.repository.AccountRepository;
import com.looksee.models.repository.TestRepository;
import com.looksee.services.AccountService;
import com.looksee.services.DomainService;
import com.looksee.utils.BrowserUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 *	API for interacting with {@link User} data
 */
@Controller
@RequestMapping(path = "v1/ide-test-export", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "IDE Test Export V1", description = "IDE Test Export API")
public class IdeTestExportController extends BaseApiController {
	@SuppressWarnings("unused")
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private AccountRepository account_repo;

	@Autowired
	private TestRepository test_repo;

	@Autowired
	private AccountService account_service;

	@Autowired
	private DomainService domain_service;

	/**
     * Updates {@link Test} using an array of {@link JSONObject}s containing info for {@link PageState}s
     *  {@link Element}s and {@link ActionOLD}s
	 *
	 * @param json_str JSON String
	 *
	 * @return A boolean value indicating that the system successfully created a {@link Test} using the provided JSON
	 *
	 * @throws Exception
	 */
    @RequestMapping(method = RequestMethod.PUT)
    @Operation(summary = "Update IDE test", description = "Update a test using JSON data containing page states, elements, and actions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated test", content = @Content(schema = @Schema(type = "boolean"))),
        @ApiResponse(responseCode = "400", description = "Invalid JSON data"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<Boolean> update(HttpServletRequest request,
    									  @RequestBody(required=true) String json_str)
    										throws Exception {

    	return new ResponseEntity<>(Boolean.TRUE, HttpStatus.ACCEPTED );
    }

    /**
     * Contructs a new {@link Test} using an array of {@link JSONObject}s containing info for {@link PageState}s
     *  {@link Element}s and {@link ActionOLD}s
	 *
	 * @param json_str JSON String
	 *
	 * @return A boolean value indicating that the system successfully created a {@link Test} using the provided JSON
	 *
	 * @throws Exception
	 */
    @RequestMapping(method = RequestMethod.POST)
    @Operation(summary = "Create IDE test", description = "Create a new test using JSON data containing page states, elements, and actions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created test", content = @Content(schema = @Schema(type = "boolean"))),
        @ApiResponse(responseCode = "400", description = "Invalid JSON data"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<Boolean> create(HttpServletRequest request,
    									  @RequestBody(required=true) String json_str)
    										throws Exception {
    	Account acct = getAuthenticatedAccount(request.getUserPrincipal());

    	JSONObject test_json = new JSONObject(json_str);
    	
    	String formatted_url = BrowserUtils.sanitizeUrl(test_json.getString("domain_url"), false);
		formatted_url = BrowserUtils.sanitizeUrl(formatted_url, false);
		URL domain_url = new URL(formatted_url);
	
		
		if(test_json.has("key")){
			String test_key = test_json.getString("key");
			
			Test test = test_repo.findByKey(test_key, formatted_url, acct.getId());
			if(test != null){
				test.setArchived(true);
				test_repo.save(test);
			}
		}
		
    	Domain domain = domain_service.findByHostForUser(domain_url.getHost(), acct.getEmail());
    	if(domain == null){
    		domain = new Domain(domain_url.getProtocol(), domain_url.getHost(),formatted_url, "");
    		domain = domain_service.save(domain);
    		SegmentAnalyticsHelper.sendDomainCreatedInRecorder(acct.getEmail(), domain.getKey());
    	}

    	Map<String, Object> options = new HashMap<String, Object>();
		options.put("browser", BrowserType.CHROME);
		options.put("domain_key", domain.getKey());
		
		account_service.addDomainToAccount(acct, domain);

		/*
		Message<JSONObject> message = new Message<JSONObject>(acct.getUserId(), test_json, options, domain);

		ActorRef testCreationActor = actor_system.actorOf(SpringExtProvider.get(actor_system)
				.props("testCreationActor"), "test_creation_actor"+UUID.randomUUID());

		testCreationActor.tell(message, null);
*/
		return new ResponseEntity<>(Boolean.TRUE, HttpStatus.ACCEPTED );
	}
}
