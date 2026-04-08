package com.crawlerApi.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import com.crawlerApi.generators.report.GeneratePDFReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.looksee.audits.performance.PerformanceInsight;
import com.looksee.exceptions.MissingSubscriptionException;
import com.looksee.exceptions.UnknownAccountException;
import com.looksee.gcp.PubSubUrlMessagePublisherImpl;
import com.looksee.models.Account;
import com.looksee.models.Domain;
import com.looksee.models.Element;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.TestUser;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.audit.stats.AuditStats;
import com.looksee.models.competitiveanalysis.Competitor;
import com.looksee.models.competitiveanalysis.brand.Brand;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.dto.AuditUpdateDto;
import com.looksee.models.dto.CompetitorDto;
import com.looksee.models.dto.DomainDto;
import com.looksee.models.dto.DomainSettingsDto;
import com.looksee.models.dto.PageStatisticDto;
import com.looksee.models.dto.UXIssueReportDto;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.browser.enums.BrowserType;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.WCAGComplianceLevel;
import com.looksee.models.message.DomainAuditUrlMessage;
import com.looksee.models.repository.TestUserRepository;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.CompetitorService;
import com.looksee.services.DesignSystemService;
import com.looksee.services.DomainService;
import com.looksee.services.ReportService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.AuditUtils;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ContentUtils;
import com.looksee.utils.PDFDocUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API endpoints for interacting with {@link Domain} data
 */
