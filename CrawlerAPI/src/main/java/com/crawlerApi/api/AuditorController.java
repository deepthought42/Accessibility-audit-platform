package com.crawlerApi.api;

import java.security.Principal;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.crawlerApi.service.Auth0Service;
import com.looksee.audits.performance.PerformanceInsight;
import com.looksee.exceptions.AuditCreationException;
import com.looksee.exceptions.UnknownAccountException;
import com.looksee.models.Account;
import com.looksee.models.PageState;
import com.looksee.models.SimplePage;
import com.looksee.models.dto.AuditRecordDto;
import com.looksee.services.PageStateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API for interacting with audit functionality
 */
@Controller
@RequestMapping(path = "v1/auditor", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auditor V1", description = "Auditor API")
public class AuditorController extends BaseApiController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private final Auth0Service auth0Service;
	private final com.crawlerApi.service.AuditService auditService;
	private final PageStateService pageService;
	
	@Autowired
	public AuditorController(
			Auth0Service auth0Service,
			com.crawlerApi.service.AuditService auditService,
			PageStateService pageService) {
		this.auth0Service = auth0Service;
		this.auditService = auditService;
		this.pageService = pageService;
	}
	
	/**
	 * Starts an audit on the provided URL based on the audit level.
	 * If it is a Page audit, only a single page is audited. 
	 * If it is a domain audit, then the entire domain is crawled and audited.
	 * 
	 * @param request HTTP request containing the authenticated user
	 * @param auditRequest The audit request containing URL and level
	 * @return A new audit record DTO
	 * @throws UnknownAccountException if the user account cannot be found
	 * @throws AuditCreationException if the audit cannot be created
	 */
	@RequestMapping(path="/start", method = RequestMethod.POST)
	@Operation(summary = "Start audit", description = "Start an audit on the provided URL based on the audit level")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully started audit", content = @Content(schema = @Schema(type = "object", implementation = AuditRecordDto.class))),
		@ApiResponse(responseCode = "400", description = "Invalid audit request"),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	public @ResponseBody ResponseEntity<AuditRecordDto> startAudit(
			HttpServletRequest request,
			@RequestBody(required=true) AuditStartRequest auditRequest
	) throws UnknownAccountException, AuditCreationException {
		
		log.info("Received audit start request for URL: {} with level: {}",
				auditRequest.getUrl(), auditRequest.getLevel());
		
		// Get the authenticated user's account
		Principal principal = request.getUserPrincipal();
		Optional<Account> accountOpt = auth0Service.getCurrentUserAccount(principal);
		
		if (accountOpt.isEmpty()) {
			log.warn("Unknown account for principal: {}", principal.getName());
			throw new UnknownAccountException();
		}
		
		Account account = accountOpt.get();
		log.info("Starting audit for account: {}", account.getId());
		
		// Validate audit request
		if (auditRequest.getUrl() == null || auditRequest.getUrl().trim().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}
		
		if (auditRequest.getLevel() == null) {
			return ResponseEntity.badRequest().build();
		}
		
		// Start the audit using the service
		try {
			AuditRecordDto auditRecord = auditService.startAudit(
				auditRequest.getUrl(),
				auditRequest.getLevel(),
				account
			);
			
			log.info("Successfully started audit with ID: {}", auditRecord.getId());
			return ResponseEntity.ok(auditRecord);
			
		} catch (AuditCreationException e) {
			log.error("Failed to create audit for URL: {} and account: {}",
					auditRequest.getUrl(), account.getId(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
    
	/**
	 * Retrieves page information for a given URL
	 * 
	 * @param request HTTP request
	 * @param url The URL of the page to retrieve
	 * @return SimplePage containing page information
	 */
    @RequestMapping(method = RequestMethod.GET)
	@Operation(summary = "Get page by URL", description = "Retrieve page information for the given URL")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved page", content = @Content(schema = @Schema(type = "object", implementation = SimplePage.class))),
		@ApiResponse(responseCode = "400", description = "Invalid URL"),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "404", description = "Page not found"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
    public ResponseEntity<SimplePage> getPage(
    		HttpServletRequest request,
			@RequestParam(value="url", required=true) String url) {
    	
    	if (url == null || url.trim().isEmpty()) {
    		return ResponseEntity.badRequest().build();
    	}
    	
    	try {
    		PageState page = pageService.findByUrl(url);
    		
    		if (page == null) {
    			log.warn("Page not found for URL: {}", url);
    			return ResponseEntity.notFound().build();
    		}
    		
    		log.info("Found page with key: {}", page.getKey());
    		
    		SimplePage simplePage = new SimplePage(
    			page.getUrl(),
    			page.getViewportScreenshotUrl(),
    			page.getFullPageScreenshotUrl(),
    			page.getFullPageWidth(),
    			page.getFullPageHeight(),
    			page.getSrc(),
    			page.getKey(), 
    			page.getId()
    		);
    		
    		return ResponseEntity.ok(simplePage);
    		
    	} catch (Exception e) {
    		log.error("Error retrieving page for URL: {}", url, e);
    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    	}
    }
    
    /**
     * Retrieves performance insights for a given page
     * 
     * @param request HTTP request containing the authenticated user
     * @param pageKey The key of the page to get insights for
     * @return Performance insights for the page
     * @throws UnknownAccountException if the user account cannot be found
     */
    @PreAuthorize("hasAuthority('read:actions')")
    @RequestMapping(method = RequestMethod.GET, path="/{page_key}/insights")
	@Operation(summary = "Get page insights", description = "Retrieve performance insights for the given page")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved page insights", content = @Content(schema = @Schema(type = "object", implementation = PerformanceInsight.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
    public ResponseEntity<PerformanceInsight> getInsights(
    		HttpServletRequest request,
			@PathVariable(value="page_key", required=true) String pageKey
	) throws UnknownAccountException {
    	
    	// Verify the user is authenticated
    	Principal principal = request.getUserPrincipal();
    	Optional<Account> accountOpt = auth0Service.getCurrentUserAccount(principal);

    	if (accountOpt.isEmpty()) {
    		log.warn("Unknown account for principal: {}", principal.getName());
    		throw new UnknownAccountException();
    	}
    	
    	log.info("Finding page insights for page key: {}", pageKey);
    	
    	// TODO: Implement insights retrieval
    	// PerformanceInsight insights = pageService.getAuditInsights(pageKey);
    	
    	return ResponseEntity.notFound().build(); // Temporary until implementation
    }
}