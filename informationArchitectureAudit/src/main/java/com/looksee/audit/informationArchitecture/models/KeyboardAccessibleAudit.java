package com.looksee.audit.informationArchitecture.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
import com.looksee.services.PageStateService;

/**
 * Audits keyboard accessibility for WCAG 2.1 Section 2.1.1 compliance, checking that
 * interactive elements are focusable and operable via keyboard.
 *
 * <p><b>Class invariant:</b> All {@code @Autowired} dependencies ({@code auditService},
 * {@code pageStateService}) are non-null after Spring construction.</p>
 */
@Component
public class KeyboardAccessibleAudit implements IExecutablePageStateAudit {

	@Autowired
	private AuditService auditService;

    @Autowired
    private PageStateService pageStateService;

	List<String> bad_link_text_list;
	
	public KeyboardAccessibleAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Scores links on a page based on if the link has an href value present, the url format is valid and the
	 *   url goes to a location that doesn't produce a 4xx error
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		Objects.requireNonNull(page_state, "page_state must not be null");
		Objects.requireNonNull(audit_record, "audit_record must not be null");

		//check if page state already had a link audit performed.
		Set<UXIssueMessage> issue_messages = new HashSet<>();
		String ada_compliance = "WCAG 2.1 Section 1.3.1 - Tables";

		Set<String> labels = new HashSet<>();
		labels.add("information architecture");
		labels.add("accessibility");
		labels.add("wcag");
		
        List<ElementState> allElements = pageStateService.getElementStates(page_state.getId());
        List<ElementState> elements = checkKeyboardAccessibility(allElements);

        for(ElementState element: elements){
            String description = "Element cannot be navigated to or interacted with using only a keyboard. This can prevent users who rely on keyboard navigation from accessing important content or functionality.";
            String recommendation = "Ensure the element is focusable with tabindex=\"0\" and can be activated using the keyboard by assigning an appropriate role (e.g., role=\"button\") and handling key events like Enter or Space.";
            String title = "Element not keyboard accessible";

            UXIssueMessage issue_msg = new ElementStateIssueMessage(Priority.HIGH,
                                                                description,
                                                                recommendation,
                                                                element,
                                                                AuditCategory.ACCESSIBILITY,
                                                                labels,
                                                                ada_compliance,
                                                                title,
                                                                0,
                                                                1);
            issue_messages.add(issue_msg);
        }

		String why_it_matters = "Grouping form controls within a <fieldset> element is important for accessibility because it provides a clear, semantic structure that enhances the understanding of the form's organization, especially for users with disabilities. The <fieldset> element, often paired with a <legend>, helps screen readers and other assistive technologies to convey related groups of controls as a single, coherent unit, ensuring that users can navigate and comprehend the form's layout more effectively. This practice aligns with WCAG 2.1 guidelines, supporting a more inclusive and accessible web experience.";
		
		Set<String> categories = new HashSet<>();
		categories.add(AuditCategory.INFORMATION_ARCHITECTURE.getShortName());
		
		String description = "Making sure your links are setup correctly is incredibly important";
		
		int points_earned = 0;
		int max_points = 0;
		for(UXIssueMessage issue_msg : issue_messages) {
			points_earned += issue_msg.getPoints();
			max_points += issue_msg.getMaxPoints();
		}
		
		Audit audit = new Audit(AuditCategory.INFORMATION_ARCHITECTURE,
								AuditSubcategory.NAVIGATION,
								AuditName.LINKS,
								points_earned,
								issue_messages,
								AuditLevel.PAGE,
								max_points,
								page_state.getUrl(),
								why_it_matters,
								description,
								true);

		Objects.requireNonNull(audit, "Postcondition failed: audit must not be null");
		return auditService.save(audit);
	}

    
	/**
     * Checks the entire HTML page for keyboard accessibility compliance with WCAG 2.1 Section 2.1.1.
     * The method will return a list of elements that might not be operable through a keyboard interface.
     *
     * Precondition: The WebDriver must be initialized and the page must be fully loaded.
     * Postcondition: The returned list will contain WebElements that are not keyboard accessible or will be empty if all elements are accessible.
     *
     * @return List of non-keyboard accessible elements as WebElements.
     */
    public static List<ElementState> checkKeyboardAccessibility(List<ElementState> elements) {
        Objects.requireNonNull(elements, "elements must not be null");
        
        List<ElementState> nonAccessibleElements = new ArrayList<>();

        // Loop through all elements and check if they can be focused
        for (ElementState element : elements) {
            String tabindex = element.getAttribute("tabindex");
            String role = element.getAttribute("role");
            
            // Check if element is keyboard accessible
            boolean isAccessible = (tabindex != null && !tabindex.isEmpty()) ||
								(role != null && !role.isEmpty());
            
            if (!isAccessible) {
                nonAccessibleElements.add(element);
            }
        }

        Objects.requireNonNull(nonAccessibleElements, "Postcondition failed: returned list must not be null");
        return nonAccessibleElements;
    }
}
