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
 * Responsible for executing an accessibility audit on figure elements on a page
 * to ensure WCAG 2.1 compliance by checking for the presence of a figcaption element.
 *
 * <p>This audit evaluates figure elements to ensure they provide accessible
 * alternatives for users with disabilities. Elements are considered compliant
 * if they contain a figcaption element that provides a textual description of the content.
 *
 * <p>The audit supports WCAG Level A compliance by ensuring that figure elements
 * comply with the WCAG 2.1 success criterion 1.1.1.</p>
 *
 * WCAG Level - A
 * WCAG Success Criterion - https://www.w3.org/TR/UNDERSTANDING-WCAG20/meaning-supplements.html
 */
@Component
@NoArgsConstructor
public class FigureAltTextAudit implements IExecutablePageStateAudit {
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	/**
	 * Executes an accessibility audit on figure elements to ensure WCAG 2.1 compliance.
	 *
	 * <p><strong>Preconditions:</strong></p>
	 * <ul>
	 *   <li>{@code page_state} must not be null</li>
	 *   <li>{@code page_state.getElements()} must return a valid collection of ElementState objects</li>
	 *   <li>{@code audit_service} and {@code issue_message_service} must be properly injected</li>
	 * </ul>
	 *
	 * <p><strong>Postconditions:</strong></p>
	 * <ul>
	 *   <li>Returns a non-null Audit object with category CONTENT, subcategory IMAGERY, and name ALT_TEXT</li>
	 *   <li>All figure elements from the page state have been evaluated for figcaption presence</li>
	 *   <li>Issue messages have been created and saved for each figure element (compliance or violation)</li>
	 *   <li>The returned audit contains the total score calculated from all figure elements</li>
	 *   <li>All issue messages are associated with the returned audit</li>
	 * </ul>
	 *
	 * <p><strong>Invariants:</strong></p>
	 * <ul>
	 *   <li>Points earned cannot exceed max points possible</li>
	 *   <li>Each figure element generates exactly one issue message</li>
	 *   <li>All issue messages have appropriate priority levels (HIGH for violations, NONE for compliance)</li>
	 * </ul>
	 *
	 * <p><strong>Behavior:</strong></p>
	 * <ul>
	 *   <li>Filters page elements to find only figure elements</li>
	 *   <li>For each figure element, parses its HTML content and searches for figcaption tags</li>
	 *   <li>Creates violation issues for figures without figcaption or with empty figcaption text</li>
	 *   <li>Creates compliance issues for figures with proper figcaption content</li>
	 *   <li>Calculates overall accessibility score based on compliance rate</li>
	 *   <li>Persists all audit data and issue messages to the database</li>
	 * </ul>
	 *
	 * @param page_state The page state containing elements to audit, must not be null
	 * @param audit_record The audit record for tracking this audit execution
	 * @param design_system The design system context (unused in this implementation)
	 * @return A completed Audit object with accessibility compliance results for figure elements
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
		
		List<ElementState> element_states = new ArrayList<>();
		for(ElementState element : page_state.getElements()) {
			if("figure".equalsIgnoreCase(element.getName())) {
				element_states.add(element);
			}
		}
		
		String why_it_matters = "Ensuring Figure elements have <figcaption> tag ensures that all users understand the purpose of figure elements on your site.";
		String ada_compliance = "Your website does not meet the level A ADA compliance requirement for" +
				" 'Alt' text within figure elements.";

		//score each link element
		for(ElementState figure_element : element_states) {
			Document jsoup_doc = Jsoup.parseBodyFragment(figure_element.getAllText(), page_state.getUrl());
			Element caption_element = jsoup_doc.getElementsByTag("figcaption").first();

			if(caption_element == null || caption_element.text().isEmpty()){
				String title = "figure does not have <figcaption> element";
				String description = "figure does not have <figcaption> element";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
					Priority.HIGH,
					description, 
					"figure tag should have a <figcaption> element defined within the SVG",
					null,
					AuditCategory.CONTENT,
					labels,
					ada_compliance,
					title,
					0,
					1);

				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), figure_element.getId());
				issue_messages.add(issue_message);
			}
			else{
				String title = "figure has <figcaption> included!";
				String description = "Well done! This figure is considered accessible according to WCAG 2.1 section 1.1.1";
				
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
				issue_message_service.addElement(issue_message.getId(), figure_element.getId());
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

		String description = "figure tags should have <figcaption> elements and they should not be empty";

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
