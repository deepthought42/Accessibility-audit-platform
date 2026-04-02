package com.looksee.gcp;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.api.services.pagespeedonline.v5.PagespeedInsights;
import com.google.api.services.pagespeedonline.v5.model.LighthouseAuditResultV5;
import com.google.api.services.pagespeedonline.v5.model.PagespeedApiPagespeedResponseV5;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.audit.recommend.Recommendation;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class for interacting with the Google PageSpeed Insights API
 */
@Component
public class PageSpeedInsightUtils {
	private static Logger log = LoggerFactory.getLogger(PageSpeedInsightUtils.class.getName());

	@Value("${gcp.api.key}")
	private static String API_KEY;

	/**
	 * Retrieves Google PageSpeed Insights result from their API
	 * 
	 * @param url url to get page insights for
	 * @return the page insights
	 * 
	 * @throws IOException if an I/O error occurs
	 * @throws GeneralSecurityException if a security error occurs
	 * 
	 * precondition: url != null
	 * precondition: !url.isEmpty()
	 */
	public PagespeedApiPagespeedResponseV5 getPageInsights(String url) throws IOException, GeneralSecurityException {
	    assert url != null;
	    assert !url.isEmpty();
	    
		JacksonFactory jsonFactory = new JacksonFactory();
	    NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

	    HttpRequestInitializer httpRequestInitializer = null; //this can be null here!
	    PagespeedInsights p = new PagespeedInsights.Builder(transport, jsonFactory, httpRequestInitializer).setApplicationName("CrawlerApi").build();
	    
	    PagespeedInsights.Pagespeedapi.Runpagespeed runpagespeed  = p.pagespeedapi().runpagespeed().setUrl(url).setKey(API_KEY);
	    List<String> category = new ArrayList<>();
	    category.add("performance");
	    category.add("accessibility");
	    //category.add("best-practices");
	    //category.add("pwa");
	    category.add("seo");
	    runpagespeed.setCategory(category);
	    
	    return runpagespeed.execute();
	}
	
	/**
	 * Extracts all accessibility issues from page speed insights api as {@link UXIssueMessage}
	 * 
	 * @param page_speed_response {@link PagespeedApiPagespeedResponseV5} to extract accessibility issues from
	 * @return list of {@link UXIssueMessage}s
	 * 
	 * precondition: page_speed_response != null
	 */
	public List<UXIssueMessage> extractAccessibilityIssues(
			PagespeedApiPagespeedResponseV5 page_speed_response
	) {
		assert page_speed_response != null;
		
		List<UXIssueMessage> ux_issues = new ArrayList<UXIssueMessage>();
		
		log.warn("extracting page speed audit results for accessibility");
	    Map<String, LighthouseAuditResultV5> audit_map = page_speed_response.getLighthouseResult().getAudits();
		   
	    for(LighthouseAuditResultV5 audit_record  : audit_map.values()) {
			Set<Recommendation> recommendations = new HashSet<>();
			
    		UXIssueMessage issue_msg = new UXIssueMessage(
    											Priority.HIGH,
    											audit_record.getDescription(),
    											ObservationType.PAGE_STATE,
    											AuditCategory.INFORMATION_ARCHITECTURE,
    											"wcag compliance",
    											new HashSet<>(),
    											audit_record.getExplanation(),
    											audit_record.getTitle(),
    											-1,
    											-1,
    											"");
    				
    		ux_issues.add(issue_msg);
    		
    		//InsightType insight_type = PerformanceAuditor.getAuditType(audit_record, audit_ref_map);
    		
    		log.warn("audit record id  ....  "+audit_record.getId());
    		log.warn("audit record description  ....  "+audit_record.getDescription());
    		log.warn("audit record display value  ....  "+audit_record.getDisplayValue());
    		log.warn("audit record error msg  ....  "+audit_record.getErrorMessage());
    		log.warn("audit record explanation  ....  "+audit_record.getExplanation());
    		log.warn("audit record score display mode ....  "+audit_record.getScoreDisplayMode());
    		log.warn("audit record title  ....  "+audit_record.getTitle());
    		log.warn("audit record numeric value  ....  "+audit_record.getNumericValue());
    		log.warn("audit record score ....  "+audit_record.getScore());
    		log.warn("audit record warnings  ....  "+audit_record.getWarnings());
    		log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	    }
		return ux_issues;
	}
	
