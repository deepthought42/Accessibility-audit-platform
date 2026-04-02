package com.crawlerApi.api;

import java.security.Principal;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.looksee.audits.performance.PerformanceInsight;
import com.looksee.exceptions.InsufficientSubscriptionException;
import com.looksee.exceptions.UnknownAccountException;
import com.looksee.models.Account;
import com.looksee.models.competitiveanalysis.Competitor;
import com.looksee.models.message.CompetitorMessage;
import com.looksee.services.AccountService;
import com.looksee.services.CompetitorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 *	API for interacting with {@link User} data
 */
@Controller
@RequestMapping(path = "v1/competitors", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Competitors V1", description = "Competitors API")
public class CompetitorController extends BaseApiController {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	
	@Autowired
	private AccountService account_service;
	
	@Autowired
	private CompetitorService competitor_service;
	
	/**
     * Retrieves list of {@link PerformanceInsight insights} with a given key
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException
     */
    @RequestMapping(method = RequestMethod.GET, path="/{competitor_id}")
    @Operation(summary = "Start competitor analysis", description = "Start analysis for the given competitor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully started competitor analysis"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient subscription"),
        @ApiResponse(responseCode = "404", description = "Competitor not found")
    })
    public void startAnalysis(HttpServletRequest request,
			@PathVariable(value="competitor_id", required=true) long competitor_id
	) throws InsufficientSubscriptionException {
    	Principal principal = request.getUserPrincipal();
		String user_id = principal.getName();
		Account account = account_service.findByUserId(user_id);
		
    	Optional<Competitor> competitor_opt = competitor_service.findById(competitor_id);
    	if(competitor_opt.isPresent()) {
    		Competitor competitor = competitor_opt.get();
    		
    		CompetitorMessage competitor_message = new CompetitorMessage(competitor.getId(), account.getId(), competitor);
    	}
    }
}