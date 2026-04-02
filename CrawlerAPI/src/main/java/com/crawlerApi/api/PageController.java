package com.crawlerApi.api;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.looksee.audits.performance.PerformanceInsight;
import com.looksee.exceptions.UnknownAccountException;
import com.looksee.models.Account;
import com.looksee.models.PageState;
import com.looksee.models.SimplePage;
import com.looksee.services.AccountService;
import com.looksee.services.PageStateService;

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
@RequestMapping(path = "v1/pages", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Pages V1", description = "Pages API")
public class PageController extends BaseApiController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private AccountService account_service;
	
	@Autowired
	private PageStateService page_service;

    /**
     * Retrieves list of {@link PerformanceInsight insights} with a given key
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.GET)
    @Operation(summary = "Get page by URL", description = "Retrieve page information for the given URL")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved page", content = @Content(schema = @Schema(type = "object", implementation = SimplePage.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "Page not found")
    })
    public SimplePage getPage(HttpServletRequest request,
			@RequestParam(value="url", required=true) String url
	) throws UnknownAccountException  {
    	Account acct = getAuthenticatedAccount(request.getUserPrincipal());
    	
    	PageState page = page_service.findByUrl(url);
    	
        log.info("finding page :: "+page.getKey());
        
        SimplePage simple_page = new SimplePage(
        								page.getUrl(),
        								page.getViewportScreenshotUrl(),
        								page.getFullPageScreenshotUrl(),
        								page.getFullPageWidth(),
        								page.getFullPageHeight(),
        								page.getSrc(),
        								page.getKey(),
        								page.getId());
        return simple_page;
    }
    
    /**
     * Retrieves list of {@link PerformanceInsight insights} with a given key
     * 
     * @param key account key
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException
     */
    @PreAuthorize("hasAuthority('read:actions')")
    @RequestMapping(method = RequestMethod.GET, path="/$page_key/insights")
    @Operation(summary = "Get page insights", description = "Retrieve performance insights for the given page")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved page insights", content = @Content(schema = @Schema(type = "object", implementation = PerformanceInsight.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public PerformanceInsight getInsights(HttpServletRequest request,
			@PathVariable(value="page_key", required=true) String page_key
	) throws UnknownAccountException {
    	Account acct = getAuthenticatedAccount(request.getUserPrincipal());
    	
        log.info("finding all page insights :: "+page_key);
        return null; //page_service.getAuditInsights(page_state_key);
    }
    
}