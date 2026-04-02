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
 * Responsible for executing an accessibility audit on area, input, and embed
 * elements on a page to ensure WCAG 2.1 compliance by checking for the
 * presence of an alt attribute.
 *
 * <p>This audit evaluates area, input, and embed elements to ensure they
 * provide accessible alternatives for users with disabilities. Elements are
 * considered compliant if they contain an alt attribute that provides a
 * textual description of the content.
 *
 * <p>The audit supports WCAG Level A compliance by ensuring that area, input, and embed
 * elements comply with the WCAG 2.1 success criterion 1.1.1.</p>
 *
 * WCAG Level - A
 * WCAG Success Criterion - https://www.w3.org/TR/UNDERSTANDING-WCAG20/meaning-supplements.html
 */
@Component
@NoArgsConstructor
public class ImageAltTextAudit implements IExecutablePageStateAudit {
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private UXIssueMessageService issue_message_service;
	
	/**
	 * Executes an accessibility audit on area, input, and embed elements to ensure WCAG 2.1 compliance for alt text.
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
	 *   <li>All area, input, and embed elements from the page state have been evaluated for alt attribute presence and content</li>
	 *   <li>Issue messages have been created and saved for each element (compliance or violation)</li>
	 *   <li>The returned audit contains the total score calculated from all evaluated elements</li>
	 *   <li>All issue messages are associated with the returned audit</li>
	 * </ul>
	 * 
	 * <p><strong>Invariants:</strong></p>
	 * <ul>
	 *   <li>Points earned cannot exceed max points possible</li>
	 *   <li>Each element generates exactly one issue message</li>
	 *   <li>All issue messages have appropriate priority levels (HIGH for violations, NONE for compliance)</li>
	 * </ul>
	 * 
	 * <p><strong>Behavior:</strong></p>
	 * <ul>
	 *   <li>Filters page elements to find only area, input, and embed elements</li>
	 *   <li>For each element, parses its HTML content using Jsoup and searches for alt attribute</li>
	 *   <li>Creates violation issues for elements without alt attribute or with empty alt attribute value</li>
	 *   <li>Creates compliance issues for elements with proper alt attribute content</li>
	 *   <li>Calculates overall accessibility score based on compliance rate</li>
	 *   <li>Persists all audit data and issue messages to the database</li>
	 * </ul>
	 * 
	 * @param page_state The page state containing elements to audit, must not be null
	 * @param audit_record The audit record for tracking this audit execution
	 * @param design_system The design system context (unused in this implementation)
	 * @return A completed Audit object with accessibility compliance results for area, input, and embed elements
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

		// tags not covered = Object, applet, iframe, svg, canvas, video, audio and figure
		List<ElementState> alt_text_elements = new ArrayList<>();
		for(ElementState element : page_state.getElements()) {
			if("area".equalsIgnoreCase(element.getName())) {
				alt_text_elements.add(element);
			}
			else if("input".equalsIgnoreCase(element.getName())) {
				alt_text_elements.add(element);
			}
			else if("embed".equalsIgnoreCase(element.getName())) {
				alt_text_elements.add(element);
			}
		}
		
		String why_it_matters = "Alt-text helps with both SEO and accessibility. Search engines use alt-text"
				+ " to help determine how usable and your site is as a way of ranking your site.";
		
		String ada_compliance = "Your website does not meet the level A ADA compliance requirement for" +
				" 'Alt' text for images present on the website.";

		//score each link element
		for(ElementState alt_element : alt_text_elements) {
			Document jsoup_doc = Jsoup.parseBodyFragment(alt_element.getOuterHtml(), page_state.getUrl());
			Element element = jsoup_doc.getElementsByTag(alt_element.getName()).first();
			
			//Check if element has "alt" attribute present
			if(element.hasAttr("alt")) {
				if(element.attr("alt").isEmpty()) {
					String title = "Alternative text value is empty";
					String description = "Alternative text value is empty";
					
					ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																	Priority.HIGH, 
																	description, 
																	alt_element.getName()+" tag should have alternative text defined",
																	null,
																	AuditCategory.CONTENT,
																	labels,
																	ada_compliance,
																	title,
																	0,
																	1);
					
					issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
					issue_message_service.addElement(issue_message.getId(), alt_element.getId());
					issue_messages.add(issue_message);
				}
				else {
					String title = "Image has alt text value set!";
					String description = "Well done! By providing an alternative text value, you are providing a more inclusive experience";
					
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
					issue_message_service.addElement(issue_message.getId(), alt_element.getId());
					issue_messages.add(issue_message);
				}
			}
			else {
				String title= "Images without alternative text attribute";
				String description = "Images without alternative text attribute";
				
				ElementStateIssueMessage issue_message = new ElementStateIssueMessage(
																Priority.HIGH, 
																description, 
																alt_element.getName()+" tag should have 'alt' text attribute defined",
																null,
																AuditCategory.CONTENT, 
																labels,
																ada_compliance,
																title,
																0,
																1);
				
				issue_message = (ElementStateIssueMessage) issue_message_service.save(issue_message);
				issue_message_service.addElement(issue_message.getId(), alt_element.getId());
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

		String description = "Images without alternative text defined as a non empty string value";

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