	/**
	 * Extracts all accessibility issues from page speed insights api as {@link UXIssueMessage}
	 * 
	 * @param audit_record {@link LighthouseAuditResultV5} to extract accessibility issues from
	 * 
	 * @return list of {@link UXIssueMessage}s with accessibility issues
	 * 
	 * precondition: audit_record != null
	 */
	public List<UXIssueMessage> extractAccessibilityAuditDetails(LighthouseAuditResultV5 audit_record) {
		assert audit_record != null;

		List<UXIssueMessage> ux_issues = new ArrayList<>();
		if(audit_record.getDetails() != null) {
			List<Object> items = (List)audit_record.getDetails().get("items");
			
			for(Object item: items) {
				ArrayMap<String, Object> obj = (ArrayMap)item;
				ArrayMap<String, Object> json_obj = (ArrayMap)obj.get("node");
				String explanation = json_obj.get("explanation").toString();
				String snippet = json_obj.get("snippet").toString();
				snippet = snippet.replaceAll(">\\s+<","> <");
				snippet = snippet.replace("\n", "");
				snippet = snippet.replace("required=\"\"", "required");
				//ElementState element_state = element_state_service.findByOuterHtml(user_id, snippet);

				int fix_all_idx = explanation.indexOf("Fix all of the following:");
				int fix_any_idx = explanation.indexOf("Fix any of the following:");
				explanation = explanation.trim();
				
				String optional_details = null;
				String required_details = null;
				if(fix_all_idx > 0 && fix_any_idx == 0) {
					optional_details = explanation.substring(0, fix_all_idx);
					required_details = explanation.substring(fix_all_idx);
				}
				else if(fix_all_idx == 0 && fix_any_idx > 0) {
					required_details = explanation.substring(0, fix_all_idx);
					optional_details = explanation.substring(fix_any_idx);
				}
				else {
					if(fix_all_idx >= 0) {
						required_details = explanation;
						optional_details = "";
					}
					else if(fix_any_idx >= 0) {
						optional_details = explanation;
						required_details = "";
					}
				}

				String[] optional_change_messages = optional_details.split("\\n");
				String[] required_change_messages = required_details.split("\\n");

				/*
				AccessibilityDetailNode detail = new AccessibilityDetailNode();
				detail.setOptionalChangeMessages(optional_change_messages);
				detail.setRequiredChangeMessages(required_change_messages);
				detail = (AccessibilityDetailNode)audit_detail_service.save(detail);
				detail.setElement(element_state);
				detail = (AccessibilityDetailNode)audit_detail_service.save(detail);
				*/
				
				
	    		log.warn("audit record id  ....  "+audit_record.getId());
	    		log.warn("audit record description  ....  "+audit_record.getDescription());
	    		log.warn("audit record display value  ....  "+audit_record.getDisplayValue());
	    		log.warn("audit record error msg  ....  "+audit_record.getErrorMessage());
	    		log.warn("audit record explanation  ....  "+audit_record.getExplanation());
	    		log.warn("audit record score display mode ....  "+audit_record.getScoreDisplayMode());
	    		log.warn("audit record title  ....  "+audit_record.getTitle());
	    		log.warn("audit record numeric value  ....  "+audit_record.getNumericValue());
	    		log.warn("audit record score ....  "+audit_record.getScore());
	    		log.warn("audit record warnings  ....  "+audit_record.getWarnings());
				Set<Recommendation> recommendations = new HashSet<>();
				
				UXIssueMessage issue_msg = new UXIssueMessage(
						Priority.HIGH,
						required_details,
						ObservationType.PAGE_STATE,
						AuditCategory.INFORMATION_ARCHITECTURE,
						"wcag compliance",
						new HashSet<>(),
						"",
						audit_record.getTitle(),
						-1,
						-1,
						"");

				ux_issues.add(issue_msg);
			}
		}
		
		return ux_issues;
	}

