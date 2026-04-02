package com.looksee.visualDesignAudit.audit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.models.ImageElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.Score;
import com.looksee.models.audit.interfaces.IExecutablePageStateAudit;
import com.looksee.models.audit.messages.StockImageIssueMessage;
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
 * Responsible for executing an audit on the images on a page to detect
 * potential copyright infringement by identifying images that appear on
 * other websites, indicating they may be stock images or unlicensed content.
 *
 * <p>This audit evaluates images to ensure they are unique to the site and
 * not stock images or unlicensed content. Images that are flagged as appearing
 * on other websites are considered potential copyright violations and receive
 * 0 points, while unique images receive 1 point.
 *
 * <p>The audit supports WCAG Level A compliance by ensuring that images are
 * unique to the site and not stock images or unlicensed content to ensure
 * accessibility compliance with WCAG 2.1 success criterion 1.1.1.
 *
 * WCAG Level - A
 * WCAG Success Criterion - https://www.w3.org/TR/UNDERSTANDING-WCAG20/meaning-supplements.html
 */
@Component
@NoArgsConstructor
public class ImageAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ImageAudit.class);
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	/**
	 * Executes a copyright audit on images to detect potential copyright
	 * infringement by identifying images that appear on other websites,
	 * indicating they may be stock images or unlicensed content.
	 * 
	 * <p><strong>Preconditions:</strong></p>
	 * <ul>
	 *   <li>{@code page_state} must not be null</li>
	 *   <li>{@code page_state.getElements()} must return a valid collection of ElementState objects</li>
	 *   <li>{@code audit_service} and {@code issue_message_service} must be properly injected</li>
	 *   <li>{@code BrowserUtils.getImageElements()} must return a list of image elements from the page</li>
	 * </ul>
	 * 
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null Audit object with category CONTENT, subcategory IMAGERY, and name IMAGE_COPYRIGHT</li>
	 *   <li>All image elements from the page state have been evaluated for copyright compliance</li>
	 *   <li>Issue messages have been created and saved for each image element (compliance or violation)</li>
	 *   <li>The returned audit contains the total score calculated from all image elements</li>
	 *   <li>All issue messages are associated with the returned audit</li>
	 *   <li>Each image element has been checked for the {@code isImageFlagged()} property</li>
	 * </ul>
	 * 
	 * <p><strong>Invariants:</strong></p>
	 * <ul>
	 *   <li>Points earned cannot exceed max points possible</li>
	 *   <li>Each image element generates exactly one issue message</li>
	 *   <li>All issue messages have appropriate priority levels (MEDIUM for violations, NONE for compliance)</li>
	 *   <li>Issue messages contain proper labels: "imagery" and "copyright"</li>
	 *   <li>Violation issues have 0 points earned, compliance issues have 1 point earned</li>
	 *   <li>Each image element contributes exactly 1 point to the maximum possible score</li>
	 * </ul>
	 * 
	 * <p><strong>Behavior:</strong></p>
	 * <ul>
	 *   <li>Filters page elements to find only image elements using {@code BrowserUtils.getImageElements()}</li>
	 *   <li>For each image element, checks the {@code isImageFlagged()} property to determine if it appears on other websites</li>
	 *   <li>Creates violation issues for images that are flagged (found on other websites) with copyright warnings</li>
	 *   <li>Creates compliance issues for images that are unique to the site</li>
	 *   <li>Calculates overall copyright compliance score based on uniqueness rate</li>
	 *   <li>Persists all audit data and issue messages to the database</li>
	 *   <li>Returns a completed Audit object with copyright compliance results</li>
	 * </ul>
	 * 
	 * @param page_state The page state containing elements to audit, must not be null
	 * @param audit_record The audit record for tracking this audit execution
	 * @param design_system The design system context (unused in this implementation)
	 * @return A completed Audit object with copyright compliance results for image elements
	 */
	@Override
	public Audit execute(PageState page_state,
						AuditRecord audit_record,
						DesignSystem design_system) {
		assert page_state != null;
		
		//get all elements that are text containers
		List<ImageElementState> element_list = BrowserUtils.getImageElements(page_state.getElements());
		
		Score copyright_score = calculateCopyrightScore(element_list);
		String why_it_matters = "";
		String description = "";

		Audit audit = new Audit(AuditCategory.CONTENT,
								AuditSubcategory.IMAGERY,
								AuditName.IMAGE_COPYRIGHT,
								copyright_score.getPointsAchieved(),
								null,
								AuditLevel.PAGE,
								copyright_score.getMaxPossiblePoints(),
								page_state.getUrl(),
								why_it_matters,
								description,
								false);
						
		audit = audit_service.save(audit);
		audit_service.addAllIssues(audit.getId(), copyright_score.getIssueMessages());
		return audit;
	}


	/**
	 * Reviews images for potential copyright infringement / lack of uniqueness by checking if other sites have 
	 * the exact same image. Images that are flagged as appearing on other websites are considered potential
	 * copyright violations and receive 0 points, while unique images receive 1 point.
	 * 
	 * <p><strong>Preconditions:</strong></p>
	 * <ul>
	 *   <li>{@code elements} must not be null</li>
	 *   <li>All elements in the list must be valid ImageElementState objects</li>
	 *   <li>{@code issue_message_service} must be properly injected</li>
	 * </ul>
	 * 
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null Score object with calculated points and issue messages</li>
	 *   <li>Each image element has been evaluated for the {@code isImageFlagged()} property</li>
	 *   <li>Issue messages have been created and saved for each image element</li>
	 *   <li>Points earned equals the number of unique images (not flagged)</li>
	 *   <li>Max points equals the total number of images evaluated</li>
	 * </ul>
	 * 
	 * <p><strong>Invariants:</strong></p>
	 * <ul>
	 *   <li>Points earned cannot exceed max points possible</li>
	 *   <li>Each image element generates exactly one issue message</li>
	 *   <li>Flagged images receive 0 points, unique images receive 1 point</li>
	 *   <li>All issue messages have appropriate priority levels (MEDIUM for violations, NONE for compliance)</li>
	 * </ul>
	 * 
	 * @param elements List of ImageElementState objects to evaluate for copyright compliance, must not be null
	 * @return Score object containing points earned, max possible points, and associated issue messages
	 */
	public Score calculateCopyrightScore(List<ImageElementState> elements) {
		int points_earned = 0;
		int max_points = 0;
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		Set<String> labels = new HashSet<>();
		labels.add("imagery");
		labels.add("copyright");
		
		String ada_compliance = "There are no ADA compliance requirements for this category";
		
		for(ImageElementState element: elements) {
			if(element.isImageFlagged()) {
				log.warn("Creating UX issue for image was for copyright");
	
				//return new Score(1, 1, new HashSet<>());
				String recommendation = "This image was found on another website. You should validate that you have paid to license this image.";
				String title = "Image was found on another website";
				String description = "Image was found on another website";
				
				StockImageIssueMessage issue_message = new StockImageIssueMessage(
																Priority.MEDIUM,
																description,
																recommendation,
																element,
																AuditCategory.CONTENT,
																labels,
																ada_compliance,
																title,
																0,
																1,
																true);
				
				issue_message = (StockImageIssueMessage) issue_message_service.save(issue_message);
				//issue_message_service.addElement(issue_message.getId(), element.getId());
				issue_messages.add(issue_message);
				
				points_earned += 0;
				max_points += 1;
			}
			else {
				points_earned += 1;
				max_points += 1;
				String recommendation = "";
				String title = "Image is unique";
				String description = "This image is unique to your site. Well done!";
				
				StockImageIssueMessage issue_message = new StockImageIssueMessage(
																Priority.NONE,
																description,
																recommendation,
																element,
																AuditCategory.CONTENT,
																labels,
																ada_compliance,
																title,
																1,
																1,
																false);

				issue_message = (StockImageIssueMessage) issue_message_service.save(issue_message);
				//issue_message_service.addElement(issue_message.getId(), element.getId());
				issue_messages.add(issue_message);
			}
		}
		return new Score(points_earned, max_points, issue_messages);
	}
}