@Controller
@RequestMapping(path = "v1/domains", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Domains V1", description = "Domains API")
public class DomainController extends BaseApiController {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private AuditRecordService audit_record_service;

	@Autowired
	private AccountService account_service;

	@Autowired
	private DomainService domain_service;

	@Autowired
	private AuditService audit_service;

	@Autowired
	private UXIssueMessageService ux_issue_service;

	@Autowired
	private TestUserRepository test_user_repo;

	@Autowired
	private CompetitorService competitor_service;

	@Autowired
	private DesignSystemService design_system_service;
	
	@Autowired
	private PubSubUrlMessagePublisherImpl url_topic;


	/**
	 */
	// @PreAuthorize("hasAuthority('read:domains')")
	@RequestMapping(method = RequestMethod.GET)
    @Operation(summary = "List all domains", description = "Get paginated list of domains with search and filtering")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved domains", content = @Content(schema = @Schema(type = "object", implementation = DomainDto.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
	public @ResponseBody Set<DomainDto> getAll(HttpServletRequest request) throws UnknownAccountException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName();

		Account acct = account_service.findByUserId(id);
		if (acct == null) {
			log.warn("unknown account...");
			throw new UnknownAccountException();
		}
		/*
		 * else if(acct.getSubscriptionToken() == null){ throw new
		 * MissingSubscriptionException(); }
		 */

		Set<Domain> domains = domain_service.getDomainsForAccount(acct.getId());
		
		Set<DomainDto> domain_info_set = new HashSet<>();
		for (Domain domain : domains) {
			Optional<DomainAuditRecord> audit_opt = audit_record_service.findMostRecentDomainAuditRecord(domain.getId());
			double data_extraction_progress = 0.0;
			if(audit_opt.isPresent()) {
				DomainAuditRecord domain_audit = (DomainAuditRecord)audit_opt.get();
				data_extraction_progress = audit_service.calculateDataExtractionProgress(domain_audit.getId());
			}
			
			DomainDto domainDTO = new DomainDto(domain.getId(), domain.getUrl(), data_extraction_progress);
			domain_info_set.add(domainDTO);
		}

		return domain_info_set;
	}

	/**
	 * Create a new {@link Domain domain}
	 * 
	 * @throws UnknownAccountException
	 * @throws MalformedURLException
	 */
	@PreAuthorize("hasAuthority('write:domains')")
	@RequestMapping(method = RequestMethod.POST)
	@Operation(summary = "Create a new domain", description = "Create a new domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully created domain", content = @Content(schema = @Schema(type = "object", implementation = DomainDto.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody Domain create(HttpServletRequest request,
									@RequestBody(required = true) Domain domain)
			throws UnknownAccountException, MalformedURLException {

		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account acct = account_service.findByUserId(id);

		if (acct == null) {
			throw new UnknownAccountException();
		} else if (acct.getSubscriptionToken() == null) {
			throw new MissingSubscriptionException();
		}

		if(domain.getUrl() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Domain URL is required");
		}
		String lowercase_url = domain.getUrl().toLowerCase();
		URL formatted_url = new URL(BrowserUtils.sanitizeUserUrl(lowercase_url));
		
		Domain domain_record = domain_service.createDomain(formatted_url, acct.getId());
		try {
			MessageBroadcaster.sendDomainAdded(acct.getUserId(), domain);
		} catch (JsonProcessingException e) {
			log.error("Error occurred while sending domain message to user");
		}

		return domain_record;
	}

	/**
	 * Create a new {@link Domain domain}
	 * 
	 * @throws UnknownAccountException
	 * @throws MalformedURLException
	 */
	// @PreAuthorize("hasAuthority('write:domains')")
	@RequestMapping(method = RequestMethod.PUT)
	@Operation(summary = "Update a domain", description = "Update a domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully updated domain", content = @Content(schema = @Schema(type = "object", implementation = DomainDto.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody Domain update(HttpServletRequest request,
			@RequestParam(value = "key", required = true) String key,
			@RequestParam(value = "protocol", required = true) String protocol,
			@RequestParam(value = "browser_name", required = true) String browser_name,
			@RequestParam(value = "logo_url", required = false) String logo_url

	) throws UnknownAccountException, MalformedURLException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account acct = account_service.findByUserId(id);

		if (acct == null) {
			throw new UnknownAccountException();
		} else if (acct.getSubscriptionToken() == null) {
			throw new MissingSubscriptionException();
		}

		Domain domain = domain_service.findByKey(key, acct.getEmail());
		domain.setLogoUrl(logo_url);

		return domain_service.save(domain);
	}

	/**
	 * Create a new {@link Domain domain}
	 * 
	 * @throws UnknownAccountException
	 * @throws MalformedURLException
	 */
	// @PreAuthorize("hasAuthority('write:domains')")
	@RequestMapping(path = "/select", method = RequestMethod.PUT)
	@Operation(summary = "Select domain", description = "Select a domain for the current account")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully selected domain"),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Missing subscription")
	})
	public @ResponseBody void selectDomain(HttpServletRequest request, @RequestBody Domain domain)
			throws UnknownAccountException, MalformedURLException {

		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account acct = account_service.findByUserId(id);

		if (acct == null) {
			throw new UnknownAccountException();
		} else if (acct.getSubscriptionToken() == null) {
			throw new MissingSubscriptionException();
		}

		//acct.setLastDomain(domain.getUrl());
		account_service.save(acct);
	}

	/**
	 * Retrieves the {@link DesignSystem} for the given domain
	 * 
	 * @return list of competitors
	 */
	@RequestMapping(method = RequestMethod.GET, path = "{domain_id}/settings")
	@Operation(summary = "Get the design system for the given domain", description = "Get the design system for the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved design system", content = @Content(schema = @Schema(type = "object", implementation = DomainSettingsDto.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody DomainSettingsDto getDesignSystem(@Parameter(description = "ID of the domain", required = true)
													@PathVariable("domain_id") long domain_id,
													HttpServletRequest request) {
		DesignSystem design_system = null;
		Optional<DesignSystem> design_system_opt = domain_service.getDesignSystem(domain_id);
		if (!design_system_opt.isPresent()) {
			log.warn("no design system present. Creating new design system with default settings");
			DesignSystem design = new DesignSystem();
			design = design_system_service.save(design);
			domain_service.addDesignSystem(domain_id, design.getId());
			design_system = design;
		}

		log.warn("returning existing design system");
		design_system = design_system_opt.get();
		
		//get TestUser set
		Set<TestUser> test_users = domain_service.getTestUsers(domain_id);
		DomainSettingsDto domain_settings = new DomainSettingsDto(design_system, test_users);
		return domain_settings;
	}

	/**
	 * Update expertise setting in domain settings
	 * 
	 * @param id
	 * @return {@link Audit audit} with given ID
	 * @throws MalformedURLException
	 */
	@RequestMapping(method = RequestMethod.POST, path = "/{domain_id}/settings/wcag")
	@Operation(summary = "Update the WCAG level for the given domain", description = "Update the WCAG level for the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully updated WCAG level", content = @Content(schema = @Schema(type = "object", implementation = DesignSystem.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody DesignSystem updateWcagLevel(HttpServletRequest request,
			@PathVariable("domain_id") long domain_id, @RequestBody(required = true) DesignSystem settings)
			throws MalformedURLException {
		// Get domain
		return domain_service.updateWcagSettings(domain_id, settings.getWcagComplianceLevel().toString());
	}

	/**
	 * Update expertise setting in domain settings
	 * 
	 * @param id
	 * @return {@link Audit audit} with given ID
	 * @throws MalformedURLException
	 */
	@RequestMapping(method = RequestMethod.POST, path = "/{domain_id}/settings/expertise")
	@Operation(summary = "Update the expertise level for the given domain", description = "Update the expertise level for the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully updated expertise level", content = @Content(schema = @Schema(type = "object", implementation = DesignSystem.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody DesignSystem updateExpertise(HttpServletRequest request,
			@PathVariable("domain_id") long domain_id, @RequestBody(required = true) DesignSystem settings)
			throws MalformedURLException {
		// Get domain
		return domain_service.updateExpertiseSettings(domain_id, settings.getAudienceProficiency().toString());
	}


	/**
	 * Removes domain from the current users account
	 * 
	 * @param key
	 * @param domain
	 * @return
	 * @throws UnknownAccountException
	 */
	// @PreAuthorize("hasAuthority('delete:domains')")
	@RequestMapping(method = RequestMethod.DELETE, path = "/{domain_id}")
	@Operation(summary = "Remove the given domain", description = "Remove the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully removed domain"),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody void remove(HttpServletRequest request,
			@PathVariable(value = "domain_id", required = true) long domain_id) throws UnknownAccountException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account acct = account_service.findByUserId(id);

		if (acct == null) {
			throw new UnknownAccountException();
		} else if (acct.getSubscriptionToken() == null) {
			throw new MissingSubscriptionException();
		}

		account_service.removeDomain(acct.getId(), domain_id);
	}

	/**
	 * Retrieves pages for a given domain from the current users account
	 * 
	 * @param key
	 * @param domain
	 * @return
	 * @throws UnknownAccountException
	 */
	// @PreAuthorize("hasAuthority('read:domains')")
	@RequestMapping(method = RequestMethod.GET, path = "/{domain_id}/pages")
	@Operation(summary = "Get the pages for the given domain", description = "Get the pages for the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved pages", content = @Content(schema = @Schema(type = "object", implementation = PageStatisticDto.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody Set<PageStatisticDto> getPages(HttpServletRequest request,
			@Parameter(description = "ID of the domain", required = true)
			@PathVariable(value = "domain_id", required = true) long domain_id) throws UnknownAccountException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account acct = account_service.findByUserId(id);
				
		if (acct == null) {
			throw new UnknownAccountException();
		} else if (acct.getSubscriptionToken() == null) {
			throw new MissingSubscriptionException();
		}

		Set<PageStatisticDto> page_stats = new HashSet<>();
		// get latest domain audit record
		try {
			Optional<DomainAuditRecord> domain_audit_record = audit_record_service.findMostRecentDomainAuditRecord(domain_id);

			if (!domain_audit_record.isPresent()) {
				throw new DomainAuditNotFound();
			}
			
			//Set<AuditName> audit_labels = domain_audit_record.get().getAuditLabels();
			Set<AuditName> audit_labels = new HashSet<>();
		    Set<Audit> audit_list = audit_record_service.getAllAuditsForDomainAudit(domain_audit_record.get().getId());

			Map<String, Boolean> key_map = new HashMap<>();
			Set<PageAuditRecord> page_audits = audit_record_service.getAllPageAudits(domain_audit_record.get().getId());
			for (AuditRecord page_audit : page_audits) {
				PageAuditRecord page_audit_record = (PageAuditRecord)page_audit;
				PageState page_state = audit_record_service.getPageStateForAuditRecord(page_audit_record.getId());
				if (page_state == null) {
					log.warn("page state is null");
					continue;
				}
				if(key_map.containsKey(page_state.getKey())) {
					log.warn("page state already in key map "+page_state.getKey());
					log.warn("KEYMAP = "+key_map);
					continue;
				}

				double content_score = AuditUtils
						.calculateScore(audit_record_service.getAllContentAudits(page_audit_record.getId()));
				double info_architecture_score = AuditUtils
						.calculateScore(audit_record_service.getAllInformationArchitectureAudits(page_audit_record.getId()));
				double aesthetic_score = AuditUtils
						.calculateScore(audit_record_service.getAllAestheticAudits(page_audit_record.getId()));
				double accessibility_score = AuditUtils
						.calculateScore(audit_record_service.getAllAccessibilityAudits(page_audit_record.getId()));
	
				//CALCULATE PROGRESS
				
			    //log.warn("audit labels = "+audit_labels);
			    log.warn("audit count "+audit_list.size());
			    
			    //calculate percentage of audits that are currently complete for each category
				double aesthetic_progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS, 
																		 1, 
																		 audit_list, 
																		 audit_labels);
				double content_progress = AuditUtils.calculateProgress(AuditCategory.CONTENT, 
																		1, 
																		audit_list, 
																		audit_labels);
				double info_architecture_progress = AuditUtils.calculateProgress(AuditCategory.INFORMATION_ARCHITECTURE, 
																					1, 
																					audit_list, 
																					audit_labels);
				

				//double data_extraction_progress = domain_audit.getDataExtractionProgress();
				log.warn("aesthetic progress = "+aesthetic_progress);
				
				double audit_progress = aesthetic_progress
						+ content_progress
						+ info_architecture_progress;
				
				//retrieve all journeys for domain audit
				double overall_progress = audit_progress / 3.0;
				
				PageStatisticDto page = new PageStatisticDto(page_state.getId(), 
															 page_state.getUrl(),
															 page_state.getViewportScreenshotUrl(), 
															 content_score, 
															 content_progress,
															 info_architecture_score, 
															 info_architecture_progress, 
															 accessibility_score, 
															 0.0,
															 aesthetic_score, 
															 aesthetic_progress, 
															 page_audit_record.getId(),
															 page_audit_record.getElementsReviewed(), 
															 page_audit_record.getElementsFound(), 
															 overall_progress >= 1.0,
															 1.0);
	
				key_map.put(page_state.getKey(), Boolean.TRUE);
				page_stats.add(page);
			}
		}catch (Exception e) {
			log.error("Failed to build page stats for domain {}", domain_id, e);
		}
		
		return page_stats;
	}

	/**
	 * Retrieves {@link AuditStats} for the domain with the given ID
	 * 
	 * @return {@link PerformanceInsight insight}
	 * @throws UnknownAccountException
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/{domain_id}/stats")
	@Operation(summary = "Get the audit statistics for the given domain", description = "Get the audit statistics for the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved audit statistics", content = @Content(schema = @Schema(type = "object", implementation = AuditUpdateDto.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody AuditUpdateDto getAuditStat(HttpServletRequest request,
													@Parameter(description = "ID of the domain", required = true)
													@PathVariable("domain_id") long domain_id)
										throws UnknownAccountException {
	
		// get most recent audit record for the domain
		Optional<AuditRecord> audit_record_opt = audit_record_service.getMostRecentAuditRecordForDomain(domain_id);

		if (audit_record_opt.isPresent()) {
			DomainAuditRecord audit_record = (DomainAuditRecord)audit_record_opt.get();
			
			AuditUpdateDto audit_update = buildDomainAuditRecordDTO(audit_record.getId());
			return audit_update;
		} else {
			throw new DomainAuditNotFound();
		}
	}

	// @PreAuthorize("hasAuthority('read:domains')")
	@RequestMapping(method = RequestMethod.GET, path = "/pages")
	@Operation(summary = "Get all pages for the given URL", description = "Get all pages for the given URL")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved pages", content = @Content(schema = @Schema(type = "object", implementation = PageState.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody Set<PageState> getAllPages(HttpServletRequest request,
			@RequestParam(value = "url", required = true) String url)
			throws UnknownAccountException, MalformedURLException {
		Account acct = getAuthenticatedAccount(request.getUserPrincipal());

		if (acct.getSubscriptionToken() == null) {
			throw new MissingSubscriptionException();
		}

		URL url_obj = new URL(BrowserUtils.sanitizeUrl(url, false));
		Set<PageState> pages = domain_service.getPages(url_obj.getHost());

		// TODO filter through pages to get most recent for each unique page url
		return pages;
	}

	@SafeVarargs
	public static <T> Set<T> merge(Collection<? extends T>... collections) {
		Set<T> newSet = new HashSet<T>();
		for (Collection<? extends T> collection : collections)
			newSet.addAll(collection);
		return newSet;
	}

	/**
	 * 
	 * @param request
	 * @param host
	 * 
	 * @return a unique set of {@link Element}s belonging to all page states for the
	 *         {@link Domain} with the given host
	 * @throws UnknownAccountException
	 */
	// @PreAuthorize("hasAuthority('read:domains')")
	@RequestMapping(method = RequestMethod.GET, path = "/page_elements")
	@Operation(summary = "Get all element states for the given host", description = "Get all element states for the given host")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved element states", content = @Content(schema = @Schema(type = "object", implementation = Element.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody Set<Element> getAllElementStates(HttpServletRequest request,
			@RequestParam(value = "host", required = true) String host) throws UnknownAccountException {
		Account acct = getAuthenticatedAccount(request.getUserPrincipal());

		if (acct.getSubscriptionToken() == null) {
			throw new MissingSubscriptionException();
		}

		Set<Element> page_elements = domain_service.getElementStates(host, acct.getEmail());
		log.info("###### PAGE ELEMENT COUNT :: " + page_elements.size());
		return page_elements;
	}

	/**
	 * Create a new test user and add it to the domain
	 * 
	 * @param request
	 * @param domain_id
	 * @param username
	 * @param password
	 * @param role
	 * 
	 * 
	 * @throws UnknownAccountException
	 * @throws MalformedURLException
	 */
	// @PreAuthorize("hasAuthority('create:domains')")
	@RequestMapping(path = "/{domain_id}/users", method = RequestMethod.POST)
	@Operation(summary = "Add a user to the given domain", description = "Add a user to the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully added user", content = @Content(schema = @Schema(type = "object", implementation = TestUser.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody TestUser addUser(HttpServletRequest request,
			@PathVariable(value = "domain_id", required = true) long domain_id,
			@RequestBody TestUser test_user)
			throws UnknownAccountException, MalformedURLException {
		Account account = getAuthenticatedAccount(request.getUserPrincipal());

		if (account == null) {
			throw new UnknownAccountException();
		}

		Optional<Domain> optional_domain = domain_service.findById(domain_id);

		log.info("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
		log.info("starting to add user");
		if (optional_domain.isPresent()) {
			log.info("domain : " + domain_id);
			Set<TestUser> test_users = domain_service.getTestUsers(domain_id);

			log.info("Test users : " + test_users.size());
			for (TestUser user : test_users) {
				if (user.getUsername().equals(test_user.getUsername())) {
					log.info("User exists, returning user : " + user);
					return user;
				}
			}

			log.info("Test user does not exist for domain yet");
			test_user.setKey(test_user.generateKey());
			test_user = test_user_repo.save(test_user);
			domain_service.addTestUser(domain_id, test_user.getId());
			log.info("saved domain :: " + domain_id);
			return test_user;
		}
		throw new DomainNotFoundException();
	}

	@RequestMapping(path = "/{domain_id}/users/{user_id}", method = RequestMethod.DELETE)
	@Operation(summary = "Delete a user from the given domain", description = "Delete a user from the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully deleted user"),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody boolean deleteUser(HttpServletRequest request,
			@PathVariable(value = "domain_id", required = true) long domain_id,
			@PathVariable(value = "user_id", required = true) long user_id)
			throws UnknownAccountException, MalformedURLException {
		Account account = getAuthenticatedAccount(request.getUserPrincipal());

		return domain_service.deleteTestUser(domain_id, user_id);
	}
	
	/**
	 * Get Excel file for domain with the given id
	 * 
	 * @param request
	 * @param domain_id
	 * @param username
	 * @param password
	 * @param role
	 * 
	 * @throws UnknownAccountException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	// @PreAuthorize("hasAuthority('create:domains')")
	@RequestMapping(path = "/{domain_id}/report/excel", method = RequestMethod.GET)
	@Operation(summary = "Export the excel report for the given domain", description = "Export the excel report for the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully exported excel report", content = @Content(schema = @Schema(type = "object", implementation = Resource.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody ResponseEntity<Resource> exportExcelReport(HttpServletRequest request,
			@PathVariable(value = "domain_id", required = true) long domain_id)
			throws UnknownAccountException, FileNotFoundException, IOException {
		Optional<Domain> domain_opt = domain_service.findById(domain_id);
		if (!domain_opt.isPresent()) {
			throw new DomainNotFoundException();
		}

		Optional<AuditRecord> domain_audit = audit_record_service.getMostRecentAuditRecordForDomain(domain_opt.get().getId());
		if (!domain_audit.isPresent()) {
			throw new DomainAuditNotFound();
		}

		List<UXIssueReportDto> ux_issues = new ArrayList<>();
		Set<PageAuditRecord> page_audits = audit_record_service.getAllPageAudits(domain_audit.get().getId());
		for (AuditRecord page_audit : page_audits) {
			PageAuditRecord page_audit_record = (PageAuditRecord)page_audit;
			Set<Audit> audits = audit_record_service.getAllAuditsForPageAuditRecord(page_audit_record.getId());
			PageState page = audit_record_service.getPageStateForAuditRecord(page_audit_record.getId());
			for (Audit audit : audits) {
				Set<UXIssueMessage> messages = audit_service.getIssues(audit.getId());

				for (UXIssueMessage message : messages) {
					String element_selector = "";
					if (ObservationType.ELEMENT.equals(message.getType())
							|| ObservationType.COLOR_CONTRAST.equals(message.getType())) {
						ElementState element = ux_issue_service.getElement(message.getId());
						if (element == null) {
							continue;
						}
						element_selector = element.getCssSelector();
					} else {
						element_selector = "No specific element is associated with this issue";
					}

					UXIssueReportDto issue_dto = new UXIssueReportDto(message.getRecommendation(),
							message.getPriority(), message.getDescription(), message.getType(), message.getCategory(),
							message.getWcagCompliance(), message.getLabels(), audit.getWhyItMatters(),
							message.getTitle(), element_selector, page.getUrl());
					ux_issues.add(issue_dto);
				}

			}
		}
		URL sanitized_domain_url = new URL(BrowserUtils.sanitizeUrl(domain_opt.get().getUrl(), false));
		XSSFWorkbook workbook = ReportService.generateDomainExcelSpreadsheet(ux_issues, sanitized_domain_url);

		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			workbook.write(outputStream);

			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Disposition", "attachment; " + sanitized_domain_url.getHost() + ".xlsx");

			return ResponseEntity.ok()
					.contentType(MediaType
							.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
					.cacheControl(CacheControl.noCache()).headers(headers)
					.body(new InputStreamResource(new ByteArrayInputStream(outputStream.toByteArray())));
		}
	}

	/**
	 * Get Excel file for domain with the given id
	 * 
	 * @param request {@link HttpServletRequest}
	 * @param domain_id {@link long}
	 * @param username {@link String}
	 * @param password {@link String}
	 * @param role {@link String}
	 * 
	 * @throws UnknownAccountException {@link UnknownAccountException}
	 * @throws IOException {@link IOException}
	 * @throws FileNotFoundException {@link FileNotFoundException}
	 * @throws URISyntaxException {@link URISyntaxException}
	 */
	// @PreAuthorize("hasAuthority('create:domains')")
	@RequestMapping(path = "/{domain_id}/pdf", method = RequestMethod.GET)
	@Operation(summary = "Export the pdf report for the given domain", description = "Export the pdf report for the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully exported pdf report", content = @Content(schema = @Schema(type = "object", implementation = Resource.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody ResponseEntity<Resource> exportPdfReport(HttpServletRequest request,
												@PathVariable(value = "domain_id", required = true) long domain_id
	) throws UnknownAccountException, FileNotFoundException, IOException, URISyntaxException {
		Optional<Domain> domain_opt = domain_service.findById(domain_id);
		if (!domain_opt.isPresent()) {
			throw new DomainNotFoundException();
		}
		
		Domain domain = domain_opt.get();
		Optional<AuditRecord> domain_audit_opt = audit_record_service.getMostRecentAuditRecordForDomain(domain.getId());
		if (!domain_audit_opt.isPresent()) {
			throw new DomainAuditNotFound();
		}

		DomainAuditRecord domain_audit = (DomainAuditRecord)domain_audit_opt.get();
		
		List<UXIssueReportDto> ux_issues = new ArrayList<>();
		Set<PageAuditRecord> page_audits = audit_record_service.getAllPageAudits(domain_audit.getId());
		URL sanitized_domain_url = new URL(BrowserUtils.sanitizeUrl(domain_opt.get().getUrl(), false));
		
		GeneratePDFReport pdf_report = new GeneratePDFReport(domain.getUrl());
		
		Set<Audit> audits = new HashSet<Audit>();
		for(AuditRecord page_audit : page_audits) {
			PageAuditRecord page_audit_record = (PageAuditRecord)page_audit;
			Set<Audit> page_audit_list = audit_record_service.getAllAuditsForPageAuditRecord(page_audit_record.getId());
			page_audit_record.addAudits( page_audit_list );
			audits.addAll( page_audit_list );
		}
		
		List<AuditSubcategory> needs_improvement = PDFDocUtils.getTopFourCategoriesThatNeedImprovement(audits);
		double overall_score = AuditUtils.calculateScore(audits);
		double aesthetic_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.COLOR_MANAGEMENT);
		double color_palette_score = AuditUtils.calculateScoreByName(audits, AuditName.COLOR_PALETTE);
		double text_contrast_score = AuditUtils.calculateScoreByName(audits, AuditName.TEXT_BACKGROUND_CONTRAST);
		double non_text_contrast_score = AuditUtils.calculateScoreByName(audits, AuditName.NON_TEXT_BACKGROUND_CONTRAST);
		double written_content_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.WRITTEN_CONTENT);
		double paragraphing_score = AuditUtils.calculateScoreByName(audits, AuditName.PARAGRAPHING);
		double visuals_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.IMAGERY);
		double visuals_imagery_score = AuditUtils.calculateScoreByName(audits, AuditName.IMAGE_COPYRIGHT);
		double information_architecture_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.INFORMATION_ARCHITECTURE);
		double branding_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.BRANDING);
		double ease_of_understanding_score = AuditUtils.calculateScoreByName(audits, AuditName.READING_COMPLEXITY);
		
		double percentage_of_passing_large_text_items = AuditUtils.getPercentPassingLargeTextItems(audits);
		double percent_failing_large_text_items = 100.0 - percentage_of_passing_large_text_items;
		
		double percent_failing_small_text_items = AuditUtils.getPercentFailingSmallTextItems(audits);
		double percentage_pages_non_text_issues = AuditUtils.getCountPagesWithSubcategoryIssues(page_audits, AuditSubcategory.NON_TEXT_CONTRAST) / (double)page_audits.size();
		int number_of_pages_paragraphing_issues = AuditUtils.getCountPagesWithIssuesByAuditName(page_audits, AuditName.PARAGRAPHING);
		
		DesignSystem design_system = domain_service.getDesignSystem(domain.getId()).get();
		WCAGComplianceLevel wcag_company_compliance_level = design_system.getWcagComplianceLevel();
		int non_ada_compliant_pages = AuditUtils.getCountOfPagesWithWcagComplianceIssues(page_audits);
		double average_words_per_sentence = AuditUtils.calculateAverageWordsPerSentence(audits);
		double stock_image_percentage = AuditUtils.calculatePercentStockImages(audits);
		double percent_custom_images = 100.0 - stock_image_percentage;
		double avg_reading_complexity = AuditUtils.calculateAverageReadingComplexity(audits);
		String avg_difficulty_string = ContentUtils.getReadingDifficultyRatingByEducationLevel(avg_reading_complexity, 
																								domain_audit.getTargetUserEducation());
		String avg_grade_level = ContentUtils.getReadingGradeLevel(avg_reading_complexity);
		
		pdf_report.writeDocument(needs_improvement,
								domain.getUrl(),
								page_audits.size(),
								(int)overall_score,
								(int)aesthetic_score,
								(int)color_palette_score,
								(int)text_contrast_score,
								(int)percentage_of_passing_large_text_items,
								(int)percent_failing_large_text_items,
								(int)percent_failing_small_text_items,
								(int)non_text_contrast_score,
								(int)percentage_pages_non_text_issues,
								(int)written_content_score,
								(int)ease_of_understanding_score,
								(int)paragraphing_score,
								(int)number_of_pages_paragraphing_issues,
								(int)average_words_per_sentence,
								(int)visuals_score,
								(int)visuals_imagery_score,
								(int)percent_custom_images,
								(int)stock_image_percentage,
								wcag_company_compliance_level,
								(int)information_architecture_score,
								(int)branding_score,
								domain_audit.getColors(),
								avg_difficulty_string,
								avg_grade_level,
								non_ada_compliant_pages);
		
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			pdf_report.write(outputStream);
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Disposition", "attachment; " + sanitized_domain_url.getHost() + ".pdf");

			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType("application/pdf"))
					.cacheControl(CacheControl.noCache()).headers(headers)
					.body(new InputStreamResource(new ByteArrayInputStream(outputStream.toByteArray())));
		}
	}
	
	

	/**
	 * 
	 * @param request
	 * @param user_id
	 * @throws UnknownAccountException
	 */
	// @PreAuthorize("hasAuthority('create:test_user')")
	@RequestMapping(path = "/{domain_id}/test_users/{user_id}", method = RequestMethod.DELETE)
	@Operation(summary = "Delete a user from the given domain", description = "Delete a user from the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully deleted user"),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody void delete(HttpServletRequest request,
			@PathVariable(value = "domain_id") long domain_id,
			@PathVariable(value = "user_id") long user_id) throws UnknownAccountException {
		Account account = getAuthenticatedAccount(request.getUserPrincipal());

		domain_service.deleteTestUser(domain_id, user_id);
	}

	/**
	 * 
	 * @param request
	 * @param domain_id
	 * @return
	 * @throws UnknownAccountException
	 * @throws MalformedURLException
	 */
	// @PreAuthorize("hasAuthority('create:domains')")
	@RequestMapping(path = "{domain_id}/users", method = RequestMethod.GET)
	@Operation(summary = "Get the users for the given domain", description = "Get the users for the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved users", content = @Content(schema = @Schema(type = "object", implementation = TestUser.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody Set<TestUser> getUsers(HttpServletRequest request,
			@PathVariable(value = "domain_id", required = true) long domain_id)
			throws UnknownAccountException, MalformedURLException {
		Account account = getAuthenticatedAccount(request.getUserPrincipal());

		return domain_service.getTestUsers(domain_id);
	}

	/**
	 * Starts an audit on an entire domain
	 * 
	 * @param request
	 * @param page
	 * 
	 * @return
	 * 
	 * @throws Exception
	 */
	// @PreAuthorize("hasAuthority('execute:audits')")
	@RequestMapping(path = "/{domain_id}/start", method = RequestMethod.POST)
	@Operation(summary = "Start an audit on the given domain", description = "Start an audit on the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully started audit", content = @Content(schema = @Schema(type = "object", implementation = DomainDto.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody DomainDto startAudit(HttpServletRequest request, @PathVariable("domain_id") long domain_id)
			throws Exception
	{
		Account account = getAuthenticatedAccount(request.getUserPrincipal());
		
		Optional<Domain> domain_opt = domain_service.findById(domain_id);
		if (!domain_opt.isPresent()) {
			throw new DomainNotFoundException();
		}

		Domain domain = domain_opt.get();
		String lowercase_url = domain.getUrl().toLowerCase();
		URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl(lowercase_url));
		
		// create new audit record
		Set<AuditName> audit_list = getAuditList();
		AuditRecord audit_record = new DomainAuditRecord(ExecutionStatus.IN_PROGRESS, audit_list);
		audit_record.setUrl(domain.getUrl());
		audit_record = audit_record_service.save(audit_record, account.getId(), domain.getId());
		
		domain_service.addAuditRecord(domain.getId(), audit_record.getKey());
		DomainDto domain_dto = new DomainDto( domain.getId(), domain.getUrl(), 0.0);

		log.warn("publishing url message to url topic...");
	    JsonMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
		DomainAuditUrlMessage url_msg = new DomainAuditUrlMessage(account.getId(),
																	audit_record.getId(),
																	sanitized_url.toString(), 
																	BrowserType.CHROME);
		
		String url_msg_str = mapper.writeValueAsString(url_msg);
		url_topic.publish(url_msg_str);
		return domain_dto;
	}

	private Set<AuditName> getAuditList() {
		Set<AuditName> audit_list = new HashSet<>();
		//VISUAL DESIGN AUDIT
		audit_list.add(AuditName.TEXT_BACKGROUND_CONTRAST);
		audit_list.add(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
		
		//INFO ARCHITECTURE AUDITS
		audit_list.add(AuditName.LINKS);
		audit_list.add(AuditName.TITLES);
		audit_list.add(AuditName.ENCRYPTED);
		audit_list.add(AuditName.METADATA);
		
		//CONTENT AUDIT
		audit_list.add(AuditName.ALT_TEXT);
		audit_list.add(AuditName.READING_COMPLEXITY);
		audit_list.add(AuditName.PARAGRAPHING);
		audit_list.add(AuditName.IMAGE_COPYRIGHT);
		audit_list.add(AuditName.IMAGE_POLICY);

		return audit_list;
	}

	/**
	 * Retrieves list of {@link PerformanceInsight insights} with a given key
	 * 
	 * @param key account key
	 * @return {@link PerformanceInsight insight}
	 * @throws UnknownAccountException
	 */
	// @PreAuthorize("hasAuthority('read:actions')")
	@RequestMapping(method = RequestMethod.GET, path = "/audits")
	@Operation(summary = "Get the most recent domain audit record for the given host", description = "Get the most recent domain audit record for the given host")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved domain audit record", content = @Content(schema = @Schema(type = "object", implementation = DomainAuditRecord.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public DomainAuditRecord getMostRecentDomainAuditRecord(HttpServletRequest request,
			@PathVariable(value = "host", required = true) String host) throws UnknownAccountException {
		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account acct = account_service.findByUserId(id);

		if (acct == null) {
			throw new UnknownAccountException();
		}

		log.info("finding all page insights :: " + host);
		
		return (DomainAuditRecord)audit_record_service.getMostRecentAuditRecordForDomain(host).get();
	}

	/**
	 * Retrieves all competitors for the given domain
	 * 
	 * @return list of competitors
	 */
	@RequestMapping(method = RequestMethod.GET, path = "{domain_id}/competitors")
	@Operation(summary = "Get all competitors for the given domain", description = "Get all competitors for the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved competitors", content = @Content(schema = @Schema(type = "object", implementation = CompetitorDto.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody List<CompetitorDto> getAllCompetitors(HttpServletRequest request,
			@PathVariable("domain_id") long domain_id) {
		return domain_service.getCompetitors(domain_id).parallelStream().map(x -> {
			Brand brand = competitor_service.getMostRecentBrand(x.getId());
			boolean is_running = competitor_service.isAnalysisRunning(brand);
			return new CompetitorDto(x.getId(), x.getCompanyName(), x.getUrl(), x.getIndustry(), is_running, brand);
		}).collect(Collectors.toList());

	}

	/**
	 * Creates a new competitor and links it to the given domain
	 * 
	 * @return {@link PerformanceInsight insight}
	 * @throws UnknownAccountException
	 */
	@RequestMapping(method = RequestMethod.POST, path = "{domain_id}/competitors")
	@Operation(summary = "Create a new competitor for the given domain", description = "Create a new competitor for the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully created competitor", content = @Content(schema = @Schema(type = "object", implementation = Competitor.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody Competitor createCompetitor(HttpServletRequest request,
			@PathVariable("domain_id") long domain_id, 
			@RequestBody Competitor competitor) {
		competitor = competitor_service.save(competitor);
		domain_service.addCompetitor(domain_id, competitor.getId());

		return competitor;
	}

	/**
	 * Retrieves the color palettes for all competitors for the given domain
	 * 
	 * @return List of color Lists
	 */
	@RequestMapping(method = RequestMethod.GET, path = "{domain_id}/competitors/palettes")
	@Operation(summary = "Get all competitor palettes for the given domain", description = "Get all competitor palettes for the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved competitor palettes", content = @Content(schema = @Schema(type = "object", implementation = List.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody List<List<String>> getAllCompetitorPalettes(HttpServletRequest request,
			@PathVariable("domain_id") long domain_id) {
		List<List<String>> color_palettes = domain_service.getCompetitors(domain_id).parallelStream()
				.map(x -> competitor_service.getMostRecentBrand(x.getId())).filter(Objects::nonNull).map(x -> x
						.getColors().parallelStream().map(color -> "rgb(" + color + ")").collect(Collectors.toList()))
				.collect(Collectors.toList());

		return color_palettes;

	}

	/**
	 * Creates a new competitor and links it to the given domain
	 * 
	 * @return {@link PerformanceInsight insight}
	 * @throws UnknownAccountException
	 */
	@RequestMapping(method = RequestMethod.POST, path = "{domain_id}/policies")
	@Operation(summary = "Set the allowed image characteristics policy for the given domain", description = "Set the allowed image characteristics policy for the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully set allowed image characteristics policy"),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody void setAllowedImageCharacteristicsPolicy(HttpServletRequest request,
			@PathVariable("domain_id") long domain_id, @RequestBody List<String> allowed_image_characteristics) {
		domain_service.updateAllowedImageCharacteristics(domain_id, allowed_image_characteristics);
	}

	/**
	 * Deletes the given competitor from the given domain
	 * 
	 * @param domain_id     id value for a valid {@link Domain}
	 * @param competitor_id id value for a valid {@link Competitor}
	 */
	@RequestMapping(method = RequestMethod.DELETE, path = "{domain_id}/competitors/{competitor_id}")
	@Operation(summary = "Delete a competitor from the given domain", description = "Delete a competitor from the given domain")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully deleted competitor"),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Insufficient permissions")
	})
	public @ResponseBody void deleteCompetitor(HttpServletRequest request, @PathVariable("domain_id") long domain_id,
			@PathVariable("competitor_id") long competitor_id) {
		competitor_service.deleteById(competitor_id);
	}

	/**
	 * Build audit {@link AuditRecordDTO progress update} for a {@link DomainAuditRecord domain audit}
	 * 
	 * @param audit_msg
	 * 
	 * @return
	 */
	private AuditUpdateDto buildDomainAuditRecordDTO(long audit_record_id) {
		DomainAuditRecord domain_audit = (DomainAuditRecord)audit_record_service.findById(audit_record_id).get();
	    Set<PageAuditRecord> page_audits = audit_record_service.getAllPageAudits(domain_audit.getId());
	    log.warn("total page audits found = "+page_audits.size());
	    int total_pages = page_audits.size();
	    //Set<AuditName> audit_labels = domain_audit.getAuditLabels();
		Set<AuditName> audit_labels = new HashSet<>();
	    Set<Audit> audits = new HashSet<Audit>();
	    for(AuditRecord page_audit: page_audits) {
	    	audits.addAll(audit_record_service.getAllAuditsForPageAuditRecord(page_audit.getId()));
	    }

	    //calculate percentage of audits that are currently complete for each category
		double visual_design_progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS, total_pages, audits, audit_labels);
		double content_progress = AuditUtils.calculateProgress(AuditCategory.CONTENT, total_pages, audits, audit_labels);
		double info_architecture_progress = AuditUtils.calculateProgress(AuditCategory.INFORMATION_ARCHITECTURE, total_pages, audits, audit_labels);
		double data_extraction_progress = getDomainDataExtractionProgress(domain_audit);
		
		
		//Calculate scores
		double content_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.CONTENT);
		
		double info_architecture_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.INFORMATION_ARCHITECTURE);
		
		double visual_design_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.AESTHETICS);
		double text_contrast_score = AuditUtils.calculateScoreByName(audits, AuditName.TEXT_BACKGROUND_CONTRAST);
		double element_contrast_score = AuditUtils.calculateScoreByName(audits, AuditName.NON_TEXT_BACKGROUND_CONTRAST);
		
		double a11y_score = AuditUtils.calculateAccessibilityScore(audits);
			
		ExecutionStatus execution_status = ExecutionStatus.UNKNOWN;
		if(visual_design_progress < 1 || content_progress < 1 || visual_design_progress < 1 || data_extraction_progress < 1) {
			execution_status = ExecutionStatus.IN_PROGRESS;
		}
		else {
			execution_status = ExecutionStatus.COMPLETE;
		}
		
		String message = "";
				
		return new AuditUpdateDto( audit_record_id,
									AuditLevel.DOMAIN,
									content_score,
									content_progress,
									info_architecture_score,
									info_architecture_progress,
									a11y_score,
									visual_design_score,
									visual_design_progress,
									data_extraction_progress,
									message,
									execution_status);
	}
	
	/**
	 * Retrieves journeys from the domain audit and calculates a value between 0 and 1 that indicates the progress
	 * based on the number of journey's that are still in the CANDIDATE status vs the journeys that don't have the CANDIDATE STATUS
	 * 
	 * @param domain_audit
	 * 
	 * @return
	 */
	private double getDomainDataExtractionProgress(DomainAuditRecord domain_audit) {
		assert domain_audit != null;
		
		int candidate_count = audit_record_service.getNumberOfJourneysWithStatus(domain_audit.getId(), JourneyStatus.CANDIDATE);
		int total_journeys = audit_record_service.getNumberOfJourneys(domain_audit.getId());
		
		if(total_journeys <= 1) {
			return 0.01;
		}
		
		return (double)(total_journeys - candidate_count) / (double)total_journeys;

	}
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class DomainNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3490016388183179302L;

	public DomainNotFoundException() {
		super("Domain could not be found.");
	}
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class FormNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6272761200954686735L;

	public FormNotFoundException() {
		super("Form could not be found.");
	}
}

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class DomainAuditNotFound extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7815442042430032220L;

	public DomainAuditNotFound() {
		super("No audits were found for this domain");
	}
}

@ResponseStatus(HttpStatus.SEE_OTHER)
class ExistingAccountDomainException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8549092797919036363L;

	public ExistingAccountDomainException() {
		super("This domain already exists for your account.");
	}
}
