package com.looksee.visualDesignAudit.audit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.models.Browser;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.interfaces.IExecutablePageStateAudit;
import com.looksee.models.audit.messages.ElementStateIssueMessage;
import com.looksee.models.audit.messages.TypefacesIssue;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.services.PageStateService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.BrowserUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class TypefacesAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TypefacesAudit.class);
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	public TypefacesAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.TEXT_BACKGROUND_CONTRAST);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Identifies the typefaces used on the page and the score for how the typefaces used conform to scheme
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		assert page_state != null;
		
		List<String> font_families = new ArrayList<>();
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		List<String> raw_stylesheets = Browser.extractStylesheets(page_state.getSrc()); 
		
		//open stylesheet
		for(String stylesheet : raw_stylesheets) {
			font_families.addAll(BrowserUtils.extractFontFamiliesFromStylesheet(stylesheet));
		}
		
		log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		log.warn("font families   ::        "+font_families.size());
		log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		log.warn("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
		
		List<String> primary_typefaces = new ArrayList<>();
		Map<String, String[]> typeface_map = new HashMap<>();
		//identify 2 main typefaces. These are going to be the first fonts used in font-family sets
		for(String font_family_set : font_families) {
			String[] typeface_set = font_family_set.split(",");
			String primary_typeface = typeface_set[0];
			
			primary_typefaces.add(primary_typeface.trim().toLowerCase());
			typeface_map.put(primary_typeface, typeface_set);
		}
		
		primary_typefaces = primary_typefaces.stream().distinct().collect(Collectors.toList());
		log.warn("primary typefaces ::    " + primary_typefaces);
		log.warn("primary typefaces  size() ::    " + primary_typefaces.size());

		
		String why_it_matters = "Clean typography, with the use of only 1 to 2 typefaces, invites users to" + 
				" the text on your website. It plays an important role in how clear, distinct" + 
				" and legible the textual content is.";
		
		String ada_compliance = "Your typography meets ADA requirements." + 
				" Images of text are not used and text is resizable. San-Serif typeface has" + 
				" been used across the pages.";
			
		Set<String> labels = new HashSet<>();
		labels.add("typography");
		labels.add("content");
		
		//SCORE PRIMARY TYPEFACES 
		int score = 0;
		int total_possible_points = 3;
		log.warn("Font families found  :::   "+primary_typefaces.size());
	
		//evaluate typefaces
		if(primary_typefaces.size() <= 2) {
			score += 1;
			String recommendation = "You might want to consider using 2 typefaces for the best experience";
			TypefacesIssue observation = new TypefacesIssue(
														primary_typefaces, 
														"Only 1 typeface was found", 
														recommendation,
														Priority.LOW,
														AuditCategory.AESTHETICS,
														labels,
														ada_compliance, 
														1,
														1, "");
			
			issue_messages.add(issue_message_service.save(observation));
		}
		else if(primary_typefaces.size() > 2) {
			score += 0;
			String recommendation = "For the best experience, you should use no more than 2 typefaces";
			
			TypefacesIssue observation = new TypefacesIssue(
														primary_typefaces, 
														"Identified " +primary_typefaces.size()+" typefaces.  ( " + primary_typefaces+ "). With too many typefaces your user experience will seem incoherent and inconsistent. Simplicity is best and you should have no more than 2 typefaces", 
														recommendation, 
														Priority.LOW,
														AuditCategory.AESTHETICS,
														labels,
														ada_compliance,
														0,
														1, "");
			issue_messages.add(issue_message_service.save(observation));
		}
		total_possible_points++;		
		
		
		//SCORE TYPEFACE SEQUENCE CONSISTENCY - TYPEFACES SHOULD ALWAYS APPEAR WITH THE SAME TYPEFACES.
		//validate that sets that start with the same typeface have the same number of typefaces for backup, and that they are in the same order
		
		
		//map font sequences
		Map<String, List<String>> forward_connection_graph = new HashMap<>();
		for(String primary_typeface : typeface_map.keySet()) {
			String[] typeface_arr = typeface_map.get(primary_typeface);
			
			for(int idx = 0; idx < typeface_arr.length; idx++) {
				List<String> linked_typefaces = new ArrayList<>();
				if(forward_connection_graph.containsKey(typeface_arr[idx])) {
					linked_typefaces = forward_connection_graph.get(typeface_arr[idx]);
				}
				if(idx < (typeface_arr.length - 1)){
					linked_typefaces.add(typeface_arr[idx+1]);
				}
				forward_connection_graph.put(typeface_arr[idx], linked_typefaces);
			}
		}
		
		//reduce graph instances to unique sets
		for(String font : forward_connection_graph.keySet()) {
			forward_connection_graph.put(font, forward_connection_graph.get(font).stream().distinct().collect(Collectors.toList()));
		}
		
		//check graph for loop - if any keys within graph have more than 1 font associated with it.
		for(String font : forward_connection_graph.keySet() ) {
			//if connected set has more than 1 element then an inconsistency exists
			if(forward_connection_graph.get(font).size() > 1){
				score += 0;
				String recommendation = "Typefaces that are listed for font cascading should always appear in the same order. We found fonts that are competing for the same position in the cascading list. This can create an inconsistent experience and should be avoided.";
				String description = "Too many typefaces used";
				TypefacesIssue issue = new TypefacesIssue(forward_connection_graph.get(font), 
															description,
															recommendation,
															Priority.LOW,
															AuditCategory.AESTHETICS,
															labels,
															ada_compliance,
															0,
															1, "");
				issue_messages.add(issue_message_service.save(issue));
			}
			else {
				score += 1;
				String recommendation = "";
				String description = "Typefaces are consistently used and you are using just the right number of typefaces to keep the interface interesting without being too busy";
				TypefacesIssue issue = new TypefacesIssue(forward_connection_graph.get(font), 
															description,
															recommendation,
															Priority.LOW,
															AuditCategory.AESTHETICS,
															labels,
															ada_compliance,
															1,
															1, "");
				issue_messages.add(issue_message_service.save(issue));
			}
			
			total_possible_points++;
		}
		
		//END TYPEFACE SEQUENCE CONSISTENCY SCORING
		
		
		
		//GET TYPEFACES ACTUALLY RENDERED BY SYSTEM AND GENERATE SCORE BASED ON TYPEFACE CASCADE SETTINGS
		List<ElementState> element_list = BrowserUtils.getTextElements(page_state_service.getElementStates(page_state.getKey()));
		Set<String> observed_fonts = new HashSet<>();
		Set<UXIssueMessage> no_fallback_font = new HashSet<>();

		
		for(ElementState element : element_list) {
			
			String font_family = element.getRenderedCssValues().get("font-family");
			if(primary_typefaces.contains(font_family) ) {
				score +=1;
				String recommendation = "";
				String description = "Text element rendered using the primary typeface.";
				String title = "Text rendered with a primary typeface";

				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.MEDIUM,
																	description,
																	recommendation,
																	element,
																	AuditCategory.AESTHETICS,
																	labels,
																	ada_compliance,
																	title,
																	1,
																	1);
				no_fallback_font.add(issue_message);
			}
			else {
				String recommendation = "Check that the primary font is included and loaded correctly";
				String description = "Text element rendered with a fallback typeface instead of the desired font.";
				String title = "Text rendered with a fallback typeface";

				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.MEDIUM,
																description,
																recommendation,
																element,
																AuditCategory.AESTHETICS,
																labels,
																ada_compliance,
																title,
																0,
																1);
				no_fallback_font.add(issue_message);
			}
			
			total_possible_points += 1;
			
			observed_fonts.add(font_family);
		}
		
		String description = "";
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.AESTHETICS.toString());
		
		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.TYPOGRAPHY,
						 AuditName.TYPEFACES,
						 score,
						 issue_messages,
						 AuditLevel.PAGE,
						 total_possible_points,
						 page_state.getUrl(),
						 why_it_matters,
						 description,
						 false);
	}
}