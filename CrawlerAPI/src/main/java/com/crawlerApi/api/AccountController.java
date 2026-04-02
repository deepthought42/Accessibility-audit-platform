package com.crawlerApi.api;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.security.auth.login.AccountNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.crawlerApi.analytics.SegmentAnalyticsHelper;
import com.crawlerApi.security.SecurityConfig;
import com.crawlerApi.service.Auth0Service;
import com.looksee.exceptions.UnknownAccountException;
import com.looksee.models.Account;
import com.looksee.services.AccountService;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 *	API for interacting with {@link User} data
 */
@Controller
@RequestMapping(path = "v1/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Accounts V1", description = "Accounts API")
public class AccountController extends BaseApiController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected SecurityConfig appConfig;

    @Autowired
    private AccountService account_service;
    
    @Autowired
    private Auth0Service auth0Service;

    /**
     * Create new account
     *
     * @param authorization_header
     * @param account
     * @param principal
     *
     * @return
     * @throws Exception
     */

    @CrossOrigin(origins = "18.232.225.224, 34.233.19.82, 52.204.128.250, 3.132.201.78, 3.19.44.88, 3.20.244.231", maxAge = 3600)
    @RequestMapping(method = RequestMethod.POST)
    @Operation(summary = "Create a new account", description = "Create a new account with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully created account", content = @Content(schema = @Schema(type = "object", implementation = Account.class))),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "409", description = "Account already exists")
    })
    @ResponseBody
    public Account create(
    		HttpServletRequest request,
    		@RequestBody(required=true) Account account
    ) throws Exception{
    	log.warn("auth key passed :: " +request.getHeader("Authorization"));
    	Account acct = account_service.findByEmail(account.getEmail());

    	//create account
        if(acct != null){
        	//return error that account already exists
        	return acct;
        }

    	Map<String, Object> customerParams = new HashMap<String, Object>();
    	customerParams.put("description", "Customer for "+account.getEmail());
		
    	acct = new Account(account.getUserId(), account.getEmail(), "", "");
    	acct.setApiToken(UUID.randomUUID().toString());
        //final String username = usernameService.getUsername();
        // log username of user requesting account creation
        acct = account_service.save(acct);

    	SegmentAnalyticsHelper.identify(Long.toString(acct.getId()));
	   	//SegmentAnalyticsHelper.signupEvent(acct.getUserId());

        return acct;
    }

    /**
     * 
     * @param request
     * @param step_name
     * @return
     * @throws UnknownAccountException
     */
    @RequestMapping(path ="/onboarding_step", method = RequestMethod.POST)
    @Operation(summary = "Set onboarding step", description = "Mark an onboarding step as completed")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated onboarding steps", content = @Content(schema = @Schema(type = "array", implementation = String.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public List<String> setOnboardingStep(HttpServletRequest request, 
    										@Parameter(description = "Name of the onboarding step to mark as completed", required = true)
    										@RequestParam(value="step_name", required=true) String step_name) throws UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	Optional<Account> accountOpt = auth0Service.getCurrentUserAccount(principal);
    	
    	if(accountOpt.isEmpty()){
    		throw new UnknownAccountException();
    	}
    	
    	Account acct = accountOpt.get();
    	
        List<String> onboarding = acct.getOnboardedSteps();
        onboarding.add(step_name);
        acct.setOnboardedSteps(onboarding);
        account_service.save(acct);

        return acct.getOnboardedSteps();
    }

    //@PreAuthorize("hasAuthority('read:accounts')")
    @RequestMapping(path ="/onboarding_steps_completed", method = RequestMethod.GET)
    @Operation(summary = "Get onboarding steps", description = "Get all completed onboarding steps")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved onboarding steps", content = @Content(schema = @Schema(type = "array", implementation = String.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public List<String> getOnboardingSteps(HttpServletRequest request) throws UnknownAccountException {
    	Principal principal = request.getUserPrincipal();
    	Optional<Account> accountOpt = auth0Service.getCurrentUserAccount(principal);
    	
    	if(accountOpt.isEmpty()){
    		throw new UnknownAccountException();
    	}
    	
    	Account acct = accountOpt.get();

        return acct.getOnboardedSteps();
    }

    /**
     * Retrieves {@link Account account} with a given key
     *
     * @param key account key
     * @return {@link Account account}
     * @throws UnknownAccountException
     */
    @PreAuthorize("hasAuthority('read:accounts')")
    @RequestMapping( method = RequestMethod.GET)
    @Operation(summary = "Get current account", description = "Get the current authenticated user's account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved account", content = @Content(schema = @Schema(type = "object", implementation = Account.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Account get(HttpServletRequest request)
    		throws UnknownAccountException 
	{
    	Principal principal = request.getUserPrincipal();
    	Optional<Account> accountOpt = auth0Service.getCurrentUserAccount(principal);

		if(accountOpt.isEmpty()){
        	log.warn("Unknown account exception thrown");
    		throw new UnknownAccountException();
    	}
    	
    	Account acct = accountOpt.get();
    	
        return acct;
    }

	@PreAuthorize("hasAuthority('update:accounts')")
    @RequestMapping(value ="/{id}", method = RequestMethod.PUT)
    @Operation(summary = "Update account", description = "Update the account with the given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated account", content = @Content(schema = @Schema(type = "object", implementation = Account.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public Account update(final @PathVariable String key,
    					  final @Validated @RequestBody Account account) {
        log.info("update invoked");
        return account_service.save(account);
    }

	@PreAuthorize("hasAuthority('update:accounts')")
    @RequestMapping(value ="/{id}/refreshToken", method = RequestMethod.PUT)
    @Operation(summary = "Refresh API token", description = "Generate a new API token for the account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully refreshed API token", content = @Content(schema = @Schema(type = "object", implementation = Account.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public Account updateApiToken(final @PathVariable long id) throws AccountNotFoundException {
        log.info("update invoked");
        Optional<Account> optional_acct = account_service.findById(id);
        if(optional_acct.isPresent()){
        	Account account = optional_acct.get();
        	account.setApiToken(UUID.randomUUID().toString());
        	return account_service.save(account);
        }
        else{
        	throw new AccountNotFoundException();
        }
    }

	/**
	 * Deletes account
	 *
	 * @param request
	 * @throws UnirestException
	 */
	@PreAuthorize("hasAuthority('delete:accounts')")
    @RequestMapping(method = RequestMethod.DELETE)
    @Operation(summary = "Delete account", description = "Delete the current authenticated user's account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deleted account"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public void delete(HttpServletRequest request) throws UnirestException, UnknownAccountException{
		Principal principal = request.getUserPrincipal();
    	Optional<Account> accountOpt = auth0Service.getCurrentUserAccount(principal);
    	
    	if(accountOpt.isEmpty()){
    		throw new UnknownAccountException();
    	}
    	
    	Account account = accountOpt.get();
    	
    	//remove account
        account_service.deleteAccount(account.getId());
    }
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class AccountExistsException extends RuntimeException {
	private static final long serialVersionUID = 7200878662560716216L;

	public AccountExistsException() {
		super("Account already exists");
	}
}

