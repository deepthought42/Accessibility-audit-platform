package com.looksee.contentAudit.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.looksee.models.ElementState;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.interfaces.IExecutablePageStateAudit;
import com.looksee.models.audit.messages.ElementStateIssueMessage;
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.Priority;
import com.looksee.services.AuditService;
import com.looksee.services.UXIssueMessageService;

import lombok.NoArgsConstructor;

/**
 * Responsible for executing an accessibility audit on object and canvas
 * elements on a page to determine adherence to WCAG 2.1 compliance
 * requirements for alternative text and accessibility features.
 *
 * <p>This audit evaluates object and canvas elements to ensure they provide
 * accessible alternatives for users with disabilities. Elements are considered
 * compliant if they contain either alternative text content or link elements
 * that provide accessible navigation or description.</p>
 *
 * <p>The audit supports WCAG Level A compliance by ensuring that embedded
 * objects and canvas elements have proper accessibility features implemented.</p>
 *
 * WCAG Level - A
 * WCAG Success Criterion - https://www.w3.org/TR/UNDERSTANDING-WCAG20/meaning-supplements.html
 */
@Component
@NoArgsConstructor
public class ObjectAltTextAudit implements IExecutablePageStateAudit {
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	/**
	 * Executes an accessibility audit on object and canvas elements to ensure WCAG 2.1 compliance.
	 * 
	 * <p><strong>Preconditions:</strong></p>
	 * <ul>
	 *   <li>{@code page_state} must not be null</li>
	 *   <li>{@code page_state.getElements()} must return a valid collection of ElementState objects</li>
	 *   <li>{@code page_state.getUrl()} must return a valid URL string for Jsoup parsing context</li>
	 *   <li>{@code audit_service} and {@code issue_message_service} must be properly injected</li>
	 * </ul>
	 * 
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null Audit object with category CONTENT, subcategory IMAGERY, and name ALT_TEXT</li>
	 *   <li>All object and canvas elements from the page state have been evaluated for alternative text or link presence</li>
	 *   <li>Issue messages have been created and saved for each object/canvas element (compliance or violation)</li>
	 *   <li>The returned audit contains the total score calculated from all object/canvas elements</li>
	 *   <li>All issue messages are associated with the returned audit</li>
	 *   <li>Each object/canvas element has been parsed using Jsoup to extract HTML content</li>
	 * </ul>
	 * 
	 * <p><strong>Invariants:</strong></p>
	 * <ul>
	 *   <li>Points earned cannot exceed max points possible</li>
	 *   <li>Each object/canvas element generates exactly one issue message</li>
	 *   <li>All issue messages have appropriate priority levels (HIGH for violations, NONE for compliance)</li>
	 *   <li>Issue messages contain proper labels: "alt_text" and "wcag"</li>
	 *   <li>Violation issues have 0 points earned, compliance issues have 1 point earned</li>
	 *   <li>Each object/canvas element contributes exactly 1 point to the maximum possible score</li>
	 * </ul>
	 * 
	 * <p><strong>Behavior:</strong></p>
	 * <ul>
	 *   <li>Filters page elements to find only object and canvas elements</li>
	 *   <li>For each object/canvas element, parses its HTML content using Jsoup.parseBodyFragment()</li>
	 *   <li>Checks for presence of alternative text content via element.getAllText()</li>
	 *   <li>Checks for presence of link elements within the parsed HTML content</li>
	 *   <li>Creates violation issues for object/canvas elements without alternative text AND without link elements</li>
	 *   <li>Creates compliance issues for object/canvas elements with either alternative text OR link elements</li>
	 *   <li>Calculates overall accessibility score based on compliance rate</li>
	 *   <li>Persists all audit data and issue messages to the database</li>
	 * </ul>
	 * 
	 * @param page_state The page state containing elements to audit, must not be null
	 * @param audit_record The audit record for tracking this audit execution
	 * @param design_system The design system context (unused in this implementation)
	 * @return A completed Audit object with accessibility compliance results for object and canvas elements
	 * @throws NullPointerException if {@code page_state} is null
	 */
	@Override
	public Audit execute(PageState page_state,
						AuditRecord audit_record,
						DesignSystem design_system) {
		// Preconditions
		Objects.requireNonNull(page_state, "page_state must not be null");
		
		Set<UXIssueMessage> issue_messages = new HashSet<>();

		Set<String> labels = new HashSet<>();
		labels.add("alt_text");
		labels.add("wcag");
		
		// tags not covered = applet, iframe, svg, canvas, video, audio and figure
		List<ElementState> input_elements = new ArrayList<>();
		for(ElementState element : page_state.getElements()) {
			if("object".equalsIgnoreCase(element.getName())) {
				input_elements.add(element);
			}
			else if("canvas".equalsIgnoreCase(element.getName())) {
				input_elements.add(element);
			}
		}
		
		String why_it_matters = "Giving names to input controls helps with both SEO and accessibility.";
		
		String ada_compliance = "Your website does not meet the level A ADA compliance requirement for" + 
				" 'Alt' text on input controls present on the website.";

		//score each link element
		for(ElementState input_element : input_elements) {
			Document jsoup_doc = Jsoup.parseBodyFragment(input_element.getAllText(), page_state.getUrl());
			Element link_element = jsoup_doc.getElementsByTag("a").first();

			if(input_element.getAllText().isEmpty() && link_element == null){
				String title = input_element.getAllText()+" tag does not have alt text or link defined";
				String description = input_element.getAllText()+" alternative text value is empty";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
					Priority.HIGH,
					description, 
					input_element.getName()+" element should have alternative text defined",
					null,
					AuditCategory.CONTENT,
					labels,
					ada_compliance,
					title,
					0,
					1);

				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), input_element.getId());
				issue_messages.add(issue_message);
			}
			else{
				String title = input_element.getAllText()+" has alternative text or link included!";
				String description = "Well done! By providing an alternative text description or link, you are providing a more inclusive experience";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.NONE,
																description,
																"",
																null,
																AuditCategory.CONTENT,
																labels,
																ada_compliance,
																title,
																1,
																1);

				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), input_element.getId());
				issue_messages.add(issue_message);
			}
		}
		
		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			points_earned += issue_msg.getPoints();
			max_points += issue_msg.getMaxPoints();
		}

		// Invariant: points earned cannot exceed max points
		assert points_earned <= max_points : "points_earned (" + points_earned + ") exceeds max_points (" + max_points + ")";

		String description = "Inputs and other controls should have alternative text defined as a non empty string value";

		Audit audit = new Audit(AuditCategory.CONTENT,
								AuditSubcategory.IMAGERY,
								AuditName.ALT_TEXT,
								points_earned,
								null,
								AuditLevel.PAGE,
								max_points,
								page_state.getUrl(),
								why_it_matters,
								description,
								true);

		audit = audit_service.save(audit);
		audit_service.addAllIssues(audit.getId(), issue_messages);

		// Postcondition: audit must be non-null and persisted
		assert audit != null : "audit must not be null after save";

		return audit;
	}
}