	/**
	 * Extracts all font size issues from page speed insights api as {@link UXIssueMessage}
	 * 
	 * @param page_speed_response {@link PagespeedApiPagespeedResponseV5}
	 * @return list of {@link UXIssueMessage}s with font size issues
	 *
	 * precondition: page_speed_response != null
	 */
	public List<UXIssueMessage> extractFontSizeIssues(PagespeedApiPagespeedResponseV5 page_speed_response) {
		assert page_speed_response != null;

		List<UXIssueMessage> ux_issues = new ArrayList<UXIssueMessage>();
		
		log.warn("extracting page speed audit results for accessibility");
	    Map<String, LighthouseAuditResultV5> audit_map = page_speed_response.getLighthouseResult().getAudits();
	    LighthouseAuditResultV5 audit_record  = audit_map.get("font-size");
		Set<Recommendation> recommendations = new HashSet<>();
		
		UXIssueMessage issue_msg = new UXIssueMessage(
											Priority.HIGH,
											audit_record.getDescription(),
											ObservationType.PAGE_STATE,
											AuditCategory.INFORMATION_ARCHITECTURE,
											"wcag compliance",
											new HashSet<>(),
											audit_record.getExplanation(),
											audit_record.getTitle(),
											-1, -1, "");
				
		ux_issues.add(issue_msg);
		
		//InsightType insight_type = PerformanceAuditor.getAuditType(audit_record, audit_ref_map);
		
		log.warn("audit record id  ....  "+audit_record.getId());
		
		log.warn("audit record description  ....  "+audit_record.getDescription());
		log.warn("audit record display value  ....  "+audit_record.getDisplayValue());
		log.warn("audit record error msg  ....  "+audit_record.getErrorMessage());
		log.warn("audit record explanation  ....  "+audit_record.getExplanation());
		log.warn("audit record score display mode ....  "+audit_record.getScoreDisplayMode());
		log.warn("audit record title  ....  "+audit_record.getTitle());
		log.warn("audit record numeric value  ....  "+audit_record.getNumericValue());
		log.warn("audit record score ....  "+audit_record.getScore());
		log.warn("audit record warnings  ....  "+audit_record.getWarnings());
		log.warn("audit details :: "+audit_record.getDetails());
		log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		
		return ux_issues;
	}
	
	/**
	 * Extracts all issues from page speed insights api as {@link UXIssueMessage}
	 * 
	 * @param page_speed_response {@link PagespeedApiPagespeedResponseV5}
	 * @return list of {@link UXIssueMessage}s with issues from page speed insights api
	 *
	 * precondition: page_speed_response != null
	 */
	public List<UXIssueMessage> extractIssues(PagespeedApiPagespeedResponseV5 page_speed_response) {
		assert page_speed_response != null;

		List<UXIssueMessage> ux_issues = new ArrayList<UXIssueMessage>();
		
		log.warn("extracting page speed audit results for accessibility");
	    Map<String, LighthouseAuditResultV5> audit_map = page_speed_response.getLighthouseResult().getAudits();
	    
	    for(LighthouseAuditResultV5 audit_record : audit_map.values()) {
			
	    	if(audit_record.getExplanation() != null && !audit_record.getExplanation().trim().contentEquals("null")) {
	    		log.warn("audit record id  ....  "+audit_record.getId());
				log.warn("audit record description  ....  "+audit_record.getDescription());
				log.warn("audit record display value  ....  "+audit_record.getDisplayValue());
				log.warn("audit record error msg  ....  "+audit_record.getErrorMessage());
				log.warn("audit record explanation  ...."+audit_record.getExplanation());
				log.warn("audit record score display mode ....  "+audit_record.getScoreDisplayMode());
				log.warn("audit record title  ....  "+audit_record.getTitle());
				log.warn("audit record numeric value  ....  "+audit_record.getNumericValue());
				log.warn("audit record score ....  "+audit_record.getScore());
				log.warn("audit record warnings  ....  "+audit_record.getWarnings());
				log.warn("audit details :: "+audit_record.getDetails());
				log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	    	}
	    }
	    
	    LighthouseAuditResultV5 audit_record  = audit_map.get("font-size");
    	
		UXIssueMessage issue_msg = new UXIssueMessage(
											Priority.HIGH,
											audit_record.getDescription(),
											ObservationType.PAGE_STATE,
											AuditCategory.INFORMATION_ARCHITECTURE,
											"wcag compliance",
											new HashSet<>(),
											audit_record.getExplanation(),
											audit_record.getTitle(),
											-1,
											-1,
											"");
				
		ux_issues.add(issue_msg);
		
		//InsightType insight_type = PerformanceAuditor.getAuditType(audit_record, audit_ref_map);
		

		return ux_issues;
	}
	
}
