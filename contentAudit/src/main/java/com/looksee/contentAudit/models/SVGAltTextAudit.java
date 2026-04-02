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
 * Responsible for executing an accessibility audit on SVG elements on a page
 * to ensure WCAG 2.1 compliance by checking for the presence of &lt;title&gt;
 * and &lt;desc&gt; elements within SVG tags.
 *
 * <p>This audit evaluates SVG elements to ensure they provide accessible
 * alternatives for users with disabilities. Elements are considered compliant
 * if they contain either a title element that provides a textual description
 * of the content or a desc element that provides a textual description of the
 * content.</p>
 *
 * <p>The audit supports WCAG Level A compliance by ensuring that SVG elements
 * comply with the WCAG 2.1 success criterion 1.1.1.</p>
 *
 * WCAG Level - A
 * WCAG Success Criterion - https://www.w3.org/TR/UNDERSTANDING-WCAG20/meaning-supplements.html
 */
@Component
@NoArgsConstructor
public class SVGAltTextAudit implements IExecutablePageStateAudit {
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	/**
	 * Executes an accessibility audit on SVG elements to ensure WCAG 2.1 compliance for alternative text.
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
	 *   <li>All SVG elements from the page state have been evaluated for accessibility compliance</li>
	 *   <li>Issue messages have been created and saved for each SVG element (compliance or violation)</li>
	 *   <li>The returned audit contains the total score calculated from all SVG elements</li>
	 *   <li>All issue messages are associated with the returned audit</li>
	 *   <li>Each SVG element has been parsed using Jsoup to extract HTML content</li>
	 * </ul>
	 * 
	 * <p><strong>Invariants:</strong></p>
	 * <ul>
	 *   <li>Points earned cannot exceed max points possible</li>
	 *   <li>Each SVG element generates exactly two issue messages (one for title element, one for desc element)</li>
	 *   <li>All issue messages have appropriate priority levels (HIGH for violations, NONE for compliance)</li>
	 *   <li>Issue messages contain proper labels: "alt_text" and "wcag"</li>
	 *   <li>Violation issues have 0 points earned, compliance issues have 1 point earned</li>
	 *   <li>Each SVG element contributes exactly 2 points to the maximum possible score (1 for title, 1 for desc)</li>
	 * </ul>
	 * 
	 * <p><strong>Behavior:</strong></p>
	 * <ul>
	 *   <li>Filters page elements to find only SVG elements</li>
	 *   <li>For each SVG element, parses its HTML content using Jsoup.parseBodyFragment()</li>
	 *   <li>Checks for presence of &lt;title&gt; elements using getElementsByTag("title").first()</li>
	 *   <li>Checks for presence of &lt;desc&gt; elements using getElementsByTag("desc").first()</li>
	 *   <li>Creates violation issues for SVG elements missing title elements or with empty title content</li>
	 *   <li>Creates violation issues for SVG elements missing desc elements or with empty desc content</li>
	 *   <li>Creates compliance issues for SVG elements with proper title and desc elements</li>
	 *   <li>Calculates overall accessibility score based on compliance rate</li>
	 *   <li>Persists all audit data and issue messages to the database</li>
	 * </ul>
	 * 
	 * @param page_state The page state containing elements to audit, must not be null
	 * @param audit_record The audit record for tracking this audit execution
	 * @param design_system The design system context (unused in this implementation)
	 * @return A completed Audit object with accessibility compliance results for SVG elements
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
		
		// tags not covered = canvas, video, audio and figure
		List<ElementState> element_states = new ArrayList<>();
		for(ElementState element : page_state.getElements()) {
			if("svg".equalsIgnoreCase(element.getName())) {
				element_states.add(element);
			}
		}
		
		String why_it_matters = "Ensuring SVG elements have <title> and <desc> tags ensures that all users understand the purpose of SVG elements on your site.";
		String ada_compliance = "Your website does not meet the level A ADA compliance requirement for" +
				" 'Alt' text within SVG elements.";

		//score each link element
		for(ElementState svg_element : element_states) {
			Document jsoup_doc = Jsoup.parseBodyFragment(svg_element.getAllText(), page_state.getUrl());
			Element title_element = jsoup_doc.getElementsByTag("title").first();
			Element description_element = jsoup_doc.getElementsByTag("desc").first();

			if(title_element == null || title_element.text().isEmpty()){
				String title = "SVG does not have title element";
				String description = "SVG does not have title element";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
					Priority.HIGH,
					description,
					"SVG tag should have a <title> element defined within the SVG",
					null,
					AuditCategory.CONTENT,
					labels,
					ada_compliance,
					title,
					0,
					1);

				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), svg_element.getId());
				issue_messages.add(issue_message);
			}
			else{
				String title = "SVG has title included!";
				String description = "Well done! This SVG is considered accessible according to WCAG 2.1 section 1.1.1";
				
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
				issue_message_service.addElement(issue_message.getId(), svg_element.getId());
				issue_messages.add(issue_message);
			}

			if(description_element == null || description_element.text().isEmpty()){
				String title = "SVG does not have <desc> element";
				String description = "SVG does not have <desc> element";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
					Priority.HIGH,
					description, 
					"SVG tag should have a <desc> element defined within the SVG",
					null,
					AuditCategory.CONTENT,
					labels,
					ada_compliance,
					title,
					0,
					1);

				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), svg_element.getId());
				issue_messages.add(issue_message);
			}
			else{
				String title = "SVG has description included!";
				String description = "Well done! This SVG is considered accessible according to WCAG 2.1 section 1.1.1";
				
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
				issue_message_service.addElement(issue_message.getId(), svg_element.getId());
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

		String description = "SVG tags should have <title> and <desc> elements and they should not be empty";

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
