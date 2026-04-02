package com.looksee.visualDesignAudit.audit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.models.ColorData;
import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.interfaces.IExecutablePageStateAudit;
import com.looksee.models.audit.messages.ColorContrastIssueMessage;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.audit.recommend.ColorContrastRecommendation;
import com.looksee.models.audit.recommend.Recommendation;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.models.enums.WCAGComplianceLevel;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ColorUtils;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class TextColorContrastAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TextColorContrastAudit.class);
	
	@Autowired
	private AuditService audit_service;

	@Autowired
	private UXIssueMessageService issue_message_service;
	
	public TextColorContrastAudit() {}

	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 * 
	 * WCAG Success Criteria Source - https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html
	 * 
	 * There is no level A compliance
	 * Level AA is the requirement used withiin common laws and standards
	 * Level AAA This is for companies looking to provide an exceptional experience with color contrast
	 * 
	 * Compliance level is determined by the {@link DesignSystem} if it isn't null, otherwise defaults to AAA level
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		assert page_state != null;
		
		WCAGComplianceLevel wcag_compliance = WCAGComplianceLevel.AAA;
		
		if(design_system != null) {
			wcag_compliance = design_system.getWcagComplianceLevel();
			
			if(wcag_compliance.equals(WCAGComplianceLevel.A)) {
				return null;
			}
		}
		
		//List<ElementState> elements = page_state_service.getElementStates(page_state.getId());
		//filter elements that aren't text elements
		List<ElementState> element_list = BrowserUtils.getTextElements(page_state.getElements());
		
		String why_it_matters = "Color, just like the overall design, goes beyond aesthetics. It impacts the" + 
				" usability and functionality of your website, deciding what information" + 
				" stands out to the user." + 
				" A good contrast ratio makes your content easy to read and navigate" + 
				" through, creating a comfortable and engaging experience for your user. ";

		Set<UXIssueMessage> issue_messages = new HashSet<>();
		
		//analyze screenshots of all text images for contrast
		for(ElementState element : element_list) {
			Set<String> labels = new HashSet<>();
			labels.add(AuditCategory.AESTHETICS.toString().toLowerCase());
			labels.add("accessibility");
			labels.add("color contrast");
			labels.add("wcag");
			
			ColorData background_color = new ColorData(element.getBackgroundColor());
			ColorData font_color = new ColorData(element.getForegroundColor());		

			String og_font_size_str = element.getRenderedCssValues().get("font-size");
			String font_weight = element.getRenderedCssValues().get("font-weight");
			String font_size_str = og_font_size_str.replace("px", "");
			
			double font_size = BrowserUtils.convertPxToPt(Double.parseDouble(font_size_str.trim()));
			//if font size is greater than 18 point(24px) or if greater than 14 point(18.5px) and bold then check if contrast > 3 ("A Compliance")
			//NOTE: The following measures of font size are in pixels not font points
			if(font_size >= 18 || (font_size >= 14 && BrowserUtils.isTextBold(font_weight))) {
				
				if( element.getTextContrast() < 3 ) {
					//low contrast header issue
					String title = "Large text has low contrast";
					String ada_compliance = "Text that is larger than 18 point or larger than 14 point and bold should meet the minimum contrast ratio of 3:1.";
					String description = "Headline text has low contrast against the background";
					String recommendation = "Increase the contrast by either making the text darker or the background lighter";
					
					Set<Recommendation> recommendations = generateTextContrastRecommendations(font_color,
																							background_color,
																							font_size,
																							!BrowserUtils.isTextBold(font_weight));
	
					ColorContrastIssueMessage low_header_contrast_observation = new ColorContrastIssueMessage(
																							Priority.HIGH,
																							description,
																							element.getTextContrast(),
																							font_color.rgb(),
																							background_color.rgb(),
																							element,
																							AuditCategory.AESTHETICS,
																							labels,
																							ada_compliance,
																							title,
																							font_size+"",
																							0,
																							2,
																							recommendation);

					//check if element already has this issue associated
					boolean was_executed = issue_message_service.hasAuditBeenExecuted(AuditName.TEXT_BACKGROUND_CONTRAST, element.getId());
					
					//if element does NOT have issue associated, then save issue and associate with element
					if(!was_executed) {
						low_header_contrast_observation = issue_message_service.saveColorContrast(low_header_contrast_observation);
						issue_message_service.addElement(low_header_contrast_observation.getId(), element.getId());
						issue_messages.add(low_header_contrast_observation);							
					}
				}
				else if(element.getTextContrast() >= 3 && element.getTextContrast() < 4.5) {
					if(WCAGComplianceLevel.AAA.equals(wcag_compliance) || WCAGComplianceLevel.UNKNOWN.equals(wcag_compliance)){

						//100% score
						//AA WCAG 2.1
						String title = "Large text is not compliant for level " + wcag_compliance;
						String ada_compliance = "Text that is larger than 18pt font or larger than 14pt and bolded should meets minimum contrast of 4.5:1 for WCAG 2.1 AAA standard.";
						//String description = "Headline text has recommended contrast against the background for <a href='https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html'>WCAG 2.1 AA</a> standard";
						String description = "Headline text doesn't meet recommended contrast against the background for WCAG 2.1 AAA standard";
						labels.add("WCAG 2.1 AAA");

						String recommendation = "To reach AAA standards for WCAG 2.1 increase contrast to 4.5:1";
						Set<Recommendation> recommendations = generateTextContrastRecommendations(font_color, background_color, font_size, !BrowserUtils.isTextBold(font_weight));
						
						ColorContrastIssueMessage low_header_contrast_observation = new ColorContrastIssueMessage(
																								Priority.MEDIUM,
																								description,
																								element.getTextContrast(),
																								font_color.rgb(),
																								background_color.rgb(),
																								element,
																								AuditCategory.AESTHETICS,
																								labels,
																								ada_compliance,
																								title,
																								font_size+"",
																								1,
																								2,
																								recommendation);
						
						//check if element already has this issue associated
						boolean was_executed = issue_message_service.hasAuditBeenExecuted(AuditName.TEXT_BACKGROUND_CONTRAST, element.getId());
						
						//if element does NOT have issue associated, then save issue and associate with element
						if(!was_executed) {
							low_header_contrast_observation = issue_message_service.saveColorContrast(low_header_contrast_observation);
							issue_message_service.addElement(low_header_contrast_observation.getId(), element.getId());
							issue_messages.add(low_header_contrast_observation);							
						}
					}
					else {
						
						//100% score
						//low contrast header issue
						String title = "Large text complies with WCAG 2.1 " + wcag_compliance + " standard";
						String ada_compliance = "Text that is larger than 18pt font or larger than 14pt and bolded should meets minimum contrast of 3:1 to meet WCAG 2.1 AA standards.";
						//String description = "Headline text has recommended contrast for <a href='https://www.w3.org/WAI/WCAG21/Understanding/contrast-enhanced.html'>WCAG 2.1 AAA</a> standards against the background";
						String description = "Headline text has recommended contrast for WCAG 2.1 AA standards against the background";
						labels.add("WCAG 2.1 AAA");
						Set<Recommendation> recommendations = new HashSet<>();
						
						ColorContrastIssueMessage low_header_contrast_observation = new ColorContrastIssueMessage(
																								Priority.NONE,
																								description,
																								element.getTextContrast(),
																								font_color.rgb(),
																								background_color.rgb(),
																								element,
																								AuditCategory.AESTHETICS,
																								labels,
																								ada_compliance,
																								title,
																								font_size+"",
																								2,
																								2,
																								"");
						
						//check if element already has this issue associated
						boolean was_executed = issue_message_service.hasAuditBeenExecuted(AuditName.TEXT_BACKGROUND_CONTRAST, element.getId());
						
						//if element does NOT have issue associated, then save issue and associate with element
						if(!was_executed) {
							low_header_contrast_observation = issue_message_service.saveColorContrast(low_header_contrast_observation);
							issue_message_service.addElement(low_header_contrast_observation.getId(), element.getId());
							issue_messages.add(low_header_contrast_observation);
						}
					}
				}
				else if(element.getTextContrast() >= 4.5) {
					if(WCAGComplianceLevel.AAA.equals(wcag_compliance) || WCAGComplianceLevel.UNKNOWN.equals(wcag_compliance)){

						//100% score
						//low contrast header issue
						String title = "Large text complies with WCAG 2.1 " + wcag_compliance + " standard";
						String ada_compliance = "Text that is larger than 18pt font or larger than 14pt and bolded should meets minimum contrast of 4.5:1 to meet WCAG 2.1 AAA standards.";
						//String description = "Headline text has recommended contrast for <a href='https://www.w3.org/WAI/WCAG21/Understanding/contrast-enhanced.html'>WCAG 2.1 AAA</a> standards against the background";
						String description = "Headline text has recommended contrast for WCAG 2.1 AAA standards against the background";
						labels.add("WCAG 2.1 AAA");
						Set<Recommendation> recommendations = new HashSet<>();
						
						ColorContrastIssueMessage low_header_contrast_observation = new ColorContrastIssueMessage(
																								Priority.NONE,
																								description,
																								element.getTextContrast(),
																								font_color.rgb(),
																								background_color.rgb(),
																								element,
																								AuditCategory.AESTHETICS,
																								labels,
																								ada_compliance,
																								title,
																								font_size+"",
																								2,
																								2,
																								"");
						
						//check if element already has this issue associated
						boolean was_executed = issue_message_service.hasAuditBeenExecuted(AuditName.TEXT_BACKGROUND_CONTRAST, element.getId());
						
						//if element does NOT have issue associated, then save issue and associate with element
						if(!was_executed) {
							low_header_contrast_observation = issue_message_service.saveColorContrast(low_header_contrast_observation);
							issue_message_service.addElement(low_header_contrast_observation.getId(), element.getId());
							issue_messages.add(low_header_contrast_observation);
						}
					}
				}
			}
			else if((font_size < 18 && font_size >= 14 && !BrowserUtils.isTextBold(font_weight)) || font_size < 14 ) {
				if( element.getTextContrast() < 4.50 ) {	
					//fail
					String title = "Text has low contrast";
					String description = "Text has low contrast against the background";
					String ada_compliance = "Text that is smaller than 18 point and larger than 14 point but not bold or just smaller than 14 point fonts should meet the minimum contrast ratio of 4.5:1.";
					String recommendation = "Increase the contrast by either making the text darker or the background lighter";
					Set<Recommendation> recommendations = generateTextContrastRecommendations(font_color, 
																							  background_color, 
																							  font_size, 
																							  !BrowserUtils.isTextBold(font_weight));
					
					ColorContrastIssueMessage low_text_observation = new ColorContrastIssueMessage(
																				Priority.HIGH,
																				description,
																				element.getTextContrast(),
																				font_color.rgb(),
																				background_color.rgb(),
																				element,
																				AuditCategory.AESTHETICS,
																				labels,
																				ada_compliance,
																				title,
																				font_size+"",
																				0,
																				2,
																				recommendation);
					//check if element already has this issue associated
					boolean was_executed = issue_message_service.hasAuditBeenExecuted(AuditName.TEXT_BACKGROUND_CONTRAST, element.getId());
					
					//if element does NOT have issue associated, then save issue and associate with element
					if(!was_executed) {
						//No points are rewarded for low contrast text
						low_text_observation = issue_message_service.saveColorContrast(low_text_observation);
						issue_message_service.addElement(low_text_observation.getId(), element.getId());
						issue_messages.add(low_text_observation);
					}
				}
				else if(element.getTextContrast() >= 4.50 && element.getTextContrast() < 7.0) {
					if(WCAGComplianceLevel.AAA.equals(wcag_compliance) || WCAGComplianceLevel.UNKNOWN.equals(wcag_compliance)){

						//100% score
						String title = "Text doesn't meet WCAG 2.1 " + wcag_compliance + " standards";
						String description = "Text doesn't meet minimum contrast requirements for WCAG 2.1 AAA compliance";
						String ada_compliance = title;
						String recommendation = "To reach AAA standards for WCAG 2.1 increase contrast to 7:1";

						labels.add("WCAG 2.1 AA");
						Set<Recommendation> recommendations = generateTextContrastRecommendations(font_color, background_color, font_size, !BrowserUtils.isTextBold(font_weight));
						
						ColorContrastIssueMessage med_contrast_text_observation = new ColorContrastIssueMessage(
																					Priority.MEDIUM,
																					description,
																					element.getTextContrast(),
																					font_color.rgb(),
																					background_color.rgb(),
																					element,
																					AuditCategory.AESTHETICS,
																					labels,
																					ada_compliance,
																					title,
																					font_size+"",
																					1,
																					2,
																					recommendation);
						
						//check if element already has this issue associated
						boolean was_executed = issue_message_service.hasAuditBeenExecuted(AuditName.TEXT_BACKGROUND_CONTRAST, element.getId());
						
						//if element does NOT have issue associated, then save issue and associate with element
						if(!was_executed) {
							med_contrast_text_observation = issue_message_service.saveColorContrast(med_contrast_text_observation);
							issue_message_service.addElement(med_contrast_text_observation.getId(), element.getId());
							issue_messages.add(med_contrast_text_observation);
						}
					}
					else {
						//100% score
						String title = "Text has appropriate contrast";
						String description = "Text has recommended contrast against the background";
						String ada_compliance = "Text contrast meets WCAG 2.1 " + wcag_compliance + " standards.";
						labels.add("WCAG 2.1 AAA");
						
						ColorContrastIssueMessage high_contrast_text_observation = new ColorContrastIssueMessage(
																					Priority.NONE,
																					description,
																					element.getTextContrast(),
																					font_color.rgb(),
																					background_color.rgb(),
																					element,
																					AuditCategory.AESTHETICS,
																					labels,
																					ada_compliance,
																					title,
																					font_size+"",
																					2,
																					2,
																					"");
						//check if element already has this issue associated
						boolean was_executed = issue_message_service.hasAuditBeenExecuted(AuditName.TEXT_BACKGROUND_CONTRAST, element.getId());
						
						//if element does NOT have issue associated, then save issue and associate with element
						if(!was_executed) {
							high_contrast_text_observation = issue_message_service.saveColorContrast(high_contrast_text_observation);
							issue_message_service.addElement(high_contrast_text_observation.getId(), element.getId());
							issue_messages.add(high_contrast_text_observation);
						}
					}
				}
				else if(element.getTextContrast() >= 7.0) {
					if(WCAGComplianceLevel.AAA.equals(wcag_compliance) || WCAGComplianceLevel.UNKNOWN.equals(wcag_compliance)){

						//100% score
						String title = "Text has appropriate contrast";
						String description = "Text has recommended contrast against the background";
						String ada_compliance = "Text contrast meets WCAG 2.1 enhanced(AAA) standards.";
						labels.add("WCAG 2.1 AAA");
						
						ColorContrastIssueMessage high_contrast_text_observation = new ColorContrastIssueMessage(
																					Priority.NONE,
																					description,
																					element.getTextContrast(),
																					font_color.rgb(),
																					background_color.rgb(),
																					element,
																					AuditCategory.AESTHETICS,
																					labels,
																					ada_compliance,
																					title,
																					font_size+"",
																					2,
																					2,
																					"");
						
						//check if element already has this issue associated
						boolean was_executed = issue_message_service.hasAuditBeenExecuted(AuditName.TEXT_BACKGROUND_CONTRAST, element.getId());
						
						//if element does NOT have issue associated, then save issue and associate with element
						if(!was_executed) {
							high_contrast_text_observation = issue_message_service.saveColorContrast(high_contrast_text_observation);
							log.warn("high contrast text issue = "+high_contrast_text_observation);
							issue_message_service.addElement(high_contrast_text_observation.getId(), element.getId());
							issue_messages.add(high_contrast_text_observation);		
						}
					}
				}
			}

		}
		
		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			
			points_earned += issue_msg.getPoints();
			max_points += issue_msg.getMaxPoints();
		}
		
		Audit audit = new Audit(AuditCategory.AESTHETICS,
								 AuditSubcategory.COLOR_MANAGEMENT,
							     AuditName.TEXT_BACKGROUND_CONTRAST,
							     points_earned,
								 issue_messages,
							     AuditLevel.PAGE,
							     max_points,
							     page_state.getUrl(),
							     why_it_matters,
							     "Text with contrast below 4.5",
							     true);
		
		audit = audit_service.save(audit);
		audit_service.addAllIssues(audit.getId(), issue_messages);
		return audit;
	}

	/**
	 * Generates {@link Set} of {@link ColorContrastRecommendation recommendations} based on the text color, background color and font_size
	 * 	NOTE : assumes a light color scheme only. Doesn't currently account for dark color scheme
	 * 
	 * @param font_color
	 * @param background_color
	 * @param font_size
	 * @param is_bold TODO
	 * 
	 * @pre font_color != null
	 * @pre background_color != null
	 * 
	 * @return
	 */
	private Set<Recommendation> generateTextContrastRecommendations(ColorData font_color,
																	 ColorData background_color, 
																	 double font_size, 
																	 boolean is_bold) {
		assert font_color != null;
		assert background_color != null;
		
		Set<Recommendation> recommendations = new HashSet<>();
		
		//generate color suggestions with different background color shades (text doesn't change)
		
		boolean is_dark_theme = false;
		//if text is lighter than background then it's dark theme
		//otherwise light theme
		
		ColorContrastRecommendation recommended_bg_color = ColorUtils.findCompliantBackgroundColor(font_color, background_color, is_dark_theme, font_size, is_bold);
		recommendations.add( recommended_bg_color);
		
		//generate color suggestions with different text color shades (background doesn't change)
		ColorContrastRecommendation recommended_font_color = ColorUtils.findCompliantFontColor(font_color, background_color, is_dark_theme, font_size, is_bold);
		recommendations.add( recommended_font_color);
		
		
		//generate color suggestions with varying text and background colors that are within a bounded range of the original color
		// NOTE: This involves pushing these values in opposing directions until we find a pair that meets WCAG 2.1 AAA standards. 
		//       Then, the pair of colors are shifted together to find new color pairs
		
		
		return recommendations;
	}

}