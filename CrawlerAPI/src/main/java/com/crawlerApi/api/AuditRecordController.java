package com.crawlerApi.api;

import java.net.MalformedURLException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.crawlerApi.security.SecurityConfig;
import com.looksee.audits.performance.PerformanceInsight;
import com.looksee.browsing.Crawler;
import com.looksee.exceptions.MissingSubscriptionException;
import com.looksee.exceptions.UnknownAccountException;
import com.looksee.models.Account;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.SimpleElement;
import com.looksee.models.SimplePage;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.AuditScore;
import com.looksee.models.audit.DomainAuditRecord;
import com.looksee.models.audit.ElementIssueTwoWayMapping;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.audit.stats.AuditStats;
import com.looksee.models.audit.stats.DomainAuditStats;
import com.looksee.models.audit.stats.PageAuditStats;
import com.looksee.models.dto.AuditRecordDto;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.enums.JourneyStatus;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.JourneyService;
import com.looksee.services.PageStateService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.AuditUtils;

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
@RequestMapping(path = "v1/auditrecords", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Audit Records V1", description = "Audit Records API")
public class AuditRecordController extends BaseApiController {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

   	public final static long SECS_PER_HOUR = 60 * 60;
	
	@Autowired
	private AccountService account_service;
	
    @Autowired
    protected SecurityConfig appConfig;
    
    @Autowired
    protected AuditService audit_service;
    
    @Autowired
    protected UXIssueMessageService issue_message_service;
    
    @Autowired
    protected AuditRecordService audit_record_service;
    
    @Autowired
    protected Crawler crawler;
    
    @Autowired
    protected PageStateService page_state_service;
    
	@Autowired
	protected JourneyService journey_service;
	
	/**
     * Creates a new {@link AuditRecord observation}
     * 
     * @return returns a List of {@link AuditRecord audits}
	 * 
     * @throws UnknownAccountException
     */
    @RequestMapping(method = RequestMethod.GET)
	@Operation(summary = "Get audit records for the given account", description = "Get the audit records for the given account")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved audit records", content = @Content(schema = @Schema(type = "array", implementation = AuditRecordDto.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "403", description = "Missing subscription")
	})
    public @ResponseBody List<AuditRecordDto> getAuditRecords(
												HttpServletRequest request
	) throws UnknownAccountException {

		Principal principal = request.getUserPrincipal();
		String id = principal.getName();
		Account acct = account_service.findByUserId(id);
		
		if(acct == null){
			throw new UnknownAccountException();
		}
		else if(acct.getSubscriptionToken() == null){
			throw new MissingSubscriptionException();
		}

		List<AuditRecord> audits_records = audit_record_service.findByAccountId(acct.getId());
		return buildAudits(audits_records);
    }
	
	/**
     * Requests a an audit report by creating a user account and adding the audit record to the account.
	 * 
     */
    @RequestMapping(method = RequestMethod.POST, value="/{audit_record_id}/report")
	@Operation(summary = "Request a report for the given audit record", description = "Request a report for the given audit record")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully requested report"),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "404", description = "Audit record not found")
	})
    public @ResponseBody void requestReport(
										HttpServletRequest request,
										@PathVariable("audit_record_id") long audit_record_id,
										@RequestBody Account acct
	) {
    	Account acct_record = account_service.findByEmail(acct.getEmail());
    	if(acct_record == null) {
    		acct_record = account_service.save(acct);
    	}
    	account_service.addAuditRecord(acct_record.getId(), audit_record_id);
    }

    /**
     * Creates a new {@link Observation observation}
     * 
     * @return returns set of {@link SimplePage pages}
     * @throws UnknownAccountException thrown if the account is not found
     */
    @RequestMapping(method = RequestMethod.GET, value="/{audit_record_id}/pages")
	@Operation(summary = "Get pages for the given audit record", description = "Get the pages for the given audit record")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved pages", content = @Content(schema = @Schema(type = "array", implementation = SimplePage.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "404", description = "Audit record not found")
	})
    public @ResponseBody Set<SimplePage> getPages(
						    		HttpServletRequest request,
									@PathVariable("audit_record_id") long audit_record_id
	) {
    	//get audit record
    	Optional<AuditRecord> audit_record = audit_record_service.findById(audit_record_id);
    	
    	if(audit_record.isPresent()) {
    		AuditRecord audit = audit_record.get();
    		if(audit instanceof PageAuditRecord) {
    			PageState page_state = audit_record_service.getPageStateForAuditRecord(audit.getId());
    			
    			Set<SimplePage> pages = new HashSet<>();
    			
    			SimplePage simple_page = new SimplePage( page_state.getUrl(),
														page_state.getViewportScreenshotUrl(),
														page_state.getFullPageScreenshotUrl(),
														page_state.getFullPageWidth(),
														page_state.getFullPageHeight(),
														page_state.getSrc(),
														page_state.getKey(),
														page_state.getId());
    			pages.add(simple_page);
    			return pages;
    		}
    		else {
    			//get all page states for domain audit record
    			Set<PageState> page_states = audit_record_service.getPageStatesForDomainAuditRecord(audit_record_id);
    			Set<SimplePage> pages = new HashSet<>();
    			
    			for(PageState page_state: page_states) {
	    			SimplePage simple_page = new SimplePage( page_state.getUrl(),
															page_state.getViewportScreenshotUrl(),
															page_state.getFullPageScreenshotUrl(),
															page_state.getFullPageWidth(),
															page_state.getFullPageHeight(),
															page_state.getSrc(),
															page_state.getKey(),
															page_state.getId());
	    			pages.add(simple_page);
    			}
    			
    			return pages;
    		}
    	}
    	else {
    		throw new AuditRecordNotFoundException();
    	}
    }
    
    /**
     * Retrieves a dataset consisting of {@link ElementState elements} and {@link UXIssueMessage issues}
     * 
     * @param id
     * @return {@link Audit audit} with given ID
     * @throws MalformedURLException {@link MalformedURLException}
     */
    @RequestMapping(method= RequestMethod.GET, path="/{audit_record_id}/elements")
	@Operation(summary = "Get page audit elements", description = "Get the elements for the given audit record")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved page audit elements", content = @Content(schema = @Schema(type = "object", implementation = ElementIssueTwoWayMapping.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "404", description = "Audit record not found"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
    public @ResponseBody ElementIssueTwoWayMapping getPageAuditElements(
														HttpServletRequest request,
														@PathVariable("audit_record_id") long audit_record_id
	) throws MalformedURLException {
    	//Get most recent audits
		Set<Audit> audits = audit_record_service.getAllAuditsForPageAuditRecord(audit_record_id);

    	//retrieve element set
		Collection<? extends UXIssueMessage> issues = audit_service.retrieveUXIssues(audits);

    	//retrieve issue set
		Collection<SimpleElement> elements = audit_service.retrieveElementSet(issues);

    	//Map audits to page states
		Map<String, Set<String>> element_issue_map = audit_service.generateElementIssuesMap(audits);

    	//generate IssueElementMap
		Map<String, String> issue_element_map = audit_service.generateIssueElementMap(audits);

		AuditScore score = AuditUtils.extractAuditScore(audits);
		String page_src = audit_record_service.getPageStateForAuditRecord(audit_record_id).getSrc();

		try{
			//package both elements into an object definition
			ElementIssueTwoWayMapping mapping = new ElementIssueTwoWayMapping(issues,
													elements,
													issue_element_map,
													element_issue_map,
													score,
													page_src);

			return mapping;
		}
		catch(Exception e){
			log.error("Failed to create issue-element mapping for audit record {}", audit_record_id, e);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build issue map", e);
		}
	}
    
    /**
     * Creates a new {@link Observation observation}
     * 
     * @return {@link PerformanceInsight insight}
     * @throws UnknownAccountException 
     */
    @RequestMapping(method = RequestMethod.GET, value="/{audit_record_id}/stats")
	@Operation(summary = "Get audit stats for the given audit record", description = "Get the stats for the given audit record")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Successfully retrieved audit stats", content = @Content(schema = @Schema(type = "object", implementation = AuditStats.class))),
		@ApiResponse(responseCode = "401", description = "Authentication required"),
		@ApiResponse(responseCode = "404", description = "Audit record not found")
	})
    public @ResponseBody AuditStats getAuditStat(
									HttpServletRequest request,
									@PathVariable("audit_record_id") long audit_record_id
	) throws UnknownAccountException {
    	//get audit record
		Optional<AuditRecord> audit_record_opt = audit_record_service.findById(audit_record_id);

		if(audit_record_opt.isPresent()) {
			AuditRecord audit_record = audit_record_opt.get();

			long page_count = 1;
			double score = 0.0;
			int audit_count = 0;

			double written_content_score = 0.0;
			double imagery_score = 0.0;
			double videos_score = 0.0;
			double audio_score = 0.0;

			double seo_score = 0.0;
			double menu_analysis_score = 0.0;
			double performance_score = 0.0;

			//double aesthetic_score = 0.0;
			double text_color_contrast_score = 0.0;
			double non_text_color_contrast_score = 1.0;
			
			double typography_score = 0.0;
			double whitespace_score = 0.0;
			double branding_score = 0.0;

			Set<Audit> audits = new HashSet<>();
			if(audit_record instanceof DomainAuditRecord){
				audits = audit_record_service.getAllAuditsForDomainAudit(audit_record.getId());
			}
			else if(audit_record instanceof PageAuditRecord){
				audits = audit_record_service.getAllAudits(audit_record.getId());
			}
			
			written_content_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.WRITTEN_CONTENT);
			imagery_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.IMAGERY);
			//videos_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.VIDEOS);
			//audio_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.AUDIO);

			seo_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.SEO);
			//menu_analysis_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.MENU_ANALYSIS);
			//performance_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.PERFORMANCE);
			double link_score = AuditUtils.calculateScoreByName(audits, AuditName.LINKS);

			//aesthetic_score = AuditUtils.calculateScore(audits);
			log.warn("audits = "+audits);
			text_color_contrast_score = AuditUtils.calculateScoreByName(audits, AuditName.TEXT_BACKGROUND_CONTRAST);
			log.warn("text color contrast score = "+text_color_contrast_score);
			non_text_color_contrast_score = AuditUtils.calculateScoreByName(audits, AuditName.NON_TEXT_BACKGROUND_CONTRAST);
			//typography_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.TYPOGRAPHY);
			//whitespace_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.WHITESPACE);
			//branding_score = AuditUtils.calculateSubcategoryScore(audits, AuditSubcategory.BRANDING);

			audits.parallelStream().mapToDouble(audit -> {
				if (audit.getTotalPossiblePoints() == 0) {
					return 1;
				} else {
					return (audit.getPoints() / (double) audit.getTotalPossiblePoints());
				}}).sum();
