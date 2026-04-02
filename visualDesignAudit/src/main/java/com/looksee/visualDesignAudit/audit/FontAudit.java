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
import org.springframework.stereotype.Component;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.interfaces.IExecutablePageStateAudit;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.audit.recommend.Recommendation;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import com.looksee.utils.ElementStateUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class FontAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(FontAudit.class);
	
	public FontAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.TEXT_BACKGROUND_CONTRAST);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Identifies font sizes and weights for headers and text elements on the page
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		assert page_state != null;
		
		Map<String, List<ElementState>> header_element_map = new HashMap<>();
		for(ElementState element : page_state.getElements()) {
			if(ElementStateUtils.isHeader(element.getName())) {
				if(header_element_map.containsKey(element.getName())) {
					header_element_map.get(element.getName()).add(element);
				}
				else {
					List<ElementState> element_states = new ArrayList<>();
					element_states.add(element);
					header_element_map.put(element.getName(), element_states);
				}
			}
		}
		
		String why_it_matters = "Clean typography, with the use of only 1 to 2 typefaces, invites users to" + 
				" the text on your website. It plays an important role in how clear, distinct" + 
				" and legible the textual content is.";
		
		String ada_compliance = "Your typography meets ADA requirements." + 
				" Images of text are not used and text is resizable. San-Serif typeface has" + 
				" been used across the pages.";
		
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		Set<String> labels = new HashSet<>();
		labels.add("aesthetics");
		
		int score = 0;
		int max_score = 0;
		//check header buckets for consistency within bucket
		for(String header_tag : header_element_map.keySet()) {
			List<String> font_sizes = new ArrayList<>();
			List<String> line_heights = new ArrayList<>();
			List<String> font_weights = new ArrayList<>();
			List<String> font_variants = new ArrayList<>();
			
			log.warn("Header tag :: "+header_tag);
			for(ElementState element : header_element_map.get(header_tag)) {
				String font_size = element.getRenderedCssValues().get("font-size");
				String line_height = element.getRenderedCssValues().get("line-height");
				String font_weight = element.getRenderedCssValues().get("font-weight");
				String font_variant = element.getRenderedCssValues().get("font-variant");
				
				font_sizes.add(font_size);
				line_heights.add(line_height);
				font_weights.add(font_weight);
				font_variants.add(font_variant);
				
				//log.warn("font size :: "+element.getRenderedCssValues().get("font-size"));
				//log.warn("line height :: "+element.getRenderedCssValues().get("line-height"));
				//log.warn("font weight :: "+element.getRenderedCssValues().get("font-weight"));
				//log.warn("font variant :: "+element.getRenderedCssValues().get("font-variant"));
				//log.warn("font family :: "+element.getRenderedCssValues().get("font-family"));
				
			}
			
			font_sizes = makeDistinct(font_sizes);
			line_heights = makeDistinct(line_heights);
			font_weights = makeDistinct(font_weights);
			font_variants = makeDistinct(font_variants);
			
			if(font_sizes.size() > 1) {
				log.warn("font sizes :: "+font_sizes);
				String title = "Font sizes are inconsistent for "+header_tag+" headers on the page";
				String description = "Found that " + header_tag + " headers are using font sizes of "+font_sizes+"";
				String wcag_compliance = "";
				String recommendation = "To have a consistent experience your " + header_tag + " tags should all have the same size.";
				Set<Recommendation> recommendations = new HashSet<>();
				//recommendations.add(new Recommendation(recommendation));
				
				UXIssueMessage ux_issue = new UXIssueMessage(
						Priority.HIGH,
						description,
						ObservationType.TYPOGRAPHY,
						AuditCategory.AESTHETICS,
						wcag_compliance,
						labels,
						why_it_matters,
						title,
						0, 
						1,
						recommendation);
				issue_messages.add(ux_issue);
			}
			else {
				score += 1;
				String title = "Font sizes are consistent for "+header_tag+" headers on the page";
				String description = "Found that " + header_tag + " headers are using font sizes of "+font_sizes+"";
				String wcag_compliance = "";
				String recommendation = "To have a consistent experience your " + header_tag + " tags should all have the same size.";
				Set<Recommendation> recommendations = new HashSet<>();
				//recommendations.add(new Recommendation(recommendation));
				
				UXIssueMessage ux_issue = new UXIssueMessage(
						Priority.HIGH,
						description,
						ObservationType.TYPOGRAPHY,
						AuditCategory.AESTHETICS,
						wcag_compliance,
						labels,
						why_it_matters,
						title,
						1, 
						1,
						recommendation);
				issue_messages.add(ux_issue);
			}
			max_score +=1;
			
			
			if(font_weights.size() > 1) {
				log.warn("font weights:: "+font_weights);
				String title = "Font weights are inconsistent for "+header_tag+" headers on the page";
				String description = "Found that " + header_tag + " headers are using the following font weights. "+font_sizes+"";
				String wcag_compliance = "";
				String recommendation = "To have a consistent experience your " + header_tag + " tags should all have the same weight.";
				Set<Recommendation> recommendations = new HashSet<>();
				//recommendations.add(new Recommendation(recommendation));
				
				UXIssueMessage ux_issue = new UXIssueMessage(
						Priority.HIGH,
						description,
						ObservationType.TYPOGRAPHY,
						AuditCategory.AESTHETICS,
						wcag_compliance,
						labels,
						why_it_matters,
						title,
						0,
						1,
						recommendation);
				issue_messages.add(ux_issue);
			}
			else {
				score += 1;
				log.warn("font weights:: "+font_weights);
				String title = "Font weights are consistent for "+header_tag+" headers on the page";
				String description = "Found that " + header_tag + " headers are using the following font weights. "+font_sizes+"";
				String wcag_compliance = "";
				String recommendation = "";
				Set<Recommendation> recommendations = new HashSet<>();
				//recommendations.add(new Recommendation(recommendation));
				
				UXIssueMessage ux_issue = new UXIssueMessage(
						Priority.HIGH,
						description,
						ObservationType.TYPOGRAPHY,
						AuditCategory.AESTHETICS,
						wcag_compliance,
						labels,
						why_it_matters,
						title,
						1,
						1,
						recommendation);
				issue_messages.add(ux_issue);
			}
			max_score +=1;
		}
		
		
		//audit for font sizes less than 12px only for elements with text
		int font_size_score = 0;
		int total_score = 0;

		for(ElementState element : page_state.getElements()) {
			String font_size_str = element.getRenderedCssValues().get("font-size");
			font_size_str = font_size_str.replace("px", "");
			double font_size = Double.parseDouble(font_size_str.trim());
			boolean owns_text = !element.getOwnedText().isEmpty();
			
			if(owns_text && font_size < 12) {
				
				String title = "font-size is too small for mobile devices";
				String description = "Text has a font size of " + font_size_str + " which is too small to be readable on a mobile device";
				String wcag_compliance = "";
				String recommendation = "Make sure to use a font that is greater than 12px. Anything smaller is impossible to read on a mobile device. On a desktop device, text smaller than 12px can be hard to reduce, especially for he visually impaired";
				Set<Recommendation> recommendations = new HashSet<>();
				//recommendations.add(new Recommendation(recommendation));
				
				UXIssueMessage ux_issue = new UXIssueMessage(
						Priority.HIGH,
						description,
						ObservationType.TYPOGRAPHY,
						AuditCategory.AESTHETICS,
						wcag_compliance,
						labels,
						why_it_matters,
						title,
						0,
						1,
						recommendation);
				
				issue_messages.add(ux_issue);
			}
			else if(owns_text && font_size >=12 ) {
				String title = "font-size is properly sized for mobile devices";
				String description = "Text has a font size of " + font_size_str + " which is properly sized to be readable on a mobile device";
				String wcag_compliance = "";
				String recommendation = "Well done!";
				Set<Recommendation> recommendations = new HashSet<>();
				
				UXIssueMessage ux_issue = new UXIssueMessage(
						Priority.HIGH,
						description,
						ObservationType.TYPOGRAPHY,
						AuditCategory.AESTHETICS,
						wcag_compliance,
						labels,
						why_it_matters,
						title,
						1,
						1,
						recommendation);
				
				issue_messages.add(ux_issue);
				font_size_score++;
			}
			total_score++;
		}
		String description = "";
		
		return new Audit(AuditCategory.AESTHETICS,
						 AuditSubcategory.TYPOGRAPHY,
						 AuditName.FONT,
						 (font_size_score + score),
						 issue_messages,
						 AuditLevel.PAGE,
						 (total_score + max_score),
						 page_state.getUrl(),
						 why_it_matters,
						 description,
						 false);
	}
	

	public static List<String> makeDistinct(List<String> from){
		return from.stream().distinct().sorted().collect(Collectors.toList());
	}
	
}