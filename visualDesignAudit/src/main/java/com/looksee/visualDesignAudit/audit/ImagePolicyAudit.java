package com.looksee.visualDesignAudit.audit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.models.ElementState;
import com.looksee.models.ImageElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.Score;
import com.looksee.models.audit.interfaces.IExecutablePageStateAudit;
import com.looksee.models.audit.messages.ElementStateIssueMessage;
import com.looksee.models.audit.messages.ReadingComplexityIssueMessage;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;
import com.looksee.utils.BrowserUtils;

import lombok.NoArgsConstructor;

/**
 * Responsible for executing an audit on images on a page to determine
 * compliance with content policy restrictions for adult content and violence
 * based on the design system's allowed image characteristics.
 *
 * <p>This audit evaluates images to ensure they comply with content policy
 * restrictions for adult content and violence. Images that contain adult
 * content or violence are considered non-compliant and receive 0 points,
 * while images that do not contain adult content or violence are considered
 * compliant and receive 1 point.
 *
 * This audit is not part of the WCAG 2.1 compliance audit. Instead this audit
 * is focused on ensuring that images comply with a brand's content policy
 * restrictions for adult content and violence.
 */
@Component
@NoArgsConstructor
public class ImagePolicyAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ImagePolicyAudit.class);
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	/**
	 * Executes a content policy audit on images to ensure compliance with design system
	 * restrictions for adult content and violence.
	 *
	 * <p><strong>Preconditions:</strong></p>
	 * <ul>
	 *   <li>{@code page_state} must not be null</li>
	 *   <li>{@code page_state.getElements()} must return a valid collection of ElementState objects</li>
	 *   <li>{@code design_system} must not be null and must have valid allowed image characteristics</li>
	 *   <li>{@code audit_service} and {@code issue_message_service} must be properly injected</li>
	 *   <li>{@code BrowserUtils.getTextElements()} must return a list of elements that may contain ImageElementState objects</li>
	 * </ul>
	 * 
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null Audit object with category CONTENT, subcategory IMAGERY, and name IMAGE_POLICY</li>
	 *   <li>All image elements from the page state have been evaluated for policy compliance</li>
	 *   <li>Issue messages have been created and saved for each image element (compliance or violation)</li>
	 *   <li>The returned audit contains the total score calculated from all image elements</li>
	 *   <li>All issue messages are associated with the returned audit</li>
	 *   <li>Each image element has been checked against the design system's allowed image characteristics</li>
	 * </ul>
	 * 
	 * <p><strong>Invariants:</strong></p>
	 * <ul>
	 *   <li>Points earned cannot exceed max points possible</li>
	 *   <li>Each image element generates exactly one issue message</li>
	 *   <li>All issue messages have appropriate priority levels (MEDIUM for violations, NONE for compliance)</li>
	 *   <li>Issue messages contain proper labels: "imagery" and "policy"</li>
	 *   <li>Violation issues have 0 points earned, compliance issues have 1 point earned</li>
	 *   <li>Each image element contributes exactly 1 point to the maximum possible score</li>
	 * </ul>
	 * 
	 * <p><strong>Behavior:</strong></p>
	 * <ul>
	 *   <li>Filters page elements to find only image elements using {@code BrowserUtils.getTextElements()}</li>
	 *   <li>For each image element, checks {@code isAdultContent()} and {@code isViolentContent()} properties</li>
	 *   <li>Compares image characteristics against {@code design_system.getAllowedImageCharacteristics()}</li>
	 *   <li>Creates violation issues for images with adult content when "ADULT" is not in allowed characteristics</li>
	 *   <li>Creates violation issues for images with violent content when "VIOLENCE" is not in allowed characteristics</li>
	 *   <li>Creates compliance issues for images that meet policy requirements</li>
	 *   <li>Calculates overall policy compliance score based on compliance rate</li>
	 *   <li>Persists all audit data and issue messages to the database</li>
	 *   <li>Returns a completed Audit object with policy compliance results</li>
	 * </ul>
	 * 
	 * @param page_state The page state containing elements to audit, must not be null
	 * @param audit_record The audit record for tracking this audit execution
	 * @param design_system The design system containing allowed image characteristics, must not be null
	 * @return A completed Audit object with content policy compliance results for image elements
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		assert page_state != null;
		
		//get all elements that are text containers
		List<ElementState> element_list = BrowserUtils.getTextElements(page_state.getElements());
		
		String why_it_matters = "";
		String description = "";

		Score image_policy_score = calculateImagePolicyViolationScore(element_list, design_system);
		
		Audit audit = new Audit(AuditCategory.CONTENT,
								AuditSubcategory.IMAGERY,
								AuditName.IMAGE_POLICY,
								image_policy_score.getPointsAchieved(),
								null,
								AuditLevel.PAGE,
								image_policy_score.getMaxPossiblePoints(),
								page_state.getUrl(),
								why_it_matters,
								description,
								false);
		
		audit = audit_service.save(audit);
		audit_service.addAllIssues(audit.getId(), image_policy_score.getIssueMessages());
		return audit;
	}


	/**
	 * Calculates policy compliance score for images based on content restrictions for
	 * adult content and violence defined in the design system.
	 *
	 * <p><strong>Preconditions:</strong></p>
	 * <ul>
	 *   <li>{@code element_list} must not be null</li>
	 *   <li>{@code design_system} must not be null</li>
	 *   <li>{@code design_system.getAllowedImageCharacteristics()} must return a valid collection</li>
	 *   <li>{@code issue_message_service} must be properly injected</li>
	 * </ul>
	 * 
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null Score object with calculated points and issue messages</li>
	 *   <li>All ImageElementState objects in the element list have been evaluated</li>
	 *   <li>Issue messages have been created and saved for each image element</li>
	 *   <li>Score points reflect compliance with design system image policies</li>
	 * </ul>
	 * 
	 * <p><strong>Invariants:</strong></p>
	 * <ul>
	 *   <li>Points earned cannot exceed max points possible</li>
	 *   <li>Each image element contributes exactly 1 point to max points</li>
	 *   <li>Compliant images earn 1 point, non-compliant images earn 0 points</li>
	 *   <li>All issue messages have appropriate priority levels (MEDIUM for violations, NONE for compliance)</li>
	 *   <li>Issue messages contain labels: "imagery" and "policy"</li>
	 * </ul>
	 * 
	 * <p><strong>Behavior:</strong></p>
	 * <ul>
	 *   <li>Iterates through all ElementState objects in the element list</li>
	 *   <li>Filters for ImageElementState objects only</li>
	 *   <li>For each image, checks isAdultContent() and isViolentContent() properties</li>
	 *   <li>Compares against design system's allowed image characteristics</li>
	 *   <li>Creates violation issues for adult content when "ADULT" not in allowed characteristics</li>
	 *   <li>Creates violation issues for violent content when "VIOLENCE" not in allowed characteristics</li>
	 *   <li>Creates compliance issues for images meeting policy requirements</li>
	 *   <li>Calculates total points earned and maximum possible points</li>
	 * </ul>
	 *
	 * @param element_list - list of element states to evaluate, must not be null
	 * @param design_system - design system containing image policy restrictions, must not be null
	 * @return Score object containing points earned, max points, and issue messages for image policy compliance
	 */
	private Score calculateImagePolicyViolationScore(List<ElementState> element_list,
													DesignSystem design_system) {
		int points_earned = 0;
		int max_points = 0;
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		Set<String> labels = new HashSet<>();
		labels.add("imagery");
		labels.add("policy");
		
		String ada_compliance = "There are no ADA compliance requirements for this category";
		
		for(ElementState element: element_list) {
			if(element instanceof ImageElementState) {
				ImageElementState img_element = (ImageElementState)element;
				if(img_element.isAdultContent() && !design_system.getAllowedImageCharacteristics().contains("ADULT")) {
					log.warn("Creating issue for image with adult content");
		
					//return new Score(1, 1, new HashSet<>());
					String recommendation = "Use an image without nudity";
					String title = "Nudity detected";
					String description = "Image contains nudity and/or adult content";

					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.MEDIUM,
																	description,
																	recommendation,
																	element,
																	AuditCategory.CONTENT,
																	labels,
																	ada_compliance,
																	title,
																	0,
																	1);
					
					issue_message = (ReadingComplexityIssueMessage) issue_message_service.save(issue_message);
					//issue_message_service.addElement(issue_message.getId(), element.getId());
					issue_messages.add(issue_message);
					
					points_earned += 0;
					max_points += 1;
				}
				else if(img_element.isViolentContent() && !design_system.getAllowedImageCharacteristics().contains("VIOLENCE")) {
					log.warn("Creating issue for image with violent content");
		
					//return new Score(1, 1, new HashSet<>());
					String recommendation = "Use an image without voilence";
					String title = "Violent imagery";
					String description = "Image contains violence";
					
					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.MEDIUM,
																	description,
																	recommendation,
																	element,
																	AuditCategory.CONTENT,
																	labels,
																	ada_compliance,
																	title,
																	0,
																	1);
					
					issue_message = (ReadingComplexityIssueMessage) issue_message_service.save(issue_message);
					//issue_message_service.addElement(issue_message.getId(), element.getId());
					issue_messages.add(issue_message);
					
					points_earned += 0;
					max_points += 1;
				}
				else {
					points_earned += 1;
					max_points += 1;
					String recommendation = "";
					String title = "Image complies with policy";
					String description = "This image complies with the domain policy. Well done!";
					
					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.NONE,
																	description,
																	recommendation,
																	element,
																	AuditCategory.CONTENT,
																	labels,
																	ada_compliance,
																	title,
																	1,
																	1);

					issue_message = (ReadingComplexityIssueMessage) issue_message_service.save(issue_message);
					//issue_message_service.addElement(issue_message.getId(), element.getId());
					issue_messages.add(issue_message);
				}
			}
		}
		
		return new Score(points_earned, max_points, issue_messages);
	}
}