/*
			for (Audit audit : audits) {
				// get issues
				if (audit.getTotalPossiblePoints() == 0) {
					score += 1;
				} else {
					score += (audit.getPoints() / (double) audit.getTotalPossiblePoints());
				}
			}
			*/
			audit_count += audits.size();

			log.debug("audit record id = "+audit_record.getId());
			log.debug("aesthetic audit progress = "+audit_record.getAestheticAuditProgress());
			double overall_score = ( score / audit_count ) * 100 ;

			//build stats object
			log.debug("page count = " + page_count);
			log.debug("---------------------------------------------");
			Set<AuditName> audit_labels = new HashSet<AuditName>();
			audit_labels.add(AuditName.TEXT_BACKGROUND_CONTRAST);
			audit_labels.add(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
			
			audit_labels.add(AuditName.IMAGE_COPYRIGHT);
			audit_labels.add(AuditName.IMAGE_POLICY);
			audit_labels.add(AuditName.ALT_TEXT);
			audit_labels.add(AuditName.READING_COMPLEXITY);
			audit_labels.add(AuditName.PARAGRAPHING);
			
			audit_labels.add(AuditName.TITLES);
			audit_labels.add(AuditName.LINKS);
			audit_labels.add(AuditName.METADATA);
			audit_labels.add(AuditName.ENCRYPTED);
			
			ExecutionStatus execution_status = ExecutionStatus.UNKNOWN;
			
			double visual_design_progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS,
																	1,
																	audits,
																	audit_labels);
			
			double content_progress = AuditUtils.calculateProgress(AuditCategory.CONTENT,
																	1,
																	audits,
																	audit_labels);

			double info_architecture_progress = AuditUtils.calculateProgress(AuditCategory.INFORMATION_ARCHITECTURE,
																			1,
																			audits,
																			audit_labels);
															
			double content_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.CONTENT);
			double info_architecture_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.INFORMATION_ARCHITECTURE);
			double visual_design_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.AESTHETICS);
			double a11y_score = AuditUtils.calculateAccessibilityScore(audits);
			
			log.warn("Accessibility score = "+a11y_score);
			if(content_progress >= 1 && info_architecture_progress >= 1 && visual_design_progress >= 1) {
				execution_status = ExecutionStatus.COMPLETE;
			}
			else {
				execution_status = ExecutionStatus.IN_PROGRESS;
			}
			
			// get Page Count
			if(audit_record instanceof DomainAuditRecord){
				page_count = audit_record_service.getPageAuditCount(audit_record.getId());
				int journeysExplored = audit_record_service.getNumberOfJourneysWithoutStatus(audit_record.getId(), JourneyStatus.CANDIDATE);
				int journeysTotal = audit_record_service.getNumberOfJourneys(audit_record.getId());
				DomainAuditStats audit_stats = new DomainAuditStats(audit_record.getId(),
														journeysExplored,
														journeysTotal,
														a11y_score,
														content_score,
														written_content_score,
														imagery_score,
														info_architecture_score,
														seo_score,
														visual_design_score,
														text_color_contrast_score,
														non_text_color_contrast_score,
														execution_status,
														link_score);
					return audit_stats;
			}
			else if(audit_record instanceof PageAuditRecord){
				page_count = 1;
				PageAuditStats audit_stats = new PageAuditStats(audit_record.getId(),
														audit_record.getStartTime(),
														audit_record.getEndTime(),
														a11y_score,
														content_score,
														written_content_score,
														imagery_score,
														videos_score,
														audio_score,
														info_architecture_score,
														seo_score,
														menu_analysis_score,
														performance_score,
														visual_design_score,
														text_color_contrast_score,
														non_text_color_contrast_score,
														typography_score,
														whitespace_score,
														branding_score,
														-1,
														execution_status,
														link_score);
					return audit_stats;
			}
			
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown audit record type");
		}
		else {
			throw new AuditRecordNotFoundException();
		}
    }

	/**
	 * Convert list of {@link AuditRecord audit_records} to list of {@link AuditDTO}
	 * 
	 * @param audits_records {@link AuditRecord audit records}
	 * @return {@link AuditRecordDto audit record dto}
	 */
	private List<AuditRecordDto> buildAudits(List<AuditRecord> audits_records) {
		List<AuditRecordDto> auditDtoList = new ArrayList<>();
		for(AuditRecord audit_record: audits_records){

			Set<Audit> audits = new HashSet<>();
			if(audit_record instanceof DomainAuditRecord){
				audits = audit_record_service.getAllAuditsForDomainAudit(audit_record.getId());
			}
			else if(audit_record instanceof PageAuditRecord){
				audits = audit_record_service.getAllAudits(audit_record.getId());
			}

			double content_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.CONTENT);
			double info_architecture_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.INFORMATION_ARCHITECTURE);
			double visual_design_score = AuditUtils.calculateScoreByCategory(audits, AuditCategory.AESTHETICS);

			auditDtoList.add(new AuditRecordDto(audit_record.getId(),
									audit_record.getStatus(),
									audit_record.getLevel(),
									audit_record.getStartTime(),
									visual_design_score,
									content_score,
									info_architecture_score,
									audit_record.getCreatedAt(),
									audit_record.getEndTime(),
									audit_record.getUrl()));
		}

		return auditDtoList;
	}
}

@ResponseStatus(HttpStatus.SEE_OTHER)
class AuditRecordNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 794045239226319408L;

	public AuditRecordNotFoundException() {
		super("Oh no! We couldn't find the audit record you asked for.");
	}
}
