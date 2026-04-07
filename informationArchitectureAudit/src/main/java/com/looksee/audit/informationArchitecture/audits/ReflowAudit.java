package com.looksee.audit.informationArchitecture.audits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.looksee.models.audit.messages.UXIssueMessage;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import com.looksee.services.AuditService;
import com.looksee.services.ElementStateService;
import com.looksee.services.PageStateService;

/**
 * Audits page elements for WCAG 2.1 Section 1.4.10 compliance, checking that content
 * reflows properly without requiring horizontal scrolling.
 *
 * <p><b>Class invariant:</b> All {@code @Autowired} dependencies ({@code auditService},
 * {@code elementStateService}, {@code pageStateService}) are non-null after Spring construction.</p>
 */
@Component
public class ReflowAudit implements IExecutablePageStateAudit {
    private static final Logger log = LoggerFactory.getLogger(ReflowAudit.class);


	@Autowired
	private AuditService auditService;

    @Autowired
    private ElementStateService elementStateService;

    @Autowired
    private PageStateService pageStateService;

	List<String> bad_link_text_list;
	
	public ReflowAudit() {
		//super(buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.LINKS);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Evaluates page elements for reflow compliance, identifying fixed-width elements
	 * that may cause horizontal scrolling.
	 *
	 * @pre {@code page_state != null}
	 * @pre {@code audit_record != null}
	 * @post returned {@code Audit} is non-null and persisted
	 */
	@Override
	public Audit execute(PageState page_state, AuditRecord audit_record, DesignSystem design_system) {
		Objects.requireNonNull(page_state, "page_state must not be null");
		Objects.requireNonNull(audit_record, "audit_record must not be null");

		//check if page state already had a link audit performed.
		Set<UXIssueMessage> issue_messages = new HashSet<>();

		Set<String> labels = new HashSet<>();
		labels.add("information architecture");
		labels.add("accessibility");
		labels.add("tables");
		labels.add("wcag");
		
        issue_messages.addAll(checkForCompliance(page_state));
        
        

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
                                AuditName.REFLOW,
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

    private List<UXIssueMessage> checkForCompliance(PageState page_state) {
        List<UXIssueMessage> issues = new ArrayList<>();
        List<ElementState> elements = pageStateService.getElementStates(page_state.getId());
        for(ElementState element: elements){
            issues.add(checkElementForReflowCompliance(element));
        }

        return issues;
    }

    /**
     * Checks an individual element for reflow compliance.
     *
     * @pre {@code element != null}
     * @post returned {@code UXIssueMessage} is non-null
     */
    private UXIssueMessage checkElementForReflowCompliance(ElementState element) {
        Objects.requireNonNull(element, "Precondition failed: element must not be null");

        // Check for fixed-width elements that could cause reflow issues
        String width = element.getRenderedCssValues().get("width");
        String overflow = element.getRenderedCssValues().get("overflow");

        if (isFixedWidth(width) && !"visible".equals(overflow)) {
            log.info("{}", "Potential reflow issue found in element: " + element.getName() + " with CSS Selector: " + element.getCssSelector());

            String description = "Web content doesn't properly adjust to fit within the viewport, causing users to scroll horizontally or lose access to information. This often happens on small screens or when zoomed in, making it difficult for users to read or interact with the content effectively.";
            String wcag_compliance = "WCAG 2.1 Section 1.4.10 - Reflow";
            String why_it_matters = "Reflow matters because it ensures that all users, including those with low vision or those accessing content on smaller screens, can easily read and interact with web content without excessive horizontal scrolling. This improves accessibility by providing a more seamless and user-friendly experience, allowing everyone to access information without barriers.";
            String title = "Web content doesn't adjust to fit the viewport";
            String recommendation = "Ensure that your web content is designed to be responsive by using flexible layouts, relative units like percentages for widths, and CSS media queries. Avoid fixed-width elements and make sure that all content, including text, images, and interactive elements, can adapt to fit within the viewport without requiring horizontal scrolling.";
    
            return new UXIssueMessage(Priority.MEDIUM,
                                    description,
                                    ObservationType.REFLOW,
                                    AuditCategory.ACCESSIBILITY,
                                    wcag_compliance,
                                    new HashSet<>(),
                                    why_it_matters, 
                                    title, 
                                    0, 
                                    0, 
                                    recommendation);
    
        }

        Objects.requireNonNull(element.getName(), "Postcondition failed: element tag name should not be null");

        String description = "Web content adjusts to fit the viewport";
        String wcag_compliance = "WCAG 2.1 Section 1.4.10 - Reflow";
        String why_it_matters = "Reflow matters because it ensures that all users, including those with low vision or those accessing content on smaller screens, can easily read and interact with web content without excessive horizontal scrolling. This improves accessibility by providing a more seamless and user-friendly experience, allowing everyone to access information without barriers.";
        String title = "Web content adjusts to fit the viewport";
        String recommendation = "Ensure that your web content is designed to be responsive by using flexible layouts, relative units like percentages for widths, and CSS media queries. Avoid fixed-width elements and make sure that all content, including text, images, and interactive elements, can adapt to fit within the viewport without requiring horizontal scrolling.";

        return new UXIssueMessage(Priority.MEDIUM,
                                description,
                                ObservationType.REFLOW,
                                AuditCategory.ACCESSIBILITY,
                                wcag_compliance,
                                new HashSet<>(),
                                why_it_matters,
                                title,
                                0,
                                0,
                                recommendation);
    }

    /**
     * Determines if the CSS width value represents a fixed width.
     *
     * @pre {@code width != null && !width.isEmpty()}
     * @post returns {@code true} if width ends with "px" or "%"
     */
    private boolean isFixedWidth(String width) {
        Objects.requireNonNull(width, "width must not be null");
        if (width.isEmpty()) {
            throw new IllegalArgumentException("width must not be empty");
        }
        return width.endsWith("px") || width.endsWith("%");
    }
}
