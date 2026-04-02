package com.crawlerApi.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.crawlerApi.security.SecurityConfig;
import com.looksee.exceptions.ExistingRuleException;
import com.looksee.exceptions.MissingSubscriptionException;
import com.looksee.exceptions.RuleValueRequiredException;
import com.looksee.exceptions.UnknownAccountException;
import com.looksee.models.Account;
import com.looksee.models.ElementState;
import com.looksee.models.rules.Rule;
import com.looksee.models.rules.RuleType;
import com.looksee.services.AccountService;
import com.looksee.services.ElementStateService;
import com.looksee.services.RuleService;

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
@RequestMapping(path = "v1/elements", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Elements V1", description = "Elements API")
public class ElementController extends BaseApiController {
	private static Logger log = LoggerFactory.getLogger(ElementController.class);

	@Autowired
	private ElementStateService element_service;

	@Autowired
	private AccountService account_service;

	@Autowired
	private RuleService rule_service;

    @Autowired
    protected SecurityConfig appConfig;

    /**
     * Adds {@link Rule} to {@link ElementState element} with a given id
     *
     * @param id element id
     * @return {@link ElementState element}
     * @throws UnknownAccountException {@link UnknownAccountException}
     * @throws RuleValueRequiredException {@link RuleValueRequiredException}
     * @throws ExistingRuleException {@link ExistingRuleException}
     * @throws MissingSubscriptionException {@link MissingSubscriptionException}
     */
    //@PreAuthorize("hasAuthority('create:rule')")
    @RequestMapping(path="/$element_key/rules", method = RequestMethod.POST)
	@Operation(summary = "Add a rule to the given element", description = "Add a rule to the given element")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully added rule", content = @Content(schema = @Schema(type = "object", implementation = ElementState.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
    public ElementState addRule(
    		HttpServletRequest request,
			@PathVariable(value="element_key", required=true) String element_key,
			@RequestParam(value="type", required=true) String type,
			@RequestParam(value="value", required=false) String value
		) throws RuleValueRequiredException, UnknownAccountException, ExistingRuleException
    {
    	Account acct = getAuthenticatedAccount(request.getUserPrincipal());
    	
    	if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	Rule rule = rule_service.findRule(type, value);
    	validateRule(rule);

		Rule rule_record = rule_service.save(rule);
    	
		log.warn("element key :: "+element_key);
		log.warn("rule record key :: " + rule_record.getKey());
		
    	element_service.addRuleToFormElement(acct.getEmail(), element_key, rule_record);
    	return element_service.findByKey(element_key);
    }
    
	/**
	 * Validates the rule
	 * @param rule {@link Rule}
	 */
    private void validateRule(Rule rule) {

    	Rule min_value_rule = null;
    	Rule max_value_rule = null;
    	Rule min_length_rule = null;
    	Rule max_length_rule = null;
    	Map<String, Integer> rule_duplicate_map = new HashMap<>();
		if(!rule_duplicate_map.containsKey(rule.getKey())){
			rule_duplicate_map.put(rule.getKey(), 0);
		}
		rule_duplicate_map.put(rule.getKey(), rule_duplicate_map.get(rule.getKey())+1);
		if(rule.getType().equals(RuleType.MIN_VALUE)){
			min_value_rule = rule;
		}
		else if(rule.getType().equals(RuleType.MAX_VALUE)){
			max_value_rule = rule;
		}
		else if(rule.getType().equals(RuleType.MIN_LENGTH)){
			min_length_rule = rule;
		}
		else if(rule.getType().equals(RuleType.MAX_LENGTH)){
			max_length_rule = rule;
		}

    	for(int rule_value : rule_duplicate_map.values()){
    		if(rule_value > 1){
    			throw new DuplicatesNotAllowedException();
    		}
    	}
    	//check that min/max rules are valid
    	if( min_value_rule != null && (min_value_rule.getValue().isEmpty()
    			|| !StringUtils.isNumeric(min_value_rule.getValue())
    			|| Integer.parseInt(min_value_rule.getValue()) <= 0)){
    		throw new MinValueMustBePositiveNumber();
    	}
		if( max_value_rule != null && (max_value_rule.getValue().isEmpty()
				|| !StringUtils.isNumeric(max_value_rule.getValue())
				|| Integer.parseInt(max_value_rule.getValue()) <= 0)){
			throw new MaxValueMustBePositiveNumber();
    	}
		if( min_length_rule != null && (min_length_rule.getValue().isEmpty()
				|| !StringUtils.isNumeric(min_length_rule.getValue())
    			|| Integer.parseInt(min_length_rule.getValue()) <= 0)){
			throw new MinLengthMustBePositiveNumber();
		}
		if( max_length_rule != null && (max_length_rule.getValue().isEmpty()
				|| !StringUtils.isNumeric(max_length_rule.getValue())
    			|| Integer.parseInt(max_length_rule.getValue()) <= 0)){
			throw new MaxLengthMustBePositiveNumber();
		}

		if(min_value_rule != null && max_value_rule != null){
			int min_value = Integer.parseInt(min_value_rule.getValue());
			int max_value = Integer.parseInt(max_value_rule.getValue());
			if(min_value > max_value){
				throw new MinCannotBeGreaterThanMaxException();
			}
		}
		if(min_length_rule != null && max_length_rule != null){
			int min_length = Integer.parseInt(min_length_rule.getValue());
			int max_length = Integer.parseInt(max_length_rule.getValue());
			if(min_length > max_length){
				throw new MinCannotBeGreaterThanMaxException();
			}
		}
	}

	/**
     * Adds {@link Rule} to {@link ElementState element} with a given id
     *
     * @param id element id
     * @return {@link ElementState element}
     * @throws UnknownAccountException {@link UnknownAccountException}
     * @throws RuleValueRequiredException {@link RuleValueRequiredException}
     * @throws MissingSubscriptionException {@link MissingSubscriptionException}
     */
    //@PreAuthorize("hasAuthority('create:rule')")
    @RequestMapping(path="/$element_key/rules/$rule_key", method = RequestMethod.DELETE)
    @Operation(summary = "Remove rule from element", description = "Remove a rule from the given element")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully removed rule", content = @Content(schema = @Schema(type = "object", implementation = ElementState.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Missing subscription"),
        @ApiResponse(responseCode = "404", description = "Element or rule not found")
    })
    public ElementState removeRule(
    		HttpServletRequest request,
			@PathVariable(value="element_key", required=true) String element_key,
			@PathVariable(value="rule_key", required=true) String rule_key
    	) throws RuleValueRequiredException, UnknownAccountException
    {
    	Account acct = getAuthenticatedAccount(request.getUserPrincipal());
    	
    	if(acct.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	element_service.removeRule(acct.getEmail(), element_key, rule_key);
    	return element_service.findByKey(element_key);
    }

    /**
     * Adds {@link Rule} to {@link ElementState element} with a given id
     *
     * @param id element id
     * @return {@link ElementState element}
     * @throws UnknownAccountException {@link UnknownAccountException}
     */
    //@PreAuthorize("hasAuthority('create:rule')")
    @RequestMapping(path="/elements", method = RequestMethod.PUT)
    @Operation(summary = "Update element", description = "Update the given element")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated element", content = @Content(schema = @Schema(type = "object", implementation = ElementState.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Missing subscription"),
        @ApiResponse(responseCode = "400", description = "Invalid element data")
    })
    public ElementState update(
    		HttpServletRequest request,
    		@RequestBody ElementState element_state
		) throws UnknownAccountException
    {
		Account account = getAuthenticatedAccount(request.getUserPrincipal());
		
		if(account.getSubscriptionToken() == null){
			throw new MissingSubscriptionException();
		}
		
		//validateRules(element_state.getRules());
		//check that min/max length rules are valid
		log.warn("element update state experienced in element controller");
      element_state = element_service.save(element_state);
      return element_service.findByKey(element_state.getKey());
    }
    
    /**
     * Adds {@link Rule} to {@link ElementState element} with a given id
     *
     * @param id element id
     * @return {@link ElementState element}
     * @throws UnknownAccountException {@link UnknownAccountException}
     */
    //@ApiOperation(value = "updates form element", response = Iterable.class)
    //@PreAuthorize("hasAuthority('create:rule')")
    @RequestMapping(path="/forms/$form_key/elements", method = RequestMethod.PUT)
    @Operation(summary = "Update form element", description = "Update the given form element")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated form element", content = @Content(schema = @Schema(type = "object", implementation = ElementState.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Missing subscription"),
        @ApiResponse(responseCode = "400", description = "Invalid element data"),
        @ApiResponse(responseCode = "404", description = "Form not found")
    })
    public ElementState updateFormElement(
    		HttpServletRequest request,
    		@PathVariable(value="form_key", required=true) String form_key,
    		@RequestBody ElementState element_state
		) throws UnknownAccountException
    {
    	Account account = getAuthenticatedAccount(request.getUserPrincipal());
    	
    	if(account.getSubscriptionToken() == null){
    		throw new MissingSubscriptionException();
    	}
    	
    	//validateRules(element_state.getRules());
    	//check that min/max length rules are valid
    	log.warn("element update state experienced in element controller");
		element_state = element_service.saveFormElement(element_state);
		return element_service.findByKey(element_state.getKey());
    }

	/**
	 * Validates the rules
	 * @param rules {@link Set<Rule>}
	 */
	private void validateRules(Set<Rule> rules) {
		Rule min_value_rule = null;
    	Rule max_value_rule = null;
    	Rule min_length_rule = null;
    	Rule max_length_rule = null;
    	Map<String, Integer> rule_duplicate_map = new HashMap<>();
    	for(Rule rule : rules){
    		if(!rule_duplicate_map.containsKey(rule.getKey())){
    			rule_duplicate_map.put(rule.getKey(), 0);
    		}
    		rule_duplicate_map.put(rule.getKey(), rule_duplicate_map.get(rule.getKey())+1);
    		if(rule.getType().equals(RuleType.MIN_VALUE)){
    			min_value_rule = rule;
    		}
    		else if(rule.getType().equals(RuleType.MAX_VALUE)){
    			max_value_rule = rule;
    		}
    		else if(rule.getType().equals(RuleType.MIN_LENGTH)){
    			min_length_rule = rule;
    		}
    		else if(rule.getType().equals(RuleType.MAX_LENGTH)){
				max_length_rule = rule;
			}
    	}

    	for(int value : rule_duplicate_map.values()){
    		if(value > 1){
    			throw new DuplicatesNotAllowedException();
    		}
    	}
    	//check that min/max rules are valid
    	if( min_value_rule != null && (min_value_rule.getValue().isEmpty()
    			|| !StringUtils.isNumeric(min_value_rule.getValue())
    			|| Integer.parseInt(min_value_rule.getValue()) <= 0)){
    		throw new MinValueMustBePositiveNumber();
    	}
		if( max_value_rule != null && (max_value_rule.getValue().isEmpty()
				|| !StringUtils.isNumeric(max_value_rule.getValue())
				|| Integer.parseInt(max_value_rule.getValue()) <= 0)){
			throw new MaxValueMustBePositiveNumber();
    	}
		if( min_length_rule != null && (min_length_rule.getValue().isEmpty()
				|| !StringUtils.isNumeric(min_length_rule.getValue())
    			|| Integer.parseInt(min_length_rule.getValue()) <= 0)){
			throw new MinLengthMustBePositiveNumber();
		}
		if( max_length_rule != null && (max_length_rule.getValue().isEmpty()
				|| !StringUtils.isNumeric(max_length_rule.getValue())
    			|| Integer.parseInt(max_length_rule.getValue()) <= 0)){
			throw new MaxLengthMustBePositiveNumber();
		}

		if(min_value_rule != null && max_value_rule != null){
			int min_value = Integer.parseInt(min_value_rule.getValue());
			int max_value = Integer.parseInt(max_value_rule.getValue());
			if(min_value > max_value){
				throw new MinCannotBeGreaterThanMaxException();
			}
		}
		if(min_length_rule != null && max_length_rule != null){
			int min_length = Integer.parseInt(min_length_rule.getValue());
			int max_length = Integer.parseInt(max_length_rule.getValue());
			if(min_length > max_length){
				throw new MinCannotBeGreaterThanMaxException();
			}
		}
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class RuleExistsException extends RuntimeException {
	/**
	 *
	 */
	private static final long serialVersionUID = 7200878662560716216L;

	public RuleExistsException() {
		super("The rule is already associated with the requested element.");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class MinValueMustBePositiveNumber extends RuntimeException {
	/**
	 *
	 */
	private static final long serialVersionUID = 4419265853468867824L;

	public MinValueMustBePositiveNumber() {
		super("Minimum value rule must contain a positive number");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class MinLengthMustBePositiveNumber extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = -2254262252488883657L;

	public MinLengthMustBePositiveNumber() {
		super("Minimum length rule must contain a positive number.");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class MaxLengthMustBePositiveNumber extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 4334601359263388271L;

	public MaxLengthMustBePositiveNumber() {
		super("Max length value rule must contain a positive number.");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class MaxValueMustBePositiveNumber extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 250328142799757755L;

	public MaxValueMustBePositiveNumber() {
		super("Max value rule must contain a positive number.");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class MinCannotBeGreaterThanMaxException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 4423969190558092393L;

	public MinCannotBeGreaterThanMaxException() {
		super("Minimum value cannot be greater than max value");
	}
}

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
class DuplicatesNotAllowedException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = 6335991211635956501L;

	public DuplicatesNotAllowedException() {
		super("Elements cannot have duplcate rules");
	}
}
